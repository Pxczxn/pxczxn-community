package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.FavoriteContentPageView;

import java.util.List;

public record FavoriteContentPageResponse(
        String folderId,
        List<FavoriteContentResponse> records,
        long total,
        int pageNum,
        int pageSize
) {

    static FavoriteContentPageResponse from(
            FavoriteContentPageView view
    ) {
        return new FavoriteContentPageResponse(
                view.folderId().toString(),
                view.records().stream()
                        .map(FavoriteContentResponse::from)
                        .toList(),
                view.total(),
                view.pageNum(),
                view.pageSize()
        );
    }
}
