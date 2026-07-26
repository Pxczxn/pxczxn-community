package top.pxczxn.community.notification.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_notification_recipient")
public class CommunityNotificationRecipient {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long notificationId;

    private Long recipientUserId;

    private String status;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
