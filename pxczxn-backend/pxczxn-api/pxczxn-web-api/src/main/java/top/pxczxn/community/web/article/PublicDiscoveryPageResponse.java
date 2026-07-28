package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.PublicDiscoveryPageView;

import java.util.List;

public record PublicDiscoveryPageResponse(List<PublicArticleSummaryResponse> records, long total, int pageNum, int pageSize, String sort, String sortExplanation) {
    static PublicDiscoveryPageResponse from(PublicDiscoveryPageView view) {
        var page = view.page();
        return new PublicDiscoveryPageResponse(page.records().stream().map(PublicArticleSummaryResponse::from).toList(), page.total(), page.pageNum(), page.pageSize(), view.sort().name(), view.sortExplanation());
    }
}
