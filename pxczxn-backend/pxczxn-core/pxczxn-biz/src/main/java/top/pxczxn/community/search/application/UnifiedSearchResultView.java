package top.pxczxn.community.search.application;

import java.time.LocalDateTime;

public record UnifiedSearchResultView(
        String type,
        Long targetId,
        String title,
        String titleHighlightHtml,
        String excerptHighlightHtml,
        String canonicalPath,
        Long authorUserId,
        String authorName,
        String blogName,
        LocalDateTime occurredAt
) {
}
