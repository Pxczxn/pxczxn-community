package top.pxczxn.community.chat.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import top.pxczxn.community.block.application.CommunityBlockService;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.chat.model.CommunityChatMessage;
import top.pxczxn.community.chat.persistence.CommunityChatMessageMapper;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.sanction.application.CommunitySanctionService;
import top.pxczxn.community.sanction.application.SanctionAction;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityChatServiceImplTest {

    private CommunityChatMessageMapper messageMapper;
    private CommunityFollowMapper followMapper;
    private CommunityUserMapper userMapper;
    private CommunityBlockService blockService;
    private ApplicationEventPublisher eventPublisher;
    private CommunitySanctionService sanctionService;
    private CommunityChatServiceImpl service;

    @BeforeEach
    void setUp() {
        messageMapper = mock(CommunityChatMessageMapper.class);
        followMapper = mock(CommunityFollowMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        blockService = mock(CommunityBlockService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        sanctionService = mock(CommunitySanctionService.class);
        service = new CommunityChatServiceImpl(
                messageMapper, followMapper, userMapper, blockService, eventPublisher, mock(CommunityAbuseGuard.class), sanctionService
        );
        when(userMapper.selectById(10L)).thenReturn(activeUser(10L, 100L));
        when(userMapper.selectById(20L)).thenReturn(activeUser(20L, 200L));
    }

    @Test
    void mutualFollowersCanSendAndPublishCommittedDeliveryEvent() {
        when(followMapper.countMutualBlogPair(10L, 100L, 20L, 200L)).thenReturn(1L);
        when(messageMapper.insert(any(CommunityChatMessage.class))).thenReturn(1);

        CommunityChatMessageView sent = service.send(10L, 20L, " Hello ");

        assertThat(sent.senderUserId()).isEqualTo(10L);
        assertThat(sent.recipientUserId()).isEqualTo(20L);
        assertThat(sent.contentText()).isEqualTo("Hello");
        assertThat(sent.status()).isEqualTo("SENT");
        verify(sanctionService).requireActionAllowed(10L, SanctionAction.MESSAGE);
        verify(eventPublisher).publishEvent(new CommunityChatMessageSentEvent(sent));
    }

    @Test
    void messageBanPreventsMessagePersistence() {
        when(followMapper.countMutualBlogPair(10L, 100L, 20L, 200L)).thenReturn(1L);
        doThrow(new BusinessException(403, "当前账号已被限制发送私信"))
                .when(sanctionService).requireActionAllowed(10L, SanctionAction.MESSAGE);

        assertThatThrownBy(() -> service.send(10L, 20L, "hello"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("限制发送私信");

        verify(messageMapper, never()).insert(any(CommunityChatMessage.class));
    }

    @Test
    void oneWayFollowCannotSendOrReadAnotherUsersConversation() {
        when(followMapper.countMutualBlogPair(10L, 100L, 20L, 200L)).thenReturn(0L);

        assertThatThrownBy(() -> service.send(10L, 20L, "hello"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mutual followers");
        assertThatThrownBy(() -> service.history(10L, 20L, 50))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mutual followers");

        verify(messageMapper, never()).insert(any(CommunityChatMessage.class));
        verify(messageMapper, never()).history(any(), any(), anyInt());
    }

    @Test
    void authorizedHistoryIsBoundToTheRequestedPairAndSortedChronologically() {
        when(followMapper.countMutualBlogPair(10L, 100L, 20L, 200L)).thenReturn(1L);
        CommunityChatMessage latest = message(200L, 20L, 10L, "latest");
        CommunityChatMessage first = message(100L, 10L, 20L, "first");
        when(messageMapper.history(10L, 20L, 100)).thenReturn(List.of(latest, first));

        List<CommunityChatMessageView> history = service.history(10L, 20L, 999);

        assertThat(history).extracting(CommunityChatMessageView::id)
                .containsExactly(100L, 200L);
        verify(messageMapper).history(10L, 20L, 100);
    }

    @Test
    void authorizedReadOnlyMarksUnreadMessagesReceivedByTheActor() {
        when(followMapper.countMutualBlogPair(10L, 100L, 20L, 200L)).thenReturn(1L);

        service.markRead(10L, 20L);

        verify(messageMapper).markRead(eq(20L), eq(10L), any());
    }

    private static CommunityUser activeUser(Long id, Long personalBlogId) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setPersonalBlogId(personalBlogId);
        user.setStatus("NORMAL");
        return user;
    }

    private static CommunityChatMessage message(
            Long id,
            Long senderUserId,
            Long recipientUserId,
            String content
    ) {
        CommunityChatMessage message = new CommunityChatMessage();
        message.setId(id);
        message.setSenderUserId(senderUserId);
        message.setRecipientUserId(recipientUserId);
        message.setContentText(content);
        message.setStatus("SENT");
        return message;
    }
}
