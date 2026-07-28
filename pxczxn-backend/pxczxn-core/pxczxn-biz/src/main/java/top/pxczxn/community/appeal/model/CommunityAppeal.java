package top.pxczxn.community.appeal.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("community_appeal")
public class CommunityAppeal {
    @TableId(type = IdType.INPUT) private Long id;
    private Long reportId;
    private Long appellantUserId;
    private String appealReason;
    private String evidenceJson;
    private String status;
    private Long reviewerAdminId;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private Integer lockVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
