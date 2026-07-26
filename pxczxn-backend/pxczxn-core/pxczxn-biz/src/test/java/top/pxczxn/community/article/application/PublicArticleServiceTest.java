package top.pxczxn.community.article.application;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mars.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticlePermissionDecision;
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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicArticleServiceTest {

    private ArticleMapper articleMapper;
    private ArticleVersionMapper versionMapper;
    private BlogMapper blogMapper;
    private BlogSettingMapper settingMapper;
    private BlogCategoryMapper categoryMapper;
    private ArticleTagMapper articleTagMapper;
    private PlatformTagMapper tagMapper;
    private CommunityUserMapper userMapper;
    private ArticlePermissionService permissionService;
    private PublicArticleService service;
    private Article article;
    private ArticleVersion publishedVersion;
    private Blog blog;
    private BlogCategory category;
    private CommunityUser author;

    @BeforeEach
    void setUp() {
        articleMapper = mock(ArticleMapper.class);
        versionMapper = mock(ArticleVersionMapper.class);
        blogMapper = mock(BlogMapper.class);
        settingMapper = mock(BlogSettingMapper.class);
        categoryMapper = mock(BlogCategoryMapper.class);
        articleTagMapper = mock(ArticleTagMapper.class);
        tagMapper = mock(PlatformTagMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        permissionService = mock(ArticlePermissionService.class);
        service = new PublicArticleService(
                articleMapper,
                versionMapper,
                blogMapper,
                settingMapper,
                categoryMapper,
                articleTagMapper,
                tagMapper,
                userMapper,
                permissionService
        );

        blog = new Blog();
        blog.setId(200L);
        blog.setBlogType("PERSONAL");
        blog.setOwnerUserId(100L);
        blog.setName("Alice 的博客");
        blog.setSlug("alice");
        blog.setSummary("博客简介");
        blog.setStatus("ACTIVE");

        author = new CommunityUser();
        author.setId(100L);
        author.setUsername("alice");
        author.setDisplayName("Alice");
        author.setBio("写作者");
        author.setStatus("NORMAL");

        category = new BlogCategory();
        category.setId(400L);
        category.setBlogId(200L);
        category.setName("技术");
        category.setSlug("tech");
        category.setDescription("技术文章");
        category.setIsDefault(0);
        category.setSortOrder(1);

        article = new Article();
        article.setId(300L);
        article.setBlogId(200L);
        article.setAuthorUserId(100L);
        article.setCategoryId(400L);
        article.setTitle("公开版本");
        article.setSlug("public-version");
        article.setSummary("文章摘要");
        article.setContentMode("MARKDOWN");
        article.setVisibility("PUBLIC");
        article.setPublishStatus("PUBLISHED");
        article.setReviewStatus("APPROVED");
        article.setCurrentVersionId(501L);
        article.setPublishedVersionId(500L);
        article.setCanonicalPath("/alice/300/public-version");
        article.setPublishedAt(LocalDateTime.now().minusDays(1));
        article.setUpdatedAt(LocalDateTime.now());
        article.setViewCount(12L);
        article.setLikeCount(3L);
        article.setFavoriteCount(2L);
        article.setCommentCount(1L);

        publishedVersion = new ArticleVersion();
        publishedVersion.setId(500L);
        publishedVersion.setArticleId(300L);
        publishedVersion.setContentMode("MARKDOWN");
        publishedVersion.setMarkdownContent("# private source");
        publishedVersion.setRenderedHtml("<h1>safe published</h1>");
        publishedVersion.setPlainText("safe published");
        publishedVersion.setTocJson("[]");
        publishedVersion.setWordCount(2);
        publishedVersion.setReadingTimeMinutes(1);

        BlogSetting setting = new BlogSetting();
        setting.setBlogId(200L);
        setting.setThemeKey("starry");
        setting.setThemeConfigJson("{\"accent\":\"blue\"}");

        when(permissionService.requirePublicArticle(
                300L,
                ArticleAction.VIEW_DETAIL
        )).thenReturn(new ArticlePublicAccess(
                null,
                author,
                blog,
                article,
                null
        ));
        when(versionMapper.selectById(500L)).thenReturn(publishedVersion);
        when(categoryMapper.selectById(400L)).thenReturn(category);
        when(settingMapper.selectOne(any())).thenReturn(setting);
        when(articleTagMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void detailReadsOnlyPublishedVersionAndBuildsSeo() {
        PublicArticleDetailView result = service.detail(300L);

        assertThat(result.renderedHtml())
                .isEqualTo("<h1>safe published</h1>");
        assertThat(result.canonicalPath())
                .isEqualTo("/alice/300/public-version");
        assertThat(result.blog().themeKey()).isEqualTo("starry");
        assertThat(result.seo().title())
                .isEqualTo("公开版本 - Alice 的博客");
        verify(versionMapper).selectById(500L);
        verify(versionMapper, never()).selectById(501L);
    }

    @Test
    void missingPublishedSnapshotIsHiddenAsNotFound() {
        when(versionMapper.selectById(500L)).thenReturn(null);

        assertThatThrownBy(() -> service.detail(300L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(404);
    }

    @Test
    void permissionDenialStopsBeforeReadingContent() {
        when(permissionService.requirePublicArticle(
                300L,
                ArticleAction.VIEW_DETAIL
        )).thenThrow(new BusinessException(404, "文章不存在"));

        assertThatThrownBy(() -> service.detail(300L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(404);
        verify(versionMapper, never()).selectById(any());
    }

    @Test
    void blogPageFiltersByOwnedCategoryAndHydratesPublishedSnapshot() {
        arrangePublicBlog();
        when(categoryMapper.selectOne(any())).thenReturn(category);
        Page<Article> page = new Page<>(1, 10);
        page.setRecords(List.of(article));
        page.setTotal(1);
        when(articleMapper.selectPublicPage(
                any(IPage.class),
                eq(200L),
                eq(400L)
        )).thenReturn(page);
        when(userMapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(author));
        when(versionMapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(publishedVersion));
        when(categoryMapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(category));
        when(permissionService.decidePublic(
                eq(ArticleAction.LIST_PUBLIC),
                eq(null),
                eq(author),
                eq(blog),
                eq(article),
                eq(null),
                eq(false)
        )).thenReturn(ArticlePermissionDecision.grant());

        PublicArticlePageView result = service.page(
                "alice",
                new PublicArticleQuery("tech", 1, 10)
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().getFirst().articleId()).isEqualTo(300L);
        assertThat(result.records().getFirst().wordCount()).isEqualTo(2);
        assertThat(result.records().getFirst().category().slug())
                .isEqualTo("tech");
    }

    @Test
    void invalidPageSizeStopsBeforeBlogLookup() {
        assertThatThrownBy(() -> service.page(
                "alice",
                new PublicArticleQuery(null, 1, 51)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
        verify(blogMapper, never()).selectOne(any());
    }

    @Test
    void categoriesExposeOnlyActiveBlogCategories() {
        arrangePublicBlog();
        when(categoryMapper.selectList(any())).thenReturn(List.of(category));

        List<PublicArticleCategoryView> result =
                service.categories("alice");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().categoryId()).isEqualTo(400L);
        assertThat(result.getFirst().slug()).isEqualTo("tech");
    }

    @Test
    void inactiveBlogIsHiddenFromLists() {
        when(blogMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.page(
                "alice",
                new PublicArticleQuery(null, 1, 10)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(404);
        verify(articleMapper, never()).selectPublicPage(
                any(),
                any(),
                any()
        );
    }

    @Test
    void activeTagOrderIsPreservedAndHiddenTagIsExcluded() {
        ArticleTag first = reference(300L, 700L, 0);
        ArticleTag second = reference(300L, 701L, 1);
        when(articleTagMapper.selectList(any()))
                .thenReturn(List.of(first, second));
        PlatformTag active = tag(700L, "Java", "java", "ACTIVE");
        PlatformTag hidden = tag(701L, "Hidden", "hidden", "HIDDEN");
        when(tagMapper.selectBatchIds(any(Collection.class)))
                .thenReturn(List.of(hidden, active));

        PublicArticleDetailView result = service.detail(300L);

        assertThat(result.tags()).extracting(PublicArticleTagView::slug)
                .containsExactly("java");
    }

    private void arrangePublicBlog() {
        when(blogMapper.selectOne(any())).thenReturn(blog);
        when(userMapper.selectById(100L)).thenReturn(author);
    }

    private static ArticleTag reference(
            Long articleId,
            Long tagId,
            int sortOrder
    ) {
        ArticleTag reference = new ArticleTag();
        reference.setArticleId(articleId);
        reference.setTagId(tagId);
        reference.setSortOrder(sortOrder);
        return reference;
    }

    private static PlatformTag tag(
            Long id,
            String name,
            String slug,
            String status
    ) {
        PlatformTag tag = new PlatformTag();
        tag.setId(id);
        tag.setName(name);
        tag.setSlug(slug);
        tag.setStatus(status);
        return tag;
    }
}
