package top.pxczxn.community.team.application;

public record TeamSummaryView(Long teamId, Long blogId, String name, String slug, String summary,
                              Long avatarFileId, Long backgroundFileId, long articleCount, long followerCount) { }
