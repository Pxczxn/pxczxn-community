package top.pxczxn.community.web.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.function.Supplier;

/** Serializes registrations in this single deployment and enforces the public-canary cap. */
@Component
@RequiredArgsConstructor
public class CanaryRegistrationGate {
    private final CommunityUserMapper userMapper;

    @Value("${pxczxn.community.canary.registration-enabled:true}")
    private boolean registrationEnabled = true;

    /** Zero means registration is fully open; positive values cap all community accounts. */
    @Value("${pxczxn.community.canary.maximum-registered-users:0}")
    private long maximumRegisteredUsers;

    public synchronized <T> T register(Supplier<T> operation) {
        if (!registrationEnabled) {
            throw new BusinessException(403, "当前不开放注册");
        }
        if (maximumRegisteredUsers > 0) {
            Long count = userMapper.selectCount(Wrappers.<CommunityUser>lambdaQuery());
            if (count != null && count >= maximumRegisteredUsers) {
                throw new BusinessException(429, "灰度注册名额已满");
            }
        }
        return operation.get();
    }

    void setRegistrationEnabledForTest(boolean value) {
        registrationEnabled = value;
    }

    void setMaximumRegisteredUsersForTest(long value) {
        maximumRegisteredUsers = value;
    }
}
