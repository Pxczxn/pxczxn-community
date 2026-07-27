package top.pxczxn.community.web.report;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.report.application.CommunityReportService;
import top.pxczxn.community.report.application.CommunityReportView;
import top.pxczxn.community.report.application.CreateCommunityReportCommand;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.platform.common.result.Result;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/reports")
public class CommunityReportController {
    private final CommunityReportService service; private final CommunityAuth auth;
    @PostMapping public Result<ReportResponse> create(@RequestBody ReportRequest request) { return Result.ok(ReportResponse.from(service.create(auth.getLoginUserId(), new CreateCommunityReportCommand(request == null ? null : request.targetType(), request == null ? null : request.targetId(), request == null ? null : request.reasonCode(), request == null ? null : request.description(), request == null ? null : request.evidenceJson())))); }
    @GetMapping("/me") public Result<List<ReportResponse>> mine() { return Result.ok(service.mine(auth.getLoginUserId()).stream().map(ReportResponse::from).toList()); }
    public record ReportRequest(String targetType, Long targetId, String reasonCode, String description, String evidenceJson) { }
    public record ReportResponse(String id, String reporterUserId, String targetType, String targetId, String reasonCode, String description, String evidenceJson, String status, String assigneeAdminId, String resolutionCode, String resolutionNote, Integer lockVersion) {
        static ReportResponse from(CommunityReportView v) { return new ReportResponse(id(v.id()), id(v.reporterUserId()), v.targetType(), id(v.targetId()), v.reasonCode(), v.description(), v.evidenceJson(), v.status(), id(v.assigneeAdminId()), v.resolutionCode(), v.resolutionNote(), v.lockVersion()); }
        private static String id(Long value) { return value == null ? null : value.toString(); }
    }
}
