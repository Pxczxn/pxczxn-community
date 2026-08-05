package top.pxczxn.community.team.submission.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.team.persistence.TeamCountRow;
import top.pxczxn.community.team.submission.model.TeamSubmission;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TeamSubmissionMapper extends BaseMapper<TeamSubmission> {
    @Select("SELECT * FROM team_submission WHERE idempotency_key = #{idempotencyKey} LIMIT 1")
    TeamSubmission findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Select("SELECT * FROM team_submission WHERE submitted_by_user_id = #{userId} ORDER BY created_at DESC")
    List<TeamSubmission> findByAuthor(@Param("userId") Long userId);

    @Select("SELECT * FROM team_submission WHERE target_team_id = #{teamId} ORDER BY created_at DESC")
    List<TeamSubmission> findByTeam(@Param("teamId") Long teamId);

    @Select("SELECT * FROM team_submission WHERE status IN ('PLATFORM_PENDING', 'PLATFORM_PUBLISHING') ORDER BY created_at ASC")
    List<TeamSubmission> findPlatformQueue();

    /**
     * Submissions waiting for team review, grouped by team.
     * Drives the "待审投稿" badge without loading full submission rows.
     */
    @Select("""
            <script>
            SELECT target_team_id AS teamId, COUNT(*) AS total
            FROM team_submission
            WHERE status = 'TEAM_PENDING'
              AND target_team_id IN
              <foreach item="item" collection="teamIds" open="(" separator="," close=")">#{item}</foreach>
            GROUP BY target_team_id
            </script>
            """)
    List<TeamCountRow> countPendingByTeams(@Param("teamIds") List<Long> teamIds);

    @Select("SELECT COUNT(*) FROM team_submission WHERE target_team_id = #{teamId} AND status = #{status}")
    int countByTeamAndStatus(@Param("teamId") Long teamId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM team_submission WHERE submitted_by_user_id = #{userId} "
            + "AND target_team_id = #{teamId} AND status = 'TEAM_REVISION_REQUIRED'")
    int countRevisionRequiredForAuthor(@Param("teamId") Long teamId, @Param("userId") Long userId);

    @Update("UPDATE team_submission SET status = #{nextStatus}, team_reviewer_user_id = #{reviewerId}, "
            + "team_review_comment = #{comment}, team_reviewed_at = #{now}, updated_at = #{now}, "
            + "lock_version = lock_version + 1 WHERE id = #{id} AND status = 'TEAM_PENDING' "
            + "AND lock_version = #{expectedLockVersion}")
    int decideByTeam(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion,
                     @Param("nextStatus") String nextStatus, @Param("reviewerId") Long reviewerId,
                     @Param("comment") String comment, @Param("now") LocalDateTime now);

    @Update("UPDATE team_submission SET status = #{nextStatus}, platform_reviewer_admin_id = #{reviewerId}, "
            + "platform_review_comment = #{comment}, platform_reviewed_at = #{now}, updated_at = #{now}, "
            + "lock_version = lock_version + 1 WHERE id = #{id} AND status = 'PLATFORM_PENDING' "
            + "AND lock_version = #{expectedLockVersion}")
    int decideByPlatform(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion,
                         @Param("nextStatus") String nextStatus, @Param("reviewerId") Long reviewerId,
                         @Param("comment") String comment, @Param("now") LocalDateTime now);

    @Update("UPDATE team_submission SET status = 'PLATFORM_PUBLISHING', platform_reviewer_admin_id = #{reviewerId}, "
            + "platform_review_comment = #{comment}, platform_reviewed_at = #{now}, updated_at = #{now}, "
            + "lock_version = lock_version + 1 WHERE id = #{id} AND status = 'PLATFORM_PENDING' "
            + "AND lock_version = #{expectedLockVersion}")
    int beginPlatformPublication(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion,
                                 @Param("reviewerId") Long reviewerId, @Param("comment") String comment,
                                 @Param("now") LocalDateTime now);

    @Update("UPDATE team_submission SET status = 'PUBLISHED', published_team_article_id = #{articleId}, "
            + "updated_at = #{now}, lock_version = lock_version + 1 WHERE id = #{id} "
            + "AND status = 'PLATFORM_PUBLISHING' AND lock_version = #{expectedLockVersion}")
    int completePublication(@Param("id") Long id, @Param("expectedLockVersion") Integer expectedLockVersion,
                            @Param("articleId") Long articleId, @Param("now") LocalDateTime now);
}
