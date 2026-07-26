package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.MomentDeletionView;

public record MomentDeletionResponse(
        String momentId,
        String status,
        int lockVersion,
        boolean idempotentReplay,
        long sourceRepostCount
) {

    static MomentDeletionResponse from(MomentDeletionView view) {
        return new MomentDeletionResponse(
                view.momentId().toString(),
                view.status(),
                view.lockVersion(),
                view.idempotentReplay(),
                view.sourceRepostCount()
        );
    }
}
