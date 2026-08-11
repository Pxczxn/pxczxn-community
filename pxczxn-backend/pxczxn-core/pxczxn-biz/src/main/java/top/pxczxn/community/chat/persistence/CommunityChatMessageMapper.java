package top.pxczxn.community.chat.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.chat.model.CommunityChatMessage;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommunityChatMessageMapper extends BaseMapper<CommunityChatMessage> {

    @Select("""
            SELECT peer.peer_user_id AS peerUserId,
                   u.username AS peerUsername,
                   u.display_name AS peerDisplayName,
                   u.avatar_file_id AS peerAvatarFileId,
                   m.content_text AS lastMessage,
                   m.created_at AS lastMessageAt,
                   COALESCE(unread.unread_count, 0) AS unreadCount
            FROM community_chat_message m
            JOIN (
                SELECT CASE WHEN sender_user_id = #{actor} THEN recipient_user_id ELSE sender_user_id END AS peer_user_id,
                       MAX(id) AS last_message_id
                FROM community_chat_message
                WHERE (sender_user_id = #{actor} OR recipient_user_id = #{actor})
                  AND deleted_at IS NULL
                  AND ((sender_user_id = #{actor} AND sender_deleted_at IS NULL)
                    OR (recipient_user_id = #{actor} AND recipient_deleted_at IS NULL))
                GROUP BY CASE WHEN sender_user_id = #{actor} THEN recipient_user_id ELSE sender_user_id END
            ) peer ON peer.last_message_id = m.id
            JOIN community_user u ON u.id = peer.peer_user_id
            LEFT JOIN (
                SELECT sender_user_id AS peer_user_id, COUNT(*) AS unread_count
                FROM community_chat_message
                WHERE recipient_user_id = #{actor}
                  AND status = 'SENT'
                  AND deleted_at IS NULL
                  AND recipient_deleted_at IS NULL
                GROUP BY sender_user_id
            ) unread ON unread.peer_user_id = peer.peer_user_id
            ORDER BY m.id DESC
            LIMIT #{limit}
            """)
    List<CommunityChatConversationRow> conversations(
            @Param("actor") Long actor,
            @Param("limit") int limit
    );

    @Select("""
            SELECT * FROM community_chat_message
            WHERE ((sender_user_id = #{actor} AND recipient_user_id = #{peer})
                OR (sender_user_id = #{peer} AND recipient_user_id = #{actor}))
              AND deleted_at IS NULL
              AND ((sender_user_id = #{actor} AND sender_deleted_at IS NULL)
                OR (recipient_user_id = #{actor} AND recipient_deleted_at IS NULL))
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<CommunityChatMessage> history(
            @Param("actor") Long actor,
            @Param("peer") Long peer,
            @Param("limit") int limit
    );

    @Update("""
            UPDATE community_chat_message
            SET status = 'READ', read_at = #{now}
            WHERE sender_user_id = #{sender}
              AND recipient_user_id = #{recipient}
              AND status = 'SENT'
            """)
    int markRead(
            @Param("sender") Long sender,
            @Param("recipient") Long recipient,
            @Param("now") LocalDateTime now
    );

    @Update("""
            UPDATE community_chat_message
            SET sender_deleted_at = CASE WHEN sender_user_id = #{userId} THEN #{deletedAt} ELSE sender_deleted_at END,
                recipient_deleted_at = CASE WHEN recipient_user_id = #{userId} THEN #{deletedAt} ELSE recipient_deleted_at END
            WHERE sender_user_id = #{userId} OR recipient_user_id = #{userId}
            """)
    int hideForUser(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);
}
