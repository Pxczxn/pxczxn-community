package top.pxczxn.community.admin.taxonomy;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
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
import top.pxczxn.community.taxonomy.application.CreatePlatformTagCommand;
import top.pxczxn.community.taxonomy.application.PlatformTagService;
import top.pxczxn.community.taxonomy.application.UpdatePlatformTagCommand;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/tags")
public class AdminPlatformTagController {

    private final PlatformTagService tagService;

    @GetMapping
    @SaCheckPermission("community:tag:list")
    public Result<List<AdminPlatformTagResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return Result.ok(tagService.listAdmin(status, keyword)
                .stream()
                .map(AdminPlatformTagResponse::from)
                .toList());
    }

    @PostMapping
    @SaCheckPermission("community:tag:add")
    public Result<AdminPlatformTagResponse> create(
            @RequestBody CreateAdminPlatformTagRequest request
    ) {
        return Result.ok(AdminPlatformTagResponse.from(tagService.create(
                StpUtil.getLoginIdAsLong(),
                new CreatePlatformTagCommand(
                        request.name(),
                        request.slug(),
                        request.description()
                )
        )));
    }

    @PatchMapping("/{tagId}")
    @SaCheckPermission("community:tag:edit")
    public Result<AdminPlatformTagResponse> update(
            @PathVariable Long tagId,
            @RequestBody UpdateAdminPlatformTagRequest request
    ) {
        return Result.ok(AdminPlatformTagResponse.from(tagService.update(
                tagId,
                new UpdatePlatformTagCommand(
                        request.name(),
                        request.slug(),
                        request.description(),
                        request.status()
                )
        )));
    }

    @DeleteMapping("/{tagId}")
    @SaCheckPermission("community:tag:delete")
    public Result<Void> delete(@PathVariable Long tagId) {
        tagService.delete(tagId);
        return Result.ok();
    }
}
