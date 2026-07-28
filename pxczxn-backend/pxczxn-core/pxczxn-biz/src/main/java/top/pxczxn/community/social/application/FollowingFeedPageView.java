package top.pxczxn.community.social.application;

import java.util.List;

public record FollowingFeedPageView(List<FollowingFeedItemView> records, long total, int pageNum, int pageSize) {}
