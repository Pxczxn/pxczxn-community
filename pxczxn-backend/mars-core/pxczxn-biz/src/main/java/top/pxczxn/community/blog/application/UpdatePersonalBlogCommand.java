package top.pxczxn.community.blog.application;

public record UpdatePersonalBlogCommand(
        String name,
        String slug,
        String summary,
        Long avatarFileId,
        boolean clearAvatar,
        Long backgroundFileId,
        boolean clearBackground
) {
}
