package top.pxczxn.community.web.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.chat.application.CommunityChatConversationView;
import top.pxczxn.community.chat.application.CommunityChatService;
import top.pxczxn.community.shared.auth.CommunityAuth;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityChatControllerTest {

    private CommunityChatService service;
    private CommunityAuth auth;
    private CommunityChatController controller;

    @BeforeEach
    void setUp() {
        service = mock(CommunityChatService.class);
        auth = mock(CommunityAuth.class);
        controller = new CommunityChatController(service, auth);
    }

    @Test
    void conversationsExposePeerAndUnreadStateForTheLoggedInUser() {
        when(auth.getLoginUserId()).thenReturn(1L);
        when(service.conversations(1L, 20)).thenReturn(List.of(new CommunityChatConversationView(
                2L, "peer", "Peer", 3L, "hello", LocalDateTime.of(2026, 8, 11, 9, 0), 4
        )));

        var result = controller.conversations(20).getData();

        assertThat(result).singleElement().satisfies(conversation -> {
            assertThat(conversation.peerUserId()).isEqualTo("2");
            assertThat(conversation.peerAvatarFileId()).isEqualTo("3");
            assertThat(conversation.unreadCount()).isEqualTo(4);
        });
        verify(service).conversations(1L, 20);
    }
}
