package top.pxczxn.community.article.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.article.model.Article;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ArticleMapper extends BaseMapper<Article> {

    @Select("""
            SELECT COUNT(*)
            FROM article a
            INNER JOIN community_user author
                    ON author.id = a.author_user_id
                   AND author.status IN ('NORMAL', 'LIMITED')
            WHERE a.blog_id = #{blogId}
              AND a.deleted_at IS NULL
              AND a.published_version_id IS NOT NULL
              AND a.visibility = 'PUBLIC'
              AND a.publish_status NOT IN ('HIDDEN', 'TAKEN_DOWN', 'DELETED')
            """)
    long countPublicByBlog(@Param("blogId") Long blogId);

    @Select("""
            <script>
            SELECT a.*
            FROM article a
            INNER JOIN community_user author
                    ON author.id = a.author_user_id
                   AND author.status IN ('NORMAL', 'LIMITED')
            WHERE a.blog_id = #{blogId}
              AND a.deleted_at IS NULL
              AND a.published_version_id IS NOT NULL
              AND a.visibility = 'PUBLIC'
              AND a.publish_status NOT IN ('HIDDEN', 'TAKEN_DOWN', 'DELETED')
              <if test="categoryId != null">
                AND a.category_id = #{categoryId}
              </if>
            ORDER BY a.published_at DESC, a.id DESC
            </script>
            """)
    IPage<Article> selectPublicPage(
            IPage<Article> page,
            @Param("blogId") Long blogId,
            @Param("categoryId") Long categoryId
    );

    @Select("""
            SELECT a.*
            FROM article a
            INNER JOIN community_user author
                    ON author.id = a.author_user_id
                   AND author.status IN ('NORMAL', 'LIMITED')
            INNER JOIN blog b
                    ON b.id = a.blog_id
                   AND b.status = 'ACTIVE'
                   AND b.deleted_at IS NULL
            WHERE a.deleted_at IS NULL
              AND a.published_version_id IS NOT NULL
              AND a.visibility = 'PUBLIC'
              AND a.publish_status NOT IN ('HIDDEN', 'TAKEN_DOWN', 'DELETED')
            ORDER BY a.published_at DESC, a.id DESC
            """)
    IPage<Article> selectDiscoverPublicPage(IPage<Article> page);

    @Select("""
            <script>
            SELECT a.*
            FROM article a
            INNER JOIN community_user author ON author.id = a.author_user_id AND author.status IN ('NORMAL', 'LIMITED')
            INNER JOIN blog b ON b.id = a.blog_id AND b.status = 'ACTIVE' AND b.deleted_at IS NULL
            WHERE a.deleted_at IS NULL AND a.published_version_id IS NOT NULL
              AND a.visibility = 'PUBLIC' AND a.publish_status = 'PUBLISHED'
            <choose>
              <when test="sort == 'VIEWS'">ORDER BY a.view_count DESC, a.published_at DESC, a.id DESC</when>
              <when test="sort == 'LIKES'">ORDER BY a.like_count DESC, a.published_at DESC, a.id DESC</when>
              <when test="sort == 'FAVORITES'">ORDER BY a.favorite_count DESC, a.published_at DESC, a.id DESC</when>
              <when test="sort == 'COMMENTS'">ORDER BY a.comment_count DESC, a.published_at DESC, a.id DESC</when>
              <when test="sort == 'QUALITY'">ORDER BY (CASE WHEN a.summary IS NOT NULL AND CHAR_LENGTH(TRIM(a.summary)) >= 40 THEN 2 ELSE 0 END + CASE WHEN a.cover_file_id IS NOT NULL THEN 1 ELSE 0 END + CASE WHEN a.review_status = 'APPROVED' THEN 1 ELSE 0 END) DESC, a.published_at DESC, a.id DESC</when>
              <when test="sort == 'RISK'">ORDER BY CASE WHEN a.review_status = 'APPROVED' THEN 0 ELSE 1 END ASC, a.published_at DESC, a.id DESC</when>
              <otherwise>ORDER BY a.published_at DESC, a.id DESC</otherwise>
            </choose>
            </script>
            """)
    IPage<Article> selectDiscoverRankedPage(IPage<Article> page, @Param("sort") String sort);

    @Select("""
            SELECT DATE(created_at) AS day, COUNT(*) AS count
            FROM article
            WHERE created_at >= #{from}
              AND deleted_at IS NULL
            GROUP BY DATE(created_at)
            ORDER BY day
            """)
    List<Map<String, Object>> selectDailyCreations(
            @Param("from") LocalDateTime from
    );

    // ---------------------------------------------------------------------
    // Team workspace aggregates
    // ---------------------------------------------------------------------

    /** Article counts grouped by publish status, feeding the workspace stat cards in one round trip. */
    @Select("""
            SELECT publish_status AS status, COUNT(*) AS total
            FROM article
            WHERE blog_id = #{blogId}
              AND deleted_at IS NULL
            GROUP BY publish_status
            """)
    List<ArticleStatusCountRow> countByBlogGroupedByPublishStatus(@Param("blogId") Long blogId);

    /** Newest articles of a blog regardless of publish status; the workspace shows drafts too. */
    @Select("""
            SELECT *
            FROM article
            WHERE blog_id = #{blogId}
              AND deleted_at IS NULL
            ORDER BY COALESCE(updated_at, created_at) DESC, id DESC
            LIMIT #{limit}
            """)
    List<Article> findRecentByBlog(@Param("blogId") Long blogId, @Param("limit") int limit);

    @Select("""
            SELECT COALESCE(SUM(view_count), 0)
            FROM article
            WHERE blog_id = #{blogId}
              AND deleted_at IS NULL
            """)
    long sumViewCountByBlog(@Param("blogId") Long blogId);

    @Select("""
            SELECT COALESCE(SUM(like_count + comment_count + favorite_count), 0)
            FROM article
            WHERE blog_id = #{blogId}
              AND deleted_at IS NULL
            """)
    long sumInteractionCountByBlog(@Param("blogId") Long blogId);

    /** Articles that need human attention: rejected review, taken down, or failed publication. */
    @Select("""
            SELECT COUNT(*)
            FROM article
            WHERE blog_id = #{blogId}
              AND deleted_at IS NULL
              AND (review_status IN ('REVISION_REQUIRED', 'REJECTED')
                   OR publish_status IN ('TAKEN_DOWN', 'PUBLISH_FAILED'))
            """)
    int countRiskByBlog(@Param("blogId") Long blogId);

    /**
     * Articles of one blog regardless of publish status, newest first, optionally filtered by a
     * single {@code publish_status}. Backs the team workspace content page.
     */
    @Select("""
            <script>
            SELECT *
            FROM article
            WHERE blog_id = #{blogId}
              AND deleted_at IS NULL
              <if test="publishStatus != null">
                AND publish_status = #{publishStatus}
              </if>
            ORDER BY COALESCE(updated_at, created_at) DESC, id DESC
            LIMIT #{limit}
            </script>
            """)
    List<Article> findByBlog(@Param("blogId") Long blogId,
                             @Param("publishStatus") String publishStatus,
                             @Param("limit") int limit);

    /** Per-author article counts inside one blog, used by the member list contribution column. */
    @Select("""
            <script>
            SELECT author_user_id AS authorUserId, COUNT(*) AS total
            FROM article
            WHERE blog_id = #{blogId}
              AND deleted_at IS NULL
              AND author_user_id IN
              <foreach item="item" collection="authorUserIds" open="(" separator="," close=")">#{item}</foreach>
            GROUP BY author_user_id
            </script>
            """)
    List<ArticleAuthorCountRow> countByBlogAndAuthors(@Param("blogId") Long blogId,
                                                      @Param("authorUserIds") List<Long> authorUserIds);
}
