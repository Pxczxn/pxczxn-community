package top.pxczxn.community.web.creator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.creator.application.CommunityCreatorIdeaService;
import top.pxczxn.community.creator.application.CommunityCreatorIdeaView;
import top.pxczxn.platform.common.result.Result;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/creator/ideas")
public class CommunityCreatorIdeaController {
    private final CommunityCreatorIdeaService service;

    @GetMapping
    public Result<List<Response>> mine() { return Result.ok(service.mine().stream().map(Response::from).toList()); }

    @PostMapping
    public Result<Response> create(@Valid @RequestBody CreateRequest request) {
        return Result.ok(Response.from(service.create(request.title(), request.content(), request.tags(), request.sourceType())));
    }

    public record CreateRequest(@NotBlank String title, @NotBlank String content, List<String> tags, String sourceType) { }
    public record Response(String id, String title, String content, List<String> tags, String sourceType, LocalDateTime createdAt) {
        static Response from(CommunityCreatorIdeaView view) { return new Response(view.id().toString(), view.title(), view.content(), view.tags(), view.sourceType(), view.createdAt()); }
    }
}
