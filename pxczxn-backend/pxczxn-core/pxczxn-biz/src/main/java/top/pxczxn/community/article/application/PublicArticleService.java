package top.pxczxn.community.article.application;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.block.application.CommunityBlockService;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.permission.ArticlePublicAccess;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogCategory;
import top.pxczxn.community.blog.model.BlogSetting;
import top.pxczxn.community.blog.persistence.BlogCategoryMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.blog.persistence.BlogSettingMapper;
import top.pxczxn.community.taxonomy.model.ArticleTag;
import top.pxczxn.community.taxonomy.model.PlatformTag;
import top.pxczxn.community.taxonomy.persistence.ArticleTagMapper;
import top.pxczxn.community.taxonomy.persistence.PlatformTagMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicArticleService {

    private static final Pattern SLUG_PATTERN =
            Pattern.compile("[a-z0-9]+(?:[-_][a-z0-9]+)*");
    private static final Set<String> PUBLIC_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");

    private final ArticleMapper articleMapper;
    private final ArticleVersionMapper versionMapper;
    private final BlogMapper blogMapper;
    private final BlogSettingMapper settingMapper;
    private final BlogCategoryMapper categoryMapper;
    private final ArticleTagMapper articleTagMapper;
    private final PlatformTagMapper tagMapper;
    private final CommunityUserMapper userMapper;
    private final ArticlePermissionService permissionService;
    private final CommunityBlockService blockService;
    private final CommunityAuth communityAuth;

    @Transactional(readOnly = true)
    public PublicArticleDetailView detail(Long articleId) {
        ArticlePublicAccess access = permissionService.requirePublicArticle(
                articleId,
                ArticleAction.VIEW_DETAIL
        );
        Article article = access.article();
        if (isBlocked(article)) {
            throw new BusinessException(404, "文章不存在");
        }
        ArticleVersion version = requirePublishedVersion(article);
        BlogSetting setting = findSetting(access.blog().getId());
        PublicArticleCategoryView category =
                category(article.getCategoryId(), access.blog().getId());
        List<PublicArticleTagView> tags =
                tagsByArticleIds(List.of(article.getId()))
                        .getOrDefault(article.getId(), List.of());
        String canonicalPath = canonicalPath(access.blog(), article);
        String description = seoDescription(article, version);
        return new PublicArticleDetailView(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getSummary(),
                article.getCoverFileId(),
                version.getContentMode(),
                article.getVisibility(),
                version.getRenderedHtml(),
                version.getTocJson(),
                safeInt(version.getWordCount()),
                Math.max(1, safeInt(version.getReadingTimeMinutes())),
                article.getPublishedAt(),
                article.getUpdatedAt(),
                safeLong(article.getViewCount()),
                safeLong(article.getLikeCount()),
                safeLong(article.getFavoriteCount()),
                safeLong(article.getCommentCount()),
                canonicalPath,
                author(access.author()),
                blog(access.blog(), setting),
                category,
                tags,
                new PublicArticleSeoView(
                        article.getTitle() + " - " + access.blog().getName(),
                        description,
                        canonicalPath
                )
        );
    }

    @Transactional(readOnly = true)
    public PublicArticlePageView page(
            String blogSlug,
            PublicArticleQuery rawQuery
    ) {
        PublicArticleQuery query = normalizeQuery(rawQuery);
        BlogAccess access = requirePublicBlog(blogSlug);
        if (blockService.isBlogBlocked(
                communityAuth.getOptionalLoginUserId(), access.blog().getId())) {
            return new PublicArticlePageView(List.of(), 0, query.pageNum(), query.pageSize());
        }
        BlogCategory selectedCategory = query.categorySlug() == null
                ? null
                : requireCategory(access.blog().getId(), query.categorySlug());
        IPage<Article> result = articleMapper.selectPublicPage(
                new Page<>(query.pageNum(), query.pageSize()),
                access.blog().getId(),
                selectedCategory == null ? null : selectedCategory.getId()
        );
        List<Article> articles = result.getRecords() == null
                ? List.of()
                : result.getRecords();
        if (articles.isEmpty()) {
            return new PublicArticlePageView(
                    List.of(),
                    result.getTotal(),
                    query.pageNum(),
                    query.pageSize()
            );
        }

        List<PublicArticleSummaryView> records = hydrateSummary(articles);
        return new PublicArticlePageView(
                List.copyOf(records),
                result.getTotal(),
                query.pageNum(),
                query.pageSize()
        );
    }

    /**
     * Returns the newest publicly listable articles across active blogs.  It is a
     * transparent chronological source for the discovery page, not a recommendation
     * algorithm; ranking remains an explicit presentation concern in the client.
     */
    @Transactional(readOnly = true)
    public PublicArticlePageView discover(PublicDiscoveryQuery rawQuery) {
        PublicArticleQuery query = normalizeDiscoveryQuery(rawQuery);
        IPage<Article> result = articleMapper.selectDiscoverPublicPage(
                new Page<>(query.pageNum(), query.pageSize()),
                rawQuery.keyword(),
                rawQuery.tagSlug()
        );
        List<Article> articles = result.getRecords() == null
                ? List.of()
                : result.getRecords();
        if (articles.isEmpty()) {
            return new PublicArticlePageView(List.of(), result.getTotal(), query.pageNum(), query.pageSize());
        }
        List<PublicArticleSummaryView> records = hydrateSummary(articles);
        return new PublicArticlePageView(records, result.getTotal(), query.pageNum(), query.pageSize());
    }

    @Transactional(readOnly = true)
    public PublicDiscoveryPageView discoverRanked(PublicDiscoveryQuery rawQuery) {
        PublicArticleQuery query = normalizeDiscoveryQuery(rawQuery);
        PublicDiscoverySort sort = PublicDiscoverySort.parse(rawQuery.sort());
        IPage<Article> result = articleMapper.selectDiscoverRankedPage(
                new Page<>(query.pageNum(), query.pageSize()),
                sort.name(),
                rawQuery.keyword(),
                rawQuery.tagSlug()
        );
        List<Article> articles = result.getRecords() == null ? List.<Article>of() : result.getRecords();
        List<PublicArticleSummaryView> records = articles.isEmpty()
                ? List.of()
                : hydrateSummary(articles);
        return new PublicDiscoveryPageView(new PublicArticlePageView(records, result.getTotal(), query.pageNum(), query.pageSize()), sort);
    }

    /**
     * Batch hydrate articles into summary views. Resolves authors, versions,
     * categories, and tags in bulk to avoid N+1 queries.
     */
    private List<PublicArticleSummaryView> hydrateSummary(List<Article> articles) {
        List<Article> filtered = articles.stream()
                .filter(article -> !isBlocked(article))
                .toList();
        if (filtered.isEmpty()) {
            return List.of();
        }
        Map<Long, CommunityUser> authors = byId(
                userMapper.selectBatchIds(distinct(
                        filtered.stream().map(Article::getAuthorUserId).toList()
                )),
                CommunityUser::getId
        );
        Map<Long, ArticleVersion> versions = byId(
                versionMapper.selectBatchIds(distinct(
                        filtered.stream().map(Article::getPublishedVersionId).toList()
                )),
                ArticleVersion::getId
        );
        List<Long> categoryIds = distinct(
                filtered.stream().map(Article::getCategoryId).filter(Objects::nonNull).toList()
        );
        Map<Long, BlogCategory> categories = categoryIds.isEmpty()
                ? Map.of()
                : byId(categoryMapper.selectBatchIds(categoryIds), BlogCategory::getId);
        Map<Long, List<PublicArticleTagView>> tags =
                tagsByArticleIds(filtered.stream().map(Article::getId).toList());

        // Group articles by blog for blog info lookup
        List<Long> blogIds = distinct(filtered.stream().map(Article::getBlogId).toList());
        Map<Long, Blog> blogs = blogIds.isEmpty()
                ? Map.of()
                : byId(blogMapper.selectBatchIds(blogIds), Blog::getId);
        Map<Long, BlogSetting> settings = blogIds.isEmpty()
                ? Map.of()
                : byId(settingMapper.selectList(Wrappers.<BlogSetting>lambdaQuery()
                        .in(BlogSetting::getBlogId, blogIds)), BlogSetting::getBlogId);

        List<PublicArticleSummaryView> records = new ArrayList<>();
        for (Article article : filtered) {
            CommunityUser articleAuthor = authors.get(article.getAuthorUserId());
            ArticleVersion version = versions.get(article.getPublishedVersionId());
            if (version == null
                    || !Objects.equals(version.getArticleId(), article.getId())
                    || version.getRenderedHtml() == null) {
                continue;
            }
            Blog blog = blogs.get(article.getBlogId());
            if (blog == null) {
                continue;
            }
            BlogSetting setting = settings.get(article.getBlogId());
            BlogCategory category = categories.get(article.getCategoryId());
            records.add(new PublicArticleSummaryView(
                    article.getId(),
                    article.getTitle(),
                    article.getSlug(),
                    article.getSummary(),
                    article.getCoverFileId(),
                    version.getContentMode(),
                    author(articleAuthor),
                    category(category, article.getBlogId()),
                    tags.getOrDefault(article.getId(), List.of()),
                    article.getPublishedAt(),
                    article.getUpdatedAt(),
                    safeInt(version.getWordCount()),
                    Math.max(1, safeInt(version.getReadingTimeMinutes())),
                    safeLong(article.getViewCount()),
                    safeLong(article.getLikeCount()),
                    safeLong(article.getFavoriteCount()),
                    safeLong(article.getCommentCount()),
                    canonicalPath(blog, article)
            ));
        }
        return List.copyOf(records);
    }

    @Transactional(readOnly = true)
    public List<PublicArticleCategoryView> categories(String blogSlug) {
        BlogAccess access = requirePublicBlog(blogSlug);
        return categoryMapper.selectList(
                Wrappers.<BlogCategory>lambdaQuery()
                        .eq(BlogCategory::getBlogId, access.blog().getId())
                        .isNull(BlogCategory::getDeletedAt)
                        .orderByAsc(
                                BlogCategory::getSortOrder,
                                BlogCategory::getId
                        )
        ).stream().map(value -> category(
                value,
                access.blog().getId()
        )).toList();
    }

    private BlogAccess requirePublicBlog(String rawSlug) {
        String slug = normalizeSlug(rawSlug, "博客地址");
        Blog blog = blogMapper.selectOne(
                Wrappers.<Blog>lambdaQuery()
                        .eq(Blog::getSlug, slug)
                        .eq(Blog::getStatus, "ACTIVE")
                        .isNull(Blog::getDeletedAt)
                        .last("LIMIT 1")
        );
        if (blog == null) {
            throw new BusinessException(404, "博客不存在");
        }
        CommunityUser owner = userMapper.selectById(blog.getOwnerUserId());
        if (owner == null
                || !PUBLIC_USER_STATUSES.contains(owner.getStatus())) {
            throw new BusinessException(404, "博客不存在");
        }
        return new BlogAccess(blog);
    }

    private BlogCategory requireCategory(Long blogId, String rawSlug) {
        String slug = normalizeSlug(rawSlug, "分类地址");
        BlogCategory category = categoryMapper.selectOne(
                Wrappers.<BlogCategory>lambdaQuery()
                        .eq(BlogCategory::getBlogId, blogId)
                        .eq(BlogCategory::getSlug, slug)
                        .isNull(BlogCategory::getDeletedAt)
                        .last("LIMIT 1")
        );
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        return category;
    }

    private ArticleVersion requirePublishedVersion(Article article) {
        ArticleVersion version =
                versionMapper.selectById(article.getPublishedVersionId());
        if (version == null
                || !Objects.equals(version.getArticleId(), article.getId())
                || version.getRenderedHtml() == null) {
            throw new BusinessException(404, "文章不存在");
        }
        return version;
    }

    private boolean isPublicListRecord(
            Blog blog,
            CommunityUser author,
            Article article,
            ArticleVersion version
    ) {
        return version != null
                && Objects.equals(version.getArticleId(), article.getId())
                && version.getRenderedHtml() != null
                && permissionService.decidePublic(
                        ArticleAction.LIST_PUBLIC,
                        null,
                        author,
                        blog,
                        article,
                        null,
                        false
                ).allowed();
    }

    private boolean isBlocked(Article article) {
        return blockService.isContentBlocked(
                communityAuth.getOptionalLoginUserId(),
                article.getAuthorUserId(),
                article.getBlogId(),
                "ARTICLE",
                article.getId()
        );
    }

    private Map<Long, List<PublicArticleTagView>> tagsByArticleIds(
            List<Long> articleIds
    ) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        List<ArticleTag> references = articleTagMapper.selectList(
                Wrappers.<ArticleTag>lambdaQuery()
                        .in(ArticleTag::getArticleId, distinct(articleIds))
                        .orderByAsc(
                                ArticleTag::getArticleId,
                                ArticleTag::getSortOrder,
                                ArticleTag::getTagId
                        )
        );
        if (references.isEmpty()) {
            return Map.of();
        }
        Map<Long, PlatformTag> activeTags = tagMapper.selectBatchIds(
                distinct(references.stream().map(ArticleTag::getTagId).toList())
        ).stream()
                .filter(tag -> "ACTIVE".equals(tag.getStatus()))
                .collect(Collectors.toMap(
                        PlatformTag::getId,
                        Function.identity()
                ));
        Map<Long, List<PublicArticleTagView>> result = new LinkedHashMap<>();
        for (ArticleTag reference : references) {
            PlatformTag tag = activeTags.get(reference.getTagId());
            if (tag != null) {
                result.computeIfAbsent(
                        reference.getArticleId(),
                        ignored -> new ArrayList<>()
                ).add(new PublicArticleTagView(
                        tag.getId(),
                        tag.getName(),
                        tag.getSlug()
                ));
            }
        }
        result.replaceAll((ignored, values) -> List.copyOf(values));
        return Map.copyOf(result);
    }

    private PublicArticleCategoryView category(
            Long categoryId,
            Long blogId
    ) {
        return categoryId == null
                ? null
                : category(categoryMapper.selectById(categoryId), blogId);
    }

    private static PublicArticleCategoryView category(
            BlogCategory category,
            Long blogId
    ) {
        if (category == null
                || category.getDeletedAt() != null
                || !Objects.equals(category.getBlogId(), blogId)) {
            return null;
        }
        return new PublicArticleCategoryView(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                Integer.valueOf(1).equals(category.getIsDefault())
        );
    }

    private BlogSetting findSetting(Long blogId) {
        return settingMapper.selectOne(
                Wrappers.<BlogSetting>lambdaQuery()
                        .eq(BlogSetting::getBlogId, blogId)
                        .last("LIMIT 1")
        );
    }

    private static PublicArticleAuthorView author(CommunityUser user) {
        return new PublicArticleAuthorView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarFileId()
        );
    }

    private static PublicArticleBlogView blog(
            Blog blog,
            BlogSetting setting
    ) {
        return new PublicArticleBlogView(
                blog.getId(),
                blog.getBlogType(),
                blog.getName(),
                blog.getSlug(),
                blog.getSummary(),
                blog.getAvatarFileId(),
                blog.getBackgroundFileId(),
                setting == null || setting.getThemeKey() == null
                        ? "light"
                        : setting.getThemeKey(),
                setting == null ? null : setting.getThemeConfigJson()
        );
    }

    private static PublicArticleQuery normalizeQuery(
            PublicArticleQuery query
    ) {
        int pageNum = query == null || query.pageNum() == null
                ? 1
                : query.pageNum();
        int pageSize = query == null || query.pageSize() == null
                ? 10
                : query.pageSize();
        if (pageNum < 1) {
            throw new BusinessException(400, "页码必须大于 0");
        }
        if (pageSize < 1 || pageSize > 50) {
            throw new BusinessException(400, "每页数量必须在 1-50 之间");
        }
        String categorySlug = query == null
                || query.categorySlug() == null
                || query.categorySlug().isBlank()
                ? null
                : normalizeSlug(query.categorySlug(), "分类地址");
        return new PublicArticleQuery(categorySlug, pageNum, pageSize);
    }

    private static PublicArticleQuery normalizeDiscoveryQuery(PublicDiscoveryQuery rawQuery) {
        int pageNum = rawQuery == null || rawQuery.pageNum() == null
                ? 1
                : rawQuery.pageNum();
        int pageSize = rawQuery == null || rawQuery.pageSize() == null
                ? 10
                : rawQuery.pageSize();
        if (pageNum < 1) {
            throw new BusinessException(400, "页码必须大于 0");
        }
        if (pageSize < 1 || pageSize > 50) {
            throw new BusinessException(400, "每页数量必须在 1-50 之间");
        }
        return new PublicArticleQuery(null, pageNum, pageSize);
    }

    private static String canonicalPath(Blog blog, Article article) {
        String canonical = article.getCanonicalPath();
        return canonical == null || canonical.isBlank()
                ? ArticlePublicationPolicy.canonicalPath(blog, article)
                : canonical;
    }

    private static String seoDescription(
            Article article,
            ArticleVersion version
    ) {
        String source = article.getSummary();
        if (source == null || source.isBlank()) {
            source = version.getPlainText();
        }
        if (source == null) {
            return "";
        }
        String normalized = source.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160
                ? normalized
                : normalized.substring(0, 160);
    }

    private static String normalizeSlug(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(400, label + "不能为空");
        }
        String value = Normalizer.normalize(
                raw.trim(),
                Normalizer.Form.NFKC
        ).toLowerCase(Locale.ROOT);
        if (value.length() > 160 || !SLUG_PATTERN.matcher(value).matches()) {
            throw new BusinessException(400, label + "格式无效");
        }
        return value;
    }

    private static <T> Map<Long, T> byId(
            Collection<T> values,
            Function<T, Long> id
    ) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return values.stream().collect(Collectors.toMap(
                id,
                Function.identity(),
                (first, ignored) -> first
        ));
    }

    private static List<Long> distinct(List<Long> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private record BlogAccess(Blog blog) {
    }
}
