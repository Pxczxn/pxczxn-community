package top.pxczxn.community.series.persistence;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SeriesPopularRow {
    private Long id;
    private Long blogId;
    private String title;
    private String slug;
    private String summary;
    private Long coverFileId;
    private LocalDateTime publishedAt;
    private String blogName;
    private String blogSlug;
    private String blogType;
    private String creatorName;
    private Long followCount;
}