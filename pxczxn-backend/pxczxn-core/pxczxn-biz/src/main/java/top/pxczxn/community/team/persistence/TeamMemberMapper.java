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
     * All active memberships for one user, newest membership first.
     * Backs {@code GET /api/v1/teams/me} and the smart default tab on the team entry page.
     */
    @Select("SELECT * FROM team_member WHERE user_id = #{userId} AND left_at IS NULL "
            + "ORDER BY joined_at DESC, id DESC")
    List<TeamMember> findActiveMembershipsByUser(@Param("userId") Long userId);

    /** Active member headcount grouped by team, so multi-team views stay a single query. */
    @Select("""
            <script>
            SELECT team_id AS teamId, COUNT(*) AS total
            FROM team_member
            WHERE left_at IS NULL
              AND team_id IN
              <foreach item="item" collection="teamIds" open="(" separator="," close=")">#{item}</foreach>
            GROUP BY team_id
            </script>
            """)
    List<TeamCountRow> countActiveMembersByTeams(@Param("teamIds") List<Long> teamIds);

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
