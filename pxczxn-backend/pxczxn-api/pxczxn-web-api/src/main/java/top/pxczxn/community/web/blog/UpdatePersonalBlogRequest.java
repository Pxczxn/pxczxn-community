package top.pxczxn.community.web.blog;

public record UpdatePersonalBlogRequest(
        String name,
        String slug,
        String summary,
        Long avatarFileId,
        Boolean clearAvatar,
        Long backgroundFileId,
        Boolean clearBackground
) {
}
