package top.pxczxn.community.taxonomy.application;

public record UpdatePlatformTagCommand(
        String name,
        String slug,
        String description,
        String status
) {
}
