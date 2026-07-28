package top.pxczxn.community.web.search;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.search.application.UnifiedSearchPageView;
import top.pxczxn.community.search.application.UnifiedSearchQuery;
import top.pxczxn.community.search.application.UnifiedSearchService;
import top.pxczxn.platform.common.result.Result;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/search")
public class PublicSearchController {
    private final UnifiedSearchService searchService;

    @GetMapping
    public Result<UnifiedSearchPageView> search(
            @RequestParam String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.ok(searchService.search(new UnifiedSearchQuery(keyword, type, pageNum, pageSize)));
    }
}
