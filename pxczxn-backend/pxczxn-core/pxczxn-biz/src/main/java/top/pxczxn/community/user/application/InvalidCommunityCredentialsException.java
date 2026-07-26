package top.pxczxn.community.user.application;

import com.mars.common.exception.BusinessException;

public final class InvalidCommunityCredentialsException extends BusinessException {

    public InvalidCommunityCredentialsException() {
        super(401, "邮箱或密码错误");
    }
}
