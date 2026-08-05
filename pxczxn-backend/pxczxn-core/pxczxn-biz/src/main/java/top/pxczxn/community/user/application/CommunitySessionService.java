package top.pxczxn.community.user.application;

public interface CommunitySessionService {

    CommunityLoginSession login(CommunityLoginCommand command);

    void logout();

    CurrentCommunityUser getCurrentUser();

    String forcePasswordReset(Long userId);

    void changePassword(String currentPassword, String newPassword);

    /**
     * 通过邮箱重置密码（忘记密码流程使用）。
     *
     * <p>不校验当前密码，由调用方（验证码校验）保证操作者身份；
     * 重置成功后使该用户所有会话失效，并要求下次登录后按需修改。</p>
     *
     * @param email       已归一化的邮箱
     * @param newPassword 新密码（须满足平台密码强度要求）
     */
    void resetPassword(String email, String newPassword);

    boolean requiresPasswordChange(Long userId);
}
