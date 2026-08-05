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
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.sanction.application.CommunitySanctionService;
import top.pxczxn.community.sanction.application.SanctionAction;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.model.CommunityUserLoginAccount;
import top.pxczxn.community.user.persistence.CommunityUserLoginAccountMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.security.SecureRandom;

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
    private final CommunitySanctionService sanctionService;
    private final CommunityAbuseGuard abuseGuard;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

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
        abuseGuard.check("EMAIL:" + normalizedEmail, "LOGIN", 10, 900);
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
        sanctionService.requireActionAllowed(user.getId(), SanctionAction.LOGIN);
        user = userMapper.selectById(account.getUserId());
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
                user.getUsername(),
                Boolean.TRUE.equals(account.getForcePasswordChange())
        );
    }

    @Override
    public void logout() {
        Long userId = communityAuth.getLoginUserId();
        communityAuth.logout();
        log.info("社区用户退出登录, userId={}", userId);
    }

    @Override
    @Transactional
    public String forcePasswordReset(Long userId) {
        CommunityUserLoginAccount account = emailAccount(userId);
        String temporaryPassword = temporaryPassword();
        account.setPasswordHash(BCrypt.hashpw(temporaryPassword, BCrypt.gensalt()));
        account.setForcePasswordChange(true);
        updateRequired(loginAccountMapper.updateById(account), "强制重置密码");
        communityAuth.stpLogic().logout(userId);
        return temporaryPassword;
    }

    @Override
    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        Long userId = communityAuth.getLoginUserId();
        CommunityUserLoginAccount account = emailAccount(userId);
        if (currentPassword == null || !BCrypt.checkpw(currentPassword, account.getPasswordHash())) throw new BusinessException(400, "当前密码不正确");
        if (newPassword == null || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{12,72}$")) throw new BusinessException(400, "新密码须为 12-72 位，并包含大写、小写、数字和特殊字符，且不能含空格");
        account.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        account.setForcePasswordChange(false);
        updateRequired(loginAccountMapper.updateById(account), "修改密码");
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        if (newPassword == null || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{12,72}$")) {
            throw new BusinessException(400, "新密码须为 12-72 位，并包含大写、小写、数字和特殊字符，且不能含空格");
        }
        String normalizedEmail = normalizeEmail(email);
        CommunityUserLoginAccount account = loginAccountMapper.selectOne(
                Wrappers.<CommunityUserLoginAccount>lambdaQuery()
                        .eq(CommunityUserLoginAccount::getLoginType, "EMAIL")
                        .eq(CommunityUserLoginAccount::getNormalizedIdentifier, normalizedEmail)
                        .last("LIMIT 1")
        );
        if (account == null || account.getPasswordHash() == null) {
            throw new BusinessException(404, "登录账号不存在");
        }
        account.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        account.setForcePasswordChange(false);
        account.setFailedLoginCount(0);
        account.setLockedUntil(null);
        updateRequired(loginAccountMapper.updateById(account), "重置密码");
        communityAuth.stpLogic().logout(account.getUserId());
        log.info("社区用户通过邮箱验证码重置密码, userId={}", account.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean requiresPasswordChange(Long userId) {
        return Boolean.TRUE.equals(emailAccount(userId).getForcePasswordChange());
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
        sanctionService.requireActionAllowed(userId, SanctionAction.LOGIN);
        user = userMapper.selectById(userId);
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
                account != null && Boolean.TRUE.equals(account.getForcePasswordChange()),
                user.getPersonalBlogId(),
                personalBlog == null ? null : personalBlog.getName(),
                personalBlog == null ? null : personalBlog.getSlug()
        );
    }

    private static void assertLoginAllowed(CommunityUser user) {
        switch (user.getStatus()) {
            case "NORMAL", "LIMITED", "FROZEN" -> {
                return;
            }
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

    private CommunityUserLoginAccount emailAccount(Long userId) { CommunityUserLoginAccount account=loginAccountMapper.selectOne(Wrappers.<CommunityUserLoginAccount>lambdaQuery().eq(CommunityUserLoginAccount::getUserId,userId).eq(CommunityUserLoginAccount::getLoginType,"EMAIL").last("LIMIT 1")); if(account==null)throw new BusinessException(404,"登录账号不存在"); return account; }
    private static String temporaryPassword() { StringBuilder value=new StringBuilder(16); for(int i=0;i<16;i++)value.append(TEMP_PASSWORD_ALPHABET.charAt(RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length()))); return value.toString(); }
}
