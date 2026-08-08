package top.pxczxn.community.web.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateArticleRequest(
        String blogId,
        @NotBlank(message = "文章标题不能为空")
        @Size(max = 200, message = "文章标题不能超过 200 个字符")
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
