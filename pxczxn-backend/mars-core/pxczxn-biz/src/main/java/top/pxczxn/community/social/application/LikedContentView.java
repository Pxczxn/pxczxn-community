package top.pxczxn.community.social.application;

import java.time.LocalDateTime;

public record LikedContentView(
        Long likeId,
        String targetType,
        Long targetId,
        Long authorUserId,
        Long blogId,
        String title,
        String excerpt,
        Long coverFileId,
        String canonicalPath,
        long likeCount,
        LocalDateTime likedAt
) {
}
