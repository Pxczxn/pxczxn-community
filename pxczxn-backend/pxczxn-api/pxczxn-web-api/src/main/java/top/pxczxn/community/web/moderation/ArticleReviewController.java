package top.pxczxn.community.web.moderation;

import top.pxczxn.platform.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.moderation.application.ArticleReviewSubmissionService;
import top.pxczxn.community.moderation.application.SubmitArticleReviewCommand;
import top.pxczxn.community.moderation.application.WithdrawArticleReviewCommand;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/articles")
public class ArticleReviewController {

    private final ArticleReviewSubmissionService reviewService;

    @PostMapping("/{articleId}/submit-review")
    public Result<ArticleReviewStatusResponse> submit(
            @PathVariable Long articleId,
            @Valid @RequestBody SubmitArticleReviewRequest request
    ) {
        return Result.ok(ArticleReviewStatusResponse.from(
                reviewService.submit(
                        articleId,
                        new SubmitArticleReviewCommand(
                                request.idempotencyKey(),
                                request.expectedLockVersion()
                        )
                )
        ));
    }

    @PostMapping("/{articleId}/withdraw-review")
    public Result<ArticleReviewStatusResponse> withdraw(
            @PathVariable Long articleId,
            @Valid @RequestBody WithdrawArticleReviewRequest request
    ) {
        return Result.ok(ArticleReviewStatusResponse.from(
                reviewService.withdraw(
                        articleId,
                        new WithdrawArticleReviewCommand(
                                request.expectedLockVersion(),
                                request.reason()
                        )
                )
        ));
    }

    @GetMapping("/{articleId}/review-status")
    public Result<ArticleReviewStatusResponse> status(
            @PathVariable Long articleId
    ) {
        return Result.ok(ArticleReviewStatusResponse.from(
                reviewService.getStatus(articleId)
        ));
    }
}
