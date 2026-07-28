package top.pxczxn.community.web.editorial;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import top.pxczxn.community.editorial.application.*; import top.pxczxn.platform.common.result.Result; import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/public/editorial") public class PublicEditorialController { private final EditorialCollectionService service; @GetMapping public Result<List<EditorialCollectionView>> list(){return Result.ok(service.publicList());} }
