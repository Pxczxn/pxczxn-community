package top.pxczxn.community.appeal.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.pxczxn.community.appeal.model.CommunityAppealEvent;

@Mapper
public interface CommunityAppealEventMapper extends BaseMapper<CommunityAppealEvent> {
}
