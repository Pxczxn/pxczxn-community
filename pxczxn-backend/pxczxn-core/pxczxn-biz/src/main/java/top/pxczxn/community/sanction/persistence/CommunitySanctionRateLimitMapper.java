package top.pxczxn.community.sanction.persistence;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommunitySanctionRateLimitMapper {

    @Update("""
            UPDATE community_sanction_rate_limit
            SET last_action_at = #{now}
            WHERE user_id = #{userId}
              AND action_type = #{actionType}
              AND last_action_at <= #{allowedAfter}
            """)
    int refreshIfWindowElapsed(
            @Param("userId") Long userId,
            @Param("actionType") String actionType,
            @Param("now") LocalDateTime now,
            @Param("allowedAfter") LocalDateTime allowedAfter
    );

    @Insert("""
            INSERT INTO community_sanction_rate_limit (user_id, action_type, last_action_at)
            VALUES (#{userId}, #{actionType}, #{now})
            """)
    int create(
            @Param("userId") Long userId,
            @Param("actionType") String actionType,
            @Param("now") LocalDateTime now
    );
}
