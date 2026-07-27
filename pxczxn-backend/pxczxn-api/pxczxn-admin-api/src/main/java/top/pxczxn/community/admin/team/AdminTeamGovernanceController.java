package top.pxczxn.community.admin.team;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.admin.application.AdminArticleCollaborationView;
import top.pxczxn.community.admin.application.AdminCommunityPageView;
import top.pxczxn.community.admin.application.AdminTeamGovernanceService;
import top.pxczxn.community.admin.application.AdminTeamView;
import top.pxczxn.platform.common.result.PageResult;
import top.pxczxn.platform.common.result.Result;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community")
public class AdminTeamGovernanceController {
    private final AdminTeamGovernanceService service;

    @GetMapping("/teams") @SaCheckPermission("community:team:list")
    public Result<PageResult<AdminTeamView>> teams(@RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "20") Integer pageSize) { return Result.ok(page(service.teams(status, pageNum, pageSize))); }
    @GetMapping("/teams/{teamId}") @SaCheckPermission("community:team:list")
    public Result<AdminTeamView> team(@PathVariable Long teamId) { return Result.ok(service.team(teamId)); }
    @GetMapping("/collaborations") @SaCheckPermission("community:article:list")
    public Result<PageResult<AdminArticleCollaborationView>> collaborations(@RequestParam(required = false) Long articleId,
                                                                             @RequestParam(required = false) Long userId,
                                                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                                                             @RequestParam(defaultValue = "20") Integer pageSize) { return Result.ok(page(service.collaborations(articleId, userId, pageNum, pageSize))); }
    private static <T> PageResult<T> page(AdminCommunityPageView<T> value) { return PageResult.of(value.list(), value.total(), value.pageNum(), value.pageSize()); }
}
