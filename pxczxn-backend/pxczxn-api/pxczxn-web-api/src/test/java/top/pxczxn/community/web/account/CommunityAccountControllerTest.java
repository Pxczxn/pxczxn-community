package top.pxczxn.community.web.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.user.application.CommunitySessionService;
import top.pxczxn.community.user.application.CommunityPreferenceService;
import top.pxczxn.community.user.application.CommunityPreferenceSettings;
import top.pxczxn.community.user.application.CurrentCommunityUser;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityAccountControllerTest {

    private CommunitySessionService sessionService;
    private CommunityPreferenceService preferenceService;
    private CommunityAccountController controller;

    @BeforeEach
    void setUp() {
        sessionService = mock(CommunitySessionService.class);
        preferenceService = mock(CommunityPreferenceService.class);
        controller = new CommunityAccountController(sessionService, preferenceService);
    }

    @Test
    void updatesTheCurrentUsersPublicProfile() {
        when(sessionService.updateProfile("Updated name", "Updated bio")).thenReturn(currentUser());

        var result = controller.updateProfile(new CommunityProfileUpdateRequest("Updated name", "Updated bio")).getData();

        assertThat(result.displayName()).isEqualTo("Updated name");
        assertThat(result.bio()).isEqualTo("Updated bio");
        verify(sessionService).updateProfile("Updated name", "Updated bio");
    }

    @Test
    void updatesTheCurrentUsersPreferenceSettings() {
        Map<String, Object> settings = Map.of("dndMode", true, "fontSize", "LARGE");
        when(preferenceService.update(settings)).thenReturn(new CommunityPreferenceSettings(settings));

        var result = controller.updatePreferences(
                new CommunityAccountController.CommunityPreferenceUpdateRequest(settings)
        ).getData();

        assertThat(result.settings()).containsEntry("dndMode", true);
        verify(preferenceService).update(settings);
    }

    private static CurrentCommunityUser currentUser() {
        return new CurrentCommunityUser(1L, "tester", "Updated name", "Updated bio", null,
                "tester@example.com", "NORMAL", "UNVERIFIED", false, 2L, "Test blog", "test-blog");
    }
}
