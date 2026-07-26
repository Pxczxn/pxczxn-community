package top.pxczxn.community.article.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ArticleContentProcessor {

    static final int MAX_SOURCE_CHARACTERS = 1_000_000;
    private static final int MAX_RICH_TEXT_DEPTH = 64;
    private static final int MAX_RICH_TEXT_NODES = 20_000;
    private static final Set<String> MODES = Set.of("RICH_TEXT", "MARKDOWN");

    private final ObjectMapper objectMapper;

    private final List<Extension> markdownExtensions = List.of(
            AutolinkExtension.create(),
            StrikethroughExtension.create(),
            TablesExtension.create()
    );
    private final Parser markdownParser = Parser.builder()
            .extensions(markdownExtensions)
            .build();
    private final HtmlRenderer markdownRenderer = HtmlRenderer.builder()
            .extensions(markdownExtensions)
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    RenderedArticleContent process(
            String requestedMode,
            String richTextJson,
            String markdownContent
    ) {
        String mode = normalizeMode(requestedMode);
        String normalizedRich = normalizeSource(richTextJson);
        String normalizedMarkdown = normalizeSource(markdownContent);
        String source;
        String rawHtml;

        if ("MARKDOWN".equals(mode)) {
            if (normalizedRich != null) {
                throw new BusinessException(400, "Markdown 文章不能同时提交富文本源");
            }
            source = normalizedMarkdown == null ? "" : normalizedMarkdown;
            rawHtml = markdownRenderer.render(markdownParser.parse(source));
        } else {
            if (normalizedMarkdown != null) {
                throw new BusinessException(400, "富文本文章不能同时提交 Markdown 源");
            }
            source = normalizedRich == null ? emptyRichTextDocument() : normalizedRich;
            rawHtml = renderRichText(source);
        }

        SafeDocument safe = sanitizeAndDerive(rawHtml);
        int wordCount = countWords(safe.plainText());
        return new RenderedArticleContent(
                mode,
                "RICH_TEXT".equals(mode) ? source : null,
                "MARKDOWN".equals(mode) ? source : null,
                safe.html(),
                safe.plainText(),
                safe.tocJson(),
                sha256(mode + "\n" + source),
                wordCount,
                Math.max(1, (wordCount + 299) / 300)
        );
    }

    private String renderRichText(String source) {
        JsonNode root;
        try {
            root = objectMapper.readTree(source);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "富文本 JSON 格式无效");
        }
        if (root == null || !root.isObject() || !"doc".equals(root.path("type").asText())) {
            throw new BusinessException(400, "富文本根节点必须为 doc");
        }
        StringBuilder html = new StringBuilder(Math.min(source.length(), 64_000));
        renderRichNode(root, html, 0, new NodeCounter());
        return html.toString();
    }

    private void renderRichNode(
            JsonNode node,
            StringBuilder html,
            int depth,
            NodeCounter counter
    ) {
        if (depth > MAX_RICH_TEXT_DEPTH || ++counter.count > MAX_RICH_TEXT_NODES) {
            throw new BusinessException(400, "富文本结构过于复杂");
        }
        String type = node.path("type").asText("");
        switch (type) {
            case "doc" -> appendChildren(node, html, depth, counter);
            case "paragraph" -> wrapped("p", node, html, depth, counter);
            case "blockquote" -> wrapped("blockquote", node, html, depth, counter);
            case "bulletList" -> wrapped("ul", node, html, depth, counter);
            case "listItem" -> wrapped("li", node, html, depth, counter);
            case "orderedList" -> {
                int start = Math.max(1, node.path("attrs").path("start").asInt(1));
                html.append("<ol");
                if (start != 1) {
                    html.append(" start=\"").append(start).append("\"");
                }
                html.append(">");
                appendChildren(node, html, depth, counter);
                html.append("</ol>");
            }
            case "heading" -> {
                int level = Math.min(6, Math.max(1, node.path("attrs").path("level").asInt(2)));
                wrapped("h" + level, node, html, depth, counter);
            }
            case "codeBlock" -> html.append("<pre><code>")
                    .append(HtmlUtils.htmlEscape(textContent(node)))
                    .append("</code></pre>");
            case "hardBreak" -> html.append("<br>");
            case "horizontalRule" -> html.append("<hr>");
            case "image" -> {
                JsonNode attrs = node.path("attrs");
                html.append("<img src=\"")
                        .append(HtmlUtils.htmlEscape(attrs.path("src").asText("")))
                        .append("\" alt=\"")
                        .append(HtmlUtils.htmlEscape(attrs.path("alt").asText("")))
                        .append("\"");
                if (attrs.hasNonNull("title")) {
                    html.append(" title=\"")
                            .append(HtmlUtils.htmlEscape(attrs.path("title").asText("")))
                            .append("\"");
                }
                html.append(">");
            }
            case "text" -> html.append(renderMarkedText(node));
            default -> throw new BusinessException(400, "包含不支持的富文本节点: " + type);
        }
    }

    private void appendChildren(
            JsonNode node,
            StringBuilder html,
            int depth,
            NodeCounter counter
    ) {
        JsonNode content = node.path("content");
        if (content.isMissingNode() || content.isNull()) {
            return;
        }
        if (!content.isArray()) {
            throw new BusinessException(400, "富文本 content 必须为数组");
        }
        for (JsonNode child : content) {
            if (!child.isObject()) {
                throw new BusinessException(400, "富文本节点格式无效");
            }
            renderRichNode(child, html, depth + 1, counter);
        }
    }

    private void wrapped(
            String tag,
            JsonNode node,
            StringBuilder html,
            int depth,
            NodeCounter counter
    ) {
        html.append("<").append(tag).append(">");
        appendChildren(node, html, depth, counter);
        html.append("</").append(tag).append(">");
    }

    private String renderMarkedText(JsonNode node) {
        String value = HtmlUtils.htmlEscape(node.path("text").asText(""));
        JsonNode marks = node.path("marks");
        if (!marks.isArray()) {
            return value;
        }
        for (JsonNode mark : marks) {
            String type = mark.path("type").asText("");
            value = switch (type) {
                case "bold", "strong" -> "<strong>" + value + "</strong>";
                case "italic", "em" -> "<em>" + value + "</em>";
                case "strike" -> "<del>" + value + "</del>";
                case "code" -> "<code>" + value + "</code>";
                case "link" -> {
                    JsonNode attrs = mark.path("attrs");
                    String href = HtmlUtils.htmlEscape(attrs.path("href").asText(""));
                    String title = attrs.hasNonNull("title")
                            ? " title=\"" + HtmlUtils.htmlEscape(attrs.path("title").asText("")) + "\""
                            : "";
                    yield "<a href=\"" + href + "\"" + title + ">" + value + "</a>";
                }
                default -> value;
            };
        }
        return value;
    }

    private String textContent(JsonNode node) {
        if ("text".equals(node.path("type").asText())) {
            return node.path("text").asText("");
        }
        StringBuilder text = new StringBuilder();
        JsonNode content = node.path("content");
        if (content.isArray()) {
            for (JsonNode child : content) {
                text.append(textContent(child));
            }
        }
        return text.toString();
    }

    private SafeDocument sanitizeAndDerive(String rawHtml) {
        Safelist safelist = Safelist.relaxed()
                .addTags(
                        "h1", "h2", "h3", "h4", "h5", "h6",
                        "del", "hr", "table", "thead", "tbody", "tfoot",
                        "tr", "th", "td"
                )
                .addAttributes("ol", "start")
                .addAttributes("a", "title")
                .addAttributes("img", "alt", "title")
                .addAttributes("code", "class")
                .addAttributes("th", "align")
                .addAttributes("td", "align")
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https");

        Document dirty = Jsoup.parseBodyFragment(rawHtml, "");
        Document clean = new Cleaner(safelist).clean(dirty);
        clean.outputSettings().prettyPrint(false);

        List<Map<String, Object>> toc = new ArrayList<>();
        Map<String, Integer> headingIds = new HashMap<>();
        int fallbackHeading = 0;
        for (Element heading : clean.select("h1,h2,h3,h4,h5,h6")) {
            String base = headingId(heading.text(), ++fallbackHeading);
            int count = headingIds.merge(base, 1, Integer::sum);
            String id = count == 1 ? base : base + "-" + count;
            heading.attr("id", id);
            Map<String, Object> item = new HashMap<>();
            item.put("id", id);
            item.put("text", heading.text());
            item.put("level", Integer.parseInt(heading.tagName().substring(1)));
            toc.add(item);
        }
        for (Element link : clean.select("a[href]")) {
            String href = link.attr("href").trim().toLowerCase(Locale.ROOT);
            if (href.startsWith("http://") || href.startsWith("https://")) {
                link.attr("target", "_blank");
                link.attr("rel", "nofollow noopener noreferrer");
            }
        }
        for (Element image : clean.select("img[src]")) {
            image.attr("loading", "lazy");
            image.attr("referrerpolicy", "no-referrer");
        }
        String tocJson;
        try {
            tocJson = objectMapper.writeValueAsString(toc);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("生成文章目录失败", exception);
        }
        return new SafeDocument(clean.body().html(), clean.body().text(), tocJson);
    }

    private static String headingId(String text, int fallback) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        StringBuilder slug = new StringBuilder();
        boolean separator = false;
        for (int offset = 0; offset < normalized.length(); ) {
            int codePoint = normalized.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isLetterOrDigit(codePoint)) {
                if (separator && !slug.isEmpty()) {
                    slug.append('-');
                }
                slug.appendCodePoint(codePoint);
                separator = false;
            } else {
                separator = true;
            }
        }
        return slug.isEmpty() ? "section-" + fallback : slug.toString();
    }

    private static int countWords(String text) {
        int count = 0;
        boolean inLatinToken = false;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (isCjk(codePoint)) {
                count++;
                inLatinToken = false;
            } else if (Character.isLetterOrDigit(codePoint)) {
                if (!inLatinToken) {
                    count++;
                    inLatinToken = true;
                }
            } else {
                inLatinToken = false;
            }
        }
        return count;
    }

    private static boolean isCjk(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }

    private static String normalizeMode(String requestedMode) {
        String mode = requestedMode == null
                ? ""
                : requestedMode.trim().toUpperCase(Locale.ROOT);
        if (!MODES.contains(mode)) {
            throw new BusinessException(400, "文章内容模式无效");
        }
        return mode;
    }

    private static String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.length() > MAX_SOURCE_CHARACTERS) {
            throw new BusinessException(400, "文章正文不能超过 100 万字符");
        }
        return normalized;
    }

    private static String emptyRichTextDocument() {
        return "{\"type\":\"doc\",\"content\":[]}";
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private static final class NodeCounter {
        private int count;
    }

    private record SafeDocument(String html, String plainText, String tocJson) {
    }
}
