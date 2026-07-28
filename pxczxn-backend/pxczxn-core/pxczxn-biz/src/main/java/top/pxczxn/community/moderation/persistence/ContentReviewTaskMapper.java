package top.pxczxn.community.moderation.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.moderation.model.ContentReviewTask;

import java.time.LocalDateTime;

public interface ContentReviewTaskMapper extends BaseMapper<ContentReviewTask> {

    @Select("""
            SELECT MIN(submitted_at)
            FROM content_review_task
            WHERE status IN ('QUEUED', 'AUTO_REVIEWING', 'MANUAL_REVIEWING')
            """)
    LocalDateTime selectOldestOpenTaskSubmittedAt();
}
