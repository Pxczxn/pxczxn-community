package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.FollowingFeedItemView;
import top.pxczxn.community.social.application.FollowingFeedPageView;

import java.time.LocalDateTime;
import java.util.List;

public record FollowingFeedResponse(List<Item> records, long total, int pageNum, int pageSize) {
    static FollowingFeedResponse from(FollowingFeedPageView page) {
        return new FollowingFeedResponse(page.records().stream().map(Item::from).toList(), page.total(), page.pageNum(), page.pageSize());
    }
    public record Item(String itemType, String targetId, String title, String excerpt, String canonicalPath, String authorName, String blogName, String tagName, LocalDateTime occurredAt, Boolean specialFollow) {
        static Item from(FollowingFeedItemView view) { return new Item(view.itemType(), view.targetId().toString(), view.title(), view.excerpt(), view.canonicalPath(), view.authorName(), view.blogName(), view.tagName(), view.occurredAt(), view.specialFollow() != null && view.specialFollow() == 1); }
    }
}
