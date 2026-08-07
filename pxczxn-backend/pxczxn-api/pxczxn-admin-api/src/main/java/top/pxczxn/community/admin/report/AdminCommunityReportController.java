package top.pxczxn.community.admin.report;

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
import top.pxczxn.community.report.application.CommunityReportService;
import top.pxczxn.community.report.application.CommunityReportView;
import top.pxczxn.platform.common.result.Result;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/admin-api/community/reports")
public class AdminCommunityReportController {
    private final CommunityReportService service;
    @GetMapping @SaCheckPermission("community:report:list") public Result<List<ReportResponse>> queue(@RequestParam(required = false) String status, @RequestParam(required = false) String targetType, @RequestParam(required = false) Long targetId) { return Result.ok(service.queue(status, targetType, targetId).stream().map(ReportResponse::from).toList()); }
    @GetMapping("/search") @SaCheckPermission("community:report:list") public Result<List<ReportResponse>> search(@RequestParam String keyword, @RequestParam(defaultValue = "10") int limit) { return Result.ok(service.search(keyword, limit).stream().map(ReportResponse::from).toList()); }
    @PostMapping("/{reportId}/claim") @SaCheckPermission("community:report:handle") public Result<ReportResponse> claim(@PathVariable Long reportId, @RequestBody LockRequest request) { return Result.ok(ReportResponse.from(service.claim(StpUtil.getLoginIdAsLong(), reportId, request == null ? null : request.expectedLockVersion()))); }
    @PostMapping("/{reportId}/resolve") @SaCheckPermission("community:report:handle") public Result<ReportResponse> resolve(@PathVariable Long reportId, @RequestBody ResolutionRequest request) { return Result.ok(ReportResponse.from(service.resolve(StpUtil.getLoginIdAsLong(), reportId, request == null ? null : request.expectedLockVersion(), request == null ? null : request.resolutionCode(), request == null ? null : request.resolutionNote(), false))); }
    @PostMapping("/{reportId}/dismiss") @SaCheckPermission("community:report:handle") public Result<ReportResponse> dismiss(@PathVariable Long reportId, @RequestBody ResolutionRequest request) { return Result.ok(ReportResponse.from(service.resolve(StpUtil.getLoginIdAsLong(), reportId, request == null ? null : request.expectedLockVersion(), request == null ? null : request.resolutionCode(), request == null ? null : request.resolutionNote(), true))); }
    public record LockRequest(Integer expectedLockVersion) { }
    public record ResolutionRequest(Integer expectedLockVersion, String resolutionCode, String resolutionNote) { }
    public record ReportResponse(String id, String targetType, String targetId, String reasonCode, String description, String status, String assigneeAdminId, String resolutionCode, String resolutionNote, Integer lockVersion) {
        static ReportResponse from(CommunityReportView v) { return new ReportResponse(id(v.id()), v.targetType(), id(v.targetId()), v.reasonCode(), v.description(), v.status(), id(v.assigneeAdminId()), v.resolutionCode(), v.resolutionNote(), v.lockVersion()); }
        private static String id(Long value) { return value == null ? null : value.toString(); }
    }
}
