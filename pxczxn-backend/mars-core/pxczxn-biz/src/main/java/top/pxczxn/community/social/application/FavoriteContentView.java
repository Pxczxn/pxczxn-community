package top.pxczxn.community.social.application;

import java.time.LocalDateTime;

public record FavoriteContentView(
        Long favoriteItemId,
        String targetType,
        Long targetId,
        Long authorUserId,
        Long blogId,
        String title,
        String excerpt,
        Long coverFileId,
        String canonicalPath,
        long favoriteCount,
        LocalDateTime favoritedAt
) {
}
