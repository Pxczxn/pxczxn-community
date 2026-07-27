package top.pxczxn.community.report.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @TableName("community_report")
public class CommunityReport {
    @TableId(type = IdType.INPUT) private Long id;
    private Long reporterUserId; private String targetType; private Long targetId; private String reasonCode;
    private String description; private String evidenceJson; private String status; private Long assigneeAdminId;
    private String resolutionCode; private String resolutionNote; private LocalDateTime resolvedAt;
    private Integer lockVersion; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
