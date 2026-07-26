package top.pxczxn.community.web.blog;

import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.blog.application.PersonalBlogService;
import top.pxczxn.community.blog.application.UpdateBlogSettingsCommand;
import top.pxczxn.community.blog.application.UpdatePersonalBlogCommand;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/blogs/me")
public class PersonalBlogController {

    private final PersonalBlogService personalBlogService;

    @GetMapping
    public Result<PersonalBlogResponse> getMine() {
        return Result.ok(PersonalBlogResponse.from(personalBlogService.getMine()));
    }

    @PatchMapping
    public Result<PersonalBlogResponse> updateMine(
            @RequestBody UpdatePersonalBlogRequest request
    ) {
        return Result.ok(PersonalBlogResponse.from(personalBlogService.updateMine(
                new UpdatePersonalBlogCommand(
                        request.name(),
                        request.slug(),
                        request.summary(),
                        request.avatarFileId(),
                        Boolean.TRUE.equals(request.clearAvatar()),
                        request.backgroundFileId(),
                        Boolean.TRUE.equals(request.clearBackground())
                )
        )));
    }

    @PatchMapping("/settings")
    public Result<BlogSettingsResponse> updateSettings(
            @RequestBody UpdateBlogSettingsRequest request
    ) {
        return Result.ok(BlogSettingsResponse.from(
                personalBlogService.updateMySettings(new UpdateBlogSettingsCommand(
                        request.commentScope(),
                        request.defaultVisibility(),
                        request.allowRepost(),
                        request.themeKey(),
                        request.seoTitle(),
                        request.seoDescription()
                ))
        ));
    }
}
