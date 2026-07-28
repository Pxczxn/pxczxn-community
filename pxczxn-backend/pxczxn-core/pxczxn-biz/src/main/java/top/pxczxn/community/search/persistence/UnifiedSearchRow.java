package top.pxczxn.community.search.persistence;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UnifiedSearchRow {
    private String resultType;
    private Long targetId;
    private String title;
    private String excerpt;
    private String canonicalPath;
    private Long authorUserId;
    private String authorName;
    private String blogName;
    private LocalDateTime occurredAt;
}
