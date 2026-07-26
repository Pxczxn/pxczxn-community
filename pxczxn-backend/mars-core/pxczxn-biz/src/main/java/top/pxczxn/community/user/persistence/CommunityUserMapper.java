package top.pxczxn.community.user.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.user.model.CommunityUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CommunityUserMapper extends BaseMapper<CommunityUser> {

    @Update("""
            UPDATE community_user
            SET last_login_at = #{loginAt}
            WHERE id = #{userId}
            """)
    int recordLoginSuccess(
            @Param("userId") Long userId,
            @Param("loginAt") LocalDateTime loginAt
    );

    @org.apache.ibatis.annotations.Select("""
            SELECT DATE(created_at) AS day, COUNT(*) AS count
            FROM community_user
            WHERE created_at >= #{from}
            GROUP BY DATE(created_at)
            ORDER BY day
            """)
    List<Map<String, Object>> selectDailyRegistrations(
            @Param("from") LocalDateTime from
    );
}
