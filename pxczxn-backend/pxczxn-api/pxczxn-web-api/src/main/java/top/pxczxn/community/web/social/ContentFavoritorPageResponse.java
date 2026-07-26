package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.ContentFavoritorPageView;

import java.util.List;

public record ContentFavoritorPageResponse(
        List<ContentFavoritorResponse> records,
        long total,
        int pageNum,
        int pageSize
) {

    static ContentFavoritorPageResponse from(
            ContentFavoritorPageView view
    ) {
        return new ContentFavoritorPageResponse(
                view.records().stream()
                        .map(ContentFavoritorResponse::from)
                        .toList(),
                view.total(),
                view.pageNum(),
                view.pageSize()
        );
    }
}
