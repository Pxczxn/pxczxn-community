package top.pxczxn.community.admin.governance;

import top.pxczxn.community.admin.application.AdminCommunityCommentDetailView;

import java.util.List;

public record AdminCommunityCommentDetailResponse(
        AdminCommunityCommentResponse comment,
        List<AdminGovernanceEventResponse> events
) {

    static AdminCommunityCommentDetailResponse from(
            AdminCommunityCommentDetailView view
    ) {
        return new AdminCommunityCommentDetailResponse(
                AdminCommunityCommentResponse.from(view.comment()),
                view.events().stream()
                        .map(AdminGovernanceEventResponse::from)
                        .toList()
        );
    }
}
