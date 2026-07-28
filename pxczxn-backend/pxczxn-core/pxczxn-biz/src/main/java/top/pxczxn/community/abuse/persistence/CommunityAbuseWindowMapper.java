package top.pxczxn.community.abuse.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.pxczxn.community.abuse.model.CommunityAbuseWindow;

import java.time.LocalDateTime;

@Mapper
public interface CommunityAbuseWindowMapper extends BaseMapper<CommunityAbuseWindow> {

    @Insert("""
            INSERT INTO community_abuse_window (
                actor_key, action_type, window_started_at, attempt_count, rejected_count
            ) VALUES (
                #{actorKey}, #{actionType}, #{now}, 1, 0
            ) ON DUPLICATE KEY UPDATE
                attempt_count = IF(
                    window_started_at <= DATE_SUB(#{now}, INTERVAL #{windowSeconds} SECOND),
                    1,
                    attempt_count + 1
                ),
                window_started_at = IF(
                    window_started_at <= DATE_SUB(#{now}, INTERVAL #{windowSeconds} SECOND),
                    #{now},
                    window_started_at
                )
            """)
    int recordAttempt(
            @Param("actorKey") String actorKey,
            @Param("actionType") String actionType,
            @Param("now") LocalDateTime now,
            @Param("windowSeconds") int windowSeconds
    );
}
