package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.FavoriteFolderView;

import java.time.LocalDateTime;

public record FavoriteFolderResponse(
        String folderId,
        String ownerUserId,
        String name,
        String description,
        String visibility,
        boolean defaultFolder,
        long itemCount,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    static FavoriteFolderResponse from(FavoriteFolderView view) {
        return new FavoriteFolderResponse(
                id(view.folderId()),
                id(view.ownerUserId()),
                view.name(),
                view.description(),
                view.visibility(),
                view.defaultFolder(),
                view.itemCount(),
                view.sortOrder(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}
