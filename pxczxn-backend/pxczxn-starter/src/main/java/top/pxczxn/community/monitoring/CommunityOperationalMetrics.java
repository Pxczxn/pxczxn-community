package top.pxczxn.community.monitoring;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.pxczxn.community.moderation.model.ContentReviewTask;
import top.pxczxn.community.moderation.persistence.ContentReviewTaskMapper;
import top.pxczxn.community.notification.model.CommunityNotificationRecipient;
import top.pxczxn.community.notification.persistence.CommunityNotificationRecipientMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publishes operational aggregates without adding user, IP, or content labels to Prometheus.
 */
@Slf4j
@Component
public class CommunityOperationalMetrics {

    private static final List<String> OPEN_REVIEW_STATUSES = List.of(
            "QUEUED", "AUTO_REVIEWING", "MANUAL_REVIEWING"
    );

    private final ContentReviewTaskMapper reviewTaskMapper;
    private final CommunityNotificationRecipientMapper notificationRecipientMapper;
    private final AtomicLong reviewQueueDepth = new AtomicLong();
    private final AtomicLong reviewQueueOldestSeconds = new AtomicLong();
    private final AtomicLong notificationUnreadDepth = new AtomicLong();

    public CommunityOperationalMetrics(
            ContentReviewTaskMapper reviewTaskMapper,
            CommunityNotificationRecipientMapper notificationRecipientMapper,
            MeterRegistry meterRegistry
    ) {
        this.reviewTaskMapper = reviewTaskMapper;
        this.notificationRecipientMapper = notificationRecipientMapper;
        Gauge.builder("pxczxn.community.review.queue.depth", reviewQueueDepth, AtomicLong::get)
                .description("Open content review tasks awaiting completion")
                .register(meterRegistry);
        Gauge.builder("pxczxn.community.review.queue.oldest.seconds", reviewQueueOldestSeconds, AtomicLong::get)
                .description("Age in seconds of the oldest open content review task")
                .register(meterRegistry);
        Gauge.builder("pxczxn.community.notification.unread.depth", notificationUnreadDepth, AtomicLong::get)
                .description("Unread community notification recipients")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${pxczxn.community.monitoring.metrics-refresh-interval:30s}")
    public void refresh() {
        try {
            reviewQueueDepth.set(reviewTaskMapper.selectCount(
                    Wrappers.<ContentReviewTask>lambdaQuery()
                            .in(ContentReviewTask::getStatus, OPEN_REVIEW_STATUSES)
            ));
            reviewQueueOldestSeconds.set(oldestReviewAgeSeconds(
                    reviewTaskMapper.selectOldestOpenTaskSubmittedAt()
            ));
            notificationUnreadDepth.set(notificationRecipientMapper.selectCount(
                    Wrappers.<CommunityNotificationRecipient>lambdaQuery()
                            .eq(CommunityNotificationRecipient::getStatus, "UNREAD")
            ));
        } catch (RuntimeException exception) {
            log.warn("Unable to refresh community operational metrics", exception);
        }
    }

    private static long oldestReviewAgeSeconds(LocalDateTime submittedAt) {
        if (submittedAt == null) {
            return 0;
        }
        return Math.max(0, Duration.between(submittedAt, LocalDateTime.now(ZoneOffset.UTC)).getSeconds());
    }
}
