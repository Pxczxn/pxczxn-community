package top.pxczxn.community.social.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.social.model.FavoriteFolder;

public interface FavoriteFolderMapper extends BaseMapper<FavoriteFolder> {

    @Select("""
            SELECT *
            FROM favorite_folder
            WHERE owner_user_id = #{ownerUserId}
              AND is_default = 1
              AND deleted_at IS NULL
            LIMIT 1
            """)
    FavoriteFolder findDefault(
            @Param("ownerUserId") Long ownerUserId
    );

    @Select("""
            SELECT COUNT(*)
            FROM favorite_folder
            WHERE owner_user_id = #{ownerUserId}
              AND is_default = 0
              AND deleted_at IS NULL
            """)
    long countCustomFolders(
            @Param("ownerUserId") Long ownerUserId
    );
}
