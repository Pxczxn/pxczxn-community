package top.pxczxn.community.team.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.team.model.TeamPermission;

import java.util.List;

@Mapper
public interface TeamPermissionMapper extends BaseMapper<TeamPermission> {

    @Select("SELECT permission_code FROM team_permission WHERE role_code = #{roleCode}")
    List<String> findPermissionsByRole(@Param("roleCode") String roleCode);
}
