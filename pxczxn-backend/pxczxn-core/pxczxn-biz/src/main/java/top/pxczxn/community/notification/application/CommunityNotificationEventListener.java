package top.pxczxn.community.notification.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import top.pxczxn.community.article.application.ArticlePublishedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityNotificationEventListener {

    private final CommunityNotificationDispatchService dispatchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDirect(CommunityNotificationEvent event) {
        safely(
                () -> dispatchService.createDirect(event),
                event == null ? null : event.notificationType(),
                event == null ? null : event.targetId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticlePublished(ArticlePublishedEvent event) {
        safely(
                () -> dispatchService.createArticlePublished(event),
                "ARTICLE_PUBLISHED",
                event == null ? null : event.articleId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMomentPublished(MomentPublishedNotificationEvent event) {
        safely(
                () -> dispatchService.createMomentPublished(event),
                "MOMENT_PUBLISHED",
                event == null ? null : event.momentId()
        );
    }

    private void safely(
            Runnable action,
            String notificationType,
            Long targetId
    ) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.error(
                    "Community notification failed after business commit: type={}, targetId={}",
                    notificationType,
                    targetId,
                    exception
            );
        }
    }
}
