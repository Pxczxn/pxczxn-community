package top.pxczxn.community.blog.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogCategory;
import top.pxczxn.community.blog.persistence.BlogCategoryMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogCategoryServiceTest {

    private BlogCategoryMapper categoryMapper;
    private BlogMapper blogMapper;
    private ArticleMapper articleMapper;
    private CommunityUserMapper userMapper;
    private CommunityAuth communityAuth;
    private BlogCategoryService service;

    @BeforeEach
    void setUp() {
        categoryMapper = mock(BlogCategoryMapper.class);
        blogMapper = mock(BlogMapper.class);
        articleMapper = mock(ArticleMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        communityAuth = mock(CommunityAuth.class);
        service = new BlogCategoryService(
                categoryMapper,
                blogMapper,
                articleMapper,
                userMapper,
                communityAuth
        );
        arrangeOwnedBlog();
    }

    @Test
    void defaultCategoryCannotBeDeleted() {
        BlogCategory defaultCategory = category(300L, true);
        when(categoryMapper.selectOne(any())).thenReturn(defaultCategory);

        assertThatThrownBy(() -> service.delete(300L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("默认分类不可删除");

        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void deletingCategoryMigratesArticlesToDefault() {
        BlogCategory category = category(301L, false);
        category.setArticleCount(2L);
        BlogCategory defaultCategory = category(300L, true);
        when(categoryMapper.selectOne(any())).thenReturn(category, defaultCategory);
        when(categoryMapper.update(any(), any())).thenReturn(1);

        service.delete(301L);

        verify(articleMapper).update(any(), any());
        verify(categoryMapper, org.mockito.Mockito.times(2)).update(any(), any());
    }

    @Test
    void categorySlugMustBeUrlSafe() {
        when(categoryMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(new CreateBlogCategoryCommand(
                "Java", "Java / 后端", null, 0
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类地址");
    }

    private void arrangeOwnedBlog() {
        when(communityAuth.getLoginUserId()).thenReturn(100L);
        CommunityUser user = new CommunityUser();
        user.setId(100L);
        user.setPersonalBlogId(200L);
        when(userMapper.selectById(100L)).thenReturn(user);
        Blog blog = new Blog();
        blog.setId(200L);
        blog.setOwnerUserId(100L);
        blog.setBlogType("PERSONAL");
        blog.setStatus("ACTIVE");
        when(blogMapper.selectById(200L)).thenReturn(blog);
        when(categoryMapper.selectList(any())).thenReturn(List.of());
    }

    private static BlogCategory category(Long id, boolean isDefault) {
        BlogCategory category = new BlogCategory();
        category.setId(id);
        category.setBlogId(200L);
        category.setName(isDefault ? "未分类" : "Java");
        category.setSlug(isDefault ? "uncategorized" : "java");
        category.setIsDefault(isDefault ? 1 : 0);
        category.setArticleCount(0L);
        return category;
    }
}
