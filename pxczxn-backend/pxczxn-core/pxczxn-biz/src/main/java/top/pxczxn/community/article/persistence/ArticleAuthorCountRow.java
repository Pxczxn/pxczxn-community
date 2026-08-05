package top.pxczxn.community.article.persistence;

import lombok.Getter;
import lombok.Setter;

/**
 * Article headcount grouped by author inside one blog.
 * Backs the "contribution" column of the team member list without an N+1 loop.
 */
@Getter
@Setter
public class ArticleAuthorCountRow {

    private Long authorUserId;

    private Integer total;
}
