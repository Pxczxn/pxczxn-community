package top.pxczxn.community.web.taxonomy;

import top.pxczxn.community.taxonomy.application.PlatformTagView;

public record PlatformTagResponse(
        String tagId,
        String name,
        String slug,
        String description,
        long usageCount
) {
    static PlatformTagResponse from(PlatformTagView tag) {
        return new PlatformTagResponse(
                tag.tagId().toString(),
                tag.name(),
                tag.slug(),
                tag.description(),
                tag.usageCount()
        );
    }
}
