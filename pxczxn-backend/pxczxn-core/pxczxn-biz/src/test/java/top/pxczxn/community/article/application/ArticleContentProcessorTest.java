package top.pxczxn.community.article.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleContentProcessorTest {

    private ArticleContentProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ArticleContentProcessor(new ObjectMapper());
    }

    @Test
    void markdownIsRenderedSanitizedAndDerived() {
        RenderedArticleContent content = processor.process(
                "markdown",
                null,
                """
                # 安全标题

                <script>alert(1)</script>

                [危险链接](javascript:alert(1))

                | A | B |
                |---|---|
                | 1 | 2 |
                """
        );

        assertThat(content.renderedHtml())
                .contains("<h1 id=\"安全标题\">")
                .contains("<table>")
                .doesNotContain("<script")
                .doesNotContain("javascript:");
        assertThat(content.tocJson()).contains("\"level\":1").contains("安全标题");
        assertThat(content.contentHash()).hasSize(64);
        assertThat(content.wordCount()).isPositive();
        assertThat(content.readingTimeMinutes()).isEqualTo(1);
    }

    @Test
    void richTextAllowsKnownNodesButRemovesDangerousLinks() {
        String source = """
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "heading",
                      "attrs": {"level": 2},
                      "content": [{"type": "text", "text": "架构设计"}]
                    },
                    {
                      "type": "paragraph",
                      "content": [{
                        "type": "text",
                        "text": "<script>bad</script>",
                        "marks": [{"type": "link", "attrs": {"href": "javascript:alert(1)"}}]
                      }]
                    }
                  ]
                }
                """;

        RenderedArticleContent content = processor.process(
                "RICH_TEXT", source, null
        );

        assertThat(content.renderedHtml())
                .contains("<h2 id=\"架构设计\">")
                .contains("&lt;script&gt;bad&lt;/script&gt;")
                .doesNotContain("javascript:");
        assertThat(content.markdownContent()).isNull();
        assertThat(content.richTextJson()).contains("\"type\": \"doc\"");
    }

    @Test
    void unsupportedRichTextNodeIsRejectedInsteadOfSilentlyLosingContent() {
        assertThatThrownBy(() -> processor.process(
                "RICH_TEXT",
                "{\"type\":\"doc\",\"content\":[{\"type\":\"rawHtml\"}]}",
                null
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的富文本节点");
    }

    @Test
    void twoPrimarySourcesAreRejected() {
        assertThatThrownBy(() -> processor.process(
                "MARKDOWN",
                "{\"type\":\"doc\",\"content\":[]}",
                "# 标题"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能同时提交");
    }
}
