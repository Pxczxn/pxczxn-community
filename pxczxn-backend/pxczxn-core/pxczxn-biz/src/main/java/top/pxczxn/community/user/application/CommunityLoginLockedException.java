package top.pxczxn.community.user.application;

import com.mars.common.exception.BusinessException;

public final class CommunityLoginLockedException extends BusinessException {

    public CommunityLoginLockedException(String message) {
        super(423, message);
    }
}
