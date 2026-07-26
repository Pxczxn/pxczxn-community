package top.pxczxn.community.notification.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.notification.model.CommunityNotification;

public interface CommunityNotificationMapper
        extends BaseMapper<CommunityNotification> {

    @Select("""
            SELECT *
            FROM community_notification
            WHERE deduplication_key = #{deduplicationKey}
            LIMIT 1
            """)
    CommunityNotification findByDeduplicationKey(
            @Param("deduplicationKey") String deduplicationKey
    );
}
