package top.pxczxn.community.team.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.pxczxn.community.team.model.TeamInvitation;

@Mapper
public interface TeamInvitationMapper extends BaseMapper<TeamInvitation> {
}
