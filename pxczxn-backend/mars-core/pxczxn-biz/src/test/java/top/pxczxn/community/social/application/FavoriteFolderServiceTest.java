package top.pxczxn.community.social.application;

import com.mars.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.model.FavoriteFolder;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.social.persistence.FavoriteFolderItemMapper;
import top.pxczxn.community.social.persistence.FavoriteFolderMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FavoriteFolderServiceTest {

    private FavoriteFolderMapper folderMapper;
    private FavoriteFolderItemMapper folderItemMapper;
    private CommunityUserMapper userMapper;
    private BlogMapper blogMapper;
    private CommunityFollowMapper followMapper;
    private CommunityAuth communityAuth;
    private FavoriteFolderService service;

    @BeforeEach
    void setUp() {
        folderMapper = mock(FavoriteFolderMapper.class);
        folderItemMapper = mock(FavoriteFolderItemMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        blogMapper = mock(BlogMapper.class);
        followMapper = mock(CommunityFollowMapper.class);
        communityAuth = mock(CommunityAuth.class);
        service = new FavoriteFolderService(
                folderMapper,
                folderItemMapper,
                userMapper,
                blogMapper,
                followMapper,
                communityAuth
        );
        when(communityAuth.getLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L))
                .thenReturn(user(100L, 200L));
    }

    @Test
    void mineCreatesMissingDefaultFolder() {
        when(folderMapper.findDefault(100L)).thenReturn(null);
        when(folderMapper.insert(any())).thenReturn(1);
        when(folderMapper.selectList(any())).thenReturn(List.of());

        List<FavoriteFolderView> result = service.mine();

        assertThat(result).isEmpty();
        verify(folderMapper).insert(any(FavoriteFolder.class));
    }

    @Test
    void createsPrivateCustomFolderByDefault() {
        when(folderMapper.findDefault(100L))
                .thenReturn(folder(1L, 100L, true, "PRIVATE"));
        when(folderMapper.countCustomFolders(100L)).thenReturn(2L);
        when(folderMapper.insert(any())).thenReturn(1);

        FavoriteFolderView result = service.create(
                new CreateFavoriteFolderCommand(
                        "  设计灵感  ", "参考资料", null, 20
                )
        );

        assertThat(result.name()).isEqualTo("设计灵感");
        assertThat(result.visibility()).isEqualTo("PRIVATE");
        assertThat(result.defaultFolder()).isFalse();
    }

    @Test
    void defaultFolderCannotBeRenamedOrDeleted() {
        FavoriteFolder defaultFolder =
                folder(1L, 100L, true, "PRIVATE");
        when(folderMapper.selectById(1L)).thenReturn(defaultFolder);

        assertThatThrownBy(() -> service.update(
                1L,
                new UpdateFavoriteFolderCommand(
                        "其他名称", null, false, null, null
                )
        )).isInstanceOf(BusinessException.class)
                .hasMessage("默认收藏夹不能重命名");

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("默认收藏夹不能删除");
        verify(folderItemMapper, never()).deleteByFolder(1L);
    }

    @Test
    void privateFoldersAreNotExposedToOtherUsers() {
        FavoriteFolder privateFolder =
                folder(1L, 100L, true, "PRIVATE");
        when(userMapper.selectById(100L))
                .thenReturn(user(100L, 200L));
        when(communityAuth.getOptionalLoginUserId()).thenReturn(101L);
        when(folderMapper.selectList(any()))
                .thenReturn(List.of(privateFolder));
        when(userMapper.selectById(101L))
                .thenReturn(user(101L, 201L));
        when(blogMapper.selectById(200L))
                .thenReturn(blog(200L, 100L));

        List<FavoriteFolderView> result = service.visibleFolders(100L);

        assertThat(result).isEmpty();
    }

    @Test
    void followerCanViewFollowerOnlyFolder() {
        FavoriteFolder folder =
                folder(2L, 100L, false, "FOLLOWERS_ONLY");
        when(userMapper.selectById(100L))
                .thenReturn(user(100L, 200L));
        when(communityAuth.getOptionalLoginUserId()).thenReturn(101L);
        when(folderMapper.selectList(any())).thenReturn(List.of(folder));
        when(userMapper.selectById(101L))
                .thenReturn(user(101L, 201L));
        when(blogMapper.selectById(200L))
                .thenReturn(blog(200L, 100L));
        when(followMapper.findRelation(101L, "BLOG", 200L))
                .thenReturn(new CommunityFollow());

        List<FavoriteFolderView> result = service.visibleFolders(100L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().folderId()).isEqualTo(2L);
    }

    @Test
    void deletingCustomFolderClearsMappingsAndSoftDeletesIt() {
        when(folderMapper.selectById(2L)).thenReturn(
                folder(2L, 100L, false, "PRIVATE")
        );
        when(folderMapper.update(isNull(), any())).thenReturn(1);

        service.delete(2L);

        verify(folderItemMapper).deleteByFolder(2L);
        verify(folderMapper).update(isNull(), any());
    }

    private static FavoriteFolder folder(
            Long id,
            Long ownerId,
            boolean defaultFolder,
            String visibility
    ) {
        FavoriteFolder folder = new FavoriteFolder();
        folder.setId(id);
        folder.setOwnerUserId(ownerId);
        folder.setName(defaultFolder ? "全部收藏" : "自定义");
        folder.setVisibility(visibility);
        folder.setIsDefault(defaultFolder ? 1 : 0);
        folder.setItemCount(0L);
        folder.setSortOrder(defaultFolder ? 0 : 100);
        return folder;
    }

    private static CommunityUser user(Long id, Long personalBlogId) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setPersonalBlogId(personalBlogId);
        user.setStatus("NORMAL");
        return user;
    }

    private static Blog blog(Long id, Long ownerId) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setOwnerUserId(ownerId);
        blog.setBlogType("PERSONAL");
        blog.setStatus("ACTIVE");
        return blog;
    }
}
