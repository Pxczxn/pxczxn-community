package top.pxczxn.platform.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.pxczxn.platform.message.entity.SysChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface SysChatMessageMapper extends BaseMapper<SysChatMessage> {

    /**
     * 获取用户未读消息数量
     */
    @Select("SELECT COUNT(*) FROM sys_chat_message " +
            "WHERE receiver_id = #{userId} AND is_read = 0 AND receiver_deleted = 0")
    int selectUnreadCount(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_chat_message " +
            "WHERE receiver_id = #{userId} AND sender_id = #{senderId} " +
            "AND is_read = 0 AND receiver_deleted = 0")
    int selectUnreadCountWithUser(@Param("userId") Long userId,
                                  @Param("senderId") Long senderId);

    /**
     * 获取两个用户之间的最新一条消息
     */
    @Select("SELECT * FROM sys_chat_message " +
            "WHERE (sender_id = #{userId} AND receiver_id = #{targetId} AND sender_deleted = 0) " +
            "   OR (sender_id = #{targetId} AND receiver_id = #{userId} AND receiver_deleted = 0) " +
            "ORDER BY id DESC LIMIT 1")
    SysChatMessage selectLatestMessage(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 每个可见会话返回最新一条消息。
     */
    @Select("SELECT message.* FROM sys_chat_message message " +
            "INNER JOIN (" +
            "  SELECT CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END AS contact_id, " +
            "         MAX(id) AS latest_id " +
            "  FROM sys_chat_message " +
            "  WHERE (sender_id = #{userId} AND sender_deleted = 0) " +
            "     OR (receiver_id = #{userId} AND receiver_deleted = 0) " +
            "  GROUP BY CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END" +
            ") latest ON latest.latest_id = message.id " +
            "ORDER BY message.id DESC LIMIT 50")
    List<SysChatMessage> selectVisibleRecentMessages(@Param("userId") Long userId);

    @Update("UPDATE sys_chat_message SET sender_deleted = 1 " +
            "WHERE sender_id = #{userId} AND receiver_id = #{targetId} " +
            "AND sender_deleted = 0")
    int hideSentHistory(@Param("userId") Long userId,
                        @Param("targetId") Long targetId);

    @Update("UPDATE sys_chat_message SET receiver_deleted = 1 " +
            "WHERE sender_id = #{targetId} AND receiver_id = #{userId} " +
            "AND receiver_deleted = 0")
    int hideReceivedHistory(@Param("userId") Long userId,
                            @Param("targetId") Long targetId);
}
