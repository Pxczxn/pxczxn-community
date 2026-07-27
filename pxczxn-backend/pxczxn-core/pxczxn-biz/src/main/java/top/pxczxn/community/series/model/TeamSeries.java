package top.pxczxn.community.series.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team_series")
public class TeamSeries {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long teamId;
    private Long createdByUserId;
    private String title;
    private String slug;
    private String summary;
    private Long coverFileId;
    private String serializationStatus;
    private String reviewStatus;
    private Long reviewerAdminId;
    private String reviewComment;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;
    private Integer lockVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
