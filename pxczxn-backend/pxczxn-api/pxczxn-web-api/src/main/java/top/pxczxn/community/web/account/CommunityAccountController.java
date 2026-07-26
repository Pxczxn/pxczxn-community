package top.pxczxn.community.web.account;

import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.user.application.CommunitySessionService;
import top.pxczxn.community.user.application.CurrentCommunityUser;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
public class CommunityAccountController {

    private final CommunitySessionService sessionService;

    @GetMapping("/me")
    public Result<CurrentCommunityUserView> me() {
        CurrentCommunityUser current = sessionService.getCurrentUser();
        return Result.ok(new CurrentCommunityUserView(
                current.userId().toString(),
                current.username(),
                current.displayName(),
                current.bio(),
                stringId(current.avatarFileId()),
                current.email(),
                current.status(),
                current.verificationStatus(),
                stringId(current.personalBlogId()),
                current.blogName(),
                current.blogSlug()
        ));
    }

    private static String stringId(Long id) {
        return id == null ? null : id.toString();
    }
}
