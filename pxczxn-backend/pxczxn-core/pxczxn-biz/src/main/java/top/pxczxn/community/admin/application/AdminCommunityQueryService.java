package top.pxczxn.community.admin.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mars.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogCategory;
import top.pxczxn.community.blog.persistence.BlogCategoryMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.moderation.model.ContentReviewTask;
import top.pxczxn.community.moderation.persistence.ContentReviewTaskMapper;
import top.pxczxn.community.taxonomy.model.ArticleTag;
import top.pxczxn.community.taxonomy.model.PlatformTag;
import top.pxczxn.community.taxonomy.persistence.ArticleTagMapper;
import top.pxczxn.community.taxonomy.persistence.PlatformTagMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.model.CommunityUserLoginAccount;
import top.pxczxn.community.user.persistence.CommunityUserLoginAccountMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminCommunityQueryService {

    private final CommunityUserMapper userMapper;
    private final CommunityUserLoginAccountMapper loginAccountMapper;
    private final BlogMapper blogMapper;
    private final ArticleMapper articleMapper;
    private final ArticleVersionMapper versionMapper;
    private final BlogCategoryMapper categoryMapper;
    private final ArticleTagMapper articleTagMapper;
    private final PlatformTagMapper tagMapper;
    private final ContentReviewTaskMapper reviewTaskMapper;

    @Transactional(readOnly = true)
    public AdminCommunityDashboardView dashboard() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDateTime from = today.minusDays(6).atStartOfDay();
        Map<LocalDate, long[]> metrics = new LinkedHashMap<>();
        for (int offset = 6; offset >= 0; offset--) {
            metrics.put(today.minusDays(offset), new long[2]);
        }
        mergeMetric(
                metrics,
                userMapper.selectDailyRegistrations(from),
                0
        );
        mergeMetric(
                metrics,
                articleMapper.selectDailyCreations(from),
                1
        );

        return new AdminCommunityDashboardView(
                userMapper.selectCount(null),
                userMapper.selectCount(
                        Wrappers.<CommunityUser>lambdaQuery()
                                .eq(CommunityUser::getStatus, "NORMAL")
                ),
                blogMapper.selectCount(
                        Wrappers.<Blog>lambdaQuery()
                                .isNull(Blog::getDeletedAt)
                ),
                articleMapper.selectCount(
                        Wrappers.<Article>lambdaQuery()
                                .isNull(Article::getDeletedAt)
                ),
                articleMapper.selectCount(
                        Wrappers.<Article>lambdaQuery()
                                .isNotNull(Article::getPublishedVersionId)
                                .notIn(
                                        Article::getPublishStatus,
                                        "HIDDEN",
                                        "TAKEN_DOWN",
                                        "DELETED"
                                )
                                .isNull(Article::getDeletedAt)
                ),
                articleMapper.selectCount(
                        Wrappers.<Article>lambdaQuery()
                                .eq(
                                        Article::getPublishStatus,
                                        "SCHEDULED"
                                )
                                .isNull(Article::getDeletedAt)
                ),
                articleMapper.selectCount(
                        Wrappers.<Article>lambdaQuery()
                                .eq(
                                        Article::getPublishStatus,
                                        "PUBLISH_FAILED"
                                )
                                .isNull(Article::getDeletedAt)
                ),
                reviewTaskMapper.selectCount(
                        Wrappers.<ContentReviewTask>lambdaQuery()
                                .eq(
                                        ContentReviewTask::getSubjectType,
                                        "ARTICLE"
                                )
                                .in(
                                        ContentReviewTask::getStatus,
                                        "QUEUED",
                                        "AUTO_REVIEWING",
                                        "MANUAL_REVIEWING"
                                )
                ),
                metrics.entrySet().stream()
                        .map(entry -> new AdminCommunityDailyMetricView(
                                entry.getKey(),
                                entry.getValue()[0],
                                entry.getValue()[1]
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public AdminCommunityPageView<AdminCommunityUserView> users(
            String keyword,
            String status,
            String verificationStatus,
            Integer pageNum,
            Integer pageSize
    ) {
        Page<CommunityUser> page = new Page<>(
                page(pageNum),
                size(pageSize)
        );
        var query = Wrappers.<CommunityUser>lambdaQuery()
                .and(
                        StringUtils.hasText(keyword),
                        nested -> nested
                                .like(
                                        CommunityUser::getUsername,
                                        keyword.trim()
                                )
                                .or()
                                .like(
                                        CommunityUser::getDisplayName,
                                        keyword.trim()
                                )
                )
                .eq(
                        StringUtils.hasText(status),
                        CommunityUser::getStatus,
                        normalize(status)
                )
                .eq(
                        StringUtils.hasText(verificationStatus),
                        CommunityUser::getVerificationStatus,
                        normalize(verificationStatus)
                )
                .orderByDesc(CommunityUser::getCreatedAt)
                .orderByDesc(CommunityUser::getId);
        Page<CommunityUser> result = userMapper.selectPage(page, query);
        return new AdminCommunityPageView<>(
                result.getRecords().stream().map(this::userView).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminCommunityPageView<AdminCommunityBlogView> blogs(
            String keyword,
            String blogType,
            String status,
            Integer pageNum,
            Integer pageSize
    ) {
        Page<Blog> page = new Page<>(page(pageNum), size(pageSize));
        var query = Wrappers.<Blog>lambdaQuery()
                .and(
                        StringUtils.hasText(keyword),
                        nested -> nested
                                .like(Blog::getName, keyword.trim())
                                .or()
                                .like(Blog::getSlug, keyword.trim())
                )
                .eq(
                        StringUtils.hasText(blogType),
                        Blog::getBlogType,
                        normalize(blogType)
                )
                .eq(
                        StringUtils.hasText(status),
                        Blog::getStatus,
                        normalize(status)
                )
                .isNull(Blog::getDeletedAt)
                .orderByDesc(Blog::getCreatedAt)
                .orderByDesc(Blog::getId);
        Page<Blog> result = blogMapper.selectPage(page, query);
        return new AdminCommunityPageView<>(
                result.getRecords().stream().map(this::blogView).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminCommunityPageView<AdminCommunityArticleView> articles(
            String keyword,
            String publishStatus,
            String reviewStatus,
            String visibility,
            Long blogId,
            Integer pageNum,
            Integer pageSize
    ) {
        Page<Article> page = new Page<>(page(pageNum), size(pageSize));
        var query = Wrappers.<Article>lambdaQuery()
                .and(
                        StringUtils.hasText(keyword),
                        nested -> nested
                                .like(Article::getTitle, keyword.trim())
                                .or()
                                .like(Article::getSlug, keyword.trim())
                )
                .eq(
                        StringUtils.hasText(publishStatus),
                        Article::getPublishStatus,
                        normalize(publishStatus)
                )
                .eq(
                        StringUtils.hasText(reviewStatus),
                        Article::getReviewStatus,
                        normalize(reviewStatus)
                )
                .eq(
                        StringUtils.hasText(visibility),
                        Article::getVisibility,
                        normalize(visibility)
                )
                .eq(blogId != null && blogId > 0, Article::getBlogId, blogId)
                .isNull(Article::getDeletedAt)
                .orderByDesc(Article::getUpdatedAt)
                .orderByDesc(Article::getId);
        Page<Article> result = articleMapper.selectPage(page, query);
        return new AdminCommunityPageView<>(
                result.getRecords().stream()
                        .map(article -> articleView(article, false))
                        .toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminCommunityArticleView article(Long articleId) {
        if (articleId == null || articleId <= 0) {
            throw new BusinessException(400, "文章 ID 无效");
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getDeletedAt() != null) {
            throw new BusinessException(404, "文章不存在");
        }
        return articleView(article, true);
    }

    private AdminCommunityUserView userView(CommunityUser user) {
        Blog blog = user.getPersonalBlogId() == null
                ? null
                : blogMapper.selectById(user.getPersonalBlogId());
        CommunityUserLoginAccount account =
                loginAccountMapper.selectOne(
                        Wrappers.<CommunityUserLoginAccount>lambdaQuery()
                                .eq(
                                        CommunityUserLoginAccount::getUserId,
                                        user.getId()
                                )
                                .eq(
                                        CommunityUserLoginAccount::getLoginType,
                                        "EMAIL"
                                )
                                .last("LIMIT 1")
                );
        return new AdminCommunityUserView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                account == null ? null : account.getNormalizedIdentifier(),
                user.getStatus(),
                user.getVerificationStatus(),
                user.getPersonalBlogId(),
                blog == null ? null : blog.getName(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    private AdminCommunityBlogView blogView(Blog blog) {
        CommunityUser owner = blog.getOwnerUserId() == null
                ? null
                : userMapper.selectById(blog.getOwnerUserId());
        return new AdminCommunityBlogView(
                blog.getId(),
                blog.getBlogType(),
                blog.getOwnerUserId(),
                owner == null ? null : owner.getUsername(),
                blog.getName(),
                blog.getSlug(),
                blog.getSummary(),
                blog.getStatus(),
                safeLong(blog.getArticleCount()),
                safeLong(blog.getFollowerCount()),
                blog.getCreatedAt(),
                blog.getUpdatedAt()
        );
    }

    private AdminCommunityArticleView articleView(
            Article article,
            boolean includeContent
    ) {
        Blog blog = blogMapper.selectById(article.getBlogId());
        CommunityUser author =
                userMapper.selectById(article.getAuthorUserId());
        BlogCategory category = article.getCategoryId() == null
                ? null
                : categoryMapper.selectById(article.getCategoryId());
        ArticleVersion current = includeContent
                && article.getCurrentVersionId() != null
                ? versionMapper.selectById(article.getCurrentVersionId())
                : null;
        List<Long> tagIds = articleTagMapper.selectList(
                        Wrappers.<ArticleTag>lambdaQuery()
                                .eq(ArticleTag::getArticleId, article.getId())
                                .orderByAsc(ArticleTag::getSortOrder)
                ).stream()
                .map(ArticleTag::getTagId)
                .toList();
        Map<Long, PlatformTag> tags = tagIds.isEmpty()
                ? Map.of()
                : tagMapper.selectBatchIds(tagIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                PlatformTag::getId,
                                tag -> tag
                        ));
        return new AdminCommunityArticleView(
                article.getId(),
                article.getBlogId(),
                blog == null ? null : blog.getName(),
                blog == null ? null : blog.getSlug(),
                article.getAuthorUserId(),
                author == null ? null : author.getUsername(),
                article.getCategoryId(),
                category == null ? null : category.getName(),
                article.getTitle(),
                article.getSlug(),
                article.getSummary(),
                article.getContentMode(),
                article.getVisibility(),
                article.getPublishMethod(),
                article.getPublishStatus(),
                article.getReviewStatus(),
                article.getCurrentVersionId(),
                article.getPublishedVersionId(),
                article.getReviewVersionId(),
                current == null ? null : current.getVersionNo(),
                current == null ? null : current.getRenderedHtml(),
                current == null ? null : current.getTocJson(),
                current == null ? null : current.getWordCount(),
                current == null ? null : current.getReadingTimeMinutes(),
                tagIds.stream()
                        .map(tags::get)
                        .filter(Objects::nonNull)
                        .map(PlatformTag::getName)
                        .toList(),
                article.getScheduledPublishAt(),
                article.getPublishedAt(),
                article.getCanonicalPath(),
                safeLong(article.getViewCount()),
                safeLong(article.getLikeCount()),
                safeLong(article.getFavoriteCount()),
                safeLong(article.getCommentCount()),
                safeInt(article.getLockVersion()),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    private static void mergeMetric(
            Map<LocalDate, long[]> metrics,
            List<Map<String, Object>> rows,
            int index
    ) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> row : rows) {
            Object dayValue = value(row, "day");
            Object countValue = value(row, "count");
            if (dayValue == null || countValue == null) {
                continue;
            }
            LocalDate day = LocalDate.parse(dayValue.toString());
            long[] values = metrics.get(day);
            if (values != null) {
                values[index] = ((Number) countValue).longValue();
            }
        }
    }

    private static Object value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? row.get(key.toUpperCase()) : value;
    }

    private static long page(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private static long size(Integer value) {
        return value == null ? 20 : Math.max(1, Math.min(value, 100));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
