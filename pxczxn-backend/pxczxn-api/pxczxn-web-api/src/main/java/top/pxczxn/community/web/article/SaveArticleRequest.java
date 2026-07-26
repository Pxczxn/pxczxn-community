package top.pxczxn.community.web.article;

import java.util.List;

public record SaveArticleRequest(
        String title,
        String slug,
        String summary,
        String categoryId,
        String coverFileId,
        Boolean clearCoverFile,
        String contentMode,
        String richTextJson,
        String markdownContent,
        String visibility,
        String publishMethod,
        List<String> tagIds,
        List<String> contentFileIds,
        Integer expectedLockVersion
) {
}
