package top.pxczxn.community.user.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.community.user.persistence.CommunityUserPreferenceMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityPreferenceServiceImplTest {

    @Test
    void persistsAllowedSettingsForTheCurrentCommunityUser() {
        CommunityAuth auth = mock(CommunityAuth.class);
        CommunityUserMapper users = mock(CommunityUserMapper.class);
        CommunityUserPreferenceMapper preferences = mock(CommunityUserPreferenceMapper.class);
        CommunityPreferenceService service = new CommunityPreferenceServiceImpl(
                preferences, users, auth, new ObjectMapper()
        );
        CommunityUser user = new CommunityUser();
        user.setId(7L);
        user.setStatus("NORMAL");
        when(auth.getLoginUserId()).thenReturn(7L);
        when(users.selectById(7L)).thenReturn(user);
        when(preferences.update(isNull(), any())).thenReturn(1);

        CommunityPreferenceSettings result = service.update(Map.of(
                "dndMode", true,
                "emailFrequency", "weekly"
        ));

        assertThat(result.settings()).containsEntry("dndMode", true)
                .containsEntry("emailFrequency", "weekly");
        verify(preferences).update(isNull(), any());
    }
}
