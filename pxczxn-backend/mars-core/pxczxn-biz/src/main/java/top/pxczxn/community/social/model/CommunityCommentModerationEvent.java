package top.pxczxn.community.social.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_comment_moderation_event")
public class CommunityCommentModerationEvent {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long commentId;

    private String action;

    private String actorType;

    private Long actorUserId;

    private Long actorAdminId;

    private String previousStatus;

    private String newStatus;

    private String reason;

    private String metadataJson;

    private LocalDateTime createdAt;
}
