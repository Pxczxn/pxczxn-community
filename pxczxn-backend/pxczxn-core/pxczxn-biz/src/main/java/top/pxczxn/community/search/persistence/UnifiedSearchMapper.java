package top.pxczxn.community.search.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UnifiedSearchMapper {

    @Select("""
            <script>
            SELECT * FROM (
              SELECT 'ARTICLE' AS result_type, a.id AS target_id, a.title, a.summary AS excerpt,
                     CONCAT('/articles/', a.id) AS canonical_path, a.author_user_id,
                     COALESCE(author.display_name, author.username) AS author_name, b.name AS blog_name,
                     a.published_at AS occurred_at
              FROM article a
              JOIN blog b ON b.id = a.blog_id AND b.status = 'ACTIVE' AND b.deleted_at IS NULL
              JOIN community_user author ON author.id = a.author_user_id AND author.status IN ('NORMAL', 'LIMITED')
              WHERE a.deleted_at IS NULL AND a.visibility = 'PUBLIC' AND a.publish_status = 'PUBLISHED'
                AND a.published_version_id IS NOT NULL
                AND (a.title LIKE #{pattern} ESCAPE '\\\\' OR a.summary LIKE #{pattern} ESCAPE '\\\\')
              <if test="type != null and type != 'ALL'">AND #{type} = 'ARTICLE'</if>
              UNION ALL
              SELECT 'MOMENT', m.id, CONCAT(COALESCE(author.display_name, author.username), ' 的动态'),
                     m.text_content, CONCAT('/moments/', m.id), m.actor_user_id,
                     COALESCE(author.display_name, author.username), b.name, m.created_at
              FROM community_moment m
              JOIN blog b ON b.id = m.blog_id AND b.status = 'ACTIVE' AND b.deleted_at IS NULL
              JOIN community_user author ON author.id = m.actor_user_id AND author.status IN ('NORMAL', 'LIMITED')
              WHERE m.deleted_at IS NULL AND m.visibility = 'PUBLIC' AND m.status = 'PUBLISHED'
                AND m.text_content LIKE #{pattern} ESCAPE '\\\\'
              <if test="type != null and type != 'ALL'">AND #{type} = 'MOMENT'</if>
              UNION ALL
              SELECT 'BLOG', b.id, b.name, b.summary, CONCAT('/blogs/', b.slug), b.owner_user_id,
                     COALESCE(owner.display_name, owner.username), b.name, b.updated_at
              FROM blog b
              JOIN community_user owner ON owner.id = b.owner_user_id AND owner.status IN ('NORMAL', 'LIMITED')
              WHERE b.status = 'ACTIVE' AND b.deleted_at IS NULL
                AND (b.name LIKE #{pattern} ESCAPE '\\\\' OR b.summary LIKE #{pattern} ESCAPE '\\\\')
              <if test="type != null and type != 'ALL'">AND #{type} = 'BLOG'</if>
              UNION ALL
              SELECT 'SERIES', s.id, s.title, s.summary, CONCAT('/series/', s.id), s.created_by_user_id,
                     COALESCE(creator.display_name, creator.username), b.name, s.published_at
              FROM team_series s
              JOIN team t ON t.id = s.team_id AND t.status = 'ACTIVE' AND t.deleted_at IS NULL
              JOIN blog b ON b.id = t.blog_id AND b.status = 'ACTIVE' AND b.deleted_at IS NULL
              JOIN community_user owner ON owner.id = b.owner_user_id AND owner.status IN ('NORMAL', 'LIMITED')
              LEFT JOIN community_user creator ON creator.id = s.created_by_user_id
              WHERE s.deleted_at IS NULL AND s.review_status = 'APPROVED'
                AND (s.title LIKE #{pattern} ESCAPE '\\\\' OR s.summary LIKE #{pattern} ESCAPE '\\\\')
              <if test="type != null and type != 'ALL'">AND #{type} = 'SERIES'</if>
              UNION ALL
              SELECT 'TAG', tag.id, tag.name, tag.description, CONCAT('/tags?tag=', tag.slug), NULL,
                     NULL, NULL, tag.updated_at
              FROM platform_tag tag
              WHERE tag.status = 'ACTIVE' AND tag.merged_to_tag_id IS NULL
                AND (tag.name LIKE #{pattern} ESCAPE '\\\\' OR tag.description LIKE #{pattern} ESCAPE '\\\\')
              <if test="type != null and type != 'ALL'">AND #{type} = 'TAG'</if>
              UNION ALL
              SELECT 'USER', u.id, COALESCE(u.display_name, u.username), u.bio, CONCAT('/blogs/', b.slug), u.id,
                     COALESCE(u.display_name, u.username), b.name, u.updated_at
              FROM community_user u
              LEFT JOIN blog b ON b.owner_user_id = u.id AND b.status = 'ACTIVE' AND b.deleted_at IS NULL
              WHERE u.status IN ('NORMAL', 'LIMITED')
                AND (u.username LIKE #{pattern} ESCAPE '\\\\' OR u.display_name LIKE #{pattern} ESCAPE '\\\\' OR u.bio LIKE #{pattern} ESCAPE '\\\\')
              <if test="type != null and type != 'ALL'">AND #{type} = 'USER'</if>
            ) results
            ORDER BY occurred_at DESC, target_id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<UnifiedSearchRow> selectPage(
            @Param("pattern") String pattern,
            @Param("type") String type,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize
    );

    @Select("""
            <script>
            SELECT COUNT(*) FROM (
              SELECT a.id FROM article a JOIN blog b ON b.id = a.blog_id AND b.status = 'ACTIVE' AND b.deleted_at IS NULL JOIN community_user author ON author.id = a.author_user_id AND author.status IN ('NORMAL', 'LIMITED') WHERE a.deleted_at IS NULL AND a.visibility = 'PUBLIC' AND a.publish_status = 'PUBLISHED' AND a.published_version_id IS NOT NULL AND (a.title LIKE #{pattern} ESCAPE '\\\\' OR a.summary LIKE #{pattern} ESCAPE '\\\\') <if test="type != null and type != 'ALL'">AND #{type} = 'ARTICLE'</if>
              UNION ALL SELECT m.id FROM community_moment m JOIN blog b ON b.id = m.blog_id AND b.status = 'ACTIVE' AND b.deleted_at IS NULL JOIN community_user author ON author.id = m.actor_user_id AND author.status IN ('NORMAL', 'LIMITED') WHERE m.deleted_at IS NULL AND m.visibility = 'PUBLIC' AND m.status = 'PUBLISHED' AND m.text_content LIKE #{pattern} ESCAPE '\\\\' <if test="type != null and type != 'ALL'">AND #{type} = 'MOMENT'</if>
              UNION ALL SELECT b.id FROM blog b JOIN community_user owner ON owner.id = b.owner_user_id AND owner.status IN ('NORMAL', 'LIMITED') WHERE b.status = 'ACTIVE' AND b.deleted_at IS NULL AND (b.name LIKE #{pattern} ESCAPE '\\\\' OR b.summary LIKE #{pattern} ESCAPE '\\\\') <if test="type != null and type != 'ALL'">AND #{type} = 'BLOG'</if>
              UNION ALL SELECT s.id FROM team_series s JOIN team t ON t.id = s.team_id AND t.status = 'ACTIVE' AND t.deleted_at IS NULL JOIN blog b ON b.id = t.blog_id AND b.status = 'ACTIVE' AND b.deleted_at IS NULL JOIN community_user owner ON owner.id = b.owner_user_id AND owner.status IN ('NORMAL', 'LIMITED') WHERE s.deleted_at IS NULL AND s.review_status = 'APPROVED' AND (s.title LIKE #{pattern} ESCAPE '\\\\' OR s.summary LIKE #{pattern} ESCAPE '\\\\') <if test="type != null and type != 'ALL'">AND #{type} = 'SERIES'</if>
              UNION ALL SELECT tag.id FROM platform_tag tag WHERE tag.status = 'ACTIVE' AND tag.merged_to_tag_id IS NULL AND (tag.name LIKE #{pattern} ESCAPE '\\\\' OR tag.description LIKE #{pattern} ESCAPE '\\\\') <if test="type != null and type != 'ALL'">AND #{type} = 'TAG'</if>
              UNION ALL SELECT u.id FROM community_user u WHERE u.status IN ('NORMAL', 'LIMITED') AND (u.username LIKE #{pattern} ESCAPE '\\\\' OR u.display_name LIKE #{pattern} ESCAPE '\\\\' OR u.bio LIKE #{pattern} ESCAPE '\\\\') <if test="type != null and type != 'ALL'">AND #{type} = 'USER'</if>
            ) results
            </script>
            """)
    long count(@Param("pattern") String pattern, @Param("type") String type);
}
