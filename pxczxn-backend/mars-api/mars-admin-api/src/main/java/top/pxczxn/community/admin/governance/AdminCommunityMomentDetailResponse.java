package top.pxczxn.community.admin.governance;

import top.pxczxn.community.admin.application.AdminCommunityMomentDetailView;

import java.util.List;

public record AdminCommunityMomentDetailResponse(
        AdminCommunityMomentResponse moment,
        List<AdminGovernanceEventResponse> events
) {

    static AdminCommunityMomentDetailResponse from(
            AdminCommunityMomentDetailView view
    ) {
        return new AdminCommunityMomentDetailResponse(
                AdminCommunityMomentResponse.from(view.moment()),
                view.events().stream()
                        .map(AdminGovernanceEventResponse::from)
                        .toList()
        );
    }
}
