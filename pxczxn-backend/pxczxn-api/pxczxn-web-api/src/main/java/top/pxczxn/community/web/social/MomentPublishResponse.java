package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.MomentPublishView;

public record MomentPublishResponse(
        MomentResponse moment,
        boolean moderationWarning,
        String moderationResult
) {

    static MomentPublishResponse from(MomentPublishView view) {
        return new MomentPublishResponse(
                MomentResponse.from(view.moment()),
                view.moderationWarning(),
                view.moderationResult()
        );
    }
}
