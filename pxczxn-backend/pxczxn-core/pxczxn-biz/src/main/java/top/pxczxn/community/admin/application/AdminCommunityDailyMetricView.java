package top.pxczxn.community.admin.application;

import java.time.LocalDate;

public record AdminCommunityDailyMetricView(
        LocalDate date,
        long userCount,
        long articleCount
) {
}
