package top.pxczxn.community.team.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.team.model.TeamApplication;

import java.util.List;

@Mapper
public interface TeamApplicationMapper extends BaseMapper<TeamApplication> {

    /**
     * Find pending application by applicant user ID.
     */
    @Select("SELECT * FROM team_application WHERE applicant_user_id = #{applicantUserId} AND status = 'PENDING' LIMIT 1")
    TeamApplication findPendingByApplicant(@Param("applicantUserId") Long applicantUserId);

    /**
     * Find application by idempotency key.
     */
    @Select("SELECT * FROM team_application WHERE idempotency_key = #{idempotencyKey} LIMIT 1")
    TeamApplication findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    /**
     * Check if slug exists in pending or approved applications.
     */
    @Select("SELECT COUNT(*) FROM team_application WHERE team_slug = #{slug} AND status IN ('PENDING', 'APPROVED')")
    int countBySlugInPendingOrApproved(@Param("slug") String slug);

    /**
     * Update application status to CANCELLED with optimistic lock.
     * Returns number of affected rows (1 if successful, 0 if concurrent modification).
     */
    @Update("UPDATE team_application SET status = 'CANCELLED', lock_version = lock_version + 1, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING' AND lock_version = #{lockVersion}")
    int cancelWithOptimisticLock(@Param("id") Long id, @Param("lockVersion") Integer lockVersion);

    /**
     * Update application status to APPROVED with reviewer info and optimistic lock.
     * Returns number of affected rows (1 if successful, 0 if concurrent modification or status changed).
     */
    @Update("UPDATE team_application SET status = 'APPROVED', reviewer_user_id = #{reviewerUserId}, " +
            "review_comment = #{reviewComment}, reviewed_at = NOW(), lock_version = lock_version + 1, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING' AND lock_version = #{lockVersion}")
    int approveWithOptimisticLock(@Param("id") Long id,
                                   @Param("reviewerUserId") Long reviewerUserId,
                                   @Param("reviewComment") String reviewComment,
                                   @Param("lockVersion") Integer lockVersion);

    /**
     * Update application status to REJECTED with reviewer info and optimistic lock.
     * Returns number of affected rows (1 if successful, 0 if concurrent modification or status changed).
     */
    @Update("UPDATE team_application SET status = 'REJECTED', reviewer_user_id = #{reviewerUserId}, " +
            "review_comment = #{reviewComment}, reviewed_at = NOW(), lock_version = lock_version + 1, updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING' AND lock_version = #{lockVersion}")
    int rejectWithOptimisticLock(@Param("id") Long id,
                                  @Param("reviewerUserId") Long reviewerUserId,
                                  @Param("reviewComment") String reviewComment,
                                  @Param("lockVersion") Integer lockVersion);

    /**
     * List all pending applications ordered by creation time.
     */
    @Select("SELECT * FROM team_application WHERE status = 'PENDING' ORDER BY created_at ASC")
    List<TeamApplication> listPending();
}
