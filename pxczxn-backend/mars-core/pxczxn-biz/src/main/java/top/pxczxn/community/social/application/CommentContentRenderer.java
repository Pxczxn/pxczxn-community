package top.pxczxn.community.social.application;

import com.mars.common.exception.BusinessException;
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
public class CommentContentRenderer {

    private static final int MAX_COMMENT_LENGTH = 2_000;
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s<>{}\\[\\]\"]{1,2048}",
            Pattern.CASE_INSENSITIVE
    );
    private static final Safelist COMMENT_SAFELIST = new Safelist()
            .addTags("p", "br", "a")
            .addAttributes("a", "href", "rel")
            .addProtocols("a", "href", "http", "https");

    public RenderedCommentContent render(String rawContent) {
        if (rawContent == null) {
            throw new BusinessException(400, "评论内容不能为空");
        }
        String content = Normalizer.normalize(
                rawContent, Normalizer.Form.NFKC
        ).replace("\r\n", "\n").replace('\r', '\n').strip();
        if (content.isBlank()) {
            throw new BusinessException(400, "评论内容不能为空");
        }
        if (content.length() > MAX_COMMENT_LENGTH) {
            throw new BusinessException(400, "评论内容不能超过 2000 个字符");
        }
        for (int index = 0; index < content.length(); index++) {
            char value = content.charAt(index);
            if (Character.isISOControl(value)
                    && value != '\n'
                    && value != '\t') {
                throw new BusinessException(400, "评论内容包含无效控制字符");
            }
        }

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
        html.append("</p>");

        Document dirty = Jsoup.parseBodyFragment(html.toString());
        Document clean = new Cleaner(COMMENT_SAFELIST).clean(dirty);
        clean.outputSettings().prettyPrint(false);
        return new RenderedCommentContent(content, clean.body().html());
    }

    private static void appendText(StringBuilder html, String value) {
        String escaped = HtmlUtils.htmlEscape(value);
        html.append(escaped.replace("\n", "<br>"));
    }
}
