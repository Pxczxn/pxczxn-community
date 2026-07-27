package top.pxczxn.community.team.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team_audit_event")
public class TeamAuditEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private Long actorUserId;

    private String eventType;

    private String targetType;

    private Long targetId;

    private String requestId;

    private String beforeSnapshot;

    private String afterSnapshot;

    private LocalDateTime occurredAt;
}
