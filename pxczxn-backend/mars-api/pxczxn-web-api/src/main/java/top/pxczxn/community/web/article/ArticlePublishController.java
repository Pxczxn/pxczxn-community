package top.pxczxn.community.web.article;

import com.mars.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.article.application.ArticlePublishService;
import top.pxczxn.community.article.application.PublishArticleCommand;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/articles")
public class ArticlePublishController {

    private final ArticlePublishService publishService;

    @PostMapping("/{articleId}/publish")
    public Result<ArticlePublishResponse> publish(
            @PathVariable Long articleId,
            @RequestBody PublishArticleRequest request
    ) {
        return Result.ok(ArticlePublishResponse.from(
                publishService.publish(
                        articleId,
                        request == null
                                ? null
                                : new PublishArticleCommand(
                                        request.expectedLockVersion(),
                                        request.scheduledPublishAt(),
                                        request.cancelScheduled()
                                )
                )
        ));
    }
}
