package top.pxczxn.community.social.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentContentRendererTest {

    private final CommentContentRenderer renderer =
            new CommentContentRenderer();

    @Test
    void escapesHtmlAndKeepsLineBreaks() {
        RenderedCommentContent result = renderer.render(
                "  <script>alert(1)</script>\n第二行  "
        );

        assertThat(result.contentText())
                .isEqualTo("<script>alert(1)</script>\n第二行");
        assertThat(result.renderedHtml())
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .contains("<br>");
        assertThat(result.renderedHtml()).doesNotContain("<script>");
    }

    @Test
    void linkifiesOnlyHttpLinksWithSafeRel() {
        RenderedCommentContent result = renderer.render(
                "参考 https://example.com/a?q=1"
        );

        assertThat(result.renderedHtml())
                .contains("href=\"https://example.com/a?q=1\"")
                .contains("rel=\"nofollow noopener noreferrer\"");
    }

    @Test
    void rejectsBlankAndControlCharacters() {
        assertThatThrownBy(() -> renderer.render(" \n "))
                .isInstanceOf(BusinessException.class)
                .hasMessage("评论内容不能为空");
        assertThatThrownBy(() -> renderer.render("非法\u0000字符"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("评论内容包含无效控制字符");
    }

    @Test
    void rejectsContentOverTwoThousandCharacters() {
        assertThatThrownBy(() -> renderer.render("星".repeat(2_001)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("评论内容不能超过 2000 个字符");
    }
}
