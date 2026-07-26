package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.LikedContentPageView;

import java.util.List;

public record LikedContentPageResponse(
        List<LikedContentResponse> records,
        long total,
        int pageNum,
        int pageSize
) {

    static LikedContentPageResponse from(LikedContentPageView view) {
        return new LikedContentPageResponse(
                view.records().stream()
                        .map(LikedContentResponse::from)
                        .toList(),
                view.total(),
                view.pageNum(),
                view.pageSize()
        );
    }
}
