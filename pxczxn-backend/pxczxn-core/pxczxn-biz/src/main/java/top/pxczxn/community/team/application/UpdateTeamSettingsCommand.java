package top.pxczxn.community.team.application;

/**
 * Team profile update. Team slug is intentionally immutable in V1 so existing public URLs never break.
 */
public record UpdateTeamSettingsCommand(String name, String summary, Long avatarFileId, Long backgroundFileId) {
}
