package top.pxczxn.community.block.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.pxczxn.community.block.model.CommunityBlock;

@Mapper
public interface CommunityBlockMapper extends BaseMapper<CommunityBlock> {
}
