package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.FavoriteRelationshipView;

import java.util.List;

public record FavoriteRelationshipResponse(
        String targetType,
        String targetId,
        boolean favorited,
        long favoriteCount,
        List<String> folderIds
) {

    static FavoriteRelationshipResponse from(
            FavoriteRelationshipView view
    ) {
        return new FavoriteRelationshipResponse(
                view.targetType(),
                id(view.targetId()),
                view.favorited(),
                view.favoriteCount(),
                view.folderIds().stream().map(String::valueOf).toList()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}
