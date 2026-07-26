package top.pxczxn.community.social.application;

import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogSetting;
import top.pxczxn.community.user.model.CommunityUser;

public record CommentActorContext(
        CommunityUser actor,
        Blog blog,
        BlogSetting setting
) {
}
