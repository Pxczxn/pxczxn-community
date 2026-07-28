package top.pxczxn.community.web.appeal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.appeal.application.CommunityAppealService;
import top.pxczxn.community.appeal.application.CommunityAppealView;
import top.pxczxn.community.appeal.application.AppealContextView;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.platform.common.result.Result;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1")
public class CommunityAppealController {
    private final CommunityAppealService service; private final CommunityAuth auth;
    @PostMapping("/reports/{reportId}/appeals") public Result<Response> submit(@PathVariable Long reportId, @RequestBody Request request) { return Result.ok(Response.from(service.submit(auth.getLoginUserId(), reportId, request == null ? null : request.appealReason(), request == null ? null : request.evidenceJson()))); }
    @GetMapping("/reports/{reportId}/appeal-context") public Result<ContextResponse> context(@PathVariable Long reportId) { AppealContextView value = service.context(auth.getLoginUserId(), reportId); return Result.ok(new ContextResponse(value.reportId().toString(), value.targetType(), value.targetId().toString(), value.resolutionCode(), value.resolutionNote())); }
    @GetMapping("/appeals/me") public Result<List<Response>> mine() { return Result.ok(service.mine(auth.getLoginUserId()).stream().map(Response::from).toList()); }
    public record Request(String appealReason, String evidenceJson) { }
    public record ContextResponse(String reportId, String targetType, String targetId, String resolutionCode, String resolutionNote) { }
    public record Response(String id, String reportId, String status, String appealReason, String reviewNote, Integer lockVersion) { static Response from(CommunityAppealView value) { return new Response(value.id().toString(), value.reportId().toString(), value.status(), value.appealReason(), value.reviewNote(), value.lockVersion()); } }
}
