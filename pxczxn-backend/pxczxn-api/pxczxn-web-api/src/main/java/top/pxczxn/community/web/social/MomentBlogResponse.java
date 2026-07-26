package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.MomentBlogView;

public record MomentBlogResponse(
        String blogId,
        String blogType,
        String name,
        String slug,
        String avatarFileId
) {

    static MomentBlogResponse from(MomentBlogView view) {
        if (view == null) {
            return null;
        }
        return new MomentBlogResponse(
                id(view.blogId()),
                view.blogType(),
                view.name(),
                view.slug(),
                id(view.avatarFileId())
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}
