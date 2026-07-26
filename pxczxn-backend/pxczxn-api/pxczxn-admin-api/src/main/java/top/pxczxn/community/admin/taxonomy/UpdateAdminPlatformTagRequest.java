package top.pxczxn.community.admin.taxonomy;

public record UpdateAdminPlatformTagRequest(
        String name,
        String slug,
        String description,
        String status
) {
}
