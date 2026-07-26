package top.pxczxn.community.social.application;

public record MomentDeletionView(
        Long momentId,
        String status,
        int lockVersion,
        boolean idempotentReplay,
        long sourceRepostCount
) {
}
