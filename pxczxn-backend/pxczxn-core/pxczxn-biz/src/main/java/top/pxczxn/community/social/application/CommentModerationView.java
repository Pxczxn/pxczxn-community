package top.pxczxn.community.social.application;

public record CommentModerationView(
        Long commentId,
        String status,
        int affectedComments,
        long targetCommentCount
) {
}
