package top.pxczxn.community.article.persistence;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Article headcount grouped by author inside one blog.
 * Backs the "contribution" and "last active" columns of the team member list without an N+1 loop.
 */
@Getter
@Setter
public class ArticleAuthorCountRow {

    private Long authorUserId;

    private Integer total;

    /** Most recent article update by this author in the blog; null when the author has no articles. */
    private LocalDateTime lastActiveAt;
}
