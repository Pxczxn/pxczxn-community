package top.pxczxn.community.admin.governance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.governance.application.AccountEnforcementService;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementCase;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.Result;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/account-deletions")
public class AdminAccountDeletionController {
    private final AccountEnforcementService service;
    @PostMapping @SaCheckPermission("community:account:apply")
    public Result<CommunityAccountEnforcementCase> submit(@RequestBody Request request) { return Result.ok(service.submit(StpUtil.getLoginIdAsLong(), new AccountEnforcementService.SubmitCommand(request.targetUserId(), "ACCOUNT_DELETE", request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot(), null, request.sourceReportId(), null))); }
    @PostMapping("/{id}/review") @SaCheckPermission("community:account:approve")
    public Result<CommunityAccountEnforcementCase> review(@PathVariable Long id, @RequestBody Review request) { requireSuperAdmin(); return Result.ok(service.review(StpUtil.getLoginIdAsLong(), id, new AccountEnforcementService.ReviewCommand(request.decision(), request.reviewNote()), true)); }
    @GetMapping("/{id}/confirmation-text") @SaCheckPermission("community:account:execute")
    public Result<Confirmation> confirmation(@PathVariable Long id) { requireSuperAdmin(); return Result.ok(new Confirmation(service.confirmationText(id))); }
    @PostMapping("/{id}/execute") @SaCheckPermission("community:account:execute")
    public Result<CommunityAccountEnforcementCase> execute(@PathVariable Long id, @RequestBody Execute request) { requireSuperAdmin(); return Result.ok(service.execute(StpUtil.getLoginIdAsLong(), id, request.confirmation())); }
    private static void requireSuperAdmin() { if (!StpUtil.hasRole("admin")) throw new BusinessException(403, "Only super administrators can review or execute deletion"); }
    public record Request(Long targetUserId, String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot, Long sourceReportId) { }
    public record Review(String decision, String reviewNote) { }
    public record Execute(String confirmation) { }
    public record Confirmation(String confirmationText) { }
}
