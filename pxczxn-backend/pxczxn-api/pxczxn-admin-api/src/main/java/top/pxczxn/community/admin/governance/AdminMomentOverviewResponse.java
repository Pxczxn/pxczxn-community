package top.pxczxn.community.admin.governance;

import top.pxczxn.community.admin.application.AdminMomentOverviewView;

public record AdminMomentOverviewResponse(
        long totalMoments,
        long todayNew,
        long pendingReview,
        long takenDown,
        long todayInteractions
) {

    static AdminMomentOverviewResponse from(AdminMomentOverviewView view) {
        return new AdminMomentOverviewResponse(
                view.totalMoments(),
                view.todayNew(),
                view.pendingReview(),
                view.takenDown(),
                view.todayInteractions()
        );
    }
}
