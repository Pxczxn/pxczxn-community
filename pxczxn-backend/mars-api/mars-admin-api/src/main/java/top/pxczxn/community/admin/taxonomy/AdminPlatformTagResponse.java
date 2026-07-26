package top.pxczxn.community.admin.taxonomy;

import top.pxczxn.community.taxonomy.application.PlatformTagView;

public record AdminPlatformTagResponse(
        String tagId,
        String name,
        String slug,
        String description,
        String status,
        long usageCount
) {
    static AdminPlatformTagResponse from(PlatformTagView tag) {
        return new AdminPlatformTagResponse(
                tag.tagId().toString(),
                tag.name(),
                tag.slug(),
                tag.description(),
                tag.status(),
                tag.usageCount()
        );
    }
}
