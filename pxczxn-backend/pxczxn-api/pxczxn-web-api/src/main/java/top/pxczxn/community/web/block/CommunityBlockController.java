package top.pxczxn.community.web.block;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.block.application.CommunityBlockService;
import top.pxczxn.community.block.application.CommunityBlockView;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.platform.common.result.Result;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/blocks")
public class CommunityBlockController {

    private final CommunityBlockService service;
    private final CommunityAuth auth;

    @PostMapping
    public Result<BlockResponse> block(@RequestBody BlockRequest request) {
        return Result.ok(BlockResponse.from(service.block(
                auth.getLoginUserId(),
                request == null ? null : request.targetType(),
                request == null ? null : request.targetId()
        )));
    }

    @DeleteMapping("/{targetType}/{targetId}")
    public Result<Void> unblock(@PathVariable String targetType, @PathVariable Long targetId) {
        service.unblock(auth.getLoginUserId(), targetType, targetId);
        return Result.ok();
    }

    @GetMapping
    public Result<List<BlockResponse>> mine() {
        return Result.ok(service.mine(auth.getLoginUserId()).stream().map(BlockResponse::from).toList());
    }

    public record BlockRequest(String targetType, Long targetId) { }

    public record BlockResponse(String id, String targetType, String targetId, LocalDateTime createdAt) {
        private static BlockResponse from(CommunityBlockView value) {
            return new BlockResponse(value.id().toString(), value.targetType(), value.targetId().toString(), value.createdAt());
        }
    }
}
