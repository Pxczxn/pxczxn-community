package top.pxczxn.community.social.persistence;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FollowingFeedRow {
    private String itemType;
    private Long targetId;
    private Long authorUserId;
    private Long blogId;
    private String title;
    private String excerpt;
    private String canonicalPath;
    private String authorName;
    private String blogName;
    private String tagName;
    private LocalDateTime occurredAt;
    private Integer specialFollow;
}
