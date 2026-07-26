package top.pxczxn.community.moderation.application;

import java.time.LocalDateTime;

public record AdminArticleReviewQuery(
        String status,
        String riskLevel,
        LocalDateTime submittedFrom,
        LocalDateTime submittedTo,
        int pageNum,
        int pageSize
) {
}
