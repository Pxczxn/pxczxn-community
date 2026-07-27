package top.pxczxn.community.blog.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.blog.model.Blog;

@Mapper
public interface BlogMapper extends BaseMapper<Blog> {

    /**
     * Update blog owner with optimistic lock (id + lock_version).
     * Returns 1 if successful, 0 if concurrent modification.
     */
    @Update("UPDATE blog SET owner_user_id = #{newOwnerId}, lock_version = lock_version + 1, updated_at = NOW() " +
            "WHERE id = #{blogId} AND lock_version = #{lockVersion}")
    int updateOwnerWithOptimisticLock(@Param("blogId") Long blogId,
                                       @Param("newOwnerId") Long newOwnerId,
                                       @Param("lockVersion") Integer lockVersion);
}
