package top.pxczxn.community.search.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import top.pxczxn.community.search.persistence.UnifiedSearchMapper;
import top.pxczxn.community.search.persistence.UnifiedSearchRow;
import top.pxczxn.platform.common.exception.BusinessException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UnifiedSearchService {
    private static final int MAX_KEYWORD_LENGTH = 80;
    private static final Set<String> TYPES = Set.of("ALL", "ARTICLE", "MOMENT", "BLOG", "SERIES", "TAG", "USER");

    private final UnifiedSearchMapper searchMapper;

    @Transactional(readOnly = true)
    public UnifiedSearchPageView search(UnifiedSearchQuery rawQuery) {
        NormalizedQuery query = normalize(rawQuery);
        long total = searchMapper.count(query.pattern(), query.type());
        List<UnifiedSearchResultView> records = searchMapper.selectPage(
                        query.pattern(), query.type(), (long) (query.pageNum() - 1) * query.pageSize(), query.pageSize())
                .stream()
                .map(row -> view(row, query.keyword()))
                .toList();
        return new UnifiedSearchPageView(records, total, query.pageNum(), query.pageSize());
    }

    private static UnifiedSearchResultView view(UnifiedSearchRow row, String keyword) {
        return new UnifiedSearchResultView(
                row.getResultType(), row.getTargetId(), row.getTitle(), highlight(row.getTitle(), keyword),
                highlight(excerpt(row.getExcerpt(), keyword), keyword), row.getCanonicalPath(), row.getAuthorUserId(),
                row.getAuthorName(), row.getBlogName(), row.getOccurredAt());
    }

    static String highlight(String source, String keyword) {
        if (source == null || source.isBlank()) return "";
        String value = source.replaceAll("\\s+", " ").trim();
        String lower = value.toLowerCase(Locale.ROOT);
        String needle = keyword.toLowerCase(Locale.ROOT);
        StringBuilder html = new StringBuilder(value.length() + 32);
        int start = 0;
        int match;
        while ((match = lower.indexOf(needle, start)) >= 0) {
            html.append(HtmlUtils.htmlEscape(value.substring(start, match)));
            html.append("<mark>").append(HtmlUtils.htmlEscape(value.substring(match, match + keyword.length()))).append("</mark>");
            start = match + keyword.length();
        }
        return html.append(HtmlUtils.htmlEscape(value.substring(start))).toString();
    }

    private static String excerpt(String source, String keyword) {
        if (source == null || source.isBlank()) return "";
        String value = source.replaceAll("\\s+", " ").trim();
        int match = value.toLowerCase(Locale.ROOT).indexOf(keyword.toLowerCase(Locale.ROOT));
        int start = match < 0 ? 0 : Math.max(0, match - 72);
        int end = Math.min(value.length(), start + 220);
        return (start > 0 ? "…" : "") + value.substring(start, end) + (end < value.length() ? "…" : "");
    }

    private static NormalizedQuery normalize(UnifiedSearchQuery raw) {
        String keyword = raw == null || raw.keyword() == null ? "" : Normalizer.normalize(raw.keyword().trim(), Normalizer.Form.NFKC);
        if (keyword.isBlank() || keyword.length() > MAX_KEYWORD_LENGTH) throw new BusinessException(400, "搜索词长度应为 1-80 个字符");
        int pageNum = raw.pageNum() == null ? 1 : raw.pageNum();
        int pageSize = raw.pageSize() == null ? 20 : raw.pageSize();
        if (pageNum < 1 || pageSize < 1 || pageSize > 50) throw new BusinessException(400, "分页参数无效");
        String type = raw.type() == null || raw.type().isBlank() ? "ALL" : raw.type().trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw new BusinessException(400, "搜索类型无效");
        String pattern = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return new NormalizedQuery(keyword, type, pageNum, pageSize, pattern);
    }

    private record NormalizedQuery(String keyword, String type, int pageNum, int pageSize, String pattern) {
    }
}
