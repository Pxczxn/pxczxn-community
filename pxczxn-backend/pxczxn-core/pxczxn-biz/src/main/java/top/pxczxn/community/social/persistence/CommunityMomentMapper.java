package top.pxczxn.community.social.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.social.model.CommunityMoment;

import java.util.List;

@Mapper
public interface CommunityMomentMapper extends BaseMapper<CommunityMoment> {

    /**
     * Latest public moments for the Discover Hub. Strictly filtered to:
     * - visibility = PUBLIC, status = PUBLISHED, deleted_at IS NULL
     * - blog ACTIVE / not deleted
     * - actor user status NORMAL / LIMITED
     */
    @Select("""
            SELECT m.id, m.actor_user_id, m.blog_id, m.moment_type, m.text_content,
                   m.rendered_html, m.link_url, m.article_id, m.repost_moment_id,
                   m.like_count, m.favorite_count, m.comment_count, m.repost_count,
                   m.created_at,
                   COALESCE(u.display_name, u.username) AS author_name,
                   b.name AS blog_name, b.slug AS blog_slug
            FROM community_moment m
            INNER JOIN community_user u ON u.id = m.actor_user_id AND u.status IN ('NORMAL', 'LIMITED')
            INNER JOIN blog b ON b.id = m.blog_id AND b.status = 'ACTIVE' AND b.deleted_at IS NULL
            WHERE m.deleted_at IS NULL AND m.visibility = 'PUBLIC' AND m.status = 'PUBLISHED'
            ORDER BY m.created_at DESC, m.id DESC
            LIMIT #{limit}
            """)
    List<MomentLatestRow> selectLatest(@Param("limit") int limit);
}