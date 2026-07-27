package top.pxczxn.community.team.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import top.pxczxn.community.team.model.Team;

@Mapper
public interface TeamMapper extends BaseMapper<Team> {

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
}
