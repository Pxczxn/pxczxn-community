package top.pxczxn.community.social.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.FavoriteFolder;
import top.pxczxn.community.social.model.FavoriteItem;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.social.persistence.FavoriteFolderItemMapper;
import top.pxczxn.community.social.persistence.FavoriteItemMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FavoriteServiceTest {

    private FavoriteItemMapper itemMapper;
    private FavoriteFolderItemMapper folderItemMapper;
    private FavoriteFolderService folderService;
    private CommunityContentAccessService contentAccessService;
    private ArticleMapper articleMapper;
    private CommunityMomentMapper momentMapper;
    private CommunityUserMapper userMapper;
    private BlogMapper blogMapper;
    private CommunityAuth communityAuth;
    private FavoriteService service;

    @BeforeEach
    void setUp() {
        itemMapper = mock(FavoriteItemMapper.class);
        folderItemMapper = mock(FavoriteFolderItemMapper.class);
        folderService = mock(FavoriteFolderService.class);
        contentAccessService = mock(CommunityContentAccessService.class);
        articleMapper = mock(ArticleMapper.class);
        momentMapper = mock(CommunityMomentMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        blogMapper = mock(BlogMapper.class);
        communityAuth = mock(CommunityAuth.class);
        service = new FavoriteService(
                itemMapper,
                folderItemMapper,
                folderService,
                contentAccessService,
                articleMapper,
                momentMapper,
                userMapper,
                blogMapper,
                communityAuth
        );
        CommunityUser actor = user(100L);
        when(communityAuth.getLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L)).thenReturn(actor);
    }

    @Test
    void firstFavoriteAddsDefaultAndCustomFolderOnce() {
        FavoriteFolder defaultFolder = folder(1L, true);
        FavoriteFolder customFolder = folder(2L, false);
        when(contentAccessService.requireAccessible(
                LikeTargetType.ARTICLE, 10L
        )).thenReturn(target(LikeTargetType.ARTICLE, 10L, 3));
        when(folderService.ensureDefaultFolder(100L))
                .thenReturn(defaultFolder);
        when(folderService.requireOwnedFolders(100L, List.of(2L)))
                .thenReturn(Map.of(2L, customFolder));
        when(itemMapper.findRelation(100L, "ARTICLE", 10L))
                .thenReturn(null);
        when(itemMapper.insert(any())).thenReturn(1);
        when(articleMapper.update(isNull(), any())).thenReturn(1);
        when(folderItemMapper.countMapping(any(), any())).thenReturn(0L);
        when(folderItemMapper.insert(any())).thenReturn(1);
        when(folderItemMapper.findFolderIds(any()))
                .thenReturn(List.of(1L, 2L));

        FavoriteRelationshipView result = service.favorite(
                "ARTICLE", 10L, List.of(2L)
        );

        assertThat(result.favorited()).isTrue();
        assertThat(result.favoriteCount()).isEqualTo(4);
        assertThat(result.folderIds()).containsExactly(1L, 2L);
        verify(articleMapper).update(isNull(), any());
        verify(folderService, times(2)).incrementItemCount(any());
    }

    @Test
    void repeatedFavoriteDoesNotIncrementTarget() {
        FavoriteItem item = item(20L, "ARTICLE", 10L);
        when(contentAccessService.requireAccessible(
                LikeTargetType.ARTICLE, 10L
        )).thenReturn(target(LikeTargetType.ARTICLE, 10L, 3));
        when(folderService.ensureDefaultFolder(100L))
                .thenReturn(folder(1L, true));
        when(folderService.requireOwnedFolders(100L, List.of()))
                .thenReturn(Map.of());
        when(itemMapper.findRelation(100L, "ARTICLE", 10L))
                .thenReturn(item);
        when(folderItemMapper.countMapping(1L, 20L)).thenReturn(1L);
        when(folderItemMapper.findFolderIds(20L)).thenReturn(List.of(1L));

        FavoriteRelationshipView result = service.favorite(
                "ARTICLE", 10L, List.of()
        );

        assertThat(result.favoriteCount()).isEqualTo(3);
        verify(itemMapper, never()).insert(any());
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void unfavoriteRemovesEveryFolderMappingAndDecrementsOnce() {
        FavoriteItem item = item(20L, "ARTICLE", 10L);
        when(contentAccessService.requireAccessible(
                LikeTargetType.ARTICLE, 10L
        )).thenReturn(target(LikeTargetType.ARTICLE, 10L, 1));
        when(itemMapper.findRelation(100L, "ARTICLE", 10L))
                .thenReturn(item);
        when(folderItemMapper.findFolderIds(20L))
                .thenReturn(List.of(1L, 2L));
        when(itemMapper.deleteRelation(100L, "ARTICLE", 10L))
                .thenReturn(1);
        when(articleMapper.update(isNull(), any())).thenReturn(1);

        FavoriteRelationshipView result =
                service.unfavorite("ARTICLE", 10L);

        assertThat(result.favorited()).isFalse();
        assertThat(result.favoriteCount()).isZero();
        verify(folderItemMapper).deleteByFavoriteItem(20L);
        verify(folderService).decrementItemCount(1L);
        verify(folderService).decrementItemCount(2L);
        verify(articleMapper).update(isNull(), any());
    }

    @Test
    void relationshipDoesNotExposeFoldersToAnonymousViewer() {
        when(contentAccessService.requireAccessible(
                LikeTargetType.ARTICLE, 10L
        )).thenReturn(target(LikeTargetType.ARTICLE, 10L, 4));
        when(communityAuth.getOptionalLoginUserId()).thenReturn(null);

        FavoriteRelationshipView result =
                service.relationship("ARTICLE", 10L);

        assertThat(result.favorited()).isFalse();
        assertThat(result.folderIds()).isEmpty();
        verify(itemMapper, never()).findRelation(any(), any(), any());
    }

    @Test
    void commentsCannotBeFavorited() {
        assertThatThrownBy(() ->
                service.favorite("COMMENT", 10L, List.of())
        ).hasMessage("评论不支持收藏");
    }

    @Test
    void contentAuthorCanSeeFavoritorsWithoutFolderNames() {
        when(contentAccessService.requireAccessible(
                LikeTargetType.ARTICLE, 10L
        )).thenReturn(target(LikeTargetType.ARTICLE, 10L, 2));
        FavoriteItem item = item(20L, "ARTICLE", 10L);
        item.setCreatedAt(LocalDateTime.now());
        when(itemMapper.findByTarget("ARTICLE", 10L))
                .thenReturn(List.of(item));
        when(userMapper.selectBatchIds(any()))
                .thenReturn(List.of(user(100L)));

        ContentFavoritorPageView result =
                service.favoritors("ARTICLE", 10L, 1, 20);

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().getFirst().userId()).isEqualTo(100L);
    }

    private static AccessibleContentTarget target(
            LikeTargetType type,
            Long id,
            long favoriteCount
    ) {
        return new AccessibleContentTarget(
                type,
                id,
                100L,
                200L,
                "内容",
                "摘要",
                null,
                "/blog/" + id,
                0,
                favoriteCount,
                0
        );
    }

    private static FavoriteFolder folder(Long id, boolean defaultFolder) {
        FavoriteFolder folder = new FavoriteFolder();
        folder.setId(id);
        folder.setOwnerUserId(100L);
        folder.setIsDefault(defaultFolder ? 1 : 0);
        return folder;
    }

    private static FavoriteItem item(
            Long id,
            String type,
            Long targetId
    ) {
        FavoriteItem item = new FavoriteItem();
        item.setId(id);
        item.setOwnerUserId(100L);
        item.setTargetType(type);
        item.setTargetId(targetId);
        return item;
    }

    private static CommunityUser user(Long id) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setDisplayName("用户" + id);
        user.setStatus("NORMAL");
        return user;
    }
}
