package top.pxczxn.community.admin.application;

import java.time.LocalDate;

public record AdminCommunityGovernanceDailyMetricView(
        LocalDate date,
        long reportCount,
        long sanctionCount,
        long abuseRejectCount
) {
}
