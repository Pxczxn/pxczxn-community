package top.pxczxn.community.team.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.team.model.TeamMember;

import java.util.List;

@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {

    @Select("SELECT * FROM team_member WHERE team_id = #{teamId} AND user_id = #{userId} AND left_at IS NULL")
    TeamMember findActiveMember(@Param("teamId") Long teamId, @Param("userId") Long userId);

    @Select("SELECT * FROM team_member WHERE team_id = #{teamId} AND left_at IS NULL")
    List<TeamMember> findActiveMembers(@Param("teamId") Long teamId);

    /**
     * Update member role with optimistic lock (id + lock_version).
     * Returns 1 if successful, 0 if concurrent modification.
     */
    @Update("UPDATE team_member SET role_code = #{roleCode}, lock_version = lock_version + 1 " +
            "WHERE id = #{id} AND lock_version = #{lockVersion}")
    int updateRoleWithOptimisticLock(@Param("id") Long id,
                                      @Param("roleCode") String roleCode,
                                      @Param("lockVersion") Integer lockVersion);

    @Update("UPDATE team_member SET left_at = NOW(), lock_version = lock_version + 1 "
            + "WHERE id = #{id} AND left_at IS NULL AND lock_version = #{lockVersion}")
    int leaveWithOptimisticLock(@Param("id") Long id, @Param("lockVersion") Integer lockVersion);

    @Update("UPDATE team_member SET left_at = NOW(), lock_version = lock_version + 1 "
            + "WHERE team_id = #{teamId} AND left_at IS NULL")
    int leaveAllActive(@Param("teamId") Long teamId);
}
