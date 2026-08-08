package top.pxczxn.community.team.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.team.model.Team;

@Mapper
public interface TeamMapper extends BaseMapper<Team> {

    @Select("SELECT * FROM team WHERE blog_id = #{blogId} AND deleted_at IS NULL LIMIT 1")
    Team findByBlogId(@Param("blogId") Long blogId);

    /**
     * Update team owner with optimistic lock (id + lock_version + current owner).
     * Returns 1 if successful, 0 if concurrent modification or owner changed.
     */
    @Update("UPDATE team SET owner_user_id = #{newOwnerId}, lock_version = lock_version + 1, updated_at = NOW() " +
            "WHERE id = #{teamId} AND owner_user_id = #{currentOwnerId} AND lock_version = #{lockVersion}")
    int updateOwnerWithOptimisticLock(@Param("teamId") Long teamId,
                                       @Param("currentOwnerId") Long currentOwnerId,
                                       @Param("newOwnerId") Long newOwnerId,
                                       @Param("lockVersion") Integer lockVersion);

    @Update("UPDATE team SET status = 'DISBANDED', lock_version = lock_version + 1, updated_at = NOW() "
            + "WHERE id = #{teamId} AND owner_user_id = #{ownerUserId} AND status = 'ACTIVE' "
            + "AND lock_version = #{lockVersion}")
    int disbandWithOptimisticLock(@Param("teamId") Long teamId,
                                  @Param("ownerUserId") Long ownerUserId,
                                  @Param("lockVersion") Integer lockVersion);

    /**
     * Update team portal settings with optimistic lock. Contract: full replace of the listed
     * columns — callers must send the complete desired profile (the team settings page always
     * submits all fields). Returns 1 if successful, 0 if concurrent modification.
     */
    @Update("""
            UPDATE team SET
              category = #{category},
              content_direction = #{contentDirection},
              theme = #{theme},
              seo_title = #{seoTitle},
              seo_description = #{seoDescription},
              public_members = #{publicMembers},
              allow_submissions = #{allowSubmissions},
              submission_guideline = #{submissionGuideline},
              contact_info = #{contactInfo},
              lock_version = lock_version + 1,
              updated_at = NOW()
            WHERE id = #{teamId} AND lock_version = #{lockVersion}
            """)
    int updatePortalSettingsWithOptimisticLock(@Param("teamId") Long teamId,
                                               @Param("category") String category,
                                               @Param("contentDirection") String contentDirection,
                                               @Param("theme") String theme,
                                               @Param("seoTitle") String seoTitle,
                                               @Param("seoDescription") String seoDescription,
                                               @Param("publicMembers") Boolean publicMembers,
                                               @Param("allowSubmissions") Boolean allowSubmissions,
                                               @Param("submissionGuideline") String submissionGuideline,
                                               @Param("contactInfo") String contactInfo,
                                               @Param("lockVersion") Integer lockVersion);
}
