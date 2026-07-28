package top.pxczxn.community.web.auth;

import org.junit.jupiter.api.Test;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanaryRegistrationGateTest {
    @Test
    void rejectsRegistrationWhenTheCanaryCapIsReached() {
        CommunityUserMapper mapper = mock(CommunityUserMapper.class);
        when(mapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(3L);
        CanaryRegistrationGate gate = new CanaryRegistrationGate(mapper);
        gate.setMaximumRegisteredUsersForTest(3);

        assertThatThrownBy(() -> gate.register(() -> "new-user"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo(429);
    }

    @Test
    void permitsRegistrationBelowTheCanaryCap() {
        CommunityUserMapper mapper = mock(CommunityUserMapper.class);
        when(mapper.selectCount(org.mockito.ArgumentMatchers.any())).thenReturn(2L);
        CanaryRegistrationGate gate = new CanaryRegistrationGate(mapper);
        gate.setMaximumRegisteredUsersForTest(3);

        assertThat(gate.register(() -> "new-user")).isEqualTo("new-user");
    }
}
