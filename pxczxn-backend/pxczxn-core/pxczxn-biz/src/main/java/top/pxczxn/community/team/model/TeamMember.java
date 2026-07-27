package top.pxczxn.community.team.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team_member")
public class TeamMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private Long userId;

    private String roleCode;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private Long invitedByUserId;

    private Integer lockVersion;
}
