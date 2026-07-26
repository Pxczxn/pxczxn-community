package top.pxczxn.community.social.application;

public record CommentAuthorView(
        Long userId,
        String username,
        String displayName,
        Long avatarFileId
) {
}
