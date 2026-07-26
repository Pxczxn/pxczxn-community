package top.pxczxn.community.web.social;

import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.social.application.ContentLikeService;
import top.pxczxn.community.social.application.LikeListPrivacyService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ContentLikeController {

    private final ContentLikeService service;
    private final LikeListPrivacyService privacyService;

    @PostMapping("/interactions/{targetType}/{targetId}/like")
    public Result<ContentLikeRelationshipResponse> like(
            @PathVariable String targetType,
            @PathVariable Long targetId
    ) {
        return Result.ok(ContentLikeRelationshipResponse.from(
                service.like(targetType, targetId)
        ));
    }

    @DeleteMapping("/interactions/{targetType}/{targetId}/like")
    public Result<ContentLikeRelationshipResponse> unlike(
            @PathVariable String targetType,
            @PathVariable Long targetId
    ) {
        return Result.ok(ContentLikeRelationshipResponse.from(
                service.unlike(targetType, targetId)
        ));
    }

    @GetMapping("/interactions/{targetType}/{targetId}/like")
    public Result<ContentLikeRelationshipResponse> relationship(
            @PathVariable String targetType,
            @PathVariable Long targetId
    ) {
        return Result.ok(ContentLikeRelationshipResponse.from(
                service.relationship(targetType, targetId)
        ));
    }

    @GetMapping("/social/me/likes")
    public Result<LikedContentPageResponse> myLikes(
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.ok(LikedContentPageResponse.from(
                service.myLikes(targetType, pageNum, pageSize)
        ));
    }

    @GetMapping("/users/{userId}/likes")
    public Result<LikedContentPageResponse> userLikes(
            @PathVariable Long userId,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.ok(LikedContentPageResponse.from(
                service.likesOf(userId, targetType, pageNum, pageSize)
        ));
    }

    @GetMapping("/social/me/likes/privacy")
    public Result<LikeListPrivacyResponse> myLikeListPrivacy() {
        return Result.ok(LikeListPrivacyResponse.from(
                privacyService.mine()
        ));
    }

    @PatchMapping("/social/me/likes/privacy")
    public Result<LikeListPrivacyResponse> updateLikeListPrivacy(
            @RequestBody UpdateLikeListPrivacyRequest request
    ) {
        return Result.ok(LikeListPrivacyResponse.from(
                privacyService.update(
                        request == null ? null : request.visibility()
                )
        ));
    }
}
