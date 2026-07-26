package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.CommentModerationView;

public record CommentModerationResponse(
        String commentId,
        String status,
        int affectedComments,
        long targetCommentCount
) {

    static CommentModerationResponse from(CommentModerationView view) {
        return new CommentModerationResponse(
                view.commentId().toString(),
                view.status(),
                view.affectedComments(),
                view.targetCommentCount()
        );
    }
}
