package top.pxczxn.community.article.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("article_publish_task")
public class ArticlePublishTask {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long articleId;

    private Long articleVersionId;

    private LocalDateTime scheduledPublishAt;

    private String status;

    private Integer attemptCount;

    private Integer maxAttempts;

    private LocalDateTime nextAttemptAt;

    private String lastErrorCode;

    private String lastErrorMessage;

    private Integer lockVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;
}
