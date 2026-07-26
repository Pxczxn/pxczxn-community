package top.pxczxn.community.social.application;

import java.util.List;

public record LikedContentPageView(
        List<LikedContentView> records,
        long total,
        int pageNum,
        int pageSize
) {
}
