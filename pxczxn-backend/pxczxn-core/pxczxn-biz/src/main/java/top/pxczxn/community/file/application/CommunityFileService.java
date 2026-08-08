package top.pxczxn.community.file.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.file.model.CommunityFileReference;
import top.pxczxn.community.file.model.FileObject;
import top.pxczxn.community.file.persistence.CommunityFileReferenceMapper;
import top.pxczxn.community.file.persistence.FileObjectMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityFileService {

    public static final long MAX_UPLOAD_SIZE_BYTES = 20L * 1024 * 1024;

    private static final Set<String> PUBLIC_USAGE_TYPES =
            Set.of(
                    "BLOG_AVATAR",
                    "BLOG_BACKGROUND",
                    "ARTICLE_COVER",
                    "CONTENT_IMAGE"
            );
    private static final Set<String> TARGET_TYPES =
            Set.of("BLOG", "ARTICLE", "ARTICLE_VERSION", "ACCOUNT_ENFORCEMENT_APPEAL");
    private static final Set<String> USAGE_TYPES =
            Set.of(
                    "BLOG_AVATAR",
                    "BLOG_BACKGROUND",
                    "ARTICLE_COVER",
                    "CONTENT_IMAGE",
                    "ATTACHMENT",
                    "APPEAL_EVIDENCE"
            );

    private final FileObjectMapper fileMapper;
    private final CommunityFileReferenceMapper referenceMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityAuth communityAuth;
    private final CommunityObjectStorage storage;
    private final BlogMapper blogMapper;
    private final ArticleMapper articleMapper;
    private final ArticlePermissionService articlePermissionService;
    private final CommunityAbuseGuard abuseGuard;

    @Transactional
    public CommunityFileInfo upload(UploadCommunityFileCommand command) {
        if (command == null) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        Long userId = requireActiveUser();
        abuseGuard.check("USER:" + userId, "FILE_UPLOAD", 10, 60);
        String originalName = normalizeOriginalName(command.originalName());
        byte[] source = command.content();
        if (source == null || source.length == 0) {
            throw new BusinessException(400, "文件内容不能为空");
        }
        if (source.length > MAX_UPLOAD_SIZE_BYTES) {
            throw new BusinessException(400, "单个文件不能超过 20MB");
        }
        CommunityFileInspector.InspectedFile inspected =
                CommunityFileInspector.inspect(originalName, source);
        String objectKey = objectKey(userId, inspected.extension());
        storage.upload(inspected.content(), objectKey);

        FileObject file = new FileObject();
        file.setId(IdWorker.getId());
        file.setStorageProvider(storage.provider().toUpperCase(Locale.ROOT));
        file.setObjectKey(objectKey);
        file.setOriginalName(originalName);
        file.setMimeType(inspected.mimeType());
        file.setSizeBytes((long) inspected.content().length);
        file.setSha256(sha256(inspected.content()));
        file.setStatus("ACTIVE");
        file.setCreatedByUserId(userId);
        file.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (fileMapper.insert(file) != 1) {
                throw new BusinessException(500, "保存文件元数据失败");
            }
        } catch (RuntimeException exception) {
            try {
                storage.delete(objectKey);
            } catch (RuntimeException cleanupFailure) {
                log.error("回滚文件上传时清理对象失败, objectKey={}", objectKey, cleanupFailure);
            }
            throw exception;
        }
        return toInfo(file);
    }

    @Transactional(readOnly = true)
    public CommunityFileInfo getMine(Long fileId) {
        return toInfo(requireOwnedActiveFile(fileId));
    }

    @Transactional(readOnly = true)
    public CommunityFileContent readMine(Long fileId) {
        FileObject file = requireOwnedActiveFile(fileId);
        return content(file);
    }

    @Transactional(readOnly = true)
    public CommunityFileContent readReferencedFile(Long fileId, String targetType, Long targetId, String usageType) {
        String normalizedTarget = normalizedEnum(targetType, TARGET_TYPES, "引用目标类型");
        String normalizedUsage = normalizedEnum(usageType, USAGE_TYPES, "文件用途");
        Long references = referenceMapper.selectCount(Wrappers.<CommunityFileReference>lambdaQuery()
                .eq(CommunityFileReference::getFileId, fileId)
                .eq(CommunityFileReference::getTargetType, normalizedTarget)
                .eq(CommunityFileReference::getTargetId, targetId)
                .eq(CommunityFileReference::getUsageType, normalizedUsage)
                .isNull(CommunityFileReference::getDeletedAt));
        if (references == null || references == 0) throw new BusinessException(404, "申诉附件不存在");
        return content(requireActiveFile(fileId));
    }

    @Transactional(readOnly = true)
    public CommunityFileContent readPublic(Long fileId) {
        FileObject file = requireActiveFile(fileId);
        List<CommunityFileReference> references = referenceMapper.selectList(
                Wrappers.<CommunityFileReference>lambdaQuery()
                        .eq(CommunityFileReference::getFileId, fileId)
                        .in(CommunityFileReference::getUsageType, PUBLIC_USAGE_TYPES)
                        .isNull(CommunityFileReference::getDeletedAt)
        );
        boolean publicReference = references != null
                && references.stream().anyMatch(this::isPublicReference);
        if (!publicReference) {
            throw new BusinessException(404, "文件不存在");
        }
        return content(file);
    }

    @Transactional
    public void deleteMine(Long fileId) {
        FileObject file = requireOwnedActiveFile(fileId);
        Long references = referenceMapper.selectCount(
                Wrappers.<CommunityFileReference>lambdaQuery()
                        .eq(CommunityFileReference::getFileId, fileId)
                        .isNull(CommunityFileReference::getDeletedAt)
        );
        if (references != null && references > 0) {
            throw new BusinessException(409, "文件仍被业务内容引用，不能删除");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int updated = fileMapper.update(
                null,
                Wrappers.<FileObject>lambdaUpdate()
                        .eq(FileObject::getId, file.getId())
                        .eq(FileObject::getCreatedByUserId, communityAuth.getLoginUserId())
                        .eq(FileObject::getStatus, "ACTIVE")
                        .set(FileObject::getStatus, "DELETED")
                        .set(FileObject::getDeletedAt, now)
        );
        if (updated != 1) {
            throw new BusinessException(409, "文件状态已发生变化");
        }
    }

    @Transactional
    public void linkOwnedFile(
            Long fileId,
            String targetType,
            Long targetId,
            String usageType
    ) {
        FileObject file = requireOwnedActiveFile(fileId);
        String normalizedTarget = normalizedEnum(targetType, TARGET_TYPES, "引用目标类型");
        String normalizedUsage = normalizedEnum(usageType, USAGE_TYPES, "文件用途");
        if (targetId == null || targetId <= 0) {
            throw new BusinessException(400, "引用目标 ID 无效");
        }
        Long ownerId = file.getCreatedByUserId();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        referenceMapper.update(
                null,
                Wrappers.<CommunityFileReference>lambdaUpdate()
                        .eq(CommunityFileReference::getTargetType, normalizedTarget)
                        .eq(CommunityFileReference::getTargetId, targetId)
                        .eq(CommunityFileReference::getUsageType, normalizedUsage)
                        .isNull(CommunityFileReference::getDeletedAt)
                        .set(CommunityFileReference::getDeletedAt, now)
        );

        CommunityFileReference existing = referenceMapper.selectOne(
                Wrappers.<CommunityFileReference>lambdaQuery()
                        .eq(CommunityFileReference::getFileId, fileId)
                        .eq(CommunityFileReference::getTargetType, normalizedTarget)
                        .eq(CommunityFileReference::getTargetId, targetId)
                        .eq(CommunityFileReference::getUsageType, normalizedUsage)
                        .last("LIMIT 1")
        );
        if (existing == null) {
            CommunityFileReference reference = new CommunityFileReference();
            reference.setId(IdWorker.getId());
            reference.setFileId(fileId);
            reference.setOwnerUserId(ownerId);
            reference.setTargetType(normalizedTarget);
            reference.setTargetId(targetId);
            reference.setUsageType(normalizedUsage);
            reference.setCreatedAt(now);
            if (referenceMapper.insert(reference) != 1) {
                throw new BusinessException(500, "创建文件引用失败");
            }
        } else {
            if (referenceMapper.update(
                    null,
                    Wrappers.<CommunityFileReference>lambdaUpdate()
                            .eq(CommunityFileReference::getId, existing.getId())
                            .set(CommunityFileReference::getOwnerUserId, ownerId)
                            .set(CommunityFileReference::getDeletedAt, null)
            ) != 1) {
                throw new BusinessException(500, "恢复文件引用失败");
            }
        }
    }

    @Transactional
    public void clearReference(String targetType, Long targetId, String usageType) {
        requireActiveUser();
        String normalizedTarget = normalizedEnum(targetType, TARGET_TYPES, "引用目标类型");
        String normalizedUsage = normalizedEnum(usageType, USAGE_TYPES, "文件用途");
        if (targetId == null || targetId <= 0) {
            throw new BusinessException(400, "引用目标 ID 无效");
        }
        referenceMapper.update(
                null,
                Wrappers.<CommunityFileReference>lambdaUpdate()
                        .eq(CommunityFileReference::getOwnerUserId, communityAuth.getLoginUserId())
                        .eq(CommunityFileReference::getTargetType, normalizedTarget)
                        .eq(CommunityFileReference::getTargetId, targetId)
                        .eq(CommunityFileReference::getUsageType, normalizedUsage)
                        .isNull(CommunityFileReference::getDeletedAt)
                        .set(CommunityFileReference::getDeletedAt, LocalDateTime.now(ZoneOffset.UTC))
        );
    }

    @Transactional
    public void replaceOwnedFileReferences(
            List<Long> fileIds,
            String targetType,
            Long targetId,
            String usageType
    ) {
        Long ownerId = requireActiveUser();
        String normalizedTarget = normalizedEnum(targetType, TARGET_TYPES, "引用目标类型");
        String normalizedUsage = normalizedEnum(usageType, USAGE_TYPES, "文件用途");
        if (targetId == null || targetId <= 0) {
            throw new BusinessException(400, "引用目标 ID 无效");
        }
        LinkedHashSet<Long> requested = new LinkedHashSet<>(
                fileIds == null ? List.of() : fileIds
        );
        requested.remove(null);
        if (requested.size() > 100) {
            throw new BusinessException(400, "单个内容版本最多引用 100 个文件");
        }
        for (Long fileId : requested) {
            FileObject file = requireOwnedActiveFile(fileId);
            if (!ownerId.equals(file.getCreatedByUserId())) {
                throw new BusinessException(403, "无权使用该文件");
            }
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        referenceMapper.update(
                null,
                Wrappers.<CommunityFileReference>update()
                        .eq("owner_user_id", ownerId)
                        .eq("target_type", normalizedTarget)
                        .eq("target_id", targetId)
                        .eq("usage_type", normalizedUsage)
                        .isNull("deleted_at")
                        .set("deleted_at", now)
        );
        for (Long fileId : requested) {
            CommunityFileReference existing = referenceMapper.selectOne(
                    Wrappers.<CommunityFileReference>lambdaQuery()
                            .eq(CommunityFileReference::getFileId, fileId)
                            .eq(CommunityFileReference::getTargetType, normalizedTarget)
                            .eq(CommunityFileReference::getTargetId, targetId)
                            .eq(CommunityFileReference::getUsageType, normalizedUsage)
                            .last("LIMIT 1")
            );
            if (existing == null) {
                CommunityFileReference reference = new CommunityFileReference();
                reference.setId(IdWorker.getId());
                reference.setFileId(fileId);
                reference.setOwnerUserId(ownerId);
                reference.setTargetType(normalizedTarget);
                reference.setTargetId(targetId);
                reference.setUsageType(normalizedUsage);
                reference.setCreatedAt(now);
                if (referenceMapper.insert(reference) != 1) {
                    throw new BusinessException(500, "创建文件引用失败");
                }
            } else if (referenceMapper.update(
                    null,
                    Wrappers.<CommunityFileReference>update()
                            .eq("id", existing.getId())
                            .set("owner_user_id", ownerId)
                            .set("deleted_at", null)
            ) != 1) {
                throw new BusinessException(500, "恢复文件引用失败");
            }
        }
    }

    private Long requireActiveUser() {
        Long userId = communityAuth.getLoginUserId();
        CommunityUser user = userMapper.selectById(userId);
        if (user == null || !Set.of("NORMAL", "LIMITED", "FROZEN").contains(user.getStatus())) {
            throw new BusinessException(403, "当前账号不能使用文件服务");
        }
        return userId;
    }

    private FileObject requireOwnedActiveFile(Long fileId) {
        Long userId = requireActiveUser();
        FileObject file = requireActiveFile(fileId);
        if (!userId.equals(file.getCreatedByUserId())) {
            throw new BusinessException(403, "无权使用该文件");
        }
        return file;
    }

    private boolean isPublicReference(CommunityFileReference reference) {
        if (reference == null
                || reference.getTargetId() == null
                || reference.getTargetId() <= 0) {
            return false;
        }
        return switch (reference.getUsageType()) {
            case "BLOG_AVATAR", "BLOG_BACKGROUND" ->
                    isPublicBlogReference(reference);
            case "ARTICLE_COVER" ->
                    "ARTICLE".equals(reference.getTargetType())
                            && canViewPublicArticle(reference.getTargetId());
            case "CONTENT_IMAGE" ->
                    "ARTICLE_VERSION".equals(reference.getTargetType())
                            && canViewPublishedVersion(reference.getTargetId());
            default -> false;
        };
    }

    private boolean isPublicBlogReference(CommunityFileReference reference) {
        if (!"BLOG".equals(reference.getTargetType())) {
            return false;
        }
        Blog blog = blogMapper.selectById(reference.getTargetId());
        if (blog == null
                || blog.getDeletedAt() != null
                || !"ACTIVE".equals(blog.getStatus())) {
            return false;
        }
        CommunityUser owner = userMapper.selectById(blog.getOwnerUserId());
        return owner != null
                && Set.of("NORMAL", "LIMITED").contains(owner.getStatus());
    }

    private boolean canViewPublishedVersion(Long versionId) {
        Article article = articleMapper.selectOne(
                Wrappers.<Article>lambdaQuery()
                        .eq(Article::getPublishedVersionId, versionId)
                        .isNull(Article::getDeletedAt)
                        .last("LIMIT 1")
        );
        return article != null && canViewPublicArticle(article.getId());
    }

    private boolean canViewPublicArticle(Long articleId) {
        try {
            articlePermissionService.requirePublicArticle(
                    articleId,
                    ArticleAction.VIEW_DETAIL
            );
            return true;
        } catch (BusinessException exception) {
            if (Integer.valueOf(404).equals(exception.getCode())) {
                return false;
            }
            throw exception;
        }
    }

    private FileObject requireActiveFile(Long fileId) {
        if (fileId == null || fileId <= 0) {
            throw new BusinessException(400, "文件 ID 无效");
        }
        FileObject file = fileMapper.selectById(fileId);
        if (file == null || !"ACTIVE".equals(file.getStatus()) || file.getDeletedAt() != null) {
            throw new BusinessException(404, "文件不存在");
        }
        return file;
    }

    private CommunityFileContent content(FileObject file) {
        byte[] content;
        try {
            content = storage.read(file.getObjectKey());
        } catch (RuntimeException exception) {
            log.error("读取社区文件失败, fileId={}", file.getId(), exception);
            throw new BusinessException(500, "读取文件失败");
        }
        return new CommunityFileContent(file.getOriginalName(), file.getMimeType(), content);
    }

    private static String normalizeOriginalName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }
        String normalized = raw.replace('\\', '/');
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (baseName.isEmpty() || baseName.length() > 255 || baseName.contains("\0")) {
            throw new BusinessException(400, "文件名无效");
        }
        return baseName;
    }

    private static String objectKey(Long userId, String extension) {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        return "community/" + userId + "/" + date + "/" + fileName;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private static String normalizedEnum(String raw, Set<String> allowed, String label) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(value)) {
            throw new BusinessException(400, label + "无效");
        }
        return value;
    }

    private static CommunityFileInfo toInfo(FileObject file) {
        return new CommunityFileInfo(
                file.getId(),
                file.getOriginalName(),
                file.getMimeType(),
                file.getSizeBytes(),
                file.getSha256(),
                file.getStatus(),
                file.getCreatedByUserId()
        );
    }
}
