package top.pxczxn.community.governance.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @TableName("community_account_enforcement_appeal")
public class CommunityAccountEnforcementAppeal {
    @TableId(type = IdType.INPUT) private Long id;
    private Long caseId; private Long appellantUserId; private String statement; private String evidenceSnapshot; private String status; private Long reviewedByAdminId; private String reviewNote; private LocalDateTime createdAt; private LocalDateTime reviewedAt;
    private Long primaryReviewedByAdminId; private String primaryDecision; private String primaryReviewNote; private LocalDateTime primaryReviewedAt;
    private Long finalReviewedByAdminId; private String finalDecision; private String finalReviewNote; private LocalDateTime finalReviewedAt;
}
