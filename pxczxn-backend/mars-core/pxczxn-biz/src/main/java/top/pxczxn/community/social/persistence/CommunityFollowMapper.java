package top.pxczxn.community.social.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.social.model.CommunityFollow;

public interface CommunityFollowMapper extends BaseMapper<CommunityFollow> {

    @Select("""
            SELECT *
            FROM community_follow
            WHERE follower_user_id = #{followerUserId}
              AND target_type = #{targetType}
              AND target_id = #{targetId}
            LIMIT 1
            """)
    CommunityFollow findRelation(
            @Param("followerUserId") Long followerUserId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Delete("""
            DELETE FROM community_follow
            WHERE follower_user_id = #{followerUserId}
              AND target_type = #{targetType}
              AND target_id = #{targetId}
            """)
    int deleteRelation(
            @Param("followerUserId") Long followerUserId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Select("""
            SELECT COUNT(*)
            FROM community_follow
            WHERE follower_user_id = #{followerUserId}
              AND target_type = #{targetType}
            """)
    long countFollowing(
            @Param("followerUserId") Long followerUserId,
            @Param("targetType") String targetType
    );

    @Select("""
            SELECT COUNT(*)
            FROM community_follow
            WHERE target_type = #{targetType}
              AND target_id = #{targetId}
            """)
    long countFollowers(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Select("""
            SELECT COUNT(*)
            FROM community_follow outbound
            INNER JOIN blog target_blog
                    ON target_blog.id = outbound.target_id
                   AND target_blog.blog_type = 'PERSONAL'
                   AND target_blog.status = 'ACTIVE'
                   AND target_blog.deleted_at IS NULL
            INNER JOIN community_follow inbound
                    ON inbound.follower_user_id = target_blog.owner_user_id
                   AND inbound.target_type = 'BLOG'
                   AND inbound.target_id = #{personalBlogId}
            WHERE outbound.follower_user_id = #{userId}
              AND outbound.target_type = 'BLOG'
            """)
    long countMutualBlogs(
            @Param("userId") Long userId,
            @Param("personalBlogId") Long personalBlogId
    );
}
