package top.pxczxn.platform.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.pxczxn.platform.message.entity.SysChatMessage;
import top.pxczxn.platform.message.model.ChatContact;

import java.util.List;

/**
 * 聊天消息服务接口
 */
public interface SysChatMessageService {

    /**
     * 发送消息
     */
    SysChatMessage send(Long senderId, Long receiverId, String content, Integer msgType);

    /**
     * 获取聊天记录（两人之间）
     */
    Page<SysChatMessage> getChatHistory(Long userId, Long targetId, Integer page, Integer pageSize);

    /**
     * 获取最近联系人列表
     */
    List<ChatContact> getRecentContacts(Long userId);

    /**
     * 标记消息为已读
     */
    void markAsRead(Long userId, Long senderId);

    /**
     * 获取未读消息数量
     */
    int getUnreadCount(Long userId);

    /**
     * 获取与某人的未读消息数量
     */
    int getUnreadCountWithUser(Long userId, Long senderId);
    
    /**
     * 获取与某人的最新一条消息
     */
    SysChatMessage getLatestMessage(Long userId, Long targetId);
    
    /**
     * 清空与某人的聊天记录
     */
    void clearHistory(Long userId, Long targetId);
}
