package top.pxczxn.community.web.blog;

import top.pxczxn.community.blog.application.BlogSettingsView;

public record BlogSettingsResponse(
        String commentScope,
        String defaultVisibility,
        String allowRepost,
        String themeKey,
        String seoTitle,
        String seoDescription
) {
    static BlogSettingsResponse from(BlogSettingsView settings) {
        return new BlogSettingsResponse(
                settings.commentScope(),
                settings.defaultVisibility(),
                settings.allowRepost(),
                settings.themeKey(),
                settings.seoTitle(),
                settings.seoDescription()
        );
    }
}
