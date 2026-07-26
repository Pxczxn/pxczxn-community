package top.pxczxn.community.moderation.application;

import java.util.List;

public record AdminArticleReviewPageView(
        List<AdminArticleReviewListItemView> list,
        long total,
        long pageNum,
        long pageSize
) {
}
