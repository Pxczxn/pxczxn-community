package top.pxczxn.community.report.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.report.model.CommunityReport;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommunityReportMapper extends BaseMapper<CommunityReport> {
    @Select("SELECT * FROM community_report WHERE reporter_user_id=#{userId} ORDER BY created_at DESC")
    List<CommunityReport> findByReporter(@Param("userId") Long userId);
    @Select("SELECT * FROM community_report WHERE status IN ('PENDING','ASSIGNED') AND (#{status} IS NULL OR status=#{status}) ORDER BY created_at ASC")
    List<CommunityReport> findQueue(@Param("status") String status);
    @Update("UPDATE community_report SET status='ASSIGNED',assignee_admin_id=#{adminId},updated_at=#{now},lock_version=lock_version+1 WHERE id=#{id} AND status='PENDING' AND lock_version=#{lock}")
    int claim(@Param("id") Long id, @Param("adminId") Long adminId, @Param("lock") Integer lock, @Param("now") LocalDateTime now);
    @Update("UPDATE community_report SET status=#{status},resolution_code=#{code},resolution_note=#{note},resolved_at=#{now},updated_at=#{now},lock_version=lock_version+1 WHERE id=#{id} AND status='ASSIGNED' AND assignee_admin_id=#{adminId} AND lock_version=#{lock}")
    int resolve(@Param("id") Long id, @Param("adminId") Long adminId, @Param("status") String status, @Param("code") String code, @Param("note") String note, @Param("lock") Integer lock, @Param("now") LocalDateTime now);
}
