package top.pxczxn.community.web.blog;

import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.blog.application.PersonalBlogService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/blogs")
public class PublicBlogController {

    private final PersonalBlogService personalBlogService;

    @GetMapping("/{blogSlug}")
    public Result<PublicBlogResponse> getBySlug(@PathVariable String blogSlug) {
        return Result.ok(PublicBlogResponse.from(
                personalBlogService.getPublicBySlug(blogSlug)
        ));
    }
}
