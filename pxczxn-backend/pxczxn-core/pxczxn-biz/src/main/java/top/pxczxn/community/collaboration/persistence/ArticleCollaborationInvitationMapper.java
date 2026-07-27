package top.pxczxn.community.collaboration.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.collaboration.model.ArticleCollaborationInvitation;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ArticleCollaborationInvitationMapper extends BaseMapper<ArticleCollaborationInvitation> {
    @Select("SELECT * FROM article_collaboration_invitation WHERE idempotency_key = #{key} LIMIT 1")
    ArticleCollaborationInvitation findByIdempotencyKey(@Param("key") String key);
    @Select("SELECT * FROM article_collaboration_invitation WHERE invitee_user_id = #{userId} AND status = 'PENDING' ORDER BY created_at DESC")
    List<ArticleCollaborationInvitation> findPendingByInvitee(@Param("userId") Long userId);
    @Update("UPDATE article_collaboration_invitation SET status = #{status}, responded_at = #{now}, updated_at = #{now}, lock_version = lock_version + 1 WHERE id = #{id} AND invitee_user_id = #{inviteeId} AND status = 'PENDING' AND lock_version = #{lockVersion}")
    int respond(@Param("id") Long id, @Param("inviteeId") Long inviteeId, @Param("lockVersion") Integer lockVersion, @Param("status") String status, @Param("now") LocalDateTime now);
    @Update("UPDATE article_collaboration_invitation SET status = 'CANCELLED', responded_at = #{now}, updated_at = #{now}, lock_version = lock_version + 1 WHERE id = #{id} AND invited_by_user_id = #{inviterId} AND status = 'PENDING' AND lock_version = #{lockVersion}")
    int cancel(@Param("id") Long id, @Param("inviterId") Long inviterId, @Param("lockVersion") Integer lockVersion, @Param("now") LocalDateTime now);
}
