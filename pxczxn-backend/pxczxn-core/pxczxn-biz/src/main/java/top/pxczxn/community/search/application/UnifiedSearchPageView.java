package top.pxczxn.community.search.application;

import java.util.List;

public record UnifiedSearchPageView(
        List<UnifiedSearchResultView> records,
        long total,
        int pageNum,
        int pageSize
) {
}
