package top.pxczxn.community.social.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.social.model.FavoriteFolderItem;
import top.pxczxn.community.social.model.FavoriteItem;

import java.util.List;

public interface FavoriteFolderItemMapper
        extends BaseMapper<FavoriteFolderItem> {

    @Select("""
            SELECT folder_id
            FROM favorite_folder_item
            WHERE favorite_item_id = #{favoriteItemId}
            ORDER BY created_at, id
            """)
    List<Long> findFolderIds(
            @Param("favoriteItemId") Long favoriteItemId
    );

    @Select("""
            SELECT COUNT(*)
            FROM favorite_folder_item
            WHERE folder_id = #{folderId}
              AND favorite_item_id = #{favoriteItemId}
            """)
    long countMapping(
            @Param("folderId") Long folderId,
            @Param("favoriteItemId") Long favoriteItemId
    );

    @Delete("""
            DELETE FROM favorite_folder_item
            WHERE folder_id = #{folderId}
              AND favorite_item_id = #{favoriteItemId}
            """)
    int deleteMapping(
            @Param("folderId") Long folderId,
            @Param("favoriteItemId") Long favoriteItemId
    );

    @Delete("""
            DELETE FROM favorite_folder_item
            WHERE favorite_item_id = #{favoriteItemId}
            """)
    int deleteByFavoriteItem(
            @Param("favoriteItemId") Long favoriteItemId
    );

    @Delete("""
            DELETE FROM favorite_folder_item
            WHERE folder_id = #{folderId}
            """)
    int deleteByFolder(
            @Param("folderId") Long folderId
    );

    @Select("""
            SELECT item.*
            FROM favorite_folder_item mapping
            INNER JOIN favorite_item item
                    ON item.id = mapping.favorite_item_id
            WHERE mapping.folder_id = #{folderId}
            ORDER BY mapping.created_at DESC, mapping.id DESC
            """)
    List<FavoriteItem> findItems(
            @Param("folderId") Long folderId
    );
}
