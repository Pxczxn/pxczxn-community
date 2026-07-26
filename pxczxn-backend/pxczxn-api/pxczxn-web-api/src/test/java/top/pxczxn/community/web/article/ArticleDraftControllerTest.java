package top.pxczxn.community.web.article;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.pxczxn.community.article.application.ArticleDraftService;
import top.pxczxn.community.article.application.ArticleEditorView;
import top.pxczxn.community.article.application.ArticleVersionPageView;
import top.pxczxn.community.article.application.ArticleVersionSummaryView;
import top.pxczxn.community.article.application.CreateArticleCommand;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleDraftControllerTest {

    @Test
    void createParsesRequestIdsAndSerializesBigintsAsStrings() {
        ArticleDraftService service = mock(ArticleDraftService.class);
        when(service.create(any())).thenReturn(editorView());
        ArticleDraftController controller = new ArticleDraftController(service);

        ArticleEditorResponse response = controller.create(new CreateArticleRequest(
                "标题",
                "article",
                null,
                "9223372036854774000",
                null,
                "MARKDOWN",
                null,
                "# 内容",
                "PUBLIC",
                "MANUAL",
                List.of("9223372036854773000"),
                List.of()
        )).getData();

        assertThat(response.articleId()).isEqualTo("9223372036854775000");
        assertThat(response.currentVersionId()).isEqualTo("9223372036854772000");
        assertThat(response.tagIds()).containsExactly("9223372036854773000");

        ArgumentCaptor<CreateArticleCommand> command =
                ArgumentCaptor.forClass(CreateArticleCommand.class);
        verify(service).create(command.capture());
        assertThat(command.getValue().categoryId())
                .isEqualTo(9223372036854774000L);
    }

    @Test
    void invalidStringIdIsRejectedBeforeServiceCall() {
        ArticleDraftService service = mock(ArticleDraftService.class);
        ArticleDraftController controller = new ArticleDraftController(service);

        assertThatThrownBy(() -> controller.create(new CreateArticleRequest(
                "标题",
                "article",
                null,
                "not-an-id",
                null,
                "MARKDOWN",
                null,
                "",
                null,
                null,
                List.of(),
                List.of()
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类 ID");
    }

    @Test
    void versionPageKeepsVersionIdsAsStrings() {
        ArticleDraftService service = mock(ArticleDraftService.class);
        when(service.listVersions(1L, 1, 20)).thenReturn(
                new ArticleVersionPageView(
                        List.of(new ArticleVersionSummaryView(
                                9223372036854771000L,
                                3,
                                "MARKDOWN",
                                "hash",
                                10,
                                1,
                                9223372036854770000L,
                                "MANUAL_SAVE",
                                true,
                                false,
                                LocalDateTime.now()
                        )),
                        1,
                        20,
                        1
                )
        );
        ArticleDraftController controller = new ArticleDraftController(service);

        ArticleVersionPageResponse response =
                controller.versions(1L, 1, 20).getData();

        assertThat(response.list().getFirst().versionId())
                .isEqualTo("9223372036854771000");
        assertThat(response.list().getFirst().createdByUserId())
                .isEqualTo("9223372036854770000");
    }

    private static ArticleEditorView editorView() {
        LocalDateTime now = LocalDateTime.now();
        return new ArticleEditorView(
                9223372036854775000L,
                9223372036854774000L,
                9223372036854770000L,
                9223372036854774000L,
                "标题",
                "article",
                null,
                null,
                "MARKDOWN",
                "PUBLIC",
                "MANUAL",
                "DRAFT",
                "NOT_SUBMITTED",
                9223372036854772000L,
                null,
                null,
                0,
                List.of(9223372036854773000L),
                List.of(),
                null,
                "# 内容",
                "<h1>内容</h1>",
                "内容",
                "[]",
                "hash",
                2,
                1,
                now,
                now,
                now
        );
    }
}
