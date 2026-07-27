package top.pxczxn.community.collaboration.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("article_collaborator")
public class ArticleCollaborator {
    @TableId(type = IdType.INPUT) private Long id;
    private Long articleId;
    private Long userId;
    private Long invitationId;
    private String contributionType;
    private Boolean canEdit;
    private Integer attributionOrder;
    private LocalDateTime acceptedAt;
    private LocalDateTime revokedAt;
    private Integer lockVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
