package top.pxczxn.community.team.application;

/**
 * Team profile + portal settings update. Team slug is intentionally immutable in V1 so existing
 * public URLs never break. Contract: full replace — callers send the complete desired profile.
 */
public record UpdateTeamSettingsCommand(
        String name,
        String summary,
        Long avatarFileId,
        Long backgroundFileId,
        String category,
        String contentDirection,
        String theme,
        String seoTitle,
        String seoDescription,
        Boolean publicMembers,
        Boolean allowSubmissions,
        String submissionGuideline,
        String contactInfo) {
}
