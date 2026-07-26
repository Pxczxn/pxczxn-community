package top.pxczxn.community.web.social;

public record UpdateFavoriteFolderRequest(
        String name,
        String description,
        Boolean clearDescription,
        String visibility,
        Integer sortOrder
) {
}
