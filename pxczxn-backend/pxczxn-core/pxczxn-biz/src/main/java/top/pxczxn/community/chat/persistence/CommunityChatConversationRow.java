package top.pxczxn.community.chat.persistence;

import java.time.LocalDateTime;

public record CommunityChatConversationRow(
        Long peerUserId,
        String peerUsername,
        String peerDisplayName,
        Long peerAvatarFileId,
        String lastMessage,
        LocalDateTime lastMessageAt,
        Long unreadCount
) {
}
