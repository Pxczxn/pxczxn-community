package top.pxczxn.community.social.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.social.model.FavoriteItem;

import java.util.List;

public interface FavoriteItemMapper extends BaseMapper<FavoriteItem> {

    @Select("""
            SELECT *
            FROM favorite_item
            WHERE owner_user_id = #{ownerUserId}
              AND target_type = #{targetType}
              AND target_id = #{targetId}
            LIMIT 1
            """)
    FavoriteItem findRelation(
            @Param("ownerUserId") Long ownerUserId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Delete("""
            DELETE FROM favorite_item
            WHERE owner_user_id = #{ownerUserId}
              AND target_type = #{targetType}
              AND target_id = #{targetId}
            """)
    int deleteRelation(
            @Param("ownerUserId") Long ownerUserId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Select("""
            SELECT *
            FROM favorite_item
            WHERE target_type = #{targetType}
              AND target_id = #{targetId}
            ORDER BY created_at DESC, id DESC
            """)
    List<FavoriteItem> findByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );
}
