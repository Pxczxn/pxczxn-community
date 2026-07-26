package top.pxczxn.platform.message.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前用户最近私聊联系人摘要。
 */
@Data
@Builder
public class ChatContact {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String lastMessage;
    private Integer lastMessageType;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Boolean blocked;
    private Boolean online;
}
