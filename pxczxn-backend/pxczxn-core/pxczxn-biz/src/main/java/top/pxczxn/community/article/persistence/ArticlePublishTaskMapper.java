package top.pxczxn.community.article.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.pxczxn.community.article.model.ArticlePublishTask;

import java.time.LocalDateTime;
import java.util.List;

public interface ArticlePublishTaskMapper extends BaseMapper<ArticlePublishTask> {

    @Select("""
            SELECT *
            FROM article_publish_task
            WHERE article_id = #{articleId}
              AND status IN ('WAITING', 'RUNNING', 'RETRY_WAIT')
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    ArticlePublishTask selectActiveByArticleId(
            @Param("articleId") Long articleId
    );

    @Select("""
            SELECT *
            FROM article_publish_task
            WHERE (
                    status IN ('WAITING', 'RETRY_WAIT')
                    AND next_attempt_at <= #{now}
                  )
               OR (
                    status = 'RUNNING'
                    AND updated_at <= #{staleBefore}
                  )
            ORDER BY next_attempt_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<ArticlePublishTask> selectRunnable(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("limit") int limit
    );
}
