package top.pxczxn.platform.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.message.entity.ChatGroup;
import top.pxczxn.platform.message.entity.ChatGroupMember;
import top.pxczxn.platform.message.entity.ChatGroupMessage;
import top.pxczxn.platform.message.mapper.ChatGroupMapper;
import top.pxczxn.platform.message.mapper.ChatGroupMemberMapper;
import top.pxczxn.platform.message.mapper.ChatGroupMessageMapper;
import top.pxczxn.platform.message.service.ChatGroupService;
import top.pxczxn.platform.system.entity.SysUser;
import top.pxczxn.platform.system.mapper.SysUserMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 群聊服务，集中执行成员级资源权限和消息状态约束。
 */
@Service
@RequiredArgsConstructor
public class ChatGroupServiceImpl implements ChatGroupService {

    private static final int ROLE_MEMBER = 0;
    private static final int ROLE_ADMIN = 1;
    private static final int ROLE_OWNER = 2;
    private static final int TEXT_MESSAGE = 1;
    private static final int IMAGE_MESSAGE = 2;
    private static final int FILE_MESSAGE = 3;
    private static final int SYSTEM_MESSAGE = 4;
    private static final int DEFAULT_MAX_MEMBERS = 200;
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatGroupMapper groupMapper;
    private final ChatGroupMemberMapper memberMapper;
    private final ChatGroupMessageMapper messageMapper;
    private final SysUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroup createGroup(String name, Long ownerId, List<Long> memberIds) {
        SysUser owner = requireActiveUser(ownerId, "群主不存在或已停用");
        String normalizedName = normalizeGroupName(name);

        Set<Long> distinctMembers = new LinkedHashSet<>();
        if (memberIds != null) {
            distinctMembers.addAll(memberIds);
        }
        distinctMembers.remove(null);
        distinctMembers.remove(ownerId);
        if (distinctMembers.size() + 1 > DEFAULT_MAX_MEMBERS) {
            throw new BusinessException(400, "超过群最大成员数限制");
        }
        distinctMembers.forEach(userId ->
                requireActiveUser(userId, "群成员不存在或已停用"));

        LocalDateTime now = LocalDateTime.now();
        ChatGroup group = new ChatGroup();
        group.setName(normalizedName);
        group.setOwnerId(ownerId);
        group.setStatus(1);
        group.setMaxMembers(DEFAULT_MAX_MEMBERS);
        group.setCreateTime(now);
        group.setUpdateTime(now);
        groupMapper.insert(group);

        insertMember(group.getId(), ownerId, ROLE_OWNER, now);
        for (Long userId : distinctMembers) {
            insertMember(group.getId(), userId, ROLE_MEMBER, now);
        }

        ChatGroupMessage systemMessage = sendSystemMessage(
                group.getId(),
                displayName(owner) + " 创建了群聊");
        updateReadCursor(group.getId(), ownerId, systemMessage.getId());
        group.setOwnerName(displayName(owner));
        group.setMemberCount(distinctMembers.size() + 1);
        group.setUnreadCount(0);
        return group;
    }

    @Override
    public List<ChatGroup> getUserGroups(Long userId) {
        requirePositiveId(userId, "用户ID无效");
        List<ChatGroup> groups = groupMapper.selectUserGroups(userId);
        for (ChatGroup group : groups) {
            ChatGroupMessage latestMessage = messageMapper.selectLatestMessage(group.getId());
            if (latestMessage != null) {
                group.setLastMessage(formatMessageSummary(latestMessage));
                group.setLastMessageTime(latestMessage.getSendTime());
            }
            group.setUnreadCount(memberMapper.selectGroupUnreadCount(group.getId(), userId));
        }
        return groups;
    }

    @Override
    public ChatGroup getGroupDetail(Long groupId, Long userId) {
        ChatGroup group = requireActiveGroup(groupId);
        requireMember(groupId, userId);
        group.setMembers(memberMapper.selectGroupMembers(groupId));
        group.setUnreadCount(memberMapper.selectGroupUnreadCount(groupId, userId));
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long groupId,
                            Long operatorId,
                            String name,
                            String announcement) {
        requireActiveGroup(groupId);
        requireRole(groupId, operatorId, ROLE_ADMIN, "没有权限修改群信息");
        String normalizedName = normalizeGroupName(name);
        String normalizedAnnouncement = announcement == null
                ? null
                : announcement.trim();
        if (normalizedAnnouncement != null && normalizedAnnouncement.length() > 500) {
            throw new BusinessException(400, "群公告不能超过500个字符");
        }

        ChatGroup patch = new ChatGroup();
        patch.setId(groupId);
        patch.setName(normalizedName);
        patch.setAnnouncement(normalizedAnnouncement);
        patch.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(patch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(Long groupId, Long operatorId) {
        ChatGroup group = requireActiveGroup(groupId);
        if (!Objects.equals(group.getOwnerId(), operatorId)) {
            throw new BusinessException(403, "只有群主可以解散群聊");
        }
        group.setStatus(0);
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(group);
        memberMapper.delete(new LambdaQueryWrapper<ChatGroupMember>()
                .eq(ChatGroupMember::getGroupId, groupId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitGroup(Long groupId, Long userId) {
        ChatGroup group = requireActiveGroup(groupId);
        ChatGroupMember member = requireMember(groupId, userId);
        if (member.getRole() == ROLE_OWNER
                || Objects.equals(group.getOwnerId(), userId)) {
            throw new BusinessException(
                    400,
                    "群主不能退出群聊，请先转让群主或解散群聊");
        }
        memberMapper.deleteById(member.getId());
        SysUser user = requireActiveUser(userId, "用户不存在或已停用");
        sendSystemMessage(groupId, displayName(user) + " 退出了群聊");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(Long groupId, List<Long> userIds, Long operatorId) {
        ChatGroup group = requireActiveGroup(groupId);
        requireRole(groupId, operatorId, ROLE_ADMIN, "只有群主或管理员可以添加成员");
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(400, "请选择要添加的成员");
        }

        Set<Long> distinctUserIds = new LinkedHashSet<>(userIds);
        distinctUserIds.remove(null);
        distinctUserIds.removeIf(userId ->
                memberMapper.selectMemberInfo(groupId, userId) != null);
        if (distinctUserIds.isEmpty()) {
            return;
        }

        int currentCount = memberMapper.selectMemberIds(groupId).size();
        int maxMembers = group.getMaxMembers() == null
                ? DEFAULT_MAX_MEMBERS
                : group.getMaxMembers();
        if (currentCount + distinctUserIds.size() > maxMembers) {
            throw new BusinessException(400, "超过群最大成员数限制");
        }

        long currentMessageId = messageMapper.selectLatestMessageId(groupId);
        StringBuilder addedNames = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();
        for (Long userId : distinctUserIds) {
            SysUser user = requireActiveUser(userId, "群成员不存在或已停用");
            ChatGroupMember member = insertMember(
                    groupId,
                    userId,
                    ROLE_MEMBER,
                    now);
            member.setLastReadMessageId(currentMessageId);
            member.setLastReadTime(now);
            memberMapper.updateById(member);
            if (!addedNames.isEmpty()) {
                addedNames.append("、");
            }
            addedNames.append(displayName(user));
        }

        SysUser operator = requireActiveUser(operatorId, "操作者不存在或已停用");
        sendSystemMessage(
                groupId,
                displayName(operator) + " 邀请 " + addedNames + " 加入了群聊");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long groupId, Long userId, Long operatorId) {
        requireActiveGroup(groupId);
        ChatGroupMember operator = requireRole(
                groupId,
                operatorId,
                ROLE_ADMIN,
                "没有权限移除成员");
        ChatGroupMember member = requireMember(groupId, userId);
        if (member.getRole() >= operator.getRole()) {
            throw new BusinessException(403, "不能移除同级或更高级别的成员");
        }
        memberMapper.deleteById(member.getId());
        SysUser user = requireActiveUser(userId, "用户不存在或已停用");
        sendSystemMessage(groupId, displayName(user) + " 被移出了群聊");
    }

    @Override
    public List<ChatGroupMember> getGroupMembers(Long groupId, Long userId) {
        requireActiveGroup(groupId);
        requireMember(groupId, userId);
        return memberMapper.selectGroupMembers(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAdmin(Long groupId,
                         Long userId,
                         boolean isAdmin,
                         Long operatorId) {
        ChatGroup group = requireActiveGroup(groupId);
        if (!Objects.equals(group.getOwnerId(), operatorId)) {
            throw new BusinessException(403, "只有群主可以设置管理员");
        }
        ChatGroupMember member = requireMember(groupId, userId);
        if (member.getRole() == ROLE_OWNER) {
            throw new BusinessException(400, "不能修改群主角色");
        }
        member.setRole(isAdmin ? ROLE_ADMIN : ROLE_MEMBER);
        memberMapper.updateById(member);
        SysUser user = requireActiveUser(userId, "用户不存在或已停用");
        sendSystemMessage(
                groupId,
                displayName(user) + (isAdmin ? " 被设为管理员" : " 被取消管理员"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMuted(Long groupId,
                         Long userId,
                         boolean muted,
                         Long operatorId) {
        requireActiveGroup(groupId);
        ChatGroupMember operator = requireRole(
                groupId,
                operatorId,
                ROLE_ADMIN,
                "没有权限操作");
        ChatGroupMember member = requireMember(groupId, userId);
        if (member.getRole() >= operator.getRole()) {
            throw new BusinessException(403, "不能禁言同级或更高级别的成员");
        }
        member.setMuted(muted ? 1 : 0);
        memberMapper.updateById(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(Long groupId, Long newOwnerId, Long operatorId) {
        ChatGroup group = requireActiveGroup(groupId);
        if (!Objects.equals(group.getOwnerId(), operatorId)) {
            throw new BusinessException(403, "只有群主可以转让群");
        }
        if (Objects.equals(newOwnerId, operatorId)) {
            throw new BusinessException(400, "新群主不能是当前群主");
        }

        ChatGroupMember newOwner = requireMember(groupId, newOwnerId);
        ChatGroupMember oldOwner = requireMember(groupId, operatorId);
        group.setOwnerId(newOwnerId);
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(group);

        oldOwner.setRole(ROLE_MEMBER);
        memberMapper.updateById(oldOwner);
        newOwner.setRole(ROLE_OWNER);
        memberMapper.updateById(newOwner);

        SysUser oldUser = requireActiveUser(operatorId, "原群主不存在或已停用");
        SysUser newUser = requireActiveUser(newOwnerId, "新群主不存在或已停用");
        sendSystemMessage(
                groupId,
                displayName(oldUser) + " 将群主转让给了 " + displayName(newUser));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatGroupMessage sendMessage(Long groupId,
                                        Long senderId,
                                        String content,
                                        Integer msgType) {
        requireActiveGroup(groupId);
        ChatGroupMember member = requireMember(groupId, senderId);
        if (Integer.valueOf(1).equals(member.getMuted())) {
            throw new BusinessException(403, "你已被禁言");
        }
        SysUser sender = requireActiveUser(senderId, "发送者不存在或已停用");
        int normalizedType = normalizeMessageType(msgType);
        String normalizedContent = normalizeContent(content, normalizedType);

        ChatGroupMessage message = new ChatGroupMessage();
        message.setGroupId(groupId);
        message.setSenderId(senderId);
        message.setSenderName(
                member.getNickname() == null || member.getNickname().isBlank()
                        ? displayName(sender)
                        : member.getNickname());
        message.setSenderAvatar(sender.getAvatar());
        message.setContent(normalizedContent);
        message.setMsgType(normalizedType);
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);

        touchGroup(groupId);
        updateReadCursor(groupId, senderId, message.getId());
        return message;
    }

    @Override
    public IPage<ChatGroupMessage> getMessageHistory(Long groupId,
                                                     Long userId,
                                                     int page,
                                                     int pageSize) {
        requireActiveGroup(groupId);
        requireMember(groupId, userId);
        if (page < 1) {
            throw new BusinessException(400, "页码必须大于0");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(400, "每页数量必须在1到100之间");
        }
        return messageMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<ChatGroupMessage>()
                        .eq(ChatGroupMessage::getGroupId, groupId)
                        .orderByDesc(ChatGroupMessage::getId));
    }

    @Override
    public boolean isMember(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            return false;
        }
        return memberMapper.selectMemberInfo(groupId, userId) != null;
    }

    @Override
    public List<Long> getMemberIds(Long groupId) {
        requireActiveGroup(groupId);
        return memberMapper.selectMemberIds(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long groupId, Long userId) {
        requireActiveGroup(groupId);
        requireMember(groupId, userId);
        updateReadCursor(groupId, userId, messageMapper.selectLatestMessageId(groupId));
    }

    @Override
    public int getUnreadCount(Long userId) {
        requirePositiveId(userId, "用户ID无效");
        return memberMapper.selectUnreadCount(userId);
    }

    @Override
    public int getUnreadCount(Long groupId, Long userId) {
        requireActiveGroup(groupId);
        requireMember(groupId, userId);
        return memberMapper.selectGroupUnreadCount(groupId, userId);
    }

    private ChatGroup requireActiveGroup(Long groupId) {
        requirePositiveId(groupId, "群ID无效");
        ChatGroup group = groupMapper.selectGroupDetail(groupId);
        if (group == null || !Integer.valueOf(1).equals(group.getStatus())) {
            throw new BusinessException(404, "群不存在或已解散");
        }
        return group;
    }

    private ChatGroupMember requireMember(Long groupId, Long userId) {
        requirePositiveId(userId, "用户ID无效");
        ChatGroupMember member = memberMapper.selectMemberInfo(groupId, userId);
        if (member == null) {
            throw new BusinessException(403, "你不是该群成员");
        }
        return member;
    }

    private ChatGroupMember requireRole(Long groupId,
                                        Long userId,
                                        int minimumRole,
                                        String message) {
        ChatGroupMember member = requireMember(groupId, userId);
        if (member.getRole() == null || member.getRole() < minimumRole) {
            throw new BusinessException(403, message);
        }
        return member;
    }

    private SysUser requireActiveUser(Long userId, String message) {
        requirePositiveId(userId, message);
        SysUser user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(404, message);
        }
        return user;
    }

    private ChatGroupMember insertMember(Long groupId,
                                         Long userId,
                                         int role,
                                         LocalDateTime joinedAt) {
        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setMuted(0);
        member.setLastReadMessageId(0L);
        member.setJoinTime(joinedAt);
        memberMapper.insert(member);
        return member;
    }

    private ChatGroupMessage sendSystemMessage(Long groupId, String content) {
        ChatGroupMessage message = new ChatGroupMessage();
        message.setGroupId(groupId);
        message.setSenderId(0L);
        message.setSenderName("系统消息");
        message.setContent(content);
        message.setMsgType(SYSTEM_MESSAGE);
        message.setSendTime(LocalDateTime.now());
        messageMapper.insert(message);
        touchGroup(groupId);
        return message;
    }

    private void updateReadCursor(Long groupId, Long userId, Long messageId) {
        memberMapper.updateReadCursor(
                groupId,
                userId,
                messageId,
                LocalDateTime.now());
    }

    private void touchGroup(Long groupId) {
        ChatGroup patch = new ChatGroup();
        patch.setId(groupId);
        patch.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(patch);
    }

    private String normalizeGroupName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(400, "群名称不能为空");
        }
        String normalized = name.trim();
        if (normalized.length() > 50) {
            throw new BusinessException(400, "群名称不能超过50个字符");
        }
        return normalized;
    }

    private int normalizeMessageType(Integer msgType) {
        int value = msgType == null ? TEXT_MESSAGE : msgType;
        if (value < TEXT_MESSAGE || value > FILE_MESSAGE) {
            throw new BusinessException(400, "消息类型无效");
        }
        return value;
    }

    private String normalizeContent(String content, int msgType) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        String normalized = content.trim();
        int maxLength = msgType == TEXT_MESSAGE ? 2000 : 1000;
        if (normalized.length() > maxLength) {
            throw new BusinessException(400, "消息内容过长");
        }
        if (msgType != TEXT_MESSAGE
                && !(normalized.startsWith("/")
                || normalized.startsWith("https://")
                || normalized.startsWith("http://"))) {
            throw new BusinessException(400, "媒体消息地址无效");
        }
        return normalized;
    }

    private String formatMessageSummary(ChatGroupMessage message) {
        String content = switch (message.getMsgType()) {
            case IMAGE_MESSAGE -> "[图片]";
            case FILE_MESSAGE -> "[文件]";
            case SYSTEM_MESSAGE -> "[系统消息] " + message.getContent();
            default -> message.getContent();
        };
        return message.getSenderName() + ": " + content;
    }

    private String displayName(SysUser user) {
        return user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername()
                : user.getNickname();
    }

    private void requirePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, message);
        }
    }
}
