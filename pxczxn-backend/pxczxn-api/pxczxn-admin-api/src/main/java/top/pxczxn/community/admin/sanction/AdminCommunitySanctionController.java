package top.pxczxn.community.admin.sanction;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.sanction.application.CommunitySanctionService;
import top.pxczxn.community.sanction.application.SanctionView;
import top.pxczxn.platform.common.result.Result;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/sanctions")
public class AdminCommunitySanctionController {

    private final CommunitySanctionService service;

    @GetMapping("/users/{userId}")
    @SaCheckPermission("community:sanction:list")
    public Result<List<Response>> list(@PathVariable Long userId) {
        return Result.ok(service.mine(userId).stream().map(Response::from).toList());
    }

    @PostMapping
    @SaCheckPermission("community:sanction:handle")
    public Result<Response> issue(@RequestBody Request request) {
        return Result.ok(Response.from(service.issue(
                StpUtil.getLoginIdAsLong(),
                request.targetUserId(),
                request.sanctionType(),
                request.reasonCode(),
                request.reasonNote(),
                request.sourceReportId(),
                request.expiresAt()
        )));
    }

    @PostMapping("/{id}/revoke")
    @SaCheckPermission("community:sanction:handle")
    public Result<Response> revoke(@PathVariable Long id, @RequestBody Revoke request) {
        return Result.ok(Response.from(service.revoke(
                StpUtil.getLoginIdAsLong(), id, request.note()
        )));
    }

    public record Request(
            Long targetUserId,
            String sanctionType,
            String reasonCode,
            String reasonNote,
            Long sourceReportId,
            LocalDateTime expiresAt
    ) {
    }

    public record Revoke(String note) {
    }

    public record Response(
            String id,
            String type,
            String reasonCode,
            String reasonNote,
            String status,
            LocalDateTime expiresAt
    ) {
        static Response from(SanctionView value) {
            return new Response(
                    value.id().toString(),
                    value.sanctionType(),
                    value.reasonCode(),
                    value.reasonNote(),
                    value.status(),
                    value.expiresAt()
            );
        }
    }
}
