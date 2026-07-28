package top.pxczxn.community.admin.application;

import java.util.List;

public record AdminCommunityDashboardView(
        long userCount,
        long activeUserCount,
        long blogCount,
        long articleCount,
        long publishedArticleCount,
        long scheduledArticleCount,
        long publishFailedCount,
        long pendingReviewCount,
        List<AdminCommunityDailyMetricView> dailyMetrics,
        long pendingReportCount,
        long pendingAppealCount,
        long activeSanctionCount,
        long rejectedAbuseCount,
        long averageReportResolutionMinutes,
        List<AdminCommunityGovernanceDailyMetricView> governanceDailyMetrics,
        List<AdminCommunityGovernanceAuditView> recentGovernanceAudits
) {
}
