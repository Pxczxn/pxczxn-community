package top.pxczxn.community.social.application;

public record AccessibleContentTarget(
        LikeTargetType targetType,
        Long targetId,
        Long authorUserId,
        Long blogId,
        String title,
        String excerpt,
        Long coverFileId,
        String canonicalPath,
        long likeCount,
        long favoriteCount,
        long commentCount
) {
}
