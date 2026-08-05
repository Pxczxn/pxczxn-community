package top.pxczxn.community.team.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.team.model.TeamAuditEvent;

import java.util.List;

@Mapper
public interface TeamAuditEventMapper extends BaseMapper<TeamAuditEvent> {

    /**
     * Newest collaboration events of one team.
     * The table only ever receives team collaboration events, so no event-type whitelist is applied here;
     * presentation-level filtering stays in the service so new event types show up automatically.
     */
    @Select("SELECT * FROM team_audit_event WHERE team_id = #{teamId} ORDER BY occurred_at DESC, id DESC LIMIT #{limit}")
    List<TeamAuditEvent> findRecentByTeam(@Param("teamId") Long teamId, @Param("limit") int limit);
}
