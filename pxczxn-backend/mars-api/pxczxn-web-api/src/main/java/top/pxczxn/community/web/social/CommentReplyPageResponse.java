package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.CommentReplyPageView;

import java.util.List;

public record CommentReplyPageResponse(
        String rootCommentId,
        List<CommentResponse> records,
        long total,
        int pageNum,
        int pageSize
) {

    static CommentReplyPageResponse from(CommentReplyPageView view) {
        return new CommentReplyPageResponse(
                view.rootCommentId().toString(),
                view.records().stream()
                        .map(CommentResponse::from)
                        .toList(),
                view.total(),
                view.pageNum(),
                view.pageSize()
        );
    }
}
