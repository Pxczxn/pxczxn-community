package top.pxczxn.community.collaboration.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.pxczxn.community.collaboration.model.ArticleCollaborationAuditEvent;

@Mapper
public interface ArticleCollaborationAuditEventMapper extends BaseMapper<ArticleCollaborationAuditEvent> {
}
