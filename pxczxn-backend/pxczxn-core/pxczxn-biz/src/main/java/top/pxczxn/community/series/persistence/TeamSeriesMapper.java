package top.pxczxn.community.series.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.series.model.TeamSeries;
import top.pxczxn.community.team.persistence.TeamCountRow;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TeamSeriesMapper extends BaseMapper<TeamSeries> {
    @Select("SELECT * FROM team_series WHERE team_id = #{teamId} AND slug = #{slug} AND deleted_at IS NULL LIMIT 1")
    TeamSeries findByTeamAndSlug(@Param("teamId") Long teamId, @Param("slug") String slug);

    @Select("SELECT * FROM team_series WHERE team_id = #{teamId} AND deleted_at IS NULL ORDER BY updated_at DESC")
    List<TeamSeries> findByTeam(@Param("teamId") Long teamId);

    @Select("SELECT * FROM team_series WHERE review_status = 'APPROVED' AND deleted_at IS NULL ORDER BY published_at DESC, id DESC")
    List<TeamSeries> findPublic();

    /** Approved series of one team, for the public team portal series tab. */
    @Select("SELECT * FROM team_series WHERE team_id = #{teamId} AND review_status = 'APPROVED' AND deleted_at IS NULL ORDER BY published_at DESC, id DESC")
    List<TeamSeries> findPublicByTeam(@Param("teamId") Long teamId);

    @Select("SELECT * FROM team_series WHERE id = #{id} AND review_status = 'APPROVED' AND deleted_at IS NULL LIMIT 1")
    TeamSeries findPublicById(@Param("id") Long id);

    @Select("SELECT * FROM team_series WHERE review_status = 'PENDING_REVIEW' AND deleted_at IS NULL ORDER BY updated_at ASC")
    List<TeamSeries> findReviewQueue();

    /** Series count grouped by team, used by the "my teams" cards and the workspace dashboard. */
    @Select("""
            <script>
            SELECT team_id AS teamId, COUNT(*) AS total
            FROM team_series
            WHERE deleted_at IS NULL
              AND team_id IN
              <foreach item="item" collection="teamIds" open="(" separator="," close=")">#{item}</foreach>
            GROUP BY team_id
            </script>
            """)
    List<TeamCountRow> countByTeams(@Param("teamIds") List<Long> teamIds);

    @Select("SELECT COUNT(*) FROM team_series WHERE team_id = #{teamId} AND deleted_at IS NULL "
            + "AND review_status = #{reviewStatus}")
    int countByTeamAndReviewStatus(@Param("teamId") Long teamId, @Param("reviewStatus") String reviewStatus);

    @Update("UPDATE team_series SET title = #{title}, slug = #{slug}, summary = #{summary}, cover_file_id = #{coverFileId}, "
            + "serialization_status = #{serializationStatus}, updated_at = #{now}, lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND lock_version = #{expectedLockVersion} AND review_status IN ('DRAFT', 'REJECTED') AND deleted_at IS NULL")
    int updateDraft(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion,
                    @Param("title") String title, @Param("slug") String slug, @Param("summary") String summary,
                    @Param("coverFileId") Long coverFileId, @Param("serializationStatus") String serializationStatus,
                    @Param("now") LocalDateTime now);

    @Update("UPDATE team_series SET review_status = 'PENDING_REVIEW', updated_at = #{now}, lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND lock_version = #{expectedLockVersion} AND review_status IN ('DRAFT', 'REJECTED') AND deleted_at IS NULL")
    int submitReview(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion, @Param("now") LocalDateTime now);

    @Update("UPDATE team_series SET updated_at = #{now}, lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND lock_version = #{expectedLockVersion} AND review_status IN ('DRAFT', 'REJECTED') AND deleted_at IS NULL")
    int touchDraft(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion, @Param("now") LocalDateTime now);

    @Update("UPDATE team_series SET review_status = #{reviewStatus}, reviewer_admin_id = #{adminId}, review_comment = #{comment}, reviewed_at = #{now}, "
            + "published_at = CASE WHEN #{reviewStatus} = 'APPROVED' THEN #{now} ELSE published_at END, updated_at = #{now}, lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND lock_version = #{expectedLockVersion} AND review_status = 'PENDING_REVIEW' AND deleted_at IS NULL")
    int decideReview(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion,
                     @Param("reviewStatus") String reviewStatus, @Param("adminId") Long adminId,
                     @Param("comment") String comment, @Param("now") LocalDateTime now);
}
