package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.ArticleVersionPageView;

import java.util.List;

public record ArticleVersionPageResponse(
        List<ArticleVersionSummaryResponse> list,
        int pageNum,
        int pageSize,
        long total
) {

    static ArticleVersionPageResponse from(ArticleVersionPageView view) {
        return new ArticleVersionPageResponse(
                view.list().stream().map(ArticleVersionSummaryResponse::from).toList(),
                view.pageNum(),
                view.pageSize(),
                view.total()
        );
    }
}
