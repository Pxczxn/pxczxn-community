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
}
