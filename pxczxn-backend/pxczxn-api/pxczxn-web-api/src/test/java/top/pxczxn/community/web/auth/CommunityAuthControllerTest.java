package top.pxczxn.community.web.auth;

import top.pxczxn.platform.common.result.Result;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.pxczxn.community.user.application.CommunityLoginSession;
import top.pxczxn.community.user.application.CommunityRegistrationService;
import top.pxczxn.community.user.application.CommunitySessionService;
import top.pxczxn.community.user.application.RegisterCommunityUserCommand;
import top.pxczxn.community.user.application.RegisteredCommunityUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityAuthControllerTest {

    @Test
    void registerUsesStringIdsInApiResponse() {
        CommunityRegistrationService service = mock(CommunityRegistrationService.class);
        when(service.register(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new RegisteredCommunityUser(
                        9223372036854775000L,
                        9223372036854774000L,
                        "alice",
                        "alice"
                ));
        CommunitySessionService sessionService = mock(CommunitySessionService.class);
        CommunityAuthController controller = new CommunityAuthController(service, sessionService);

        Result<CommunityRegistrationView> result = controller.register(
                new CommunityRegistrationRequest(
                        "Alice",
                        "alice@example.com",
                        "password-123",
                        "Alice"
                )
        );

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().userId()).isEqualTo("9223372036854775000");
        assertThat(result.getData().blogId()).isEqualTo("9223372036854774000");

        ArgumentCaptor<RegisterCommunityUserCommand> commandCaptor =
                ArgumentCaptor.forClass(RegisterCommunityUserCommand.class);
        verify(service).register(commandCaptor.capture());
        assertThat(commandCaptor.getValue().email()).isEqualTo("alice@example.com");
    }

    @Test
    void availabilityEndpointsDelegateToRegistrationService() {
        CommunityRegistrationService service = mock(CommunityRegistrationService.class);
        when(service.isUsernameAvailable("alice")).thenReturn(true);
        when(service.isEmailAvailable("alice@example.com")).thenReturn(false);
        CommunitySessionService sessionService = mock(CommunitySessionService.class);
        CommunityAuthController controller = new CommunityAuthController(service, sessionService);

        assertThat(controller.checkUsername("alice").getData().available()).isTrue();
        assertThat(controller.checkEmail("alice@example.com").getData().available()).isFalse();
    }

    @Test
    void loginReturnsCommunityTokenMetadataAndStringUserId() {
        CommunityRegistrationService registrationService =
                mock(CommunityRegistrationService.class);
        CommunitySessionService sessionService = mock(CommunitySessionService.class);
        when(sessionService.login(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CommunityLoginSession(
                        "pxczxn-community-token",
                        "community-token-value",
                        604800,
                        9223372036854775000L,
                        "alice"
                ));
        CommunityAuthController controller =
                new CommunityAuthController(registrationService, sessionService);

        Result<CommunityLoginView> result = controller.login(
                new CommunityLoginRequest("alice@example.com", "password-123")
        );

        assertThat(result.getData().tokenName()).isEqualTo("pxczxn-community-token");
        assertThat(result.getData().tokenValue()).isEqualTo("community-token-value");
        assertThat(result.getData().userId()).isEqualTo("9223372036854775000");
    }
}
