package top.pxczxn.community.team.application;

public record TeamPortalMemberView(Long userId, String displayName, String username, Long avatarFileId,
                                   String roleCode) { }
