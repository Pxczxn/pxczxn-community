package top.pxczxn.community.taxonomy.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.taxonomy.model.PlatformTag;

public interface PlatformTagMapper extends BaseMapper<PlatformTag> {

    @Update("""
            UPDATE platform_tag
            SET usage_count = (
                SELECT COUNT(*)
                FROM article_tag
                WHERE tag_id = #{tagId}
            )
            WHERE id = #{tagId}
            """)
    int refreshUsageCount(@Param("tagId") Long tagId);
}
