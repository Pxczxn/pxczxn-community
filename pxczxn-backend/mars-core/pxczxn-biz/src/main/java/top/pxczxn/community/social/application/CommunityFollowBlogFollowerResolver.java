package top.pxczxn.community.social.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.pxczxn.community.article.permission.BlogFollowerResolver;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;

@Component
@RequiredArgsConstructor
public class CommunityFollowBlogFollowerResolver implements BlogFollowerResolver {

    private static final String TARGET_BLOG = "BLOG";

    private final CommunityFollowMapper followMapper;

    @Override
    public boolean isFollower(Long userId, Long blogId) {
        return userId != null
                && blogId != null
                && followMapper.findRelation(userId, TARGET_BLOG, blogId) != null;
    }
}
