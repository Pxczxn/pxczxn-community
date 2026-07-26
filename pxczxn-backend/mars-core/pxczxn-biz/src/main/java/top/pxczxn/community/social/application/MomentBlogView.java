package top.pxczxn.community.social.application;

public record MomentBlogView(
        Long blogId,
        String blogType,
        String name,
        String slug,
        Long avatarFileId
) {
}
