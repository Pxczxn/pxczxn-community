package top.pxczxn.community.admin.query;

import top.pxczxn.community.admin.application.AdminCommunityDashboardView;

import java.util.List;

public record AdminCommunityDashboardResponse(
        long userCount,
        long activeUserCount,
        long blogCount,
        long articleCount,
        long publishedArticleCount,
        long scheduledArticleCount,
        long publishFailedCount,
        long pendingReviewCount,
        List<AdminCommunityDailyMetricResponse> dailyMetrics
) {

    static AdminCommunityDashboardResponse from(
            AdminCommunityDashboardView view
    ) {
        return new AdminCommunityDashboardResponse(
                view.userCount(),
                view.activeUserCount(),
                view.blogCount(),
                view.articleCount(),
                view.publishedArticleCount(),
                view.scheduledArticleCount(),
                view.publishFailedCount(),
                view.pendingReviewCount(),
                view.dailyMetrics().stream()
                        .map(AdminCommunityDailyMetricResponse::from)
                        .toList()
        );
    }
}
