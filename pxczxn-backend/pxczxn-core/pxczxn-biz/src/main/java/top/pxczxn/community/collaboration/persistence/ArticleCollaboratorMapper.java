package top.pxczxn.community.collaboration.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.collaboration.model.ArticleCollaborator;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ArticleCollaboratorMapper extends BaseMapper<ArticleCollaborator> {
    @Select("SELECT * FROM article_collaborator WHERE article_id = #{articleId} AND revoked_at IS NULL ORDER BY attribution_order ASC, id ASC")
    List<ArticleCollaborator> findActiveByArticle(@Param("articleId") Long articleId);
    @Select("SELECT * FROM article_collaborator WHERE article_id = #{articleId} AND user_id = #{userId} AND revoked_at IS NULL LIMIT 1")
    ArticleCollaborator findActive(@Param("articleId") Long articleId, @Param("userId") Long userId);
    @Update("UPDATE article_collaborator SET revoked_at = #{now}, updated_at = #{now}, lock_version = lock_version + 1 WHERE id = #{id} AND article_id = #{articleId} AND revoked_at IS NULL AND lock_version = #{lockVersion}")
    int revoke(@Param("id") Long id, @Param("articleId") Long articleId, @Param("lockVersion") Integer lockVersion, @Param("now") LocalDateTime now);
    @Update("UPDATE article_collaborator SET attribution_order = #{order}, updated_at = #{now}, lock_version = lock_version + 1 WHERE id = #{id} AND article_id = #{articleId} AND revoked_at IS NULL AND lock_version = #{lockVersion}")
    int updateOrder(@Param("id") Long id, @Param("articleId") Long articleId, @Param("order") Integer order, @Param("lockVersion") Integer lockVersion, @Param("now") LocalDateTime now);
}
