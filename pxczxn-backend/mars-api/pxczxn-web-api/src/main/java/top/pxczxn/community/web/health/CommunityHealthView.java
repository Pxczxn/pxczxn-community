package top.pxczxn.community.web.health;

public record CommunityHealthView(
        String status,
        String module,
        String version
) {
}
