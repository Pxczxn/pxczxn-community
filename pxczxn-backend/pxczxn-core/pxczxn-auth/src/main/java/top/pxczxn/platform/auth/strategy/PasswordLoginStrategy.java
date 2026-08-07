package top.pxczxn.platform.auth.strategy;

import cn.hutool.crypto.digest.BCrypt;
import top.pxczxn.platform.auth.LoginHelper;
import top.pxczxn.platform.auth.LoginRequest;
import top.pxczxn.platform.auth.LoginResult;
import top.pxczxn.platform.auth.LoginStrategy;
import top.pxczxn.platform.auth.TransientCodeStore;
import top.pxczxn.platform.auth.enums.ClientType;
import top.pxczxn.platform.auth.enums.LoginType;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.system.entity.SysUser;
import top.pxczxn.platform.system.service.SysUserService;
import top.pxczxn.platform.system.helper.SystemConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 密码登录策略（Admin/Web 通用）
 * 支持用户名+密码登录，含重试次数限制、验证码校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordLoginStrategy implements LoginStrategy {

    private final SysUserService userService;
    private final SystemConfigHelper configHelper;
    private final StringRedisTemplate redisTemplate;
    private final TransientCodeStore transientCodeStore;
    private final LoginHelper loginHelper;

    private static final String LOGIN_RETRY_KEY = "login:retry:";
    private static final String CAPTCHA_KEY = "captcha:";
    private final Map<String, LocalRetryState> localRetryCache = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    @Override
    public LoginType getType() {
        return LoginType.PASSWORD;
    }

    @Override
    public ClientType[] supportedClients() {
        return new ClientType[]{ClientType.ADMIN, ClientType.WEB};
    }

    @Override
    public LoginResult login(LoginRequest request) {
        // 1. 验证码校验
        validateCaptcha(request);

        // 2. 重试次数检查
        String retryKey = LOGIN_RETRY_KEY + request.getUsername();
        checkRetryLimit(retryKey, request.getUsername());

        // 3. 用户验证
        SysUser user = userService.getByUsername(request.getUsername());
        if (user == null) {
            incrementRetry(retryKey);
            loginHelper.recordFailLog(request.getUsername(), "用户不存在");
            throw new BusinessException("用户名或密码错误");
        }

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            incrementRetry(retryKey);
            loginHelper.recordFailLog(request.getUsername(), "密码错误");
            int remaining = configHelper.getMaxRetryCount() - getRetryCount(retryKey);
            String msg = remaining > 0 ? "用户名或密码错误，还剩" + remaining + "次机会" : "用户名或密码错误，账号已锁定";
            throw new BusinessException(msg);
        }

        // 4. 状态检查
        checkUserStatus(user);

        // 5. 用户类型校验：admin端只允许admin用户，web端只允许pc或admin用户
        checkUserType(user, request.getClientType());

        // 6. 清除重试、执行登录
        clearRetry(retryKey);
        return loginHelper.doLogin(user, request.getRememberMe());
    }

    private void validateCaptcha(LoginRequest request) {
        if (!configHelper.isCaptchaEnabled()) return;

        String captchaType = configHelper.getCaptchaType();

        if ("slider".equals(captchaType)) {
            // 滑块验证码（前端已验证，后端简单校验标识）
            if (!"slider_verified".equals(request.getCode())) {
                throw new BusinessException("请完成滑块验证");
            }
        } else if ("sms".equals(captchaType)) {
            // 短信验证码校验
            if (request.getPhone() == null || request.getCode() == null) {
                throw new BusinessException("请输入手机号和验证码");
            }
            String cacheCode = transientCodeStore.get("sms:login:" + request.getPhone());
            if (cacheCode == null || !cacheCode.equals(request.getCode())) {
                throw new BusinessException("短信验证码错误或已过期");
            }
            transientCodeStore.delete("sms:login:" + request.getPhone());
        } else {
            // 图片验证码（image / math / circle / shear 等）
            if (request.getUuid() == null || request.getCode() == null) {
                throw new BusinessException("请输入验证码");
            }
            String cacheCode = transientCodeStore.get(CAPTCHA_KEY + request.getUuid());
            transientCodeStore.delete(CAPTCHA_KEY + request.getUuid());
            if (cacheCode == null || !cacheCode.equalsIgnoreCase(request.getCode())) {
                throw new BusinessException("验证码错误或已过期");
            }
        }
    }

    private void checkRetryLimit(String retryKey, String username) {
        int retryCount = getRetryCount(retryKey);
        int maxRetry = configHelper.getMaxRetryCount();
        if (retryCount >= maxRetry) {
            Long ttl = getRetryTtlMinutes(retryKey);
            loginHelper.recordFailLog(username, "账号已锁定");
            throw new BusinessException("账号已锁定，请" + ttl + "分钟后重试");
        }
    }

    private int getRetryCount(String retryKey) {
        if (redisAvailable) {
            try {
                String str = redisTemplate.opsForValue().get(retryKey);
                return str != null ? Integer.parseInt(str) : 0;
            } catch (RuntimeException e) {
                switchToLocalRetryCache();
            }
        }
        LocalRetryState state = localRetryCache.get(retryKey);
        if (state == null || state.expiresAt().isBefore(Instant.now())) {
            localRetryCache.remove(retryKey);
            return 0;
        }
        return state.count();
    }

    private void incrementRetry(String retryKey) {
        int count = getRetryCount(retryKey) + 1;
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(
                        retryKey,
                        String.valueOf(count),
                        configHelper.getLockTime(),
                        TimeUnit.MINUTES
                );
                return;
            } catch (RuntimeException e) {
                switchToLocalRetryCache();
            }
        }
        localRetryCache.put(retryKey, new LocalRetryState(
                count,
                Instant.now().plusSeconds(configHelper.getLockTime() * 60L)
        ));
    }

    private void clearRetry(String retryKey) {
        localRetryCache.remove(retryKey);
        if (redisAvailable) {
            try {
                redisTemplate.delete(retryKey);
            } catch (RuntimeException e) {
                switchToLocalRetryCache();
            }
        }
    }

    private long getRetryTtlMinutes(String retryKey) {
        if (redisAvailable) {
            try {
                Long ttl = redisTemplate.getExpire(retryKey, TimeUnit.MINUTES);
                return ttl == null || ttl < 0 ? configHelper.getLockTime() : ttl;
            } catch (RuntimeException e) {
                switchToLocalRetryCache();
            }
        }
        LocalRetryState state = localRetryCache.get(retryKey);
        if (state == null) {
            return configHelper.getLockTime();
        }
        long seconds = Math.max(0, state.expiresAt().getEpochSecond() - Instant.now().getEpochSecond());
        return Math.max(1, (seconds + 59) / 60);
    }

    private void switchToLocalRetryCache() {
        if (redisAvailable) {
            redisAvailable = false;
            log.warn("Redis 不可用，管理员登录重试限制降级为进程内缓存");
        }
    }

    private record LocalRetryState(int count, Instant expiresAt) {
    }

    private void checkUserType(SysUser user, ClientType clientType) {
        String userType = user.getUserType();
        if (userType == null || userType.isEmpty()) {
            return; // 兼容历史数据，未设置类型的用户不限制
        }
        if (clientType == ClientType.ADMIN && !"admin".equals(userType)) {
            loginHelper.recordFailLog(user.getUsername(), "非管理端用户，无权登录后台");
            throw new BusinessException("您不是管理端用户，无法登录后台管理系统");
        }
        if (clientType == ClientType.WEB && "app".equals(userType)) {
            loginHelper.recordFailLog(user.getUsername(), "App用户无权登录PC端");
            throw new BusinessException("您是App用户，请使用App登录");
        }
    }

    private void checkUserStatus(SysUser user) {
        if (user.getStatus() == 2) {
            loginHelper.recordFailLog(user.getUsername(), "账号待审核");
            throw new BusinessException("您的账号正在审核中，请等待管理员审核通过后再登录");
        }
        if (user.getStatus() == 3) {
            loginHelper.recordFailLog(user.getUsername(), "审核未通过");
            throw new BusinessException("您的注册申请未通过审核，如有疑问请联系管理员");
        }
        if (user.getStatus() != 1) {
            loginHelper.recordFailLog(user.getUsername(), "用户已被禁用");
            throw new BusinessException("用户已被禁用");
        }
        if (user.getIsQuit() == 1) {
            loginHelper.recordFailLog(user.getUsername(), "用户已离职");
            throw new BusinessException("用户已离职");
        }
    }
}
