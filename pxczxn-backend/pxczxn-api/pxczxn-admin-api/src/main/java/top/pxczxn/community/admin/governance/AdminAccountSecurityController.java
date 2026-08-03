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
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.Result;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/accounts")
public class AdminAccountSecurityController {
    private final AccountEnforcementService service;

    @PostMapping("/{userId}/force-logout") @SaCheckPermission("community:account:freeze")
    public Result<Void> forceLogout(@PathVariable Long userId, @RequestBody Operation request) {
        service.forceLogout(StpUtil.getLoginIdAsLong(), userId, request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot());
        return Result.ok();
    }

    @PostMapping("/{userId}/force-password-reset") @SaCheckPermission("community:account:execute")
    public Result<Password> reset(@PathVariable Long userId, @RequestBody Operation request) {
        requireSuperAdmin();
        return Result.ok(new Password(service.forcePasswordReset(StpUtil.getLoginIdAsLong(), userId, request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot())));
    }

    @PostMapping("/{userId}/unlock") @SaCheckPermission("community:account:freeze")
    public Result<Void> unlock(@PathVariable Long userId, @RequestBody Operation request) {
        service.unlockAccount(StpUtil.getLoginIdAsLong(), userId, request.reasonCode(), request.userVisibleReason(), request.internalReason(), request.evidenceSnapshot());
        return Result.ok();
    }

    private static void requireSuperAdmin() { if (!StpUtil.hasRole("admin")) throw new BusinessException(403, "Only super administrators can reset credentials"); }
    public record Operation(String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot) { }
    public record Password(String temporaryPassword) { }
}
