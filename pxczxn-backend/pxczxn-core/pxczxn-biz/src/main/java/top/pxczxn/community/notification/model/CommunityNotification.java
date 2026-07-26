package top.pxczxn.community.notification.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_notification")
public class CommunityNotification {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String notificationType;

    private String category;

    private String importance;

    private Long senderUserId;

    private String title;

    private String content;

    private String targetType;

    private Long targetId;

    private String deduplicationKey;

    private String payloadJson;

    private Integer aggregateCount;

    private LocalDateTime lastActivityAt;

    private LocalDateTime createdAt;
}
