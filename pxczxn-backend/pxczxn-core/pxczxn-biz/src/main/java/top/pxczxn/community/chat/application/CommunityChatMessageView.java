package top.pxczxn.community.chat.application;

import top.pxczxn.community.chat.model.CommunityChatMessage;

import java.time.LocalDateTime;

public record CommunityChatMessageView(
        Long id,
        Long senderUserId,
        Long recipientUserId,
        String contentText,
        String status,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {

    public static CommunityChatMessageView from(CommunityChatMessage message) {
        return new CommunityChatMessageView(
                message.getId(),
                message.getSenderUserId(),
                message.getRecipientUserId(),
                message.getContentText(),
                message.getStatus(),
                message.getReadAt(),
                message.getCreatedAt()
        );
    }
}
