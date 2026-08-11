package top.pxczxn.community.web.account;

import jakarta.validation.Valid;
import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import top.pxczxn.community.user.application.CommunitySessionService;
import top.pxczxn.community.user.application.CommunityPreferenceService;
import top.pxczxn.community.user.application.CommunityPreferenceSettings;
import top.pxczxn.community.user.application.CurrentCommunityUser;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
public class CommunityAccountController {

    private final CommunitySessionService sessionService;
    private final CommunityPreferenceService preferenceService;

    @GetMapping("/me")
    public Result<CurrentCommunityUserView> me() {
        CurrentCommunityUser current = sessionService.getCurrentUser();
        return Result.ok(toView(current));
    }

    @PatchMapping("/me")
    public Result<CurrentCommunityUserView> updateProfile(@Valid @RequestBody CommunityProfileUpdateRequest request) {
        return Result.ok(toView(sessionService.updateProfile(request.displayName(), request.bio())));
    }

    @GetMapping("/preferences")
    public Result<CommunityPreferenceSettings> preferences() {
        return Result.ok(preferenceService.mine());
    }

    @PatchMapping("/preferences")
    public Result<CommunityPreferenceSettings> updatePreferences(
            @RequestBody CommunityPreferenceUpdateRequest request
    ) {
        return Result.ok(preferenceService.update(request.settings()));
    }

    private static CurrentCommunityUserView toView(CurrentCommunityUser current) {
        return new CurrentCommunityUserView(
                current.userId().toString(),
                current.username(),
                current.displayName(),
                current.bio(),
                stringId(current.avatarFileId()),
                current.email(),
                current.status(),
                current.verificationStatus(),
                current.forcePasswordChange(),
                stringId(current.personalBlogId()),
                current.blogName(),
                current.blogSlug()
        );
    }

    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody PasswordChangeRequest request) { sessionService.changePassword(request.currentPassword(), request.newPassword()); return Result.ok(); }

    public record PasswordChangeRequest(String currentPassword, String newPassword) { }

    public record CommunityPreferenceUpdateRequest(Map<String, Object> settings) { }

    private static String stringId(Long id) {
        return id == null ? null : id.toString();
    }
}
