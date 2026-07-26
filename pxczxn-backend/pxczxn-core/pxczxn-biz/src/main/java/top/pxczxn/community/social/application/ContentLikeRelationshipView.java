package top.pxczxn.community.social.application;

public record ContentLikeRelationshipView(
        String targetType,
        Long targetId,
        boolean liked,
        long likeCount
) {
}
