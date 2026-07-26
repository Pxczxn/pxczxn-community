package top.pxczxn.community.notification.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleReviewDecisionNotificationListener {

    private final ArticleReviewDecisionNotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecision(ArticleReviewDecisionNotificationEvent event) {
        try {
            notificationService.create(event);
        } catch (RuntimeException exception) {
            log.error(
                    "Review notification failed after decision commit: taskId={}, articleId={}, decision={}",
                    event.taskId(),
                    event.articleId(),
                    event.decision(),
                    exception
            );
        }
    }
}
