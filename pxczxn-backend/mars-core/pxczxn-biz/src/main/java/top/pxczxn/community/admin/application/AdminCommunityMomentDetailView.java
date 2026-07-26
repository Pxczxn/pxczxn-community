package top.pxczxn.community.admin.application;

import java.util.List;

public record AdminCommunityMomentDetailView(
        AdminCommunityMomentView moment,
        List<AdminGovernanceEventView> events
) {
}
