package top.pxczxn.community.admin.taxonomy;

public record CreateAdminPlatformTagRequest(
        String name,
        String slug,
        String description
) {
}
