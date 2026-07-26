package top.pxczxn.community.article.application;

import java.util.List;

public record PublicArticlePageView(
        List<PublicArticleSummaryView> records,
        long total,
        int pageNum,
        int pageSize
) {
}
