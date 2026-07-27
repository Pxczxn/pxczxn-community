package top.pxczxn.community.team.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team_role")
public class TeamRole {

    @TableId
    private String roleCode;

    private String roleName;

    private String description;

    private Integer sortOrder;

    private Boolean isSystem;

    private LocalDateTime createdAt;
}
