package top.pxczxn.community.blog.application;

public record BlogSettingsView(
        String commentScope,
        String defaultVisibility,
        String allowRepost,
        String themeKey,
        String seoTitle,
        String seoDescription
) {
}
