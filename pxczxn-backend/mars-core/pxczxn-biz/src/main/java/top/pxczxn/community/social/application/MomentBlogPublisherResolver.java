package top.pxczxn.community.social.application;

import top.pxczxn.community.blog.model.Blog;

public interface MomentBlogPublisherResolver {

    boolean canPublish(Long actorUserId, Blog blog);
}
