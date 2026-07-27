package top.pxczxn.community.team.application;

import java.util.List;

public record TeamPortalView(TeamSummaryView team, String ownerDisplayName, List<TeamPortalMemberView> members) { }
