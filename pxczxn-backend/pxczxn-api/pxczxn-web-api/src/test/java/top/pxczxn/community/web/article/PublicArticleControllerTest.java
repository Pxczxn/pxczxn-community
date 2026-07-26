package top.pxczxn.community.web.article;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.application.PublicArticleAuthorView;
import top.pxczxn.community.article.application.PublicArticleBlogView;
import top.pxczxn.community.article.application.PublicArticleCategoryView;
import top.pxczxn.community.article.application.PublicArticleDetailView;
import top.pxczxn.community.article.application.PublicArticlePageView;
import top.pxczxn.community.article.application.PublicArticleQuery;
import top.pxczxn.community.article.application.PublicArticleSeoView;
import top.pxczxn.community.article.application.PublicArticleService;
import top.pxczxn.community.article.application.PublicArticleSummaryView;
import top.pxczxn.community.article.application.PublicArticleTagView;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicArticleControllerTest {

    private PublicArticleService service;
    private PublicArticleController controller;

    @BeforeEach
    void setUp() {
        service = mock(PublicArticleService.class);
        controller = new PublicArticleController(service);
    }

    @Test
    void detailReturnsStringIdsAndOnlySafeRenderedContent() {
        when(service.detail(300L)).thenReturn(detail());

        var result = controller.detail(300L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().articleId()).isEqualTo("300");
        assertThat(result.getData().renderedHtml())
                .isEqualTo("<h1>safe</h1>");
        assertThat(result.getData().author().userId()).isEqualTo("100");
        assertThat(result.getData().category().categoryId())
                .isEqualTo("400");
        assertThat(result.getData().tags().getFirst().tagId())
                .isEqualTo("700");
        assertThat(Arrays.stream(
                PublicArticleDetailResponse.class.getRecordComponents()
        ).map(component -> component.getName().toLowerCase()))
                .doesNotContain(
                        "markdowncontent",
                        "richtextjson",
                        "currentversionid",
                        "reviewversionid",
                        "reviewstatus",
                        "contenthash"
                );
    }

    @Test
    void pageMapsFilterAndPaginationContract() {
        PublicArticleSummaryView summary = new PublicArticleSummaryView(
                300L,
                "公开文章",
                "public",
                "摘要",
                800L,
                "MARKDOWN",
                author(),
                category(),
                List.of(tag()),
                LocalDateTime.now(),
                LocalDateTime.now(),
                20,
                1,
                12,
                3,
                2,
                1,
                "/alice/300/public"
        );
        when(service.page(
                "alice",
                new PublicArticleQuery("tech", 2, 10)
        )).thenReturn(new PublicArticlePageView(
                List.of(summary),
                11,
                2,
                10
        ));

        var result = controller.page("alice", "tech", 2, 10);

        assertThat(result.getData().records()).hasSize(1);
        assertThat(result.getData().records().getFirst().articleId())
                .isEqualTo("300");
        assertThat(result.getData().total()).isEqualTo(11);
        verify(service).page(
                "alice",
                new PublicArticleQuery("tech", 2, 10)
        );
    }

    @Test
    void discoverMapsGlobalPublicTimelineContract() {
        when(service.discover(new PublicArticleQuery(null, 2, 10)))
                .thenReturn(new PublicArticlePageView(List.of(), 0, 2, 10));

        var result = controller.discover(2, 10);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().records()).isEmpty();
        assertThat(result.getData().pageNum()).isEqualTo(2);
        assertThat(result.getData().pageSize()).isEqualTo(10);
        verify(service).discover(new PublicArticleQuery(null, 2, 10));
    }

    @Test
    void categoriesReturnPrecisionSafeIds() {
        when(service.categories("alice")).thenReturn(List.of(category()));

        var result = controller.categories("alice");

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().getFirst().categoryId())
                .isEqualTo("400");
    }

    private static PublicArticleDetailView detail() {
        LocalDateTime now = LocalDateTime.now();
        return new PublicArticleDetailView(
                300L,
                "公开文章",
                "public",
                "摘要",
                800L,
                "MARKDOWN",
                "PUBLIC",
                "<h1>safe</h1>",
                "[]",
                20,
                1,
                now,
                now,
                12,
                3,
                2,
                1,
                "/alice/300/public",
                author(),
                new PublicArticleBlogView(
                        200L,
                        "PERSONAL",
                        "Alice 的博客",
                        "alice",
                        "简介",
                        null,
                        null,
                        "starry",
                        null
                ),
                category(),
                List.of(tag()),
                new PublicArticleSeoView(
                        "公开文章 - Alice 的博客",
                        "摘要",
                        "/alice/300/public"
                )
        );
    }

    private static PublicArticleAuthorView author() {
        return new PublicArticleAuthorView(
                100L,
                "alice",
                "Alice",
                "写作者",
                null
        );
    }

    private static PublicArticleCategoryView category() {
        return new PublicArticleCategoryView(
                400L,
                "技术",
                "tech",
                "技术文章",
                false
        );
    }

    private static PublicArticleTagView tag() {
        return new PublicArticleTagView(700L, "Java", "java");
    }
}
