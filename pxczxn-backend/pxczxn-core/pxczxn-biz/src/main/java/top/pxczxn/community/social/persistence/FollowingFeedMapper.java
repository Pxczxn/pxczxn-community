package top.pxczxn.community.social.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowingFeedMapper {
    @Select("""
            SELECT * FROM (
              SELECT 'ARTICLE' item_type, a.id target_id, a.author_user_id, a.blog_id, a.title, a.summary excerpt,
                     CONCAT('/articles/', a.id) canonical_path, COALESCE(u.display_name, u.username) author_name, b.name blog_name,
                     NULL tag_name, a.published_at occurred_at
              FROM community_follow f JOIN blog b ON b.id=f.target_id AND b.status='ACTIVE' AND b.deleted_at IS NULL
              JOIN article a ON a.blog_id=b.id AND a.deleted_at IS NULL AND a.visibility='PUBLIC' AND a.publish_status='PUBLISHED' AND a.published_version_id IS NOT NULL
              JOIN community_user u ON u.id=a.author_user_id AND u.status IN ('NORMAL','LIMITED')
              WHERE f.follower_user_id=#{userId} AND f.target_type='BLOG'
              UNION ALL
              SELECT 'MOMENT', m.id, m.actor_user_id, m.blog_id, CONCAT(COALESCE(u.display_name,u.username),' 的动态'), m.text_content,
                     CONCAT('/moments/',m.id), COALESCE(u.display_name,u.username), b.name, NULL, m.created_at
              FROM community_follow f JOIN blog b ON b.id=f.target_id AND b.status='ACTIVE' AND b.deleted_at IS NULL
              JOIN community_moment m ON m.blog_id=b.id AND m.deleted_at IS NULL AND m.visibility='PUBLIC' AND m.status='PUBLISHED'
              JOIN community_user u ON u.id=m.actor_user_id AND u.status IN ('NORMAL','LIMITED')
              WHERE f.follower_user_id=#{userId} AND f.target_type='BLOG'
              UNION ALL
              SELECT 'SERIES', s.id, s.created_by_user_id, b.id, s.title, s.summary, CONCAT('/series/',s.id),
                     COALESCE(creator.display_name,creator.username), b.name, NULL, s.published_at
              FROM community_follow f JOIN blog b ON b.id=f.target_id AND b.status='ACTIVE' AND b.deleted_at IS NULL
              JOIN series s ON s.blog_id=b.id AND s.deleted_at IS NULL AND s.review_status='APPROVED'
              LEFT JOIN community_user creator ON creator.id=s.created_by_user_id
              WHERE f.follower_user_id=#{userId} AND f.target_type='BLOG'
              UNION ALL
              SELECT 'TAG_ARTICLE', a.id, a.author_user_id, a.blog_id, a.title, a.summary, CONCAT('/articles/',a.id),
                     COALESCE(u.display_name,u.username), b.name, tag.name, a.published_at
              FROM community_follow f JOIN platform_tag tag ON tag.id=f.target_id AND tag.status='ACTIVE' AND tag.merged_to_tag_id IS NULL
              JOIN article_tag atag ON atag.tag_id=tag.id
              JOIN article a ON a.id=atag.article_id AND a.deleted_at IS NULL AND a.visibility='PUBLIC' AND a.publish_status='PUBLISHED' AND a.published_version_id IS NOT NULL
              JOIN blog b ON b.id=a.blog_id AND b.status='ACTIVE' AND b.deleted_at IS NULL
              JOIN community_user u ON u.id=a.author_user_id AND u.status IN ('NORMAL','LIMITED')
              WHERE f.follower_user_id=#{userId} AND f.target_type='TAG'
            ) feed
            ORDER BY occurred_at DESC, target_id DESC LIMIT #{offset}, #{pageSize}
            """)
    List<FollowingFeedRow> selectPage(@Param("userId") Long userId, @Param("offset") long offset, @Param("pageSize") int pageSize);

    @Select("""
            SELECT COUNT(*) FROM (
              SELECT a.id FROM community_follow f JOIN blog b ON b.id=f.target_id AND b.status='ACTIVE' AND b.deleted_at IS NULL JOIN article a ON a.blog_id=b.id AND a.deleted_at IS NULL AND a.visibility='PUBLIC' AND a.publish_status='PUBLISHED' AND a.published_version_id IS NOT NULL JOIN community_user u ON u.id=a.author_user_id AND u.status IN ('NORMAL','LIMITED') WHERE f.follower_user_id=#{userId} AND f.target_type='BLOG'
              UNION ALL SELECT m.id FROM community_follow f JOIN blog b ON b.id=f.target_id AND b.status='ACTIVE' AND b.deleted_at IS NULL JOIN community_moment m ON m.blog_id=b.id AND m.deleted_at IS NULL AND m.visibility='PUBLIC' AND m.status='PUBLISHED' JOIN community_user u ON u.id=m.actor_user_id AND u.status IN ('NORMAL','LIMITED') WHERE f.follower_user_id=#{userId} AND f.target_type='BLOG'
              UNION ALL SELECT s.id FROM community_follow f JOIN blog b ON b.id=f.target_id AND b.status='ACTIVE' AND b.deleted_at IS NULL JOIN series s ON s.blog_id=b.id AND s.deleted_at IS NULL AND s.review_status='APPROVED' WHERE f.follower_user_id=#{userId} AND f.target_type='BLOG'
              UNION ALL SELECT a.id FROM community_follow f JOIN platform_tag tag ON tag.id=f.target_id AND tag.status='ACTIVE' AND tag.merged_to_tag_id IS NULL JOIN article_tag atag ON atag.tag_id=tag.id JOIN article a ON a.id=atag.article_id AND a.deleted_at IS NULL AND a.visibility='PUBLIC' AND a.publish_status='PUBLISHED' AND a.published_version_id IS NOT NULL JOIN blog b ON b.id=a.blog_id AND b.status='ACTIVE' AND b.deleted_at IS NULL JOIN community_user u ON u.id=a.author_user_id AND u.status IN ('NORMAL','LIMITED') WHERE f.follower_user_id=#{userId} AND f.target_type='TAG'
            ) feed
            """)
    long count(@Param("userId") Long userId);
}
