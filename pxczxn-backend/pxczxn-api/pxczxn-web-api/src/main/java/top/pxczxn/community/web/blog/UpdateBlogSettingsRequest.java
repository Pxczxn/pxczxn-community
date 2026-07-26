package top.pxczxn.community.web.blog;

public record UpdateBlogSettingsRequest(
        String commentScope,
        String defaultVisibility,
        String allowRepost,
        String themeKey,
        String seoTitle,
        String seoDescription
) {
}
