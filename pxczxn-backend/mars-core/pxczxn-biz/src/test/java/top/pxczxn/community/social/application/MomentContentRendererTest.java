package top.pxczxn.community.social.application;

import com.mars.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MomentContentRendererTest {

    private final MomentContentRenderer renderer =
            new MomentContentRenderer();

    @Test
    void escapesHtmlAndCreatesSafeLinks() {
        RenderedMomentContent result = renderer.render(
                "<img src=x onerror=alert(1)>\nhttps://example.com/moment",
                true,
                false
        );

        assertThat(result.renderedHtml())
                .contains("&lt;img src=x onerror=alert(1)&gt;")
                .contains("href=\"https://example.com/moment\"")
                .contains("rel=\"nofollow noopener noreferrer\"")
                .doesNotContain("<img");
    }

    @Test
    void codeBlockNeverInterpretsMarkup() {
        RenderedMomentContent result = renderer.render(
                "if (a < b) {\n  return \"x\";\n}",
                true,
                true
        );

        assertThat(result.renderedHtml())
                .startsWith("<pre><code>")
                .contains("a &lt; b")
                .endsWith("</code></pre>");
    }

    @Test
    void optionalBlankContentReturnsNoHtml() {
        RenderedMomentContent result =
                renderer.render("  ", false, false);

        assertThat(result.textContent()).isNull();
        assertThat(result.renderedHtml()).isNull();
    }

    @Test
    void rejectsRequiredBlankAndOversizedContent() {
        assertThatThrownBy(() -> renderer.render(null, true, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("动态正文不能为空");
        assertThatThrownBy(() -> renderer.render(
                "星".repeat(4_001), true, false
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("动态正文不能超过 4000 个字符");
    }
}
