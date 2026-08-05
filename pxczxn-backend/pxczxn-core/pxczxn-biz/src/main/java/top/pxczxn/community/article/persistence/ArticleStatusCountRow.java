package top.pxczxn.community.article.persistence;

import lombok.Getter;
import lombok.Setter;

/**
 * Article headcount grouped by {@code publish_status} for a single blog.
 * Used by the team workspace dashboard so one query feeds every status card.
 */
@Getter
@Setter
public class ArticleStatusCountRow {

    private String status;

    private Integer total;
}
