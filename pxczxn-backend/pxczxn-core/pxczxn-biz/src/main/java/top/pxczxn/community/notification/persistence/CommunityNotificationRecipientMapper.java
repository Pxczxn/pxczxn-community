package top.pxczxn.community.notification.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.notification.model.CommunityNotificationRecipient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CommunityNotificationRecipientMapper
        extends BaseMapper<CommunityNotificationRecipient> {

    @Select("""
            SELECT *
            FROM community_notification_recipient
            WHERE notification_id = #{notificationId}
              AND recipient_user_id = #{recipientUserId}
            LIMIT 1
            """)
    CommunityNotificationRecipient findRelation(
            @Param("notificationId") Long notificationId,
            @Param("recipientUserId") Long recipientUserId
    );

    @Select("""
            <script>
            SELECT recipient.*
            FROM community_notification_recipient recipient
            INNER JOIN community_notification notification
                    ON notification.id = recipient.notification_id
            WHERE recipient.recipient_user_id = #{recipientUserId}
              AND recipient.status != 'ARCHIVED'
              <if test="status != null">
                AND recipient.status = #{status}
              </if>
              <if test="category != null">
                AND notification.category = #{category}
              </if>
            ORDER BY notification.last_activity_at DESC,
                     notification.id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<CommunityNotificationRecipient> selectInbox(
            @Param("recipientUserId") Long recipientUserId,
            @Param("category") String category,
            @Param("status") String status,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM community_notification_recipient recipient
            INNER JOIN community_notification notification
                    ON notification.id = recipient.notification_id
            WHERE recipient.recipient_user_id = #{recipientUserId}
              AND recipient.status != 'ARCHIVED'
              <if test="status != null">
                AND recipient.status = #{status}
              </if>
              <if test="category != null">
                AND notification.category = #{category}
              </if>
            </script>
            """)
    long countInbox(
            @Param("recipientUserId") Long recipientUserId,
            @Param("category") String category,
            @Param("status") String status
    );

    @Select("""
            SELECT notification.category AS category, COUNT(*) AS count
            FROM community_notification_recipient recipient
            INNER JOIN community_notification notification
                    ON notification.id = recipient.notification_id
            WHERE recipient.recipient_user_id = #{recipientUserId}
              AND recipient.status = 'UNREAD'
            GROUP BY notification.category
            """)
    List<Map<String, Object>> countUnreadByCategory(
            @Param("recipientUserId") Long recipientUserId
    );

    @Update("""
            <script>
            UPDATE community_notification_recipient recipient
            INNER JOIN community_notification notification
                    ON notification.id = recipient.notification_id
            SET recipient.status = 'READ',
                recipient.read_at = #{readAt}
            WHERE recipient.recipient_user_id = #{recipientUserId}
              AND recipient.status = 'UNREAD'
              <if test="category != null">
                AND notification.category = #{category}
              </if>
            </script>
            """)
    int markAllRead(
            @Param("recipientUserId") Long recipientUserId,
            @Param("category") String category,
            @Param("readAt") LocalDateTime readAt
    );
}
