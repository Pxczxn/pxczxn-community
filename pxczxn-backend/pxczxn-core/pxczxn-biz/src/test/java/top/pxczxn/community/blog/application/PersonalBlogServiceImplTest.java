package top.pxczxn.community.blog.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogSetting;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.blog.persistence.BlogSettingMapper;
import top.pxczxn.community.file.application.CommunityFileService;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalBlogServiceImplTest {

    private BlogMapper blogMapper;
    private BlogSettingMapper settingMapper;
    private CommunityUserMapper userMapper;
    private CommunityAuth communityAuth;
    private CommunityFileService fileService;
    private ArticleMapper articleMapper;
    private PersonalBlogServiceImpl service;

    @BeforeEach
    void setUp() {
        blogMapper = mock(BlogMapper.class);
        settingMapper = mock(BlogSettingMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        communityAuth = mock(CommunityAuth.class);
        fileService = mock(CommunityFileService.class);
        articleMapper = mock(ArticleMapper.class);
        service = new PersonalBlogServiceImpl(
                blogMapper,
                settingMapper,
                userMapper,
                communityAuth,
                fileService,
                articleMapper
        );
    }

    @Test
    void ownerCanReadPersonalBlogWithSettings() {
        arrangeOwnedBlog();

        PersonalBlogProfile profile = service.getMine();

        assertThat(profile.blogId()).isEqualTo(200L);
        assertThat(profile.slug()).isEqualTo("alice");
        assertThat(profile.settings().themeKey()).isEqualTo("light");
    }

    @Test
    void forgedPersonalBlogRelationshipIsRejectedBeforeUpdate() {
        when(communityAuth.getLoginUserId()).thenReturn(100L);
        CommunityUser user = user(100L, 200L, "NORMAL");
        when(userMapper.selectById(100L)).thenReturn(user);
        Blog blog = blog(200L, 999L, "PERSONAL", "ACTIVE");
        when(blogMapper.selectById(200L)).thenReturn(blog);

        assertThatThrownBy(() -> service.updateMine(new UpdatePersonalBlogCommand(
                "New name", null, null, null, false, null, false
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权访问该博客");

        verify(blogMapper, never()).update(any(), any());
    }

    @Test
    void settingsOnlyAcceptThreeProductThemes() {
        arrangeOwnedBlog();

        assertThatThrownBy(() -> service.updateMySettings(new UpdateBlogSettingsCommand(
                null, null, null, "neon", null, null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("light、dark 或 starry");
    }

    @Test
    void profileEditCannotChangeStablePublicSlug() {
        arrangeOwnedBlog();

        assertThatThrownBy(() -> service.updateMine(new UpdatePersonalBlogCommand(
                null, "alice-new", null, null, false, null, false
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("博客地址不可在资料编辑中修改");

        verify(blogMapper, never()).update(any(), any());
    }

    @Test
    void publicLookupHidesInactiveBlogAndOwner() {
        when(blogMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.getPublicBySlug("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("博客不存在");

        verify(userMapper, never()).selectById(any());
    }

    @Test
    void publicLookupReturnsOnlyPublicProfileFields() {
        Blog blog = blog(200L, 100L, "PERSONAL", "ACTIVE");
        blog.setSlug("alice_writer");
        blog.setArticleCount(12L);
        blog.setFollowerCount(36L);
        when(blogMapper.selectOne(any())).thenReturn(blog);
        CommunityUser owner = user(100L, 200L, "NORMAL");
        owner.setUsername("alice");
        owner.setDisplayName("Alice");
        owner.setBio("写作者");
        when(userMapper.selectById(100L)).thenReturn(owner);
        when(articleMapper.countPublicByBlog(200L)).thenReturn(12L);
        BlogSetting setting = new BlogSetting();
        setting.setBlogId(200L);
        setting.setThemeKey("starry");
        setting.setThemeConfigJson("{\"accent\":\"blue\"}");
        setting.setSeoTitle("Alice 的写作空间");
        setting.setSeoDescription("文章与思考");
        when(settingMapper.selectOne(any())).thenReturn(setting);

        PublicBlogProfile profile = service.getPublicBySlug("alice_writer");

        assertThat(profile.ownerUsername()).isEqualTo("alice");
        assertThat(profile.articleCount()).isEqualTo(12);
        assertThat(profile.followerCount()).isEqualTo(36);
        assertThat(profile.themeKey()).isEqualTo("starry");
        assertThat(profile.seoTitle()).isEqualTo("Alice 的写作空间");
        assertThat(profile.seoDescription()).isEqualTo("文章与思考");
    }

    private void arrangeOwnedBlog() {
        when(communityAuth.getLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L)).thenReturn(user(100L, 200L, "NORMAL"));
        when(blogMapper.selectById(200L)).thenReturn(
                blog(200L, 100L, "PERSONAL", "ACTIVE")
        );
        BlogSetting setting = new BlogSetting();
        setting.setId(300L);
        setting.setBlogId(200L);
        setting.setCommentScope("ALL_LOGGED_IN");
        setting.setDefaultVisibility("PUBLIC");
        setting.setAllowRepost("ALLOW");
        setting.setThemeKey("light");
        when(settingMapper.selectOne(any())).thenReturn(setting);
    }

    private static CommunityUser user(
            Long userId,
            Long personalBlogId,
            String status
    ) {
        CommunityUser user = new CommunityUser();
        user.setId(userId);
        user.setPersonalBlogId(personalBlogId);
        user.setStatus(status);
        return user;
    }

    private static Blog blog(
            Long blogId,
            Long ownerId,
            String type,
            String status
    ) {
        Blog blog = new Blog();
        blog.setId(blogId);
        blog.setOwnerUserId(ownerId);
        blog.setBlogType(type);
        blog.setName("Alice 的博客");
        blog.setSlug("alice");
        blog.setStatus(status);
        return blog;
    }
}
