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

    /**
     * Update blog profile with optimistic lock (id + lock_version).
     * Contract: this is a full replace of name/summary/avatar/background — callers must send
     * the complete desired profile (the team settings page always submits all fields).
     * Returns 1 if successful, 0 if concurrent modification.
     */
    @Update("UPDATE blog SET name = #{name}, summary = #{summary}, avatar_file_id = #{avatarFileId}, "
            + "background_file_id = #{backgroundFileId}, lock_version = lock_version + 1 "
            + "WHERE id = #{blogId} AND lock_version = #{lockVersion}")
    int updateProfileWithOptimisticLock(@Param("blogId") Long blogId,
                                        @Param("name") String name,
                                        @Param("summary") String summary,
                                        @Param("avatarFileId") Long avatarFileId,
                                        @Param("backgroundFileId") Long backgroundFileId,
                                        @Param("lockVersion") Integer lockVersion);
}
