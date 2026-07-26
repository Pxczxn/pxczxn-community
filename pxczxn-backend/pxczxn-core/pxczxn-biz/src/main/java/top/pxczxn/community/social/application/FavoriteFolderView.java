package top.pxczxn.community.social.application;

import java.time.LocalDateTime;

public record FavoriteFolderView(
        Long folderId,
        Long ownerUserId,
        String name,
        String description,
        String visibility,
        boolean defaultFolder,
        long itemCount,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
