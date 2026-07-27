package top.pxczxn.community.series.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team_series_article")
public class TeamSeriesArticle {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long seriesId;
    private Long articleId;
    private Integer chapterOrder;
    private Long addedByUserId;
    private LocalDateTime createdAt;
}
