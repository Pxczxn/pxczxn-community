package top.pxczxn.community.sanction.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @TableName("community_sanction")
public class CommunitySanction {
    @TableId(type = IdType.INPUT) private Long id;
    private Long targetUserId; private String sanctionType; private String reasonCode; private String reasonNote; private Long sourceReportId; private Long issuedByAdminId;
    private LocalDateTime startsAt; private LocalDateTime expiresAt; private String status; private Long revokedByAdminId; private LocalDateTime revokedAt; private String revokeNote; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
