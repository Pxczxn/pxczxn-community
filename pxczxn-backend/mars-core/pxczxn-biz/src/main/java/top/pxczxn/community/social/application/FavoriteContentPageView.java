package top.pxczxn.community.social.application;

import java.util.List;

public record FavoriteContentPageView(
        Long folderId,
        List<FavoriteContentView> records,
        long total,
        int pageNum,
        int pageSize
) {
}
