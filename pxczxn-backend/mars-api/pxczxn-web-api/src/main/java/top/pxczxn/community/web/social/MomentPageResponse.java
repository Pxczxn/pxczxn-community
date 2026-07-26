package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.MomentPageView;

import java.util.List;

public record MomentPageResponse(
        List<MomentResponse> records,
        long total,
        int pageNum,
        int pageSize
) {

    static MomentPageResponse from(MomentPageView view) {
        return new MomentPageResponse(
                view.records().stream()
                        .map(MomentResponse::from)
                        .toList(),
                view.total(),
                view.pageNum(),
                view.pageSize()
        );
    }
}
