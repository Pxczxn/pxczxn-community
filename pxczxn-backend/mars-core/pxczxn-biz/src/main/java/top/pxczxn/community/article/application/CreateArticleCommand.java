package top.pxczxn.community.article.application;

import java.util.List;

public record CreateArticleCommand(
        String title,
        String slug,
        String summary,
        Long categoryId,
        Long coverFileId,
        String contentMode,
        String richTextJson,
        String markdownContent,
        String visibility,
        String publishMethod,
        List<Long> tagIds,
        List<Long> contentFileIds
) {
}
