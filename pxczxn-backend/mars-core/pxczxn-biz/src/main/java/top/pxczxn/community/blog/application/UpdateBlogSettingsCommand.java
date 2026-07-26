package top.pxczxn.community.blog.application;

public record UpdateBlogSettingsCommand(
        String commentScope,
        String defaultVisibility,
        String allowRepost,
        String themeKey,
        String seoTitle,
        String seoDescription
) {
}
