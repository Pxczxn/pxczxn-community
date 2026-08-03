package top.pxczxn.community.governance.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_account_enforcement_case")
public class CommunityAccountEnforcementCase {
    @TableId(type = IdType.INPUT) private Long id;
    private Long targetUserId;
    private String measureType;
    private String status;
    private String reasonCode;
    private String userVisibleReason;
    private String internalReason;
    private String evidenceSnapshot;
    private String cleanupScope;
    private Long sourceReportId;
    private Long requestedByAdminId;
    private Long requestedByTeamId;
    private LocalDateTime requestedAt;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private Boolean appealAllowed;
    private LocalDateTime appealDeadlineAt;
    private LocalDateTime executeAfter;
    private LocalDateTime finalizedAt;
    private Integer lockVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
