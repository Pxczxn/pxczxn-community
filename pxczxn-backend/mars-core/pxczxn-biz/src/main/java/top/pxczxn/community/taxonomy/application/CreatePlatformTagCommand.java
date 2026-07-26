package top.pxczxn.community.taxonomy.application;

public record CreatePlatformTagCommand(
        String name,
        String slug,
        String description
) {
}
