package top.pxczxn.community.moderation.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("content_review_task")
public class ContentReviewTask {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String subjectType;

    private Long subjectId;

    private Long articleId;

    private Long fixedVersionId;

    private String reviewStage;

    private String reviewType;

    private String status;

    private String riskLevel;

    private String idempotencyKey;

    private Long submittedByUserId;

    private Long assigneeAdminId;

    private String resultCode;

    private String resultReason;

    private LocalDateTime submittedAt;

    private LocalDateTime claimedAt;

    private LocalDateTime completedAt;

    private Integer lockVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
