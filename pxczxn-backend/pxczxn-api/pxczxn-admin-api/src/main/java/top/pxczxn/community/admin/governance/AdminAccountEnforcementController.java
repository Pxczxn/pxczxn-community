package top.pxczxn.community.admin.governance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.governance.application.AccountEnforcementService;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementCase;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementAppeal;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementReview;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.Result;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/account-enforcements")
public class AdminAccountEnforcementController {
    private final AccountEnforcementService service;

    @PostMapping("/freeze")
    @SaCheckPermission("community:account:freeze")
    public Result<CommunityAccountEnforcementCase> freeze(@RequestBody FreezeRequest request) {
        return Result.ok(service.freeze(StpUtil.getLoginIdAsLong(), new AccountEnforcementService.FreezeCommand(request.targetUserId(), request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot(), request.expiresAt()), superAdmin()));
    }

    @PostMapping("/freeze/extend")
    @SaCheckPermission("community:account:freeze")
    public Result<CommunityAccountEnforcementCase> extendFreeze(@RequestBody FreezeRequest request) {
        return Result.ok(service.extendFreeze(StpUtil.getLoginIdAsLong(), new AccountEnforcementService.FreezeCommand(request.targetUserId(), request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot(), request.expiresAt()), superAdmin()));
    }

    @PostMapping("/users/{userId}/freeze/release")
    @SaCheckPermission("community:account:freeze")
    public Result<Void> releaseFreeze(@PathVariable Long userId, @RequestBody SecurityOperationRequest request) {
        service.releaseFreeze(StpUtil.getLoginIdAsLong(), userId, operation(request)); return Result.ok();
    }

    @PostMapping("/users/{userId}/login/block")
    @SaCheckPermission("community:account:freeze")
    public Result<Void> blockLogin(@PathVariable Long userId, @RequestBody FreezeRequest request) {
        throw new BusinessException(410, "Deprecated: use account freezing instead");
    }

    @PostMapping("/users/{userId}/login/restore")
    @SaCheckPermission("community:account:freeze")
    public Result<Void> restoreLogin(@PathVariable Long userId, @RequestBody SecurityOperationRequest request) {
        throw new BusinessException(410, "Deprecated: release the applicable account freeze instead");
    }

    @PostMapping("/users/{userId}/deactivate")
    @SaCheckPermission("community:account:apply")
    public Result<Void> deactivate(@PathVariable Long userId, @RequestBody SecurityOperationRequest request) {
        throw new BusinessException(410, "Account deactivation is no longer available");
    }

    @PostMapping("/users/{userId}/restore")
    @SaCheckPermission("community:account:apply")
    public Result<Void> restore(@PathVariable Long userId, @RequestBody SecurityOperationRequest request) {
        throw new BusinessException(410, "Account restoration is no longer available");
    }

    @PostMapping
    @SaCheckPermission("community:account:apply")
    public Result<CommunityAccountEnforcementCase> submit(@RequestBody SubmitRequest request) {
        return Result.ok(service.submit(StpUtil.getLoginIdAsLong(), new AccountEnforcementService.SubmitCommand(request.targetUserId(), request.measureType(), request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot(), request.cleanupScope(), request.sourceReportId(), request.expiresAt())));
    }

    @PostMapping("/{caseId}/reviews")
    @SaCheckPermission("community:account:approve")
    public Result<CommunityAccountEnforcementCase> review(@PathVariable Long caseId, @RequestBody ReviewRequest request) {
        return Result.ok(service.review(StpUtil.getLoginIdAsLong(), caseId, new AccountEnforcementService.ReviewCommand(request.decision(), request.reviewNote()), superAdmin()));
    }

    @GetMapping("/{caseId}/reviews")
    @SaCheckPermission("community:account:approve")
    public Result<List<CommunityAccountEnforcementReview>> reviews(@PathVariable Long caseId) {
        return Result.ok(service.reviews(caseId));
    }

    @PostMapping("/{caseId}/evidence")
    @SaCheckPermission("community:account:apply")
    public Result<CommunityAccountEnforcementCase> supplementEvidence(@PathVariable Long caseId, @RequestBody EvidenceSupplementRequest request) {
        return Result.ok(service.supplementEvidence(StpUtil.getLoginIdAsLong(), caseId, request.evidenceSnapshot()));
    }

    @PostMapping("/{caseId}/execute")
    @SaCheckPermission("community:account:execute")
    public Result<CommunityAccountEnforcementCase> execute(@PathVariable Long caseId, @RequestBody ExecuteRequest request) {
        requireSuperAdmin();
        return Result.ok(service.execute(StpUtil.getLoginIdAsLong(), caseId, request.confirmation()));
    }

    @GetMapping("/{caseId}/confirmation-text")
    @SaCheckPermission("community:account:execute")
    public Result<ConfirmationTextResponse> confirmationText(@PathVariable Long caseId) {
        requireSuperAdmin();
        return Result.ok(new ConfirmationTextResponse(service.confirmationText(caseId)));
    }

    @PostMapping("/users/{userId}/force-password-reset")
    @SaCheckPermission("community:account:execute")
    public Result<TemporaryPasswordResponse> forcePasswordReset(@PathVariable Long userId, @RequestBody PasswordResetRequest request) {
        requireSuperAdmin();
        return Result.ok(new TemporaryPasswordResponse(service.forcePasswordReset(
                StpUtil.getLoginIdAsLong(), userId,
                request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot()
        )));
    }

    @PostMapping("/users/{userId}/force-logout")
    @SaCheckPermission("community:account:freeze")
    public Result<Void> forceLogout(@PathVariable Long userId, @RequestBody SecurityOperationRequest request) {
        service.forceLogout(StpUtil.getLoginIdAsLong(), userId, request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot());
        return Result.ok();
    }

    @PostMapping("/users/{userId}/unlock")
    @SaCheckPermission("community:account:freeze")
    public Result<Void> unlock(@PathVariable Long userId, @RequestBody SecurityOperationRequest request) {
        service.unlockAccount(StpUtil.getLoginIdAsLong(), userId, request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot());
        return Result.ok();
    }

    @PostMapping("/users/{userId}/lock")
    @SaCheckPermission("community:account:freeze")
    public Result<Void> lock(@PathVariable Long userId, @RequestBody FreezeRequest request) {
        throw new BusinessException(410, "Manual account locking is no longer available");
    }

    @GetMapping
    @SaCheckPermission("community:account:list")
    public Result<List<CommunityAccountEnforcementCase>> list(@RequestParam(required = false) Long userId) {
        return Result.ok(service.list(userId));
    }

    @GetMapping("/appeals")
    @SaCheckPermission("community:account:approve")
    public Result<List<CommunityAccountEnforcementAppeal>> appeals() { requireSuperAdmin(); return Result.ok(service.appeals()); }

    @PostMapping("/appeals/{appealId}/review")
    @SaCheckPermission("community:account:approve")
    public Result<CommunityAccountEnforcementAppeal> reviewAppeal(@PathVariable Long appealId, @RequestBody AppealReviewRequest request) { requireSuperAdmin(); return Result.ok(service.reviewAppeal(StpUtil.getLoginIdAsLong(), appealId, request.decision(), request.reviewNote(), request.modifiedExpiresAt())); }

    private static boolean superAdmin() { return StpUtil.hasRole("admin"); }
    private static AccountEnforcementService.OperationCommand operation(SecurityOperationRequest request) { return new AccountEnforcementService.OperationCommand(request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot()); }
    private static void requireSuperAdmin() { if (!superAdmin()) throw new BusinessException(403, "仅超级管理员可以审核或执行重大账号措施"); }

    public record FreezeRequest(Long targetUserId, String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot, LocalDateTime expiresAt) { }
    public record SubmitRequest(Long targetUserId, String measureType, String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot, String cleanupScope, Long sourceReportId, LocalDateTime expiresAt) { }
    public record ReviewRequest(String decision, String reviewNote) { }
    public record EvidenceSupplementRequest(String evidenceSnapshot) { }
    public record ExecuteRequest(String confirmation) { }
    public record ConfirmationTextResponse(String confirmationText) { }
    public record AppealReviewRequest(String decision, String reviewNote, LocalDateTime modifiedExpiresAt) { }
    public record PasswordResetRequest(String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot) { }
    public record SecurityOperationRequest(String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot) { }
    public record TemporaryPasswordResponse(String temporaryPassword) { }
}
