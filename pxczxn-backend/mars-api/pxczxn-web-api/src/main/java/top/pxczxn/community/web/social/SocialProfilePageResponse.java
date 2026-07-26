package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.SocialProfilePageView;

import java.util.List;

public record SocialProfilePageResponse(
        List<SocialProfileResponse> records,
        long total,
        int pageNum,
        int pageSize
) {

    static SocialProfilePageResponse from(SocialProfilePageView view) {
        return new SocialProfilePageResponse(
                view.records().stream().map(SocialProfileResponse::from).toList(),
                view.total(),
                view.pageNum(),
                view.pageSize()
        );
    }
}
