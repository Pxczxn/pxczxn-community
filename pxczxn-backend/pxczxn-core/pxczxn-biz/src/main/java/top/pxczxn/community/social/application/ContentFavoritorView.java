package top.pxczxn.community.social.application;

import java.time.LocalDateTime;

public record ContentFavoritorView(
        Long userId,
        String username,
        String displayName,
        Long avatarFileId,
        LocalDateTime favoritedAt
) {
}
