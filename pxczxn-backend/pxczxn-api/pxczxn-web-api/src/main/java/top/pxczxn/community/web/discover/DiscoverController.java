package top.pxczxn.community.web.discover;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.discover.application.DiscoverService;
import top.pxczxn.platform.common.result.Result;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public")
public class DiscoverController {

    private final DiscoverService discoverService;

    @GetMapping("/discover")
    public Result<DiscoverResponse> discover() {
        return Result.ok(DiscoverResponse.from(discoverService.discover()));
    }
}