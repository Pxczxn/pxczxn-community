package top.pxczxn.community.web.health;

import com.mars.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CommunityHealthController {

    private final String version;

    public CommunityHealthController(
            @Value("${pxczxn.community.version:1.0.0}") String version) {
        this.version = version;
    }

    @GetMapping("/health")
    public Result<CommunityHealthView> health() {
        return Result.ok(new CommunityHealthView(
                "UP",
                "pxczxn-community",
                version
        ));
    }
}
