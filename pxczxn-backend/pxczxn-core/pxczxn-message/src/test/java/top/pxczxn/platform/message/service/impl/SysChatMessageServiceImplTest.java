package top.pxczxn.platform.message.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.message.entity.SysChatMessage;
import top.pxczxn.platform.message.mapper.SysChatMessageMapper;
import top.pxczxn.platform.system.entity.SysUser;
import top.pxczxn.platform.system.mapper.SysUserMapper;
import top.pxczxn.platform.system.service.SysUserBlacklistService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysChatMessageServiceImplTest {

    @Mock
    private SysChatMessageMapper messageMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysUserBlacklistService blacklistService;

    private SysChatMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysChatMessageServiceImpl(
                messageMapper,
                userMapper,
                blacklistService);
    }

    @Test
    void sendPersistsValidatedMessageWithTrustedSenderSnapshot() {
        SysUser sender = activeUser(10L, "alice", "爱丽丝");
        SysUser receiver = activeUser(20L, "bob", "鲍勃");
        when(userMapper.selectById(10L)).thenReturn(sender);
        when(userMapper.selectById(20L)).thenReturn(receiver);
        doAnswer(invocation -> {
            SysChatMessage message = invocation.getArgument(0);
            message.setId(99L);
            return 1;
        }).when(messageMapper).insert(any(SysChatMessage.class));

        SysChatMessage result = service.send(10L, 20L, " 你好 ", 1);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getSenderId()).isEqualTo(10L);
        assertThat(result.getSenderName()).isEqualTo("爱丽丝");
        assertThat(result.getReceiverId()).isEqualTo(20L);
        assertThat(result.getContent()).isEqualTo("你好");
        assertThat(result.getIsRead()).isZero();
        assertThat(result.getSenderDeleted()).isZero();
        assertThat(result.getReceiverDeleted()).isZero();
        verify(messageMapper).insert(result);
    }

    @Test
    void sendRejectsSelfInvalidMediaAndBlockedRecipients() {
        SysUser sender = activeUser(10L, "alice", "爱丽丝");
        SysUser receiver = activeUser(20L, "bob", "鲍勃");
        when(userMapper.selectById(10L)).thenReturn(sender);
        when(userMapper.selectById(20L)).thenReturn(receiver);

        assertThatThrownBy(() -> service.send(10L, 10L, "hello", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能给自己发送消息");
        assertThatThrownBy(() -> service.send(
                10L,
                20L,
                "javascript:alert(1)",
                2))
                .isInstanceOf(BusinessException.class)
                .hasMessage("媒体消息地址无效");

        when(blacklistService.isBlocked(10L, 20L)).thenReturn(true);
        assertThatThrownBy(() -> service.send(10L, 20L, "hello", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage("消息发送失败，对方已将你拉黑");
        verify(messageMapper, never()).insert(any(SysChatMessage.class));
    }

    @Test
    void clearHistoryOnlyHidesCurrentUsersCopies() {
        service.clearHistory(10L, 20L);

        verify(messageMapper).hideSentHistory(10L, 20L);
        verify(messageMapper).hideReceivedHistory(10L, 20L);
    }

    @Test
    void historyEnforcesConversationAndPaginationBounds() {
        assertThatThrownBy(() ->
                service.getChatHistory(10L, 10L, 1, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能查询与自己的聊天记录");

        when(userMapper.selectById(20L))
                .thenReturn(activeUser(20L, "bob", "鲍勃"));
        assertThatThrownBy(() ->
                service.getChatHistory(10L, 20L, 1, 101))
                .isInstanceOf(BusinessException.class)
                .hasMessage("每页数量必须在1到100之间");
    }

    private SysUser activeUser(Long id, String username, String nickname) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setStatus(1);
        return user;
    }
}
