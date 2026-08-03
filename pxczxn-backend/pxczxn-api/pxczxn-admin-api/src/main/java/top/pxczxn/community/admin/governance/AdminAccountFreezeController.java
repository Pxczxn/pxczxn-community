package top.pxczxn.community.admin.governance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.governance.application.AccountEnforcementService;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementCase;
import top.pxczxn.platform.common.result.Result;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/accounts")
public class AdminAccountFreezeController {
    private final AccountEnforcementService service;
    @PostMapping("/{userId}/freeze") @SaCheckPermission("community:account:freeze")
    public Result<CommunityAccountEnforcementCase> freeze(@PathVariable Long userId, @RequestBody Freeze request) { return Result.ok(service.freeze(StpUtil.getLoginIdAsLong(), command(userId, request), StpUtil.hasRole("admin"))); }
    @PostMapping("/{userId}/freeze/extend") @SaCheckPermission("community:account:freeze")
    public Result<CommunityAccountEnforcementCase> extend(@PathVariable Long userId, @RequestBody Freeze request) { return Result.ok(service.extendFreeze(StpUtil.getLoginIdAsLong(), command(userId, request), StpUtil.hasRole("admin"))); }
    @PostMapping("/{userId}/freeze/release") @SaCheckPermission("community:account:freeze")
    public Result<Void> release(@PathVariable Long userId, @RequestBody Operation request) { service.releaseFreeze(StpUtil.getLoginIdAsLong(), userId, new AccountEnforcementService.OperationCommand(request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot())); return Result.ok(); }
    private static AccountEnforcementService.FreezeCommand command(Long userId, Freeze request) { return new AccountEnforcementService.FreezeCommand(userId, request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot(), request.expiresAt()); }
    public record Freeze(String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot, LocalDateTime expiresAt) { }
    public record Operation(String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot) { }
}
