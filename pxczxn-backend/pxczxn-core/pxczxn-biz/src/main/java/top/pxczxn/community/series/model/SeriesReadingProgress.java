package top.pxczxn.community.series.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lightweight per-reader progress inside one series.
 * Only two facts are stored: where the reader stopped, and how far they ever got.
 * Re-reading an earlier chapter moves {@code lastChapterOrder} back but never {@code maxChapterOrder}.
 */
@Getter
@Setter
@TableName("series_reading_progress")
public class SeriesReadingProgress {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long userId;

    private Long seriesId;

    private Long lastArticleId;

    private Integer lastChapterOrder;

    private Integer maxChapterOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
