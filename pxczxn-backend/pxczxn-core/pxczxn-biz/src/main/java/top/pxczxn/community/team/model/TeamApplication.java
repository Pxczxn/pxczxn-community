package top.pxczxn.community.team.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team_application")
public class TeamApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicantUserId;

    private String teamName;

    private String teamSlug;

    private String description;

    private String applicationData;

    private String idempotencyKey;

    private String status;

    private Long reviewerUserId;

    private String reviewComment;

    private LocalDateTime reviewedAt;

    private Integer lockVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
