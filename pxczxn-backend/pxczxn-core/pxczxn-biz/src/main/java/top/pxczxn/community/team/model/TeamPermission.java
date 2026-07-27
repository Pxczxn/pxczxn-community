package top.pxczxn.community.team.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("team_permission")
public class TeamPermission {

    private String roleCode;

    private String permissionCode;
}
