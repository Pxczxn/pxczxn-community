package top.pxczxn.community.series.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.series.model.Series;
import top.pxczxn.community.team.persistence.TeamCountRow;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SeriesMapper extends BaseMapper<Series> {
    @Select("SELECT * FROM series WHERE blog_id = #{blogId} AND slug = #{slug} AND deleted_at IS NULL LIMIT 1")
    Series findByBlogAndSlug(@Param("blogId") Long blogId, @Param("slug") String slug);

    @Select("SELECT * FROM series WHERE blog_id = #{blogId} AND deleted_at IS NULL ORDER BY updated_at DESC")
    List<Series> findByBlog(@Param("blogId") Long blogId);

    @Select("SELECT * FROM series WHERE review_status = 'APPROVED' AND deleted_at IS NULL ORDER BY published_at DESC, id DESC")
    List<Series> findPublic();

    /** Approved series of one blog, for the public blog portal series tab. */
    @Select("SELECT * FROM series WHERE blog_id = #{blogId} AND review_status = 'APPROVED' AND deleted_at IS NULL ORDER BY published_at DESC, id DESC")
    List<Series> findPublicByBlog(@Param("blogId") Long blogId);

    @Select("SELECT * FROM series WHERE id = #{id} AND review_status = 'APPROVED' AND deleted_at IS NULL LIMIT 1")
    Series findPublicById(@Param("id") Long id);

    @Select("SELECT * FROM series WHERE review_status = 'PENDING_REVIEW' AND deleted_at IS NULL ORDER BY updated_at ASC")
    List<Series> findReviewQueue();

    /** Approved series the reader follows, newest follow first. */
    @Select("""
            SELECT s.*
            FROM series s
            JOIN community_follow f ON f.target_type = 'SERIES' AND f.target_id = s.id
            WHERE f.follower_user_id = #{userId}
              AND s.review_status = 'APPROVED'
              AND s.deleted_at IS NULL
            ORDER BY f.created_at DESC, s.id DESC
            """)
    List<Series> findFollowedByUser(@Param("userId") Long userId);

    /** Approved series the reader recently opened, newest read first; powers "continue reading". */
    @Select("""
            SELECT s.*
            FROM series s
            JOIN series_reading_progress p ON p.series_id = s.id
            WHERE p.user_id = #{userId}
              AND s.review_status = 'APPROVED'
              AND s.deleted_at IS NULL
            ORDER BY p.updated_at DESC, s.id DESC
            LIMIT #{limit}
            """)
    List<Series> findRecentlyReadByUser(@Param("userId") Long userId, @Param("limit") int limit);

    /** Series count grouped by blog, used by the workspace dashboard and "my teams" cards. */
    @Select("""
            <script>
            SELECT blog_id AS teamId, COUNT(*) AS total
            FROM series
            WHERE deleted_at IS NULL
              AND blog_id IN
              <foreach item="item" collection="blogIds" open="(" separator="," close=")">#{item}</foreach>
            GROUP BY blog_id
            </script>
            """)
    List<TeamCountRow> countByBlogs(@Param("blogIds") List<Long> blogIds);

    @Select("SELECT COUNT(*) FROM series WHERE blog_id = #{blogId} AND deleted_at IS NULL "
            + "AND review_status = #{reviewStatus}")
    int countByBlogAndReviewStatus(@Param("blogId") Long blogId, @Param("reviewStatus") String reviewStatus);

    @Update("UPDATE series SET title = #{title}, slug = #{slug}, summary = #{summary}, cover_file_id = #{coverFileId}, "
            + "serialization_status = #{serializationStatus}, updated_at = #{now}, lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND lock_version = #{expectedLockVersion} AND review_status IN ('DRAFT', 'REJECTED') AND deleted_at IS NULL")
    int updateDraft(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion,
                    @Param("title") String title, @Param("slug") String slug, @Param("summary") String summary,
                    @Param("coverFileId") Long coverFileId, @Param("serializationStatus") String serializationStatus,
                    @Param("now") LocalDateTime now);

    @Update("UPDATE series SET review_status = 'PENDING_REVIEW', updated_at = #{now}, lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND lock_version = #{expectedLockVersion} AND review_status IN ('DRAFT', 'REJECTED') AND deleted_at IS NULL")
    int submitReview(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion, @Param("now") LocalDateTime now);

    @Update("UPDATE series SET updated_at = #{now}, lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND lock_version = #{expectedLockVersion} AND review_status IN ('DRAFT', 'REJECTED') AND deleted_at IS NULL")
    int touchDraft(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion, @Param("now") LocalDateTime now);

    @Update("UPDATE series SET review_status = #{reviewStatus}, reviewer_admin_id = #{adminId}, review_comment = #{comment}, reviewed_at = #{now}, "
            + "published_at = CASE WHEN #{reviewStatus} = 'APPROVED' THEN #{now} ELSE published_at END, updated_at = #{now}, lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND lock_version = #{expectedLockVersion} AND review_status = 'PENDING_REVIEW' AND deleted_at IS NULL")
    int decideReview(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion,
                     @Param("reviewStatus") String reviewStatus, @Param("adminId") Long adminId,
                     @Param("comment") String comment, @Param("now") LocalDateTime now);
}
