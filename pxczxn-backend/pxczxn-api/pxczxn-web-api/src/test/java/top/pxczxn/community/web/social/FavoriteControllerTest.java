package top.pxczxn.community.web.social;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.social.application.CreateFavoriteFolderCommand;
import top.pxczxn.community.social.application.FavoriteContentPageView;
import top.pxczxn.community.social.application.FavoriteContentView;
import top.pxczxn.community.social.application.FavoriteFolderService;
import top.pxczxn.community.social.application.FavoriteFolderView;
import top.pxczxn.community.social.application.FavoriteRelationshipView;
import top.pxczxn.community.social.application.FavoriteService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FavoriteControllerTest {

    private FavoriteFolderService folderService;
    private FavoriteService favoriteService;
    private FavoriteController controller;

    @BeforeEach
    void setUp() {
        folderService = mock(FavoriteFolderService.class);
        favoriteService = mock(FavoriteService.class);
        controller = new FavoriteController(
                folderService, favoriteService
        );
    }

    @Test
    void createsFolderAndSerializesSnowflakeIds() {
        CreateFavoriteFolderCommand command =
                new CreateFavoriteFolderCommand(
                        "设计", "说明", "PRIVATE", 20
                );
        when(folderService.create(command)).thenReturn(
                new FavoriteFolderView(
                        300L,
                        100L,
                        "设计",
                        "说明",
                        "PRIVATE",
                        false,
                        0,
                        20,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )
        );

        var result = controller.createFolder(
                new CreateFavoriteFolderRequest(
                        "设计", "说明", "PRIVATE", 20
                )
        );

        assertThat(result.getData().folderId()).isEqualTo("300");
        assertThat(result.getData().ownerUserId()).isEqualTo("100");
        verify(folderService).create(command);
    }

    @Test
    void favoriteParsesFolderIdsAndReturnsDefaultFolder() {
        when(favoriteService.favorite(
                "ARTICLE", 400L, List.of(300L)
        )).thenReturn(new FavoriteRelationshipView(
                "ARTICLE", 400L, true, 1, List.of(200L, 300L)
        ));

        var result = controller.favorite(
                "ARTICLE",
                400L,
                new UpdateFavoriteRequest(List.of("300"))
        );

        assertThat(result.getData().targetId()).isEqualTo("400");
        assertThat(result.getData().folderIds())
                .containsExactly("200", "300");
        verify(favoriteService).favorite(
                "ARTICLE", 400L, List.of(300L)
        );
    }

    @Test
    void folderItemsSerializeEveryBusinessId() {
        when(favoriteService.folderItems(300L, 1, 20)).thenReturn(
                new FavoriteContentPageView(
                        300L,
                        List.of(new FavoriteContentView(
                                500L,
                                "ARTICLE",
                                400L,
                                100L,
                                200L,
                                "文章",
                                "摘要",
                                600L,
                                "/blog/article",
                                1,
                                LocalDateTime.now()
                        )),
                        1,
                        1,
                        20
                )
        );

        var result = controller.folderItems(300L, 1, 20);

        assertThat(result.getData().folderId()).isEqualTo("300");
        FavoriteContentResponse item =
                result.getData().records().getFirst();
        assertThat(item.favoriteItemId()).isEqualTo("500");
        assertThat(item.targetId()).isEqualTo("400");
        assertThat(item.authorUserId()).isEqualTo("100");
        assertThat(item.blogId()).isEqualTo("200");
        assertThat(item.coverFileId()).isEqualTo("600");
    }
}
