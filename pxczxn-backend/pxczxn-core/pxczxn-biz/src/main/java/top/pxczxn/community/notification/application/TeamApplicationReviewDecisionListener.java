package top.pxczxn.community.notification.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener for team application review decisions.
 * Sends notifications after the review transaction commits.
 * Notification failures are logged but do not affect the review outcome.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamApplicationReviewDecisionListener {

    private final TeamApplicationReviewDecisionNotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecision(TeamApplicationReviewDecisionEvent event) {
        try {
            notificationService.create(event);
        } catch (RuntimeException exception) {
            log.error(
                    "Team application review notification failed after decision commit: " +
                    "applicationId={}, decision={}, applicantUserId={}",
                    event.applicationId(),
                    event.decision(),
                    event.applicantUserId(),
                    exception
            );
        }
    }
}
