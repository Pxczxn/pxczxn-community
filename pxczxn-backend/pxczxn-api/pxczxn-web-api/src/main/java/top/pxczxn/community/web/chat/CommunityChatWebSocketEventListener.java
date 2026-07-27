package top.pxczxn.community.web.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import top.pxczxn.community.chat.application.CommunityChatMessageSentEvent;
import top.pxczxn.community.chat.application.CommunityChatMessageView;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityChatWebSocketEventListener {

    private final CommunityChatWebSocketHandler handler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(CommunityChatMessageSentEvent event) {
        if (event == null || event.message() == null) {
            return;
        }
        try {
            CommunityChatMessageView message = event.message();
            handler.sendChatEvent(message.senderUserId(), message);
            handler.sendChatEvent(message.recipientUserId(), message);
        } catch (RuntimeException exception) {
            log.warn("Community chat WebSocket delivery failed after commit", exception);
        }
    }
}
