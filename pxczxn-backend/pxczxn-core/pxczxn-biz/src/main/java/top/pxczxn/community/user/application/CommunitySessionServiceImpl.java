package top.pxczxn.community.user.application;

import cn.hutool.core.lang.Validator;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.model.CommunityUserLoginAccount;
import top.pxczxn.community.user.persistence.CommunityUserLoginAccountMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunitySessionServiceImpl implements CommunitySessionService {

    static final int MAXIMUM_LOGIN_ATTEMPTS = 5;
    static final int LOCK_MINUTES = 15;

    private final CommunityUserMapper userMapper;
    private final CommunityUserLoginAccountMapper loginAccountMapper;
    private final BlogMapper blogMapper;
    private final CommunityAuth communityAuth;

    @Override
    @Transactional(noRollbackFor = {
            InvalidCommunityCredentialsException.class,
            CommunityLoginLockedException.class
    })
    public CommunityLoginSession login(CommunityLoginCommand command) {
        if (command == null || command.password() == null) {
            throw new InvalidCommunityCredentialsException();
        }
        String normalizedEmail = normalizeEmail(command.email());
        CommunityUserLoginAccount account = loginAccountMapper.selectOne(
                Wrappers.<CommunityUserLoginAccount>lambdaQuery()
                        .eq(CommunityUserLoginAccount::getLoginType, "EMAIL")
                        .eq(CommunityUserLoginAccount::getNormalizedIdentifier, normalizedEmail)
                        .last("LIMIT 1")
        );
        if (account == null || account.getPasswordHash() == null) {
            log.warn("社区登录失败: 登录账号不存在");
            throw new InvalidCommunityCredentialsException();
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int failedAttempts = account.getFailedLoginCount() == null
                ? 0
                : account.getFailedLoginCount();
        if (account.getLockedUntil() != null) {
            if (account.getLockedUntil().isAfter(now)) {
                log.warn("社区登录被锁定, accountId={}", account.getId());
                throw new CommunityLoginLockedException("登录失败次数过多，请稍后再试");
            }
            updateRequired(
                    loginAccountMapper.clearLoginFailures(account.getId()),
                    "清理过期登录锁定"
            );
            failedAttempts = 0;
        }

        CommunityUser user = userMapper.selectById(account.getUserId());
        if (user == null) {
            log.error("社区登录账号缺少用户主体, accountId={}", account.getId());
            throw new InvalidCommunityCredentialsException();
        }
        assertLoginAllowed(user);

        boolean passwordMatches;
        try {
            passwordMatches = BCrypt.checkpw(command.password(), account.getPasswordHash());
        } catch (RuntimeException exception) {
            log.error("社区登录账号密码哈希无效, accountId={}", account.getId());
            passwordMatches = false;
        }
        if (!passwordMatches) {
            LocalDateTime lockedUntil = now.plusMinutes(LOCK_MINUTES);
            updateRequired(
                    loginAccountMapper.recordLoginFailure(
                            account.getId(),
                            MAXIMUM_LOGIN_ATTEMPTS,
                            lockedUntil
                    ),
                    "记录登录失败"
            );
            int nextFailedAttempts = failedAttempts + 1;
            log.warn(
                    "社区登录密码错误, accountId={}, failedAttempts={}",
                    account.getId(),
                    nextFailedAttempts
            );
            if (nextFailedAttempts >= MAXIMUM_LOGIN_ATTEMPTS) {
                throw new CommunityLoginLockedException(
                        "登录失败次数过多，账号已临时锁定 " + LOCK_MINUTES + " 分钟"
                );
            }
            throw new InvalidCommunityCredentialsException();
        }

        updateRequired(loginAccountMapper.recordLoginSuccess(account.getId(), now), "更新登录账号");
        updateRequired(userMapper.recordLoginSuccess(user.getId(), now), "更新用户登录时间");

        communityAuth.login(user.getId());
        String tokenValue = communityAuth.getTokenValue();
        log.info("社区用户登录成功, userId={}", user.getId());
        return new CommunityLoginSession(
                CommunityAuth.TOKEN_NAME,
                tokenValue,
                communityAuth.getTokenTimeout(),
                user.getId(),
                user.getUsername()
        );
    }

    @Override
    public void logout() {
        Long userId = communityAuth.getLoginUserId();
        communityAuth.logout();
        log.info("社区用户退出登录, userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentCommunityUser getCurrentUser() {
        Long userId = communityAuth.getLoginUserId();
        CommunityUser user = userMapper.selectById(userId);
        if (user == null) {
            communityAuth.logout();
            throw new BusinessException(401, "登录用户不存在");
        }
        assertLoginAllowed(user);

        CommunityUserLoginAccount account = loginAccountMapper.selectOne(
                Wrappers.<CommunityUserLoginAccount>lambdaQuery()
                        .eq(CommunityUserLoginAccount::getUserId, userId)
                        .eq(CommunityUserLoginAccount::getLoginType, "EMAIL")
                        .last("LIMIT 1")
        );
        Blog personalBlog = user.getPersonalBlogId() == null
                ? null
                : blogMapper.selectById(user.getPersonalBlogId());

        return new CurrentCommunityUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarFileId(),
                account == null ? null : account.getNormalizedIdentifier(),
                user.getStatus(),
                user.getVerificationStatus(),
                user.getPersonalBlogId(),
                personalBlog == null ? null : personalBlog.getName(),
                personalBlog == null ? null : personalBlog.getSlug()
        );
    }

    private static void assertLoginAllowed(CommunityUser user) {
        switch (user.getStatus()) {
            case "NORMAL", "LIMITED" -> {
                return;
            }
            case "FROZEN" -> throw new BusinessException(403, "账号已冻结");
            case "BANNED" -> throw new BusinessException(403, "账号已封禁");
            case "DEACTIVATED" -> throw new BusinessException(403, "账号已停用");
            case "DELETED" -> throw new BusinessException(403, "账号已删除");
            default -> throw new BusinessException(403, "账号状态异常");
        }
    }

    private static String normalizeEmail(String rawEmail) {
        if (rawEmail == null) {
            throw new InvalidCommunityCredentialsException();
        }
        String email = Normalizer.normalize(rawEmail.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        if (email.length() > 320 || !Validator.isEmail(email)) {
            throw new InvalidCommunityCredentialsException();
        }
        return email;
    }

    private static void updateRequired(int affectedRows, String operation) {
        if (affectedRows != 1) {
            throw new BusinessException(500, operation + "失败");
        }
    }
}
