package top.pxczxn.community.social.application;

import java.util.List;

public record FavoriteRelationshipView(
        String targetType,
        Long targetId,
        boolean favorited,
        long favoriteCount,
        List<Long> folderIds
) {
}
