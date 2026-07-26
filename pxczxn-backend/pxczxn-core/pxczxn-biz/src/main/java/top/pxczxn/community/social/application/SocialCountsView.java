package top.pxczxn.community.social.application;

public record SocialCountsView(
        long following,
        long followers,
        long mutual
) {
}
