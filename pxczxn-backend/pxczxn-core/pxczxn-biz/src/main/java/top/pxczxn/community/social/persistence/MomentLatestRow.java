package top.pxczxn.community.social.persistence;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MomentLatestRow {
    private Long id;
    private Long actorUserId;
    private Long blogId;
    private String momentType;
    private String textContent;
    private String renderedHtml;
    private String linkUrl;
    private Long articleId;
    private Long repostMomentId;
    private Long likeCount;
    private Long favoriteCount;
    private Long commentCount;
    private Long repostCount;
    private LocalDateTime createdAt;
    private String authorName;
    private String blogName;
    private String blogSlug;
}