package top.pxczxn.community.governance.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_account_enforcement_review")
public class CommunityAccountEnforcementReview {
    @TableId(type = IdType.AUTO) private Long id;
    private Long caseId;
    private String stage;
    private Long reviewerAdminId;
    private String decision;
    private String reviewNote;
    private LocalDateTime createdAt;
}
