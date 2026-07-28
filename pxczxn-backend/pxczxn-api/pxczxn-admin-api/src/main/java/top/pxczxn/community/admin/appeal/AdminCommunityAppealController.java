package top.pxczxn.community.admin.appeal;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.appeal.application.CommunityAppealService;
import top.pxczxn.community.appeal.application.CommunityAppealView;
import top.pxczxn.platform.common.result.Result;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/admin-api/community/appeals")
public class AdminCommunityAppealController {
    private final CommunityAppealService service;
    @GetMapping @SaCheckPermission("community:appeal:list") public Result<List<Response>> queue() { return Result.ok(service.queue().stream().map(Response::from).toList()); }
    @PostMapping("/{appealId}/uphold") @SaCheckPermission("community:appeal:handle") public Result<Response> uphold(@PathVariable Long appealId, @RequestBody Review request) { return Result.ok(Response.from(service.review(StpUtil.getLoginIdAsLong(), appealId, request == null ? null : request.expectedLockVersion(), false, request == null ? null : request.reviewNote()))); }
    @PostMapping("/{appealId}/revoke") @SaCheckPermission("community:appeal:handle") public Result<Response> revoke(@PathVariable Long appealId, @RequestBody Review request) { return Result.ok(Response.from(service.review(StpUtil.getLoginIdAsLong(), appealId, request == null ? null : request.expectedLockVersion(), true, request == null ? null : request.reviewNote()))); }
    public record Review(Integer expectedLockVersion, String reviewNote) { }
    public record Response(String id, String reportId, String status, String reviewNote, Integer lockVersion) { static Response from(CommunityAppealView value) { return new Response(value.id().toString(), value.reportId().toString(), value.status(), value.reviewNote(), value.lockVersion()); } }
}
