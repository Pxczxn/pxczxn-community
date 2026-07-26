package top.pxczxn.community.user.application;

import top.pxczxn.platform.common.exception.BusinessException;

public final class CommunityLoginLockedException extends BusinessException {

    public CommunityLoginLockedException(String message) {
        super(423, message);
    }
}
