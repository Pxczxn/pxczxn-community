package top.pxczxn.community.moderation.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.pxczxn.community.moderation.model.ContentKeywordRule;
import top.pxczxn.community.moderation.persistence.ContentKeywordRuleMapper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArticleKeywordReviewEngine {

    private static final Set<String> SEVERITIES =
            Set.of("BLOCK", "REVIEW", "WARN");
    private static final int MAX_REASON_RULE_IDS = 30;

    private final ContentKeywordRuleMapper ruleMapper;

    public KeywordReviewOutcome review(
            String title,
            String summary,
            String plainText
    ) {
        String normalizedContent = normalizeForMatching(
                String.join(
                        "\n",
                        valueOrEmpty(title),
                        valueOrEmpty(summary),
                        valueOrEmpty(plainText)
                )
        );
        List<ContentKeywordRule> rules = ruleMapper.selectList(
                Wrappers.<ContentKeywordRule>lambdaQuery()
                        .eq(ContentKeywordRule::getStatus, "ACTIVE")
                        .isNull(ContentKeywordRule::getDeletedAt)
                        .orderByAsc(ContentKeywordRule::getSortOrder)
                        .orderByAsc(ContentKeywordRule::getId)
        );
        List<Long> matchedRuleIds = new ArrayList<>();
        boolean block = false;
        boolean manual = false;
        boolean warn = false;
        for (ContentKeywordRule rule : rules) {
            String severity = normalizedSeverity(rule.getSeverity());
            String keyword = normalizeForMatching(
                    rule.getNormalizedKeyword() == null
                            ? rule.getKeyword()
                            : rule.getNormalizedKeyword()
            );
            if (severity == null
                    || keyword.isBlank()
                    || !normalizedContent.contains(keyword)) {
                continue;
            }
            matchedRuleIds.add(rule.getId());
            block |= "BLOCK".equals(severity);
            manual |= "REVIEW".equals(severity);
            warn |= "WARN".equals(severity);
        }

        if (block) {
            return outcome(
                    KeywordReviewOutcome.Decision.BLOCK,
                    "CRITICAL",
                    "AUTO_BLOCKED_KEYWORD",
                    matchedRuleIds
            );
        }
        if (manual) {
            return outcome(
                    KeywordReviewOutcome.Decision.MANUAL_REVIEW,
                    "HIGH",
                    "AUTO_ESCALATED_KEYWORD",
                    matchedRuleIds
            );
        }
        if (warn) {
            return outcome(
                    KeywordReviewOutcome.Decision.AUTO_APPROVE,
                    "MEDIUM",
                    "AUTO_APPROVED_WITH_WARNING",
                    matchedRuleIds
            );
        }
        return outcome(
                KeywordReviewOutcome.Decision.AUTO_APPROVE,
                "LOW",
                "AUTO_APPROVED",
                List.of()
        );
    }

    static String normalizeForMatching(String raw) {
        String normalized = Normalizer.normalize(
                valueOrEmpty(raw),
                Normalizer.Form.NFKC
        ).toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean previousWhitespace = false;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (Character.isWhitespace(current)) {
                if (!previousWhitespace) {
                    builder.append(' ');
                    previousWhitespace = true;
                }
            } else {
                builder.append(current);
                previousWhitespace = false;
            }
        }
        return builder.toString().trim();
    }

    private static KeywordReviewOutcome outcome(
            KeywordReviewOutcome.Decision decision,
            String riskLevel,
            String resultCode,
            List<Long> matchedRuleIds
    ) {
        return new KeywordReviewOutcome(
                decision,
                riskLevel,
                resultCode,
                reason(matchedRuleIds),
                matchedRuleIds
        );
    }

    private static String reason(List<Long> matchedRuleIds) {
        if (matchedRuleIds.isEmpty()) {
            return "自动关键词审核未命中活动规则";
        }
        String ids = matchedRuleIds.stream()
                .limit(MAX_REASON_RULE_IDS)
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        String suffix = matchedRuleIds.size() > MAX_REASON_RULE_IDS
                ? ",..."
                : "";
        return "自动关键词审核命中 "
                + matchedRuleIds.size()
                + " 条规则，规则 ID="
                + ids
                + suffix;
    }

    private static String normalizedSeverity(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return SEVERITIES.contains(value) ? value : null;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
