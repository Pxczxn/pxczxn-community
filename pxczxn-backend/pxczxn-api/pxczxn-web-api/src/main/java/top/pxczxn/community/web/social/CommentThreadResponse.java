package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.CommentThreadView;

import java.util.List;

public record CommentThreadResponse(
        CommentResponse root,
        List<CommentResponse> replyPreview,
        long replyCount
) {

    static CommentThreadResponse from(CommentThreadView view) {
        return new CommentThreadResponse(
                CommentResponse.from(view.root()),
                view.replyPreview().stream()
                        .map(CommentResponse::from)
                        .toList(),
                view.replyCount()
        );
    }
}
