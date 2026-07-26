package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.PublicArticlePageView;

import java.util.List;

public record PublicArticlePageResponse(
        List<PublicArticleSummaryResponse> records,
        long total,
        int pageNum,
        int pageSize
) {

    static PublicArticlePageResponse from(PublicArticlePageView view) {
        return new PublicArticlePageResponse(
                view.records().stream()
                        .map(PublicArticleSummaryResponse::from)
                        .toList(),
                view.total(),
                view.pageNum(),
                view.pageSize()
        );
    }
}
