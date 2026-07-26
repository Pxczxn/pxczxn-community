package top.pxczxn.community.social.application;

import java.util.List;

public record CommentReplyPageView(
        Long rootCommentId,
        List<CommentView> records,
        long total,
        int pageNum,
        int pageSize
) {
}
