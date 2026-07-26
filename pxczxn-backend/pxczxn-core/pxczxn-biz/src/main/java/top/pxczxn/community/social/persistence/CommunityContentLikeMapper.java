package top.pxczxn.community.social.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.social.model.CommunityContentLike;

public interface CommunityContentLikeMapper
        extends BaseMapper<CommunityContentLike> {

    @Select("""
            SELECT *
            FROM community_content_like
            WHERE user_id = #{userId}
              AND target_type = #{targetType}
              AND target_id = #{targetId}
            LIMIT 1
            """)
    CommunityContentLike findRelation(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Delete("""
            DELETE FROM community_content_like
            WHERE user_id = #{userId}
              AND target_type = #{targetType}
              AND target_id = #{targetId}
            """)
    int deleteRelation(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );
}
