package top.pxczxn.community.web.social;

import com.mars.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.social.application.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService service;

    @PostMapping("/interactions/{targetType}/{targetId}/comments")
    public Result<CommentResponse> create(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestBody CreateCommentRequest request
    ) {
        return Result.ok(CommentResponse.from(service.create(
                targetType,
                targetId,
                request == null ? null : request.content()
        )));
    }

    @GetMapping("/interactions/{targetType}/{targetId}/comments")
    public Result<CommentPageResponse> page(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.ok(CommentPageResponse.from(service.page(
                targetType, targetId, pageNum, pageSize
        )));
    }

    @PostMapping("/comments/{commentId}/replies")
    public Result<CommentResponse> reply(
            @PathVariable Long commentId,
            @RequestBody CreateCommentRequest request
    ) {
        return Result.ok(CommentResponse.from(service.reply(
                commentId,
                request == null ? null : request.content()
        )));
    }

    @GetMapping("/comments/{rootCommentId}/replies")
    public Result<CommentReplyPageResponse> replies(
            @PathVariable Long rootCommentId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.ok(CommentReplyPageResponse.from(service.replies(
                rootCommentId, pageNum, pageSize
        )));
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<CommentModerationResponse> delete(
            @PathVariable Long commentId
    ) {
        return Result.ok(CommentModerationResponse.from(
                service.delete(commentId)
        ));
    }

    @PostMapping("/comments/{commentId}/hide")
    public Result<CommentModerationResponse> hide(
            @PathVariable Long commentId,
            @RequestBody(required = false) HideCommentRequest request
    ) {
        return Result.ok(CommentModerationResponse.from(service.hide(
                commentId,
                request == null ? null : request.reason()
        )));
    }
}
