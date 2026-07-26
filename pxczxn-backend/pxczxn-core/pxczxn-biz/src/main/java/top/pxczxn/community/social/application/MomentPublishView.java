package top.pxczxn.community.social.application;

public record MomentPublishView(
        MomentView moment,
        boolean moderationWarning,
        String moderationResult
) {
}
