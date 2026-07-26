package top.pxczxn.community.web.social;

import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.social.application.BlogFollowService;
import top.pxczxn.community.social.application.UpdateBlogFollowCommand;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BlogFollowController {

    private final BlogFollowService service;

    @PostMapping("/blogs/{blogId}/follow")
    public Result<BlogFollowRelationshipResponse> follow(
            @PathVariable Long blogId,
            @RequestBody(required = false) UpdateBlogFollowRequest request
    ) {
        return Result.ok(BlogFollowRelationshipResponse.from(
                service.follow(blogId, command(request))
        ));
    }

    @PatchMapping("/blogs/{blogId}/follow")
    public Result<BlogFollowRelationshipResponse> update(
            @PathVariable Long blogId,
            @RequestBody UpdateBlogFollowRequest request
    ) {
        return Result.ok(BlogFollowRelationshipResponse.from(
                service.update(blogId, command(request))
        ));
    }

    @DeleteMapping("/blogs/{blogId}/follow")
    public Result<BlogFollowRelationshipResponse> unfollow(
            @PathVariable Long blogId
    ) {
        return Result.ok(BlogFollowRelationshipResponse.from(
                service.unfollow(blogId)
        ));
    }

    @GetMapping("/blogs/{blogId}/follow")
    public Result<BlogFollowRelationshipResponse> relationship(
            @PathVariable Long blogId
    ) {
        return Result.ok(BlogFollowRelationshipResponse.from(
                service.relationship(blogId)
        ));
    }

    @GetMapping("/social/me/counts")
    public Result<SocialCountsResponse> counts() {
        return Result.ok(SocialCountsResponse.from(service.myCounts()));
    }

    @GetMapping("/social/me/following")
    public Result<SocialProfilePageResponse> following(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.ok(SocialProfilePageResponse.from(
                service.myFollowing(pageNum, pageSize)
        ));
    }

    @GetMapping("/social/me/followers")
    public Result<SocialProfilePageResponse> followers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.ok(SocialProfilePageResponse.from(
                service.myFollowers(pageNum, pageSize)
        ));
    }

    private static UpdateBlogFollowCommand command(
            UpdateBlogFollowRequest request
    ) {
        return request == null
                ? null
                : new UpdateBlogFollowCommand(
                        request.notificationLevel(), request.specialFollow()
                );
    }
}
