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
import top.pxczxn.community.governance.model.CommunityAccountEnforcementAppeal;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.Result;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/account-appeals")
public class AdminAccountAppealController {
    private final AccountEnforcementService service;
    @PostMapping("/{id}/primary-review") @SaCheckPermission("community:account:approve")
    public Result<CommunityAccountEnforcementAppeal> primary(@PathVariable Long id, @RequestBody Review request) { return Result.ok(service.primaryReviewAppeal(StpUtil.getLoginIdAsLong(), id, request.decision(), request.reviewNote())); }
    @PostMapping("/{id}/final-review") @SaCheckPermission("community:account:approve")
    public Result<CommunityAccountEnforcementAppeal> finalReview(@PathVariable Long id, @RequestBody Review request) { if (!StpUtil.hasRole("admin")) throw new BusinessException(403, "Only super administrators can finalize appeals"); return Result.ok(service.finalReviewAppeal(StpUtil.getLoginIdAsLong(), id, request.decision(), request.reviewNote(), request.modifiedExpiresAt())); }
    public record Review(String decision, String reviewNote, LocalDateTime modifiedExpiresAt) { }
}
