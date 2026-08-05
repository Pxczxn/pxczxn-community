package top.pxczxn.community.team.application;

import java.util.List;

public record TeamWorkspaceView(TeamPortalView team, String viewerRole, List<String> capabilities, List<String> permissions) { }
