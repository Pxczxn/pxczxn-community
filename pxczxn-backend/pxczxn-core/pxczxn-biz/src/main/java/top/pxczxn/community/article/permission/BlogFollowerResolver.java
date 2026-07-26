package top.pxczxn.community.article.permission;

/**
 * Extension point used by FOLLOWERS_ONLY content. Until the relationship module
 * is installed, an empty resolver list safely denies follower-only access.
 */
@FunctionalInterface
public interface BlogFollowerResolver {

    boolean isFollower(Long userId, Long blogId);
}
