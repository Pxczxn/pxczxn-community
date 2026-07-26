package top.pxczxn.platform.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.pxczxn.platform.message.entity.ChatGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群成员Mapper
 */
@Mapper
public interface ChatGroupMemberMapper extends BaseMapper<ChatGroupMember> {
    
    /**
     * 查询群成员列表（带用户信息）
     */
    @Select("SELECT m.*, u.username, u.nickname as user_nickname, u.avatar " +
            "FROM sys_chat_group_member m " +
            "LEFT JOIN sys_user u ON m.user_id = u.id " +
            "WHERE m.group_id = #{groupId} " +
            "ORDER BY m.role DESC, m.join_time ASC")
    List<ChatGroupMember> selectGroupMembers(@Param("groupId") Long groupId);
    
    /**
     * 查询用户在群中的信息
     */
    @Select("SELECT m.*, u.username, u.nickname as user_nickname, u.avatar " +
            "FROM sys_chat_group_member m " +
            "LEFT JOIN sys_user u ON m.user_id = u.id " +
            "WHERE m.group_id = #{groupId} AND m.user_id = #{userId}")
    ChatGroupMember selectMemberInfo(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    /**
     * 查询群内所有成员ID
     */
    @Select("SELECT user_id FROM sys_chat_group_member WHERE group_id = #{groupId}")
    List<Long> selectMemberIds(@Param("groupId") Long groupId);

    /**
     * 获取用户全部活跃群聊的未读消息数。
     */
    @Select("SELECT COUNT(*) FROM sys_chat_group_message message " +
            "INNER JOIN sys_chat_group_member member ON member.group_id = message.group_id " +
            "INNER JOIN sys_chat_group chat_group ON chat_group.id = member.group_id " +
            "WHERE member.user_id = #{userId} AND chat_group.status = 1 " +
            "AND message.id > member.last_read_message_id " +
            "AND message.sender_id <> #{userId}")
    int selectUnreadCount(@Param("userId") Long userId);

    /**
     * 获取用户在指定群的未读消息数。
     */
    @Select("SELECT COUNT(*) FROM sys_chat_group_message message " +
            "INNER JOIN sys_chat_group_member member ON member.group_id = message.group_id " +
            "WHERE member.group_id = #{groupId} AND member.user_id = #{userId} " +
            "AND message.id > member.last_read_message_id " +
            "AND message.sender_id <> #{userId}")
    int selectGroupUnreadCount(@Param("groupId") Long groupId,
                               @Param("userId") Long userId);

    @Update("UPDATE sys_chat_group_member " +
            "SET last_read_message_id = #{messageId}, last_read_time = #{readTime} " +
            "WHERE group_id = #{groupId} AND user_id = #{userId} " +
            "AND last_read_message_id < #{messageId}")
    int updateReadCursor(@Param("groupId") Long groupId,
                         @Param("userId") Long userId,
                         @Param("messageId") Long messageId,
                         @Param("readTime") LocalDateTime readTime);
}
