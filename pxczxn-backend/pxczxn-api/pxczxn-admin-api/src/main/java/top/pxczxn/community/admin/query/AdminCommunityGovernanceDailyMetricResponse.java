package top.pxczxn.community.admin.query;

import top.pxczxn.community.admin.application.AdminCommunityGovernanceDailyMetricView;
import java.time.LocalDate;

public record AdminCommunityGovernanceDailyMetricResponse(LocalDate date, long reportCount, long sanctionCount, long abuseRejectCount) {
    static AdminCommunityGovernanceDailyMetricResponse from(AdminCommunityGovernanceDailyMetricView view) { return new AdminCommunityGovernanceDailyMetricResponse(view.date(), view.reportCount(), view.sanctionCount(), view.abuseRejectCount()); }
}
