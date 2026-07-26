package top.pxczxn.community.social.application;

public record CreateFavoriteFolderCommand(
        String name,
        String description,
        String visibility,
        Integer sortOrder
) {
}
