package top.pxczxn.community.web.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.user.application.CommunitySessionService;
import top.pxczxn.community.user.application.CurrentCommunityUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityAccountControllerTest {

    private CommunitySessionService sessionService;
    private CommunityAccountController controller;

    @BeforeEach
    void setUp() {
        sessionService = mock(CommunitySessionService.class);
        controller = new CommunityAccountController(sessionService);
    }

    @Test
    void updatesTheCurrentUsersPublicProfile() {
        when(sessionService.updateProfile("Updated name", "Updated bio")).thenReturn(currentUser());

        var result = controller.updateProfile(new CommunityProfileUpdateRequest("Updated name", "Updated bio")).getData();

        assertThat(result.displayName()).isEqualTo("Updated name");
        assertThat(result.bio()).isEqualTo("Updated bio");
        verify(sessionService).updateProfile("Updated name", "Updated bio");
    }

    private static CurrentCommunityUser currentUser() {
        return new CurrentCommunityUser(1L, "tester", "Updated name", "Updated bio", null,
                "tester@example.com", "NORMAL", "UNVERIFIED", false, 2L, "Test blog", "test-blog");
    }
}
