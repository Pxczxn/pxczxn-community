package top.pxczxn.community.user.application;

import cn.hutool.core.lang.Validator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.user.model.CommunityUserLoginAccount;
import top.pxczxn.community.user.persistence.CommunityUserLoginAccountMapper;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.mail.EmailService;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 社区用户忘记密码：发送邮箱验证码并校验重置。
 *
 * <p>验证码默认 10 分钟有效，最多允许 5 次错误尝试；
 * 优先使用 Redis 存储，Redis 不可用时降级为进程内缓存（单机开发环境可用）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityPasswordResetServiceImpl implements CommunityPasswordResetService {

    static final String CODE_KEY_PREFIX = "community:pwdreset:code:";
    static final String ATTEMPTS_KEY_PREFIX = "community:pwdreset:attempts:";
    static final int CODE_LENGTH = 6;
    static final int CODE_TTL_MINUTES = 10;
    static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{12,72}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$";

    private final CommunityUserLoginAccountMapper loginAccountMapper;
    private final CommunityAbuseGuard abuseGuard;
    private final EmailService emailService;
    private final CommunitySessionService sessionService;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, LocalEntry> localEntries = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    @Override
    public void sendResetCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        // 邮箱维度限流：无论邮箱是否注册都计次，防止对同一邮箱轰炸
        abuseGuard.check("EMAIL:" + normalizedEmail, "PASSWORD_RESET_SEND", 3, 900);

        CommunityUserLoginAccount account = loginAccountMapper.selectOne(
                Wrappers.<CommunityUserLoginAccount>lambdaQuery()
                        .eq(CommunityUserLoginAccount::getLoginType, "EMAIL")
                        .eq(CommunityUserLoginAccount::getNormalizedIdentifier, normalizedEmail)
                        .last("LIMIT 1")
        );
        // 邮箱未注册：不发送、不落码，统一返回成功避免账号枚举
        if (account == null || account.getPasswordHash() == null) {
            log.info("社区忘记密码: 邮箱未注册, 不发送验证码 email={}", normalizedEmail);
            return;
        }

        String code = generateCode();
        store(codeKey(normalizedEmail), code, Duration.ofMinutes(CODE_TTL_MINUTES));
        store(attemptsKey(normalizedEmail), String.valueOf(MAX_ATTEMPTS), Duration.ofMinutes(CODE_TTL_MINUTES));
        try {
            emailService.sendResetPassword(normalizedEmail, code, CODE_TTL_MINUTES);
            log.info("社区忘记密码验证码已发送, email={}", normalizedEmail);
        } catch (RuntimeException exception) {
            delete(codeKey(normalizedEmail));
            delete(attemptsKey(normalizedEmail));
            log.warn("社区忘记密码邮件发送失败, email={}", normalizedEmail, exception);
            throw new BusinessException(500, "验证码发送失败，请稍后重试或联系管理员");
        }
    }

    @Override
    public void resetPassword(String email, String code, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, "请输入验证码");
        }
        if (newPassword == null || !newPassword.matches(PASSWORD_PATTERN)) {
            throw new BusinessException(400, "新密码须为 12-72 位，并包含大写、小写、数字和特殊字符，且不能含空格");
        }

        String expected = get(codeKey(normalizedEmail));
        if (expected == null || !constantTimeEquals(expected, code.trim())) {
            consumeFailedAttempt(normalizedEmail);
            throw new BusinessException(400, "验证码错误或已过期");
        }

        // 先完成密码更新，成功后才消费验证码，避免更新失败导致用户被迫重新走全流程
        sessionService.resetPassword(normalizedEmail, newPassword);
        delete(codeKey(normalizedEmail));
        delete(attemptsKey(normalizedEmail));
    }

    private void consumeFailedAttempt(String normalizedEmail) {
        String attemptsKey = attemptsKey(normalizedEmail);
        String raw = get(attemptsKey);
        if (raw == null) {
            // 无有效尝试计数（异常状态），不再重建
            return;
        }
        int remaining = Integer.parseInt(raw) - 1;
        if (remaining <= 0) {
            delete(codeKey(normalizedEmail));
            delete(attemptsKey);
            return;
        }
        store(attemptsKey, String.valueOf(remaining), Duration.ofMinutes(CODE_TTL_MINUTES));
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    // ---- 验证码短期存储（Redis 优先，进程内兜底） ----

    private void store(String key, String value, Duration ttl) {
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, value, ttl);
                return;
            } catch (RuntimeException exception) {
                switchToLocalStore();
            }
        }
        localEntries.put(key, new LocalEntry(value, Instant.now().plus(ttl)));
    }

    private String get(String key) {
        if (redisAvailable) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (RuntimeException exception) {
                switchToLocalStore();
            }
        }
        LocalEntry entry = localEntries.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            localEntries.remove(key);
            return null;
        }
        return entry.value();
    }

    private void delete(String key) {
        localEntries.remove(key);
        if (redisAvailable) {
            try {
                redisTemplate.delete(key);
            } catch (RuntimeException exception) {
                switchToLocalStore();
            }
        }
    }

    private void switchToLocalStore() {
        if (redisAvailable) {
            redisAvailable = false;
            log.warn("Redis 不可用，忘记密码验证码存储降级为进程内缓存");
        }
    }

    // ---- 工具方法 ----

    private static String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append((char) ('0' + RANDOM.nextInt(10)));
        }
        return code.toString();
    }

    private static String codeKey(String normalizedEmail) {
        return CODE_KEY_PREFIX + normalizedEmail;
    }

    private static String attemptsKey(String normalizedEmail) {
        return ATTEMPTS_KEY_PREFIX + normalizedEmail;
    }

    private static String normalizeEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new BusinessException(400, "请输入邮箱地址");
        }
        String email = Normalizer.normalize(rawEmail.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        if (email.length() > 320 || !Validator.isEmail(email) || !email.matches(EMAIL_PATTERN)) {
            throw new BusinessException(400, "请输入有效邮箱，例如 name@example.com");
        }
        return email;
    }

    private record LocalEntry(String value, Instant expiresAt) {
    }
}
