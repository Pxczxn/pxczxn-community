package top.pxczxn.community.web.social;

public record PublishMomentRequest(
        String blogId,
        String momentType,
        String textContent,
        String linkUrl,
        String articleId,
        String repostMomentId,
        String visibility
) {
}
