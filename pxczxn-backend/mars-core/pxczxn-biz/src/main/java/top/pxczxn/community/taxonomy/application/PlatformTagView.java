package top.pxczxn.community.taxonomy.application;

public record PlatformTagView(
        Long tagId,
        String name,
        String slug,
        String description,
        String status,
        long usageCount
) {
}
