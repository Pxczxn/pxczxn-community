package top.pxczxn.community.social.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MomentContentRenderer {

    private static final int MAX_LENGTH = 4_000;
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s<>{}\\[\\]\"]{1,2048}",
            Pattern.CASE_INSENSITIVE
    );
    private static final Safelist SAFELIST = new Safelist()
            .addTags("p", "br", "a", "pre", "code")
            .addAttributes("a", "href", "rel")
            .addProtocols("a", "href", "http", "https");

    public RenderedMomentContent render(
            String rawContent,
            boolean required,
            boolean codeBlock
    ) {
        if (rawContent == null || rawContent.isBlank()) {
            if (required) {
                throw new BusinessException(400, "动态正文不能为空");
            }
            return new RenderedMomentContent(null, null);
        }
        String content = Normalizer.normalize(
                rawContent, Normalizer.Form.NFKC
        ).replace("\r\n", "\n").replace('\r', '\n').strip();
        if (content.isBlank()) {
            if (required) {
                throw new BusinessException(400, "动态正文不能为空");
            }
            return new RenderedMomentContent(null, null);
        }
        if (content.length() > MAX_LENGTH) {
            throw new BusinessException(400, "动态正文不能超过 4000 个字符");
        }
        for (int index = 0; index < content.length(); index++) {
            char value = content.charAt(index);
            if (Character.isISOControl(value)
                    && value != '\n'
                    && value != '\t') {
                throw new BusinessException(400, "动态正文包含无效控制字符");
            }
        }
        String html = codeBlock
                ? "<pre><code>"
                + HtmlUtils.htmlEscape(content)
                + "</code></pre>"
                : paragraph(content);
        Document dirty = Jsoup.parseBodyFragment(html);
        Document clean = new Cleaner(SAFELIST).clean(dirty);
        clean.outputSettings().prettyPrint(false);
        return new RenderedMomentContent(
                content, clean.body().html()
        );
    }

    private static String paragraph(String content) {
        StringBuilder html = new StringBuilder(content.length() + 32);
        html.append("<p>");
        Matcher matcher = URL_PATTERN.matcher(content);
        int cursor = 0;
        while (matcher.find()) {
            appendText(html, content.substring(cursor, matcher.start()));
            String url = matcher.group();
            html.append("<a href=\"")
                    .append(HtmlUtils.htmlEscape(url))
                    .append("\" rel=\"nofollow noopener noreferrer\">");
            appendText(html, url);
            html.append("</a>");
            cursor = matcher.end();
        }
        appendText(html, content.substring(cursor));
        return html.append("</p>").toString();
    }

    private static void appendText(StringBuilder html, String value) {
        html.append(
                HtmlUtils.htmlEscape(value).replace("\n", "<br>")
        );
    }
}
