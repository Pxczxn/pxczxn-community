package top.pxczxn.community.admin.query;

import top.pxczxn.community.admin.application.AdminCommunityDailyMetricView;

import java.time.LocalDate;

public record AdminCommunityDailyMetricResponse(
        LocalDate date,
        long userCount,
        long articleCount
) {

    static AdminCommunityDailyMetricResponse from(
            AdminCommunityDailyMetricView view
    ) {
        return new AdminCommunityDailyMetricResponse(
                view.date(),
                view.userCount(),
                view.articleCount()
        );
    }
}
