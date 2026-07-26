package top.pxczxn.community.article.application;

import java.util.List;

public record SaveArticleCommand(
        String title,
        String slug,
        String summary,
        Long categoryId,
        Long coverFileId,
        Boolean clearCoverFile,
        String contentMode,
        String richTextJson,
        String markdownContent,
        String visibility,
        String publishMethod,
        List<Long> tagIds,
        List<Long> contentFileIds,
        Integer expectedLockVersion
) {
}
