package top.pxczxn.community.web.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveArticleRequest(
        @NotBlank(message = "文章标题不能为空")
        @Size(max = 200, message = "文章标题不能超过 200 个字符")
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
        @NotNull(message = "乐观锁版本不能为空")
        Integer expectedLockVersion
) {
}
