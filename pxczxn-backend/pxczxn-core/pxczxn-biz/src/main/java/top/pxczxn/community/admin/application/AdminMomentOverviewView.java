package top.pxczxn.community.admin.application;

/**
 * 动态管理概览统计：仅聚合现有数据，不新增表。
 */
public record AdminMomentOverviewView(
        long totalMoments,
        long todayNew,
        long pendingReview,
        long takenDown,
        long todayInteractions
) {
}
