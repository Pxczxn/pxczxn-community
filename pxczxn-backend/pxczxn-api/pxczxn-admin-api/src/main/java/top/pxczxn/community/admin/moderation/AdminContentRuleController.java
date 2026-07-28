package top.pxczxn.community.admin.moderation;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
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
import top.pxczxn.community.moderation.application.ContentRuleService;
import top.pxczxn.community.moderation.application.ContentRuleView;
import top.pxczxn.platform.common.result.Result;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/content-rules")
public class AdminContentRuleController {
    private final ContentRuleService service;
    @GetMapping @SaCheckPermission("community:content-rule:list")
    public Result<List<ContentRuleView>> list(@RequestParam(required = false) String status) { return Result.ok(service.list(status)); }
    @PostMapping @SaCheckPermission("community:content-rule:manage")
    public Result<ContentRuleView> create(@RequestBody ContentRuleService.RuleCommand request) { return Result.ok(service.create(StpUtil.getLoginIdAsLong(), request)); }
    @PatchMapping("/{id}") @SaCheckPermission("community:content-rule:manage")
    public Result<ContentRuleView> update(@PathVariable Long id, @RequestBody ContentRuleService.RuleCommand request) { return Result.ok(service.update(id, request)); }
    @DeleteMapping("/{id}") @SaCheckPermission("community:content-rule:manage")
    public Result<Void> delete(@PathVariable Long id) { service.delete(id); return Result.ok(); }
}
