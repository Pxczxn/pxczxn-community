package top.pxczxn.community.user.application;

import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DuplicateKeyException;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogCategory;
import top.pxczxn.community.blog.model.BlogSetting;
import top.pxczxn.community.blog.persistence.BlogCategoryMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.blog.persistence.BlogSettingMapper;
import top.pxczxn.community.social.model.FavoriteFolder;
import top.pxczxn.community.social.persistence.FavoriteFolderMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.model.CommunityUserLoginAccount;
import top.pxczxn.community.user.model.CommunityUserPreference;
import top.pxczxn.community.user.persistence.CommunityUserLoginAccountMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.community.user.persistence.CommunityUserPreferenceMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityRegistrationServiceImplTest {

    private CommunityUserMapper userMapper;
    private CommunityUserLoginAccountMapper loginAccountMapper;
    private CommunityUserPreferenceMapper preferenceMapper;
    private BlogMapper blogMapper;
    private BlogSettingMapper blogSettingMapper;
    private BlogCategoryMapper blogCategoryMapper;
    private FavoriteFolderMapper favoriteFolderMapper;
    private CommunityRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(CommunityUserMapper.class);
        loginAccountMapper = mock(CommunityUserLoginAccountMapper.class);
        preferenceMapper = mock(CommunityUserPreferenceMapper.class);
        blogMapper = mock(BlogMapper.class);
        blogSettingMapper = mock(BlogSettingMapper.class);
        blogCategoryMapper = mock(BlogCategoryMapper.class);
        favoriteFolderMapper = mock(FavoriteFolderMapper.class);
        service = new CommunityRegistrationServiceImpl(
                userMapper,
                loginAccountMapper,
                preferenceMapper,
                blogMapper,
                blogSettingMapper,
                blogCategoryMapper,
                favoriteFolderMapper
        );
    }

    @Test
    void registerCreatesCompletePersonalBlogAggregate() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(loginAccountMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenReturn(1);
        when(loginAccountMapper.insert(any())).thenReturn(1);
        when(preferenceMapper.insert(any())).thenReturn(1);
        when(blogMapper.insert(any())).thenReturn(1);
        when(blogSettingMapper.insert(any())).thenReturn(1);
        when(blogCategoryMapper.insert(any())).thenReturn(1);
        when(favoriteFolderMapper.insert(any())).thenReturn(1);
        when(userMapper.updateById(any())).thenReturn(1);

        RegisteredCommunityUser result = service.register(new RegisterCommunityUserCommand(
                "  Alice_01 ",
                " Alice@Example.COM ",
                "correct-horse-battery-staple",
                " Alice "
        ));

        assertThat(result.username()).isEqualTo("alice_01");
        assertThat(result.blogSlug()).isEqualTo("alice_01");
        assertThat(result.userId()).isPositive();
        assertThat(result.blogId()).isPositive();

        ArgumentCaptor<CommunityUser> userCaptor = ArgumentCaptor.forClass(CommunityUser.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("alice_01");
        assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("Alice");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo("NORMAL");

        ArgumentCaptor<CommunityUserLoginAccount> accountCaptor =
                ArgumentCaptor.forClass(CommunityUserLoginAccount.class);
        verify(loginAccountMapper).insert(accountCaptor.capture());
        CommunityUserLoginAccount account = accountCaptor.getValue();
        assertThat(account.getNormalizedIdentifier()).isEqualTo("alice@example.com");
        assertThat(account.getLoginType()).isEqualTo("EMAIL");
        assertThat(BCrypt.checkpw("correct-horse-battery-staple", account.getPasswordHash()))
                .isTrue();

        ArgumentCaptor<Blog> blogCaptor = ArgumentCaptor.forClass(Blog.class);
        verify(blogMapper).insert(blogCaptor.capture());
        assertThat(blogCaptor.getValue().getBlogType()).isEqualTo("PERSONAL");
        assertThat(blogCaptor.getValue().getName()).isEqualTo("Alice 的博客");
        assertThat(blogCaptor.getValue().getSlug()).isEqualTo("alice_01");

        ArgumentCaptor<BlogCategory> categoryCaptor =
                ArgumentCaptor.forClass(BlogCategory.class);
        verify(blogCategoryMapper).insert(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getName()).isEqualTo("未分类");
        assertThat(categoryCaptor.getValue().getSlug()).isEqualTo("uncategorized");
        assertThat(categoryCaptor.getValue().getIsDefault()).isEqualTo(1);

        ArgumentCaptor<FavoriteFolder> folderCaptor =
                ArgumentCaptor.forClass(FavoriteFolder.class);
        verify(favoriteFolderMapper).insert(folderCaptor.capture());
        assertThat(folderCaptor.getValue().getName()).isEqualTo("全部收藏");
        assertThat(folderCaptor.getValue().getVisibility())
                .isEqualTo("PRIVATE");
        assertThat(folderCaptor.getValue().getIsDefault()).isEqualTo(1);

        ArgumentCaptor<CommunityUser> linkCaptor =
                ArgumentCaptor.forClass(CommunityUser.class);
        verify(userMapper).updateById(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getId()).isEqualTo(result.userId());
        assertThat(linkCaptor.getValue().getPersonalBlogId()).isEqualTo(result.blogId());

        InOrder inserts = inOrder(
                userMapper,
                loginAccountMapper,
                preferenceMapper,
                favoriteFolderMapper,
                blogMapper,
                blogSettingMapper,
                blogCategoryMapper
        );
        inserts.verify(userMapper).insert(any(CommunityUser.class));
        inserts.verify(loginAccountMapper).insert(any(CommunityUserLoginAccount.class));
        inserts.verify(preferenceMapper).insert(any(CommunityUserPreference.class));
        inserts.verify(favoriteFolderMapper).insert(any(FavoriteFolder.class));
        inserts.verify(blogMapper).insert(any(Blog.class));
        inserts.verify(blogSettingMapper).insert(any(BlogSetting.class));
        inserts.verify(blogCategoryMapper).insert(any(BlogCategory.class));
    }

    @Test
    void registerRejectsExistingUsernameBeforeWriting() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.register(new RegisterCommunityUserCommand(
                "alice",
                "alice@example.com",
                "password-123",
                null
        )))
                .isInstanceOf(RegistrationConflictException.class)
                .hasMessage("用户名已被使用");

        verify(userMapper, never()).insert(any());
        verify(loginAccountMapper, never()).insert(any());
    }

    @Test
    void registerMapsConcurrentEmailConflict() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(loginAccountMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenReturn(1);
        when(loginAccountMapper.insert(any())).thenThrow(new DuplicateKeyException(
                "Duplicate entry for key 'uk_login_account_type_identifier'"
        ));

        assertThatThrownBy(() -> service.register(new RegisterCommunityUserCommand(
                "alice",
                "alice@example.com",
                "password-123",
                null
        )))
                .isInstanceOf(RegistrationConflictException.class)
                .hasMessage("邮箱已被注册");
    }

    @Test
    void registerRejectsPasswordBeyondBcryptByteLimit() {
        String password = "密".repeat(25);

        assertThatThrownBy(() -> service.register(new RegisterCommunityUserCommand(
                "alice",
                "alice@example.com",
                password,
                null
        )))
                .isInstanceOf(com.mars.common.exception.BusinessException.class)
                .hasMessage("密码 UTF-8 编码后不能超过 72 字节");

        verify(userMapper, never()).selectCount(any());
        verify(userMapper, never()).insert(any());
    }
}
