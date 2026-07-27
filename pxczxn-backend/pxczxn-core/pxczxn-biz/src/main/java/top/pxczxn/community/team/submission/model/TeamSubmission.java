package top.pxczxn.community.team.submission.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team_submission")
public class TeamSubmission {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long sourceArticleId;
    private Long fixedSourceVersionId;
    private Long targetTeamId;
    private Long submittedByUserId;
    private Long supersedesSubmissionId;
    private String status;
    private Long teamReviewerUserId;
    private String teamReviewComment;
    private LocalDateTime teamReviewedAt;
    private Long platformReviewerAdminId;
    private String platformReviewComment;
    private LocalDateTime platformReviewedAt;
    private Long publishedTeamArticleId;
    private String idempotencyKey;
    private Integer lockVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
