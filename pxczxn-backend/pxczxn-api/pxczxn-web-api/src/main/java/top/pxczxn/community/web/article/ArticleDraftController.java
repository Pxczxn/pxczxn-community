package top.pxczxn.community.web.article;

import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.article.application.ArticleDraftService;
import top.pxczxn.community.article.application.CreateArticleCommand;
import top.pxczxn.community.article.application.SaveArticleCommand;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/articles")
public class ArticleDraftController {

    private final ArticleDraftService articleService;

    @PostMapping
    public Result<ArticleEditorResponse> create(
            @RequestBody CreateArticleRequest request
    ) {
        if (request == null) {
            throw new BusinessException(400, "文章信息不能为空");
        }
        return Result.ok(ArticleEditorResponse.from(articleService.create(
                new CreateArticleCommand(
                        request.title(),
                        request.slug(),
                        request.summary(),
                        optionalId(request.categoryId(), "分类"),
                        optionalId(request.coverFileId(), "封面文件"),
                        request.contentMode(),
                        request.richTextJson(),
                        request.markdownContent(),
                        request.visibility(),
                        request.publishMethod(),
                        ids(request.tagIds(), "标签"),
                        ids(request.contentFileIds(), "正文文件")
                )
        )));
    }

    @GetMapping("/{articleId}/editor")
    public Result<ArticleEditorResponse> editor(
            @PathVariable Long articleId
    ) {
        return Result.ok(ArticleEditorResponse.from(
                articleService.getEditor(articleId)
        ));
    }

    @PutMapping("/{articleId}")
    public Result<ArticleEditorResponse> save(
            @PathVariable Long articleId,
            @RequestBody SaveArticleRequest request
    ) {
        return Result.ok(ArticleEditorResponse.from(
                articleService.save(articleId, command(request), false)
        ));
    }

    @PutMapping("/{articleId}/autosave")
    public Result<ArticleEditorResponse> autosave(
            @PathVariable Long articleId,
            @RequestBody SaveArticleRequest request
    ) {
        return Result.ok(ArticleEditorResponse.from(
                articleService.save(articleId, command(request), true)
        ));
    }

    @DeleteMapping("/{articleId}")
    public Result<Void> delete(
            @PathVariable Long articleId,
            @RequestParam Integer expectedLockVersion
    ) {
        articleService.delete(articleId, expectedLockVersion);
        return Result.ok();
    }

    @GetMapping("/{articleId}/versions")
    public Result<ArticleVersionPageResponse> versions(
            @PathVariable Long articleId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.ok(ArticleVersionPageResponse.from(
                articleService.listVersions(articleId, pageNum, pageSize)
        ));
    }

    @GetMapping("/{articleId}/versions/{versionId}")
    public Result<ArticleVersionDetailResponse> version(
            @PathVariable Long articleId,
            @PathVariable Long versionId
    ) {
        return Result.ok(ArticleVersionDetailResponse.from(
                articleService.getVersion(articleId, versionId)
        ));
    }

    @PostMapping("/{articleId}/versions/{versionId}/restore")
    public Result<ArticleEditorResponse> restore(
            @PathVariable Long articleId,
            @PathVariable Long versionId,
            @RequestBody RestoreArticleVersionRequest request
    ) {
        if (request == null) {
            throw new BusinessException(400, "恢复版本请求不能为空");
        }
        return Result.ok(ArticleEditorResponse.from(
                articleService.restoreVersion(
                        articleId,
                        versionId,
                        request.expectedLockVersion()
                )
        ));
    }

    private static SaveArticleCommand command(SaveArticleRequest request) {
        if (request == null) {
            throw new BusinessException(400, "文章信息不能为空");
        }
        return new SaveArticleCommand(
                request.title(),
                request.slug(),
                request.summary(),
                optionalId(request.categoryId(), "分类"),
                optionalId(request.coverFileId(), "封面文件"),
                request.clearCoverFile(),
                request.contentMode(),
                request.richTextJson(),
                request.markdownContent(),
                request.visibility(),
                request.publishMethod(),
                ids(request.tagIds(), "标签"),
                ids(request.contentFileIds(), "正文文件"),
                request.expectedLockVersion()
        );
    }

    private static Long optionalId(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseUnsignedLong(raw.trim());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, label + " ID 无效");
        }
    }

    private static List<Long> ids(List<String> rawIds, String label) {
        if (rawIds == null) {
            return null;
        }
        return rawIds.stream()
                .map(value -> optionalId(value, label))
                .toList();
    }
}
