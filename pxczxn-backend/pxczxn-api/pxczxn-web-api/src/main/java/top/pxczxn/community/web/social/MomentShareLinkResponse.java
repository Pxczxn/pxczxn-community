package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.MomentShareLinkView;

public record MomentShareLinkResponse(
        String momentId,
        String canonicalPath
) {

    static MomentShareLinkResponse from(MomentShareLinkView view) {
        return new MomentShareLinkResponse(
                view.momentId().toString(),
                view.canonicalPath()
        );
    }
}
