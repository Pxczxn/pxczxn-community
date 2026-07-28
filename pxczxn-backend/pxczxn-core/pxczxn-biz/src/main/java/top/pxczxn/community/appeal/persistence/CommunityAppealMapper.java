package top.pxczxn.community.appeal.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.appeal.model.CommunityAppeal;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommunityAppealMapper extends BaseMapper<CommunityAppeal> {
    @Select("SELECT * FROM community_appeal WHERE appellant_user_id=#{userId} ORDER BY created_at DESC")
    List<CommunityAppeal> mine(@Param("userId") Long userId);

    @Select("SELECT * FROM community_appeal WHERE status='PENDING' ORDER BY created_at ASC")
    List<CommunityAppeal> queue();

    @Update("UPDATE community_appeal SET status=#{status},reviewer_admin_id=#{adminId},review_note=#{note},reviewed_at=#{now},updated_at=#{now},lock_version=lock_version+1 WHERE id=#{id} AND status='PENDING' AND lock_version=#{lock}")
    int review(@Param("id") Long id, @Param("adminId") Long adminId, @Param("status") String status, @Param("note") String note, @Param("lock") Integer lock, @Param("now") LocalDateTime now);
}
