package top.pxczxn.community.team.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.team.model.TeamInvitation;

@Mapper
public interface TeamInvitationMapper extends BaseMapper<TeamInvitation> {

    @Select("SELECT * FROM team_invitation WHERE idempotency_key = #{key} LIMIT 1")
    TeamInvitation findByIdempotencyKey(@Param("key") String key);

    @Select("SELECT * FROM team_invitation WHERE team_id = #{teamId} AND invitee_user_id = #{userId} "
            + "AND status = 'PENDING' LIMIT 1")
    TeamInvitation findPendingForTeamAndInvitee(@Param("teamId") Long teamId, @Param("userId") Long userId);

    @Select("SELECT * FROM team_invitation WHERE id = #{id} AND invitee_user_id = #{userId} LIMIT 1")
    TeamInvitation findForInvitee(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM team_invitation WHERE invitee_user_id = #{userId} AND status = 'PENDING' ORDER BY created_at DESC")
    java.util.List<TeamInvitation> findPendingForInvitee(@Param("userId") Long userId);

    /** Invitations the team has sent that are still awaiting an answer; drives the workspace todo badge. */
    @Select("SELECT COUNT(*) FROM team_invitation WHERE team_id = #{teamId} AND status = 'PENDING' AND expires_at > NOW()")
    int countPendingByTeam(@Param("teamId") Long teamId);

    /**
     * Invitations issued by one team, newest first, all statuses included so the manager can show
     * what happened to earlier invites instead of silently dropping them.
     */
    @Select("SELECT * FROM team_invitation WHERE team_id = #{teamId} ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    java.util.List<TeamInvitation> findByTeam(@Param("teamId") Long teamId, @Param("limit") int limit);

    @Select("SELECT * FROM team_invitation WHERE id = #{id} AND team_id = #{teamId} LIMIT 1")
    TeamInvitation findForTeam(@Param("id") Long id, @Param("teamId") Long teamId);

    /**
     * Revoke a pending invitation from the team side.
     *
     * <p>{@code is_pending} is a generated column derived from {@code status}, so leaving PENDING also
     * releases {@code uk_pending_invitation} - the same user can then be invited again, which is the
     * whole point of revoking a mistaken invite.
     */
    @Update("UPDATE team_invitation SET status = 'REVOKED', lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND team_id = #{teamId} AND status = 'PENDING' AND lock_version = #{lockVersion}")
    int revoke(@Param("id") Long id, @Param("teamId") Long teamId, @Param("lockVersion") Integer lockVersion);

    @Update("UPDATE team_invitation SET status = 'ACCEPTED', accepted_at = NOW(), lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND invitee_user_id = #{userId} AND status = 'PENDING' AND expires_at > NOW() "
            + "AND lock_version = #{lockVersion}")
    int accept(@Param("id") Long id, @Param("userId") Long userId, @Param("lockVersion") Integer lockVersion);

    @Update("UPDATE team_invitation SET status = 'REJECTED', rejected_at = NOW(), lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND invitee_user_id = #{userId} AND status = 'PENDING' AND expires_at > NOW() "
            + "AND lock_version = #{lockVersion}")
    int reject(@Param("id") Long id, @Param("userId") Long userId, @Param("lockVersion") Integer lockVersion);

    @Update("UPDATE team_invitation SET status = 'EXPIRED', lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND status = 'PENDING' AND expires_at <= NOW() AND lock_version = #{lockVersion}")
    int expire(@Param("id") Long id, @Param("lockVersion") Integer lockVersion);
}
