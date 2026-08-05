package top.pxczxn.community.admin.governance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import top.pxczxn.community.governance.application.AccountEnforcementService;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementAppeal;
import top.pxczxn.community.file.application.CommunityFileContent;
import top.pxczxn.community.file.application.CommunityFileService;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.Result;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/account-appeals")
public class AdminAccountAppealController {
    private final AccountEnforcementService service;
    private final CommunityFileService fileService;
    @GetMapping @SaCheckPermission("community:account:approve")
    public Result<List<CommunityAccountEnforcementAppeal>> list() { return Result.ok(service.appeals()); }
    @PostMapping("/{id}/primary-review") @SaCheckPermission("community:account:approve")
    public Result<CommunityAccountEnforcementAppeal> primary(@PathVariable Long id, @RequestBody Review request) { return Result.ok(service.primaryReviewAppeal(StpUtil.getLoginIdAsLong(), id, request.decision(), request.reviewNote(), StpUtil.hasRole("admin"))); }
    @PostMapping("/{id}/final-review") @SaCheckPermission("community:account:approve")
    public Result<CommunityAccountEnforcementAppeal> finalReview(@PathVariable Long id, @RequestBody Review request) { if (!StpUtil.hasRole("admin")) throw new BusinessException(403, "Only super administrators can finalize appeals"); return Result.ok(service.finalReviewAppeal(StpUtil.getLoginIdAsLong(), id, request.decision(), request.reviewNote(), request.modifiedExpiresAt())); }
    @GetMapping("/{appealId}/files/{fileId}/content") @SaCheckPermission("community:account:approve")
    public ResponseEntity<byte[]> file(@PathVariable Long appealId, @PathVariable Long fileId) {
        CommunityFileContent content = fileService.readReferencedFile(fileId, "ACCOUNT_ENFORCEMENT_APPEAL", appealId, "APPEAL_EVIDENCE");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(content.originalName(), StandardCharsets.UTF_8).build().toString()).contentType(MediaType.parseMediaType(content.mimeType())).contentLength(content.content().length).body(content.content());
    }
    public record Review(String decision, String reviewNote, LocalDateTime modifiedExpiresAt) { }
}
