package top.pxczxn.community.social.application;

import java.time.LocalDateTime;

public record FollowingFeedItemView(String itemType, Long targetId, String title, String excerpt, String canonicalPath, String authorName, String blogName, String tagName, LocalDateTime occurredAt, Integer specialFollow) {}