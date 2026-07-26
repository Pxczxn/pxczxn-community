package top.pxczxn.community.social.application;

public record PublishMomentCommand(
        Long blogId,
        String momentType,
        String textContent,
        String linkUrl,
        Long articleId,
        Long repostMomentId,
        String visibility
) {
}
