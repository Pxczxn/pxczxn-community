package top.pxczxn.community.article.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticleAuthoringContext;
import top.pxczxn.community.article.permission.ArticleCommunityAccess;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.permission.BlogArticleRole;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogCategoryMapper;
import top.pxczxn.community.file.application.CommunityFileService;
import top.pxczxn.community.file.persistence.CommunityFileReferenceMapper;
import top.pxczxn.community.taxonomy.application.PlatformTagService;
import top.pxczxn.community.taxonomy.persistence.ArticleTagMapper;
import top.pxczxn.community.user.model.CommunityUser;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleDraftServiceTest {

    private ArticleMapper articleMapper;
    private ArticleVersionMapper versionMapper;
    private ArticleTagMapper articleTagMapper;
    private CommunityFileReferenceMapper fileReferenceMapper;
    private ArticleContentProcessor contentProcessor;
    private ArticlePermissionService permissionService;
    private ArticleDraftService service;
    private Article article;

    @BeforeEach
    void setUp() {
        articleMapper = mock(ArticleMapper.class);
        versionMapper = mock(ArticleVersionMapper.class);
        BlogCategoryMapper categoryMapper = mock(BlogCategoryMapper.class);
        articleTagMapper = mock(ArticleTagMapper.class);
        fileReferenceMapper = mock(CommunityFileReferenceMapper.class);
        PlatformTagService tagService = mock(PlatformTagService.class);
        CommunityFileService fileService = mock(CommunityFileService.class);
        contentProcessor = mock(ArticleContentProcessor.class);
        permissionService = mock(ArticlePermissionService.class);
        service = new ArticleDraftService(
                articleMapper,
                versionMapper,
                categoryMapper,
                articleTagMapper,
                fileReferenceMapper,
                tagService,
                fileService,
                contentProcessor,
                permissionService
        );

        CommunityUser user = new CommunityUser();
        user.setId(100L);
        user.setStatus("NORMAL");
        user.setPersonalBlogId(200L);
        Blog blog = new Blog();
        blog.setId(200L);
        blog.setOwnerUserId(100L);
        blog.setBlogType("PERSONAL");
        blog.setStatus("ACTIVE");

        article = new Article();
        article.setId(300L);
        article.setBlogId(200L);
        article.setAuthorUserId(100L);
        article.setCategoryId(400L);
        article.setTitle("标题");
        article.setSlug("article");
        article.setContentMode("MARKDOWN");
        article.setVisibility("PUBLIC");
        article.setPublishMethod("MANUAL");
        article.setPublishStatus("DRAFT");
        article.setReviewStatus("NOT_SUBMITTED");
        article.setCurrentVersionId(500L);
        article.setLockVersion(2);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        ArticleCommunityAccess access =
                new ArticleCommunityAccess(user, blog, article, BlogArticleRole.OWNER);
        when(permissionService.requirePersonalAuthoringContext())
                .thenReturn(new ArticleAuthoringContext(user, blog));
        when(permissionService.requireCommunityArticle(any(), any()))
                .thenReturn(access);
    }

    @Test
    void staleLockIsRejectedBeforeCreatingVersion() {
        assertThatThrownBy(() -> service.save(
                300L,
                saveCommand(1),
                false
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("其他窗口");

        verify(versionMapper, never()).insert(any());
    }

    @Test
    void pendingReviewCannotBeEdited() {
        article.setPublishStatus("PENDING_REVIEW");
        when(permissionService.requireCommunityArticle(300L, ArticleAction.EDIT))
                .thenThrow(new BusinessException(
                        409,
                        "文章当前状态不能编辑，请先撤回审核"
                ));

        assertThatThrownBy(() -> service.save(
                300L,
                saveCommand(2),
                false
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能编辑");

        verify(versionMapper, never()).insert(any());
    }

    @Test
    void unchangedAutosaveDoesNotCreateAnotherVersion() {
        ArticleVersion current = currentVersion();
        when(versionMapper.selectOne(any())).thenReturn(current);
        when(articleTagMapper.selectList(any())).thenReturn(List.of());
        when(fileReferenceMapper.selectList(any())).thenReturn(List.of());
        when(contentProcessor.process("MARKDOWN", null, "# 内容"))
                .thenReturn(new RenderedArticleContent(
                        "MARKDOWN",
                        null,
                        "# 内容",
                        "<h1 id=\"内容\">内容</h1>",
                        "内容",
                        "[]",
                        "same-hash",
                        2,
                        1
                ));

        ArticleEditorView view = service.save(
                300L,
                new SaveArticleCommand(
                        null, null, null, null, null, false,
                        null, null, null, null, null,
                        null, null, 2
                ),
                true
        );

        assertThat(view.currentVersionId()).isEqualTo(500L);
        assertThat(view.lockVersion()).isEqualTo(2);
        verify(versionMapper, never()).insert(any());
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void deleteUsesOptimisticLock() {
        when(articleMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.delete(300L, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("其他窗口");
    }

    private SaveArticleCommand saveCommand(int expectedLockVersion) {
        return new SaveArticleCommand(
                null, null, null, null, null, false,
                null, null, null, null, null,
                null, null, expectedLockVersion
        );
    }

    private ArticleVersion currentVersion() {
        ArticleVersion version = new ArticleVersion();
        version.setId(500L);
        version.setArticleId(300L);
        version.setVersionNo(1);
        version.setContentMode("MARKDOWN");
        version.setMarkdownContent("# 内容");
        version.setRenderedHtml("<h1 id=\"内容\">内容</h1>");
        version.setPlainText("内容");
        version.setTocJson("[]");
        version.setContentHash("same-hash");
        version.setWordCount(2);
        version.setReadingTimeMinutes(1);
        version.setCreatedByUserId(100L);
        version.setCreationType("CREATE");
        version.setCreatedAt(LocalDateTime.now());
        return version;
    }
}
