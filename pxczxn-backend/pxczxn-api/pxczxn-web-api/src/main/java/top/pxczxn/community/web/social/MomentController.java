package top.pxczxn.community.web.social;

import jakarta.validation.Valid;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.social.application.MomentService;
import top.pxczxn.community.social.application.MomentType;
import top.pxczxn.community.social.application.PublishMomentCommand;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MomentController {

    private final MomentService service;

    @PostMapping("/moments")
    public Result<MomentPublishResponse> publish(
            @Valid @RequestBody PublishMomentRequest request
    ) {
        return Result.ok(MomentPublishResponse.from(service.publish(
                request == null
                        ? null
                        : new PublishMomentCommand(
                                optionalId(request.blogId(), "博客"),
                                request.momentType(),
                                request.textContent(),
                                request.linkUrl(),
                                optionalId(request.articleId(), "文章"),
                                optionalId(
                                        request.repostMomentId(),
                                        "转发源动态"
                                ),
                                request.visibility()
                        )
        )));
    }

    @GetMapping("/moments/{momentId}")
    public Result<MomentResponse> detail(@PathVariable Long momentId) {
        return Result.ok(MomentResponse.from(service.detail(momentId)));
    }

    @GetMapping("/moments")
    public Result<MomentPageResponse> publicFeed(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Set<String> momentTypes
    ) {
        return Result.ok(MomentPageResponse.from(
                service.publicPage(null, pageNum, pageSize, parseMomentTypes(momentTypes))
        ));
    }

    @GetMapping("/blogs/{blogId}/moments")
    public Result<MomentPageResponse> blogMoments(
            @PathVariable Long blogId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Set<String> momentTypes
    ) {
        return Result.ok(MomentPageResponse.from(
                service.publicPage(blogId, pageNum, pageSize, parseMomentTypes(momentTypes))
        ));
    }

    @GetMapping("/social/me/moments")
    public Result<MomentPageResponse> mine(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.ok(MomentPageResponse.from(
                service.mine(pageNum, pageSize)
        ));
    }

    @DeleteMapping("/moments/{momentId}")
    public Result<MomentDeletionResponse> delete(
            @PathVariable Long momentId,
            @RequestParam Integer expectedLockVersion
    ) {
        return Result.ok(MomentDeletionResponse.from(
                service.delete(momentId, expectedLockVersion)
        ));
    }

    @PostMapping("/moments/{momentId}/share-link")
    public Result<MomentShareLinkResponse> shareLink(
            @PathVariable Long momentId
    ) {
        return Result.ok(MomentShareLinkResponse.from(
                service.shareLink(momentId)
        ));
    }

    private static Long optionalId(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseUnsignedLong(raw.strip());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, label + " ID 无效");
        }
    }

    /**
     * 解析查询参数中的 momentTypes：支持 ?momentTypes=TEXT&momentTypes=LINK
     * （重复键）以及 ?momentTypes=TEXT,LINK（逗号分隔）两种形式。保留顺序、
     * 去重、空值忽略；调用 MomentType.parse 处理非法值（统一抛 400）。
     */
    private static Set<MomentType> parseMomentTypes(Set<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String part : raw) {
            if (part == null) continue;
            for (String token : part.split(",")) {
                String stripped = token.strip();
                if (!stripped.isEmpty()) {
                    normalized.add(stripped.toUpperCase(Locale.ROOT));
                }
            }
        }
        if (normalized.isEmpty()) {
            return Set.of();
        }
        return normalized.stream()
                .map(MomentType::parse)
                .collect(Collectors.toUnmodifiableSet());
    }
}
