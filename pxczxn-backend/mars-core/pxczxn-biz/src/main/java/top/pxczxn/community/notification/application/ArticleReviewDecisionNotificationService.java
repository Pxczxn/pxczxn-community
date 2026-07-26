package top.pxczxn.community.notification.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.notification.model.CommunityNotification;
import top.pxczxn.community.notification.model.CommunityNotificationRecipient;
import top.pxczxn.community.notification.persistence.CommunityNotificationMapper;
import top.pxczxn.community.notification.persistence.CommunityNotificationRecipientMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleReviewDecisionNotificationService {

    private final CommunityNotificationMapper notificationMapper;
    private final CommunityNotificationRecipientMapper recipientMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(ArticleReviewDecisionNotificationEvent event) {
        String deduplicationKey = "article-review:" + event.taskId()
                + ":" + event.decision();
        Long existing = notificationMapper.selectCount(
                Wrappers.<CommunityNotification>lambdaQuery()
                        .eq(
                                CommunityNotification::getDeduplicationKey,
                                deduplicationKey
                        )
        );
        if (existing != null && existing > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        CommunityNotification notification = new CommunityNotification();
        notification.setId(IdWorker.getId());
        notification.setNotificationType("REVIEW");
        notification.setCategory("REVIEW");
        notification.setImportance("HIGH");
        notification.setTitle(decisionTitle(event.decision()));
        notification.setContent(buildContent(event));
        notification.setTargetType("ARTICLE");
        notification.setTargetId(event.articleId());
        notification.setDeduplicationKey(deduplicationKey);
        notification.setPayloadJson(
                "{\"taskId\":\"" + event.taskId()
                        + "\",\"articleId\":\"" + event.articleId()
                        + "\",\"decision\":\"" + event.decision() + "\"}"
        );
        notification.setAggregateCount(1);
        notification.setLastActivityAt(now);
        notification.setCreatedAt(now);
        if (notificationMapper.insert(notification) != 1) {
            throw new IllegalStateException("Failed to create review notification");
        }

        CommunityNotificationRecipient recipient =
                new CommunityNotificationRecipient();
        recipient.setId(IdWorker.getId());
        recipient.setNotificationId(notification.getId());
        recipient.setRecipientUserId(event.recipientUserId());
        recipient.setStatus("UNREAD");
        recipient.setCreatedAt(now);
        if (recipientMapper.insert(recipient) != 1) {
            throw new IllegalStateException(
                    "Failed to create review notification recipient"
            );
        }
        log.info(
                "Review notification created: taskId={}, articleId={}, decision={}",
                event.taskId(),
                event.articleId(),
                event.decision()
        );
    }

    private static String decisionTitle(String decision) {
        return switch (decision) {
            case "APPROVED" -> "文章审核通过";
            case "REVISION_REQUIRED" -> "文章需要修改";
            case "REJECTED" -> "文章审核未通过";
            default -> "文章审核结果";
        };
    }

    private static String buildContent(
            ArticleReviewDecisionNotificationEvent event
    ) {
        String title = normalizeText(event.articleTitle(), "未命名文章", 120);
        String reason = normalizeText(event.reason(), "", 700);
        String result = switch (event.decision()) {
            case "APPROVED" -> "已通过审核";
            case "REVISION_REQUIRED" -> "需要修改后重新提交";
            case "REJECTED" -> "未通过审核";
            default -> "审核状态已更新";
        };
        return reason.isBlank()
                ? "《" + title + "》" + result + "。"
                : "《" + title + "》" + result + "。审核说明：" + reason;
    }

    private static String normalizeText(
            String value,
            String fallback,
            int maxLength
    ) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            normalized = fallback;
        }
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
