package top.pxczxn.community.chat.application;

import java.time.LocalDateTime;

public record CommunityChatConversationView(
        Long peerUserId,
        String peerUsername,
        String peerDisplayName,
        Long peerAvatarFileId,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
}
