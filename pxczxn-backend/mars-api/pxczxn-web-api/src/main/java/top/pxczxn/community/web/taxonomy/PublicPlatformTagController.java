package top.pxczxn.community.web.taxonomy;

import com.mars.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.taxonomy.application.PlatformTagService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class PublicPlatformTagController {

    private final PlatformTagService tagService;

    @GetMapping
    public Result<List<PlatformTagResponse>> list(
            @RequestParam(required = false) String keyword
    ) {
        return Result.ok(tagService.listPublic(keyword)
                .stream()
                .map(PlatformTagResponse::from)
                .toList());
    }
}
