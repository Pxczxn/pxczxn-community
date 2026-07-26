package top.pxczxn.community.article.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticleAuthoringContext;
import top.pxczxn.community.article.permission.ArticleCommunityAccess;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.blog.model.BlogCategory;
import top.pxczxn.community.blog.persistence.BlogCategoryMapper;
import top.pxczxn.community.file.application.CommunityFileService;
import top.pxczxn.community.file.model.CommunityFileReference;
import top.pxczxn.community.file.persistence.CommunityFileReferenceMapper;
import top.pxczxn.community.taxonomy.application.PlatformTagService;
import top.pxczxn.community.taxonomy.model.ArticleTag;
import top.pxczxn.community.taxonomy.persistence.ArticleTagMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ArticleDraftService {

    private static final Pattern SLUG_PATTERN =
            Pattern.compile("[a-z0-9]+(?:[-_][a-z0-9]+)*");
    private static final Set<String> VISIBILITIES =
            Set.of("PUBLIC", "PRIVATE", "FOLLOWERS_ONLY", "UNLISTED");
    private static final Set<String> PUBLISH_METHODS =
            Set.of("IMMEDIATE", "SCHEDULED", "MANUAL");

    private final ArticleMapper articleMapper;
    private final ArticleVersionMapper versionMapper;
    private final BlogCategoryMapper categoryMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CommunityFileReferenceMapper fileReferenceMapper;
    private final PlatformTagService tagService;
    private final CommunityFileService fileService;
    private final ArticleContentProcessor contentProcessor;
    private final ArticlePermissionService permissionService;

    @Transactional
    public ArticleEditorView create(CreateArticleCommand command) {
        if (command == null) {
            throw new BusinessException(400, "文章信息不能为空");
        }
        ArticleAuthoringContext context =
                permissionService.requirePersonalAuthoringContext();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Long articleId = IdWorker.getId();
        RenderedArticleContent content = contentProcessor.process(
                command.contentMode(),
                command.richTextJson(),
                command.markdownContent()
        );

        Article article = new Article();
        article.setId(articleId);
        article.setBlogId(context.blog().getId());
        article.setAuthorUserId(context.user().getId());
        article.setCategoryId(resolveCategory(
                context.blog().getId(), command.categoryId()
        ).getId());
        article.setTitle(requiredText(command.title(), "文章标题", 200));
        article.setSlug(createSlug(command.slug(), articleId));
        article.setSummary(createSummary(command.summary(), content.plainText()));
        article.setCoverFileId(command.coverFileId());
        article.setContentMode(content.contentMode());
        article.setVisibility(normalizeEnum(
                command.visibility(), VISIBILITIES, "PUBLIC", "文章可见性"
        ));
        article.setPublishMethod(normalizeEnum(
                command.publishMethod(), PUBLISH_METHODS, "MANUAL", "发布方式"
        ));
        article.setPublishStatus("DRAFT");
        article.setReviewStatus("NOT_SUBMITTED");
        article.setViewCount(0L);
        article.setLikeCount(0L);
        article.setFavoriteCount(0L);
        article.setCommentCount(0L);
        article.setLockVersion(0);
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        try {
            if (articleMapper.insert(article) != 1) {
                throw new BusinessException(500, "创建文章失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "当前博客已存在相同文章地址");
        }

        List<Long> contentFileIds = normalizeIds(command.contentFileIds(), 100, "正文文件");
        ArticleVersion version = createVersion(
                article,
                context.user().getId(),
                content,
                "CREATE",
                contentFileIds,
                now
        );
        if (articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", articleId)
                        .isNull("current_version_id")
                        .set("current_version_id", version.getId())
                        .set("updated_at", now)
        ) != 1) {
            throw new BusinessException(409, "文章初始化状态已发生变化");
        }
        article.setCurrentVersionId(version.getId());

        tagService.replaceArticleTags(
                articleId,
                normalizeIds(command.tagIds(), 5, "文章标签")
        );
        if (command.coverFileId() != null) {
            fileService.linkOwnedFile(
                    command.coverFileId(), "ARTICLE", articleId, "ARTICLE_COVER"
            );
        }
        return toEditor(article, version);
    }

    @Transactional(readOnly = true)
    public ArticleEditorView getEditor(Long articleId) {
        Article article = permissionService
                .requireCommunityArticle(articleId, ArticleAction.VIEW_EDITOR)
                .article();
        return toEditor(article, requireCurrentVersion(article));
    }

    @Transactional
    public ArticleEditorView save(
            Long articleId,
            SaveArticleCommand command,
            boolean autosave
    ) {
        if (command == null) {
            throw new BusinessException(400, "文章信息不能为空");
        }
        ArticleCommunityAccess access = permissionService
                .requireCommunityArticle(articleId, ArticleAction.EDIT);
        Article article = access.article();
        requireExpectedLock(article, command.expectedLockVersion());
        ArticleVersion currentVersion = requireCurrentVersion(article);

        String title = command.title() == null
                ? article.getTitle()
                : requiredText(command.title(), "文章标题", 200);
        String slug = command.slug() == null
                ? article.getSlug()
                : normalizeSlug(command.slug());
        String summary = command.summary() == null
                ? article.getSummary()
                : optionalText(command.summary(), "文章摘要", 500);
        Long categoryId = command.categoryId() == null
                ? article.getCategoryId()
                : resolveCategory(article.getBlogId(), command.categoryId()).getId();
        Long coverFileId = Boolean.TRUE.equals(command.clearCoverFile())
                ? null
                : command.coverFileId() == null
                        ? article.getCoverFileId()
                        : command.coverFileId();
        String visibility = command.visibility() == null
                ? article.getVisibility()
                : normalizeEnum(command.visibility(), VISIBILITIES, null, "文章可见性");
        String publishMethod = command.publishMethod() == null
                ? article.getPublishMethod()
                : normalizeEnum(command.publishMethod(), PUBLISH_METHODS, null, "发布方式");
        RenderedArticleContent content = resolveContent(command, currentVersion);
        List<Long> currentTagIds = loadTagIds(articleId);
        List<Long> tagIds = command.tagIds() == null
                ? currentTagIds
                : normalizeIds(command.tagIds(), 5, "文章标签");
        List<Long> currentContentFileIds = loadContentFileIds(currentVersion.getId());
        List<Long> contentFileIds = command.contentFileIds() == null
                ? currentContentFileIds
                : normalizeIds(command.contentFileIds(), 100, "正文文件");

        if (autosave && autosaveUnchanged(
                article,
                currentVersion,
                title,
                slug,
                summary,
                categoryId,
                coverFileId,
                visibility,
                publishMethod,
                tagIds,
                currentTagIds,
                contentFileIds,
                currentContentFileIds,
                content
        )) {
            return toEditor(article, currentVersion);
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ArticleVersion newVersion = createVersion(
                article,
                access.actor().getId(),
                content,
                autosave ? "AUTO_SAVE" : "MANUAL_SAVE",
                contentFileIds,
                now
        );
        String nextPublishStatus = keepsPublishedVersion(article)
                ? article.getPublishStatus()
                : "DRAFT";
        int updated;
        try {
            updated = articleMapper.update(
                    null,
                    Wrappers.<Article>update()
                            .eq("id", article.getId())
                            .eq("lock_version", command.expectedLockVersion())
                            .isNull("deleted_at")
                            .set("title", title)
                            .set("slug", slug)
                            .set("summary", summary)
                            .set("category_id", categoryId)
                            .set("cover_file_id", coverFileId)
                            .set("content_mode", content.contentMode())
                            .set("visibility", visibility)
                            .set("publish_method", publishMethod)
                            .set("publish_status", nextPublishStatus)
                            .set("review_status", "NOT_SUBMITTED")
                            .set("review_version_id", null)
                            .set("current_version_id", newVersion.getId())
                            .set("updated_at", now)
                            .setSql("lock_version = lock_version + 1")
            );
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "当前博客已存在相同文章地址");
        }
        if (updated != 1) {
            throw new BusinessException(409, "文章已在其他窗口更新，请刷新后重试");
        }

        if (command.tagIds() != null) {
            tagService.replaceArticleTags(articleId, tagIds);
        }
        updateCoverReference(article, coverFileId);
        applyUpdatedArticle(
                article,
                newVersion,
                title,
                slug,
                summary,
                categoryId,
                coverFileId,
                visibility,
                publishMethod,
                nextPublishStatus,
                now
        );
        return toEditor(article, newVersion);
    }

    @Transactional
    public ArticleEditorView restoreVersion(
            Long articleId,
            Long versionId,
            Integer expectedLockVersion
    ) {
        ArticleCommunityAccess access = permissionService
                .requireCommunityArticle(articleId, ArticleAction.EDIT);
        Article article = access.article();
        requireExpectedLock(article, expectedLockVersion);
        ArticleVersion target = requireOwnedVersion(article, versionId);
        RenderedArticleContent content = new RenderedArticleContent(
                target.getContentMode(),
                target.getRichTextJson(),
                target.getMarkdownContent(),
                target.getRenderedHtml(),
                target.getPlainText(),
                target.getTocJson(),
                target.getContentHash(),
                safeInt(target.getWordCount()),
                Math.max(1, safeInt(target.getReadingTimeMinutes()))
        );
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ArticleVersion restored = createVersion(
                article,
                access.actor().getId(),
                content,
                "RESTORE",
                loadContentFileIds(target.getId()),
                now
        );
        String nextPublishStatus = keepsPublishedVersion(article)
                ? article.getPublishStatus()
                : "DRAFT";
        int updated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("lock_version", expectedLockVersion)
                        .isNull("deleted_at")
                        .set("content_mode", restored.getContentMode())
                        .set("current_version_id", restored.getId())
                        .set("publish_status", nextPublishStatus)
                        .set("review_status", "NOT_SUBMITTED")
                        .set("review_version_id", null)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (updated != 1) {
            throw new BusinessException(409, "文章已在其他窗口更新，请刷新后重试");
        }
        article.setContentMode(restored.getContentMode());
        article.setCurrentVersionId(restored.getId());
        article.setPublishStatus(nextPublishStatus);
        article.setReviewStatus("NOT_SUBMITTED");
        article.setReviewVersionId(null);
        article.setUpdatedAt(now);
        article.setLockVersion(safeInt(article.getLockVersion()) + 1);
        return toEditor(article, restored);
    }

    @Transactional(readOnly = true)
    public ArticleVersionPageView listVersions(
            Long articleId,
            Integer requestedPageNum,
            Integer requestedPageSize
    ) {
        Article article = permissionService
                .requireCommunityArticle(articleId, ArticleAction.VIEW_EDITOR)
                .article();
        int pageNum = requestedPageNum == null ? 1 : requestedPageNum;
        int pageSize = requestedPageSize == null ? 20 : requestedPageSize;
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(400, "分页参数无效");
        }
        long total = versionMapper.selectCount(
                Wrappers.<ArticleVersion>lambdaQuery()
                        .eq(ArticleVersion::getArticleId, articleId)
        );
        long offset = (long) (pageNum - 1) * pageSize;
        if (offset > Integer.MAX_VALUE) {
            return new ArticleVersionPageView(List.of(), pageNum, pageSize, total);
        }
        List<ArticleVersionSummaryView> list = versionMapper.selectList(
                        Wrappers.<ArticleVersion>lambdaQuery()
                                .eq(ArticleVersion::getArticleId, articleId)
                                .orderByDesc(ArticleVersion::getVersionNo)
                                .last("LIMIT " + offset + ", " + pageSize)
                )
                .stream()
                .map(version -> toSummary(article, version))
                .toList();
        return new ArticleVersionPageView(list, pageNum, pageSize, total);
    }

    @Transactional(readOnly = true)
    public ArticleVersionDetailView getVersion(Long articleId, Long versionId) {
        Article article = permissionService
                .requireCommunityArticle(articleId, ArticleAction.VIEW_EDITOR)
                .article();
        return toDetail(article, requireOwnedVersion(article, versionId));
    }

    @Transactional
    public void delete(Long articleId, Integer expectedLockVersion) {
        Article article = permissionService
                .requireCommunityArticle(articleId, ArticleAction.DELETE)
                .article();
        requireExpectedLock(article, expectedLockVersion);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int updated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("lock_version", expectedLockVersion)
                        .isNull("deleted_at")
                        .set("publish_status", "DELETED")
                        .set("review_status", "CANCELLED")
                        .set("review_version_id", null)
                        .set("deleted_at", now)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (updated != 1) {
            throw new BusinessException(409, "文章已在其他窗口更新，请刷新后重试");
        }
    }

    private ArticleVersion createVersion(
            Article article,
            Long userId,
            RenderedArticleContent content,
            String creationType,
            List<Long> contentFileIds,
            LocalDateTime now
    ) {
        ArticleVersion latest = versionMapper.selectOne(
                Wrappers.<ArticleVersion>lambdaQuery()
                        .eq(ArticleVersion::getArticleId, article.getId())
                        .orderByDesc(ArticleVersion::getVersionNo)
                        .last("LIMIT 1")
        );
        ArticleVersion version = new ArticleVersion();
        version.setId(IdWorker.getId());
        version.setArticleId(article.getId());
        version.setVersionNo(latest == null ? 1 : safeInt(latest.getVersionNo()) + 1);
        version.setContentMode(content.contentMode());
        version.setRichTextJson(content.richTextJson());
        version.setMarkdownContent(content.markdownContent());
        version.setRenderedHtml(content.renderedHtml());
        version.setPlainText(content.plainText());
        version.setTocJson(content.tocJson());
        version.setContentHash(content.contentHash());
        version.setWordCount(content.wordCount());
        version.setReadingTimeMinutes(content.readingTimeMinutes());
        version.setCreatedByUserId(userId);
        version.setCreationType(creationType);
        version.setCreatedAt(now);
        try {
            if (versionMapper.insert(version) != 1) {
                throw new BusinessException(500, "保存文章版本失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "文章版本已在其他窗口保存，请刷新后重试");
        }
        fileService.replaceOwnedFileReferences(
                contentFileIds,
                "ARTICLE_VERSION",
                version.getId(),
                "CONTENT_IMAGE"
        );
        return version;
    }

    private RenderedArticleContent resolveContent(
            SaveArticleCommand command,
            ArticleVersion current
    ) {
        String mode = command.contentMode() == null
                ? current.getContentMode()
                : command.contentMode().trim().toUpperCase(Locale.ROOT);
        String rich = command.richTextJson();
        String markdown = command.markdownContent();
        if (mode.equals(current.getContentMode()) && rich == null && markdown == null) {
            rich = current.getRichTextJson();
            markdown = current.getMarkdownContent();
        } else if (!mode.equals(current.getContentMode())) {
            if ("RICH_TEXT".equals(mode) && rich == null) {
                throw new BusinessException(400, "切换到富文本模式时必须提交完整富文本源");
            }
            if ("MARKDOWN".equals(mode) && markdown == null) {
                throw new BusinessException(400, "切换到 Markdown 模式时必须提交完整 Markdown 源");
            }
        }
        return contentProcessor.process(mode, rich, markdown);
    }

    private ArticleVersion requireCurrentVersion(Article article) {
        if (article.getCurrentVersionId() == null) {
            throw new BusinessException(500, "文章当前版本缺失");
        }
        return requireOwnedVersion(article, article.getCurrentVersionId());
    }

    private ArticleVersion requireOwnedVersion(Article article, Long versionId) {
        if (versionId == null || versionId <= 0) {
            throw new BusinessException(400, "版本 ID 无效");
        }
        ArticleVersion version = versionMapper.selectOne(
                Wrappers.<ArticleVersion>lambdaQuery()
                        .eq(ArticleVersion::getId, versionId)
                        .eq(ArticleVersion::getArticleId, article.getId())
                        .last("LIMIT 1")
        );
        if (version == null) {
            throw new BusinessException(404, "文章版本不存在");
        }
        return version;
    }

    private BlogCategory resolveCategory(Long blogId, Long requestedCategoryId) {
        BlogCategory category;
        if (requestedCategoryId == null) {
            category = categoryMapper.selectOne(
                    Wrappers.<BlogCategory>lambdaQuery()
                            .eq(BlogCategory::getBlogId, blogId)
                            .eq(BlogCategory::getIsDefault, 1)
                            .isNull(BlogCategory::getDeletedAt)
                            .last("LIMIT 1")
            );
        } else {
            category = categoryMapper.selectOne(
                    Wrappers.<BlogCategory>lambdaQuery()
                            .eq(BlogCategory::getId, requestedCategoryId)
                            .eq(BlogCategory::getBlogId, blogId)
                            .isNull(BlogCategory::getDeletedAt)
                            .last("LIMIT 1")
            );
        }
        if (category == null) {
            throw new BusinessException(400, "文章分类不存在或不属于当前博客");
        }
        return category;
    }

    private void updateCoverReference(Article article, Long coverFileId) {
        if (coverFileId == null) {
            fileService.clearReference("ARTICLE", article.getId(), "ARTICLE_COVER");
        } else if (!Objects.equals(article.getCoverFileId(), coverFileId)) {
            fileService.linkOwnedFile(
                    coverFileId, "ARTICLE", article.getId(), "ARTICLE_COVER"
            );
        }
    }

    private void applyUpdatedArticle(
            Article article,
            ArticleVersion version,
            String title,
            String slug,
            String summary,
            Long categoryId,
            Long coverFileId,
            String visibility,
            String publishMethod,
            String publishStatus,
            LocalDateTime now
    ) {
        article.setTitle(title);
        article.setSlug(slug);
        article.setSummary(summary);
        article.setCategoryId(categoryId);
        article.setCoverFileId(coverFileId);
        article.setContentMode(version.getContentMode());
        article.setVisibility(visibility);
        article.setPublishMethod(publishMethod);
        article.setPublishStatus(publishStatus);
        article.setReviewStatus("NOT_SUBMITTED");
        article.setReviewVersionId(null);
        article.setCurrentVersionId(version.getId());
        article.setLockVersion(safeInt(article.getLockVersion()) + 1);
        article.setUpdatedAt(now);
    }

    private ArticleEditorView toEditor(Article article, ArticleVersion version) {
        return new ArticleEditorView(
                article.getId(),
                article.getBlogId(),
                article.getAuthorUserId(),
                article.getCategoryId(),
                article.getTitle(),
                article.getSlug(),
                article.getSummary(),
                article.getCoverFileId(),
                article.getContentMode(),
                article.getVisibility(),
                article.getPublishMethod(),
                article.getPublishStatus(),
                article.getReviewStatus(),
                article.getCurrentVersionId(),
                article.getPublishedVersionId(),
                article.getReviewVersionId(),
                safeInt(article.getLockVersion()),
                loadTagIds(article.getId()),
                loadContentFileIds(version.getId()),
                version.getRichTextJson(),
                version.getMarkdownContent(),
                version.getRenderedHtml(),
                version.getPlainText(),
                version.getTocJson(),
                version.getContentHash(),
                safeInt(version.getWordCount()),
                Math.max(1, safeInt(version.getReadingTimeMinutes())),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                version.getCreatedAt()
        );
    }

    private ArticleVersionSummaryView toSummary(
            Article article,
            ArticleVersion version
    ) {
        return new ArticleVersionSummaryView(
                version.getId(),
                safeInt(version.getVersionNo()),
                version.getContentMode(),
                version.getContentHash(),
                safeInt(version.getWordCount()),
                Math.max(1, safeInt(version.getReadingTimeMinutes())),
                version.getCreatedByUserId(),
                version.getCreationType(),
                Objects.equals(article.getCurrentVersionId(), version.getId()),
                Objects.equals(article.getPublishedVersionId(), version.getId()),
                version.getCreatedAt()
        );
    }

    private ArticleVersionDetailView toDetail(
            Article article,
            ArticleVersion version
    ) {
        return new ArticleVersionDetailView(
                version.getId(),
                version.getArticleId(),
                safeInt(version.getVersionNo()),
                version.getContentMode(),
                version.getRichTextJson(),
                version.getMarkdownContent(),
                version.getRenderedHtml(),
                version.getPlainText(),
                version.getTocJson(),
                version.getContentHash(),
                safeInt(version.getWordCount()),
                Math.max(1, safeInt(version.getReadingTimeMinutes())),
                version.getCreatedByUserId(),
                version.getCreationType(),
                loadContentFileIds(version.getId()),
                Objects.equals(article.getCurrentVersionId(), version.getId()),
                Objects.equals(article.getPublishedVersionId(), version.getId()),
                version.getCreatedAt()
        );
    }

    private List<Long> loadTagIds(Long articleId) {
        return articleTagMapper.selectList(
                        Wrappers.<ArticleTag>lambdaQuery()
                                .eq(ArticleTag::getArticleId, articleId)
                                .orderByAsc(ArticleTag::getSortOrder)
                )
                .stream()
                .map(ArticleTag::getTagId)
                .toList();
    }

    private List<Long> loadContentFileIds(Long versionId) {
        return fileReferenceMapper.selectList(
                        Wrappers.<CommunityFileReference>lambdaQuery()
                                .eq(CommunityFileReference::getTargetType, "ARTICLE_VERSION")
                                .eq(CommunityFileReference::getTargetId, versionId)
                                .eq(CommunityFileReference::getUsageType, "CONTENT_IMAGE")
                                .isNull(CommunityFileReference::getDeletedAt)
                                .orderByAsc(CommunityFileReference::getCreatedAt)
                                .orderByAsc(CommunityFileReference::getId)
                )
                .stream()
                .map(CommunityFileReference::getFileId)
                .toList();
    }

    private static boolean autosaveUnchanged(
            Article article,
            ArticleVersion currentVersion,
            String title,
            String slug,
            String summary,
            Long categoryId,
            Long coverFileId,
            String visibility,
            String publishMethod,
            List<Long> tagIds,
            List<Long> currentTagIds,
            List<Long> contentFileIds,
            List<Long> currentContentFileIds,
            RenderedArticleContent content
    ) {
        return Objects.equals(article.getTitle(), title)
                && Objects.equals(article.getSlug(), slug)
                && Objects.equals(article.getSummary(), summary)
                && Objects.equals(article.getCategoryId(), categoryId)
                && Objects.equals(article.getCoverFileId(), coverFileId)
                && Objects.equals(article.getVisibility(), visibility)
                && Objects.equals(article.getPublishMethod(), publishMethod)
                && Objects.equals(currentVersion.getContentHash(), content.contentHash())
                && Objects.equals(tagIds, currentTagIds)
                && Objects.equals(contentFileIds, currentContentFileIds);
    }

    private static void requireExpectedLock(
            Article article,
            Integer expectedLockVersion
    ) {
        if (expectedLockVersion == null || expectedLockVersion < 0) {
            throw new BusinessException(400, "必须提交有效的文章锁版本");
        }
        if (!Integer.valueOf(safeInt(article.getLockVersion())).equals(expectedLockVersion)) {
            throw new BusinessException(409, "文章已在其他窗口更新，请刷新后重试");
        }
    }

    private static boolean keepsPublishedVersion(Article article) {
        return article.getPublishedVersionId() != null
                && Set.of("PUBLISHED", "HIDDEN").contains(article.getPublishStatus());
    }

    private static String createSlug(String raw, Long articleId) {
        if (raw == null || raw.isBlank()) {
            return "article-" + Long.toUnsignedString(articleId, 36);
        }
        return normalizeSlug(raw);
    }

    private static String normalizeSlug(String raw) {
        String slug = raw == null
                ? ""
                : Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
                        .toLowerCase(Locale.ROOT);
        if (slug.length() < 1 || slug.length() > 160 || !SLUG_PATTERN.matcher(slug).matches()) {
            throw new BusinessException(
                    400,
                    "文章地址须为 1-160 位小写字母、数字、下划线或连字符"
            );
        }
        return slug;
    }

    private static String createSummary(String raw, String plainText) {
        String summary = optionalText(raw, "文章摘要", 500);
        if (summary != null) {
            return summary;
        }
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        return plainText.length() <= 200
                ? plainText
                : plainText.substring(0, 200);
    }

    private static String requiredText(String raw, String label, int max) {
        if (raw == null) {
            throw new BusinessException(400, label + "不能为空");
        }
        String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (value.isEmpty() || value.length() > max) {
            throw new BusinessException(400, label + "长度须为 1-" + max + " 个字符");
        }
        return value;
    }

    private static String optionalText(String raw, String label, int max) {
        if (raw == null) {
            return null;
        }
        String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (value.length() > max) {
            throw new BusinessException(400, label + "不能超过 " + max + " 个字符");
        }
        return value.isEmpty() ? null : value;
    }

    private static String normalizeEnum(
            String raw,
            Set<String> allowed,
            String defaultValue,
            String label
    ) {
        if ((raw == null || raw.isBlank()) && defaultValue != null) {
            return defaultValue;
        }
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(value)) {
            throw new BusinessException(400, label + "无效");
        }
        return value;
    }

    private static List<Long> normalizeIds(
            List<Long> rawIds,
            int max,
            String label
    ) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>(
                rawIds == null ? List.of() : rawIds
        );
        ids.remove(null);
        if (ids.size() > max) {
            throw new BusinessException(400, label + "最多选择 " + max + " 个");
        }
        List<Long> normalized = new ArrayList<>(ids.size());
        for (Long id : ids) {
            if (id == null || id <= 0) {
                throw new BusinessException(400, label + " ID 无效");
            }
            normalized.add(id);
        }
        return List.copyOf(normalized);
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

}
