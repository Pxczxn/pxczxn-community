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
