package top.pxczxn.community.admin.application;

import java.util.List;

public record AdminCommunityCommentDetailView(
        AdminCommunityCommentView comment,
        List<AdminGovernanceEventView> events
) {
}
