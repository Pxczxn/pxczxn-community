package top.pxczxn.community.collaboration.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("article_collaboration_invitation")
public class ArticleCollaborationInvitation {
    @TableId(type = IdType.INPUT) private Long id;
    private Long articleId;
    private Long inviteeUserId;
    private Long invitedByUserId;
    private String contributionType;
    private Boolean canEdit;
    private Integer attributionOrder;
    private String message;
    private String status;
    private String idempotencyKey;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;
    private Integer lockVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
