package top.pxczxn.community.report.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @TableName("community_report_event")
public class CommunityReportEvent {
    @TableId(type = IdType.AUTO) private Long id;
    private Long reportId; private String actorType; private Long actorId; private String eventType;
    private String beforeSnapshot; private String afterSnapshot; private LocalDateTime occurredAt;
}
