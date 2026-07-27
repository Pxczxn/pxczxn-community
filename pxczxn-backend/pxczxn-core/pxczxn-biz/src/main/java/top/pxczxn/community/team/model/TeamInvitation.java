package top.pxczxn.community.team.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team_invitation")
public class TeamInvitation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private Long inviteeUserId;

    private String roleCode;

    private String tokenHash;

    private String status;

    private Long invitedByUserId;

    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime rejectedAt;

    private Integer lockVersion;

    private LocalDateTime createdAt;
}
