package top.pxczxn.community.article.application;

import java.util.List;

public record ArticleVersionPageView(
        List<ArticleVersionSummaryView> list,
        int pageNum,
        int pageSize,
        long total
) {
}
