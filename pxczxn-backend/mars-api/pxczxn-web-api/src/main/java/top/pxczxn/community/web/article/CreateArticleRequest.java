package top.pxczxn.community.web.article;

import java.util.List;

public record CreateArticleRequest(
        String title,
        String slug,
        String summary,
        String categoryId,
        String coverFileId,
        String contentMode,
        String richTextJson,
        String markdownContent,
        String visibility,
        String publishMethod,
        List<String> tagIds,
        List<String> contentFileIds
) {
}
