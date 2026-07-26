package top.pxczxn.community.web.social;

public record CreateFavoriteFolderRequest(
        String name,
        String description,
        String visibility,
        Integer sortOrder
) {
}
