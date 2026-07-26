package top.pxczxn.community.social.application;

public record MomentArticleView(
        Long articleId,
        boolean available,
        String title,
        String summary,
        Long coverFileId,
        String canonicalPath
) {
}
