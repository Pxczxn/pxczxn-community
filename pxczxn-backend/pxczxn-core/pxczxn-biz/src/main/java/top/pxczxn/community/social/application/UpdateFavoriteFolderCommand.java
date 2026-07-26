package top.pxczxn.community.social.application;

public record UpdateFavoriteFolderCommand(
        String name,
        String description,
        Boolean clearDescription,
        String visibility,
        Integer sortOrder
) {
}
