package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.SocialCountsView;

public record SocialCountsResponse(
        long following,
        long followers,
        long mutual
) {

    static SocialCountsResponse from(SocialCountsView view) {
        return new SocialCountsResponse(
                view.following(), view.followers(), view.mutual()
        );
    }
}
