package top.pxczxn.community.chat.application;

/**
 * Raised after a direct chat message has been persisted. Delivery listeners run after commit.
 */
public record CommunityChatMessageSentEvent(CommunityChatMessageView message) {
}
