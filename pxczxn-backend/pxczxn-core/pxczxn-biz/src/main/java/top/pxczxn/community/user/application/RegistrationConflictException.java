package top.pxczxn.community.user.application;

import top.pxczxn.platform.common.exception.BusinessException;

public final class RegistrationConflictException extends BusinessException {

    private RegistrationConflictException(String message) {
        super(409, message);
    }

    public static RegistrationConflictException username() {
        return new RegistrationConflictException("用户名已被使用");
    }

    public static RegistrationConflictException email() {
        return new RegistrationConflictException("邮箱已被注册");
    }

    public static RegistrationConflictException blogSlug() {
        return new RegistrationConflictException("个人博客地址已被占用，请更换用户名");
    }

    public static RegistrationConflictException generic() {
        return new RegistrationConflictException("注册信息已存在，请更换后重试");
    }
}
