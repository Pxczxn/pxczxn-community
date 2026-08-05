package top.pxczxn.community.web.governance;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.governance.application.AccountEnforcementService;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementAppeal;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementCase;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.platform.common.result.Result;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/account-enforcements")
public class CommunityAccountEnforcementAppealController {
    private final AccountEnforcementService service; private final CommunityAuth auth;
    @PostMapping("/{caseId}/appeals") public Result<CommunityAccountEnforcementAppeal> appeal(@PathVariable Long caseId, @RequestBody AppealRequest request) { return Result.ok(service.appeal(auth.getLoginUserId(), caseId, request.statement(), request.evidenceFileIds())); }
    @GetMapping("/appeals/me") public Result<List<CommunityAccountEnforcementAppeal>> mine() { return Result.ok(service.myAppeals(auth.getLoginUserId())); }
    @GetMapping("/me") public Result<List<CommunityAccountEnforcementCase>> cases() { return Result.ok(service.mine(auth.getLoginUserId())); }
    public record AppealRequest(String statement, List<Long> evidenceFileIds) { }
}
