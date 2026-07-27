package top.pxczxn.community.collaboration.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("article_collaboration_audit_event")
public class ArticleCollaborationAuditEvent {
    @TableId(type = IdType.INPUT) private Long id;
    private Long articleId;
    private Long actorUserId;
    private String eventType;
    private Long targetUserId;
    private String beforeSnapshot;
    private String afterSnapshot;
    private LocalDateTime occurredAt;
}
