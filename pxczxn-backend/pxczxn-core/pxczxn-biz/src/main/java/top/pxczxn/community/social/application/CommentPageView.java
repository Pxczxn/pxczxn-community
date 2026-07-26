package top.pxczxn.community.social.application;

import java.util.List;

public record CommentPageView(
        String targetType,
        Long targetId,
        List<CommentThreadView> records,
        long total,
        long commentCount,
        int pageNum,
        int pageSize
) {
}
