package top.pxczxn.community.user.application;

/**
 * 社区用户忘记密码流程：邮箱验证码发送与校验重置。
 */
public interface CommunityPasswordResetService {

    /**
     * 向指定邮箱发送重置密码验证码。
     *
     * <p>无论邮箱是否注册均返回成功，避免账号枚举；
     * 仅当邮箱已注册且邮件发送成功时才会真正落库验证码。</p>
     *
     * @param email 用户邮箱
     */
    void sendResetCode(String email);

    /**
     * 校验邮箱验证码并重置密码。
     *
     * @param email       用户邮箱
     * @param code        邮箱验证码
     * @param newPassword 新密码（须满足平台密码强度要求）
     */
    void resetPassword(String email, String code, String newPassword);
}
