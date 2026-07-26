package top.pxczxn.community.social.application;

public record MomentAuthorView(
        Long userId,
        String username,
        String displayName,
        Long avatarFileId
) {
}
