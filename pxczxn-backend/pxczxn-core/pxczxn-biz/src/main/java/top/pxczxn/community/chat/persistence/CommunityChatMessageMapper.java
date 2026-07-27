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
            SELECT * FROM community_chat_message
            WHERE ((sender_user_id = #{actor} AND recipient_user_id = #{peer})
                OR (sender_user_id = #{peer} AND recipient_user_id = #{actor}))
              AND deleted_at IS NULL
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
}
