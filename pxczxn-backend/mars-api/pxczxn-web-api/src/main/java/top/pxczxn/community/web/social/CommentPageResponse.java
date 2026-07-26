package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.CommentPageView;

import java.util.List;

public record CommentPageResponse(
        String targetType,
        String targetId,
        List<CommentThreadResponse> records,
        long total,
        long commentCount,
        int pageNum,
        int pageSize
) {

    static CommentPageResponse from(CommentPageView view) {
        return new CommentPageResponse(
                view.targetType(),
                view.targetId().toString(),
                view.records().stream()
                        .map(CommentThreadResponse::from)
                        .toList(),
                view.total(),
                view.commentCount(),
                view.pageNum(),
                view.pageSize()
        );
    }
}
