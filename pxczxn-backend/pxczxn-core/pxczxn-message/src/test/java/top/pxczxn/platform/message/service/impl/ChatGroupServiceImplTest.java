package top.pxczxn.platform.message.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.message.entity.ChatGroup;
import top.pxczxn.platform.message.entity.ChatGroupMember;
import top.pxczxn.platform.message.mapper.ChatGroupMapper;
import top.pxczxn.platform.message.mapper.ChatGroupMemberMapper;
import top.pxczxn.platform.message.mapper.ChatGroupMessageMapper;
import top.pxczxn.platform.system.mapper.SysUserMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatGroupServiceImplTest {

    @Mock
    private ChatGroupMapper groupMapper;
    @Mock
    private ChatGroupMemberMapper memberMapper;
    @Mock
    private ChatGroupMessageMapper messageMapper;
    @Mock
    private SysUserMapper userMapper;

    private ChatGroupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatGroupServiceImpl(
                groupMapper,
                memberMapper,
                messageMapper,
                userMapper);
    }

    @Test
    void groupDetailIsRestrictedToActiveMembers() {
        when(groupMapper.selectGroupDetail(30L)).thenReturn(activeGroup(30L, 10L));

        assertThatThrownBy(() -> service.getGroupDetail(30L, 20L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("你不是该群成员");
    }

    @Test
    void ordinaryMembersCannotAddUsers() {
        when(groupMapper.selectGroupDetail(30L)).thenReturn(activeGroup(30L, 10L));
        when(memberMapper.selectMemberInfo(30L, 20L))
                .thenReturn(member(20L, 0, 0));

        assertThatThrownBy(() ->
                service.addMembers(30L, List.of(40L), 20L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有群主或管理员可以添加成员");
        verify(memberMapper, never()).insert(any(ChatGroupMember.class));
    }

    @Test
    void mutedMembersCannotSendGroupMessages() {
        when(groupMapper.selectGroupDetail(30L)).thenReturn(activeGroup(30L, 10L));
        when(memberMapper.selectMemberInfo(30L, 20L))
                .thenReturn(member(20L, 0, 1));

        assertThatThrownBy(() ->
                service.sendMessage(30L, 20L, "hello", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage("你已被禁言");
        verify(messageMapper, never())
                .insert(any(top.pxczxn.platform.message.entity.ChatGroupMessage.class));
    }

    @Test
    void markAsReadRequiresMembershipAndPersistsCursor() {
        when(groupMapper.selectGroupDetail(30L)).thenReturn(activeGroup(30L, 10L));
        when(memberMapper.selectMemberInfo(30L, 20L))
                .thenReturn(member(20L, 0, 0));
        when(messageMapper.selectLatestMessageId(30L)).thenReturn(88L);

        service.markAsRead(30L, 20L);

        verify(memberMapper).updateReadCursor(
                org.mockito.ArgumentMatchers.eq(30L),
                org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.eq(88L),
                any(java.time.LocalDateTime.class));
    }

    private ChatGroup activeGroup(Long id, Long ownerId) {
        ChatGroup group = new ChatGroup();
        group.setId(id);
        group.setOwnerId(ownerId);
        group.setStatus(1);
        group.setMaxMembers(200);
        return group;
    }

    private ChatGroupMember member(Long userId, int role, int muted) {
        ChatGroupMember member = new ChatGroupMember();
        member.setId(userId + 100);
        member.setGroupId(30L);
        member.setUserId(userId);
        member.setRole(role);
        member.setMuted(muted);
        member.setLastReadMessageId(0L);
        return member;
    }
}
