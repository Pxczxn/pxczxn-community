package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.LikeListPrivacyView;

public record LikeListPrivacyResponse(
        String ownerUserId,
        String visibility
) {

    static LikeListPrivacyResponse from(LikeListPrivacyView view) {
        return new LikeListPrivacyResponse(
                view.ownerUserId() == null
                        ? null
                        : view.ownerUserId().toString(),
                view.visibility()
        );
    }
}
