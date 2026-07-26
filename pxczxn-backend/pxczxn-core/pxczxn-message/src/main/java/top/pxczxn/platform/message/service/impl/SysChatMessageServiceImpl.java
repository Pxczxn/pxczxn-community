package top.pxczxn.platform.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.message.entity.SysChatMessage;
import top.pxczxn.platform.message.mapper.SysChatMessageMapper;
import top.pxczxn.platform.message.model.ChatContact;
import top.pxczxn.platform.message.service.SysChatMessageService;
import top.pxczxn.platform.system.entity.SysUser;
import top.pxczxn.platform.system.mapper.SysUserMapper;
import top.pxczxn.platform.system.service.SysUserBlacklistService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 私聊消息服务。
 *
 * <p>HTTP 服务负责持久化和权限约束，WebSocket 仅投递这里已经落库的结果。</p>
 */
@Service
@RequiredArgsConstructor
public class SysChatMessageServiceImpl implements SysChatMessageService {

    private static final int TEXT_MESSAGE = 1;
    private static final int IMAGE_MESSAGE = 2;
    private static final int FILE_MESSAGE = 3;
    private static final int MAX_PAGE_SIZE = 100;

    private final SysChatMessageMapper chatMessageMapper;
    private final SysUserMapper userMapper;
    private final SysUserBlacklistService blacklistService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysChatMessage send(Long senderId,
                               Long receiverId,
                               String content,
                               Integer msgType) {
        SysUser sender = requireActiveUser(senderId, "发送者不存在或已停用");
        requireActiveUser(receiverId, "接收者不存在或已停用");
        if (senderId.equals(receiverId)) {
            throw new BusinessException(400, "不能给自己发送消息");
        }
        if (blacklistService.isBlocked(senderId, receiverId)) {
            throw new BusinessException(403, "消息发送失败，对方已将你拉黑");
        }
        if (blacklistService.isInMyBlacklist(senderId, receiverId)) {
            throw new BusinessException(403, "请先移除黑名单后再发送消息");
        }

        int normalizedType = normalizeMessageType(msgType, false);
        String normalizedContent = normalizeContent(content, normalizedType);

        SysChatMessage message = new SysChatMessage();
        message.setSenderId(senderId);
        message.setSenderName(displayName(sender));
        message.setSenderAvatar(sender.getAvatar());
        message.setReceiverId(receiverId);
        message.setContent(normalizedContent);
        message.setMsgType(normalizedType);
        message.setIsRead(0);
        message.setSenderDeleted(0);
        message.setReceiverDeleted(0);
        message.setSendTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        return message;
    }

    @Override
    public Page<SysChatMessage> getChatHistory(Long userId,
                                               Long targetId,
                                               Integer page,
                                               Integer pageSize) {
        requirePositiveId(userId, "用户ID无效");
        if (userId.equals(targetId)) {
            throw new BusinessException(400, "不能查询与自己的聊天记录");
        }
        requireActiveUser(targetId, "聊天对象不存在或已停用");

        Page<SysChatMessage> pageParam = new Page<>(
                normalizePage(page),
                normalizePageSize(pageSize));
        LambdaQueryWrapper<SysChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(conversation -> conversation
                .and(outbound -> outbound
                        .eq(SysChatMessage::getSenderId, userId)
                        .eq(SysChatMessage::getReceiverId, targetId)
                        .eq(SysChatMessage::getSenderDeleted, 0))
                .or(inbound -> inbound
                        .eq(SysChatMessage::getSenderId, targetId)
                        .eq(SysChatMessage::getReceiverId, userId)
                        .eq(SysChatMessage::getReceiverDeleted, 0)));
        wrapper.orderByDesc(SysChatMessage::getId);
        return chatMessageMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public List<ChatContact> getRecentContacts(Long userId) {
        requirePositiveId(userId, "用户ID无效");
        List<ChatContact> contacts = new ArrayList<>();
        for (SysChatMessage message : chatMessageMapper.selectVisibleRecentMessages(userId)) {
            Long contactId = userId.equals(message.getSenderId())
                    ? message.getReceiverId()
                    : message.getSenderId();
            SysUser contact = userMapper.selectById(contactId);
            if (contact == null || !Integer.valueOf(1).equals(contact.getStatus())) {
                continue;
            }
            contacts.add(ChatContact.builder()
                    .userId(contactId)
                    .username(contact.getUsername())
                    .nickname(displayName(contact))
                    .avatar(contact.getAvatar())
                    .lastMessage(message.getContent())
                    .lastMessageType(message.getMsgType())
                    .lastMessageTime(message.getSendTime())
                    .unreadCount(getUnreadCountWithUser(userId, contactId))
                    .blocked(blacklistService.isInMyBlacklist(userId, contactId))
                    .online(false)
                    .build());
        }
        return contacts;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long senderId) {
        requirePositiveId(userId, "用户ID无效");
        requirePositiveId(senderId, "发送者ID无效");
        LambdaUpdateWrapper<SysChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysChatMessage::getReceiverId, userId)
                .eq(SysChatMessage::getSenderId, senderId)
                .eq(SysChatMessage::getIsRead, 0)
                .eq(SysChatMessage::getReceiverDeleted, 0)
                .set(SysChatMessage::getIsRead, 1);
        chatMessageMapper.update(null, wrapper);
    }

    @Override
    public int getUnreadCount(Long userId) {
        requirePositiveId(userId, "用户ID无效");
        return chatMessageMapper.selectUnreadCount(userId);
    }

    @Override
    public int getUnreadCountWithUser(Long userId, Long senderId) {
        requirePositiveId(userId, "用户ID无效");
        requirePositiveId(senderId, "发送者ID无效");
        return chatMessageMapper.selectUnreadCountWithUser(userId, senderId);
    }

    @Override
    public SysChatMessage getLatestMessage(Long userId, Long targetId) {
        requirePositiveId(userId, "用户ID无效");
        requirePositiveId(targetId, "聊天对象ID无效");
        return chatMessageMapper.selectLatestMessage(userId, targetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearHistory(Long userId, Long targetId) {
        requirePositiveId(userId, "用户ID无效");
        requirePositiveId(targetId, "聊天对象ID无效");
        if (userId.equals(targetId)) {
            throw new BusinessException(400, "聊天对象ID无效");
        }

        chatMessageMapper.hideSentHistory(userId, targetId);
        chatMessageMapper.hideReceivedHistory(userId, targetId);
    }

    private SysUser requireActiveUser(Long userId, String message) {
        requirePositiveId(userId, message);
        SysUser user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(404, message);
        }
        return user;
    }

    private void requirePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, message);
        }
    }

    private int normalizeMessageType(Integer msgType, boolean allowSystem) {
        int value = msgType == null ? TEXT_MESSAGE : msgType;
        int maximum = allowSystem ? 4 : FILE_MESSAGE;
        if (value < TEXT_MESSAGE || value > maximum) {
            throw new BusinessException(400, "消息类型无效");
        }
        return value;
    }

    private String normalizeContent(String content, int msgType) {
        if (content == null) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        String normalized = content.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(400, "消息内容不能为空");
        }
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

    private int normalizePage(Integer page) {
        if (page == null) {
            return 1;
        }
        if (page < 1) {
            throw new BusinessException(400, "页码必须大于0");
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        int value = pageSize == null ? 20 : pageSize;
        if (value < 1 || value > MAX_PAGE_SIZE) {
            throw new BusinessException(400, "每页数量必须在1到100之间");
        }
        return value;
    }

    private String displayName(SysUser user) {
        return user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername()
                : user.getNickname();
    }
}
