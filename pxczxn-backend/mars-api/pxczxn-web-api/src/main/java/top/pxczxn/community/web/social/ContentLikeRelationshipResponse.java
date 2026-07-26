package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.ContentLikeRelationshipView;

public record ContentLikeRelationshipResponse(
        String targetType,
        String targetId,
        boolean liked,
        long likeCount
) {

    static ContentLikeRelationshipResponse from(
            ContentLikeRelationshipView view
    ) {
        return new ContentLikeRelationshipResponse(
                view.targetType(),
                id(view.targetId()),
                view.liked(),
                view.likeCount()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}
