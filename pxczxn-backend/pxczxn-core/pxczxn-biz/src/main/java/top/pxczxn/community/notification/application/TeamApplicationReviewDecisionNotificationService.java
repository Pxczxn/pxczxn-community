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

/**
 * Service for creating team application review decision notifications.
 * Uses REQUIRES_NEW propagation to ensure notification failures don't rollback the review transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamApplicationReviewDecisionNotificationService {

    private final CommunityNotificationMapper notificationMapper;
    private final CommunityNotificationRecipientMapper recipientMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(TeamApplicationReviewDecisionEvent event) {
        String deduplicationKey = "team-application-review:" + event.applicationId()
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
        notification.setNotificationType("TEAM_APPLICATION_REVIEW");
        notification.setCategory("TEAM");
        notification.setImportance("HIGH");
        notification.setTitle(decisionTitle(event.decision()));
        notification.setContent(buildContent(event));
        notification.setTargetType("TEAM_APPLICATION");
        notification.setTargetId(event.applicationId());
        notification.setDeduplicationKey(deduplicationKey);
        notification.setPayloadJson(buildPayload(event));
        notification.setAggregateCount(1);
        notification.setLastActivityAt(now);
        notification.setCreatedAt(now);

        if (notificationMapper.insert(notification) != 1) {
            throw new IllegalStateException("Failed to create team application review notification");
        }

        CommunityNotificationRecipient recipient = new CommunityNotificationRecipient();
        recipient.setId(IdWorker.getId());
        recipient.setNotificationId(notification.getId());
        recipient.setRecipientUserId(event.applicantUserId());
        recipient.setStatus("UNREAD");
        recipient.setCreatedAt(now);

        if (recipientMapper.insert(recipient) != 1) {
            throw new IllegalStateException("Failed to create team application review notification recipient");
        }

        log.info(
                "Team application review notification created: applicationId={}, decision={}, applicantUserId={}",
                event.applicationId(),
                event.decision(),
                event.applicantUserId()
        );
    }

    private static String decisionTitle(String decision) {
        return switch (decision) {
            case "APPROVED" -> "团队申请已通过";
            case "REJECTED" -> "团队申请未通过";
            default -> "团队申请审核结果";
        };
    }

    private static String buildContent(TeamApplicationReviewDecisionEvent event) {
        String teamName = normalizeText(event.teamName(), "未命名团队", 100);
        String reviewComment = normalizeText(event.reviewComment(), "", 500);

        String result = switch (event.decision()) {
            case "APPROVED" -> "已通过审核，团队已创建成功";
            case "REJECTED" -> "未通过审核";
            default -> "审核状态已更新";
        };

        return reviewComment.isBlank()
                ? "您的团队申请「" + teamName + "」" + result + "。"
                : "您的团队申请「" + teamName + "」" + result + "。审核说明：" + reviewComment;
    }

    private static String buildPayload(TeamApplicationReviewDecisionEvent event) {
        StringBuilder json = new StringBuilder();
        json.append("{\"applicationId\":").append(event.applicationId());
        json.append(",\"decision\":\"").append(event.decision()).append("\"");
        json.append(",\"teamName\":\"").append(escapeJson(event.teamName())).append("\"");
        if (event.teamId() != null) {
            json.append(",\"teamId\":").append(event.teamId());
        }
        json.append("}");
        return json.toString();
    }

    private static String normalizeText(String value, String fallback, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            normalized = fallback;
        }
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
