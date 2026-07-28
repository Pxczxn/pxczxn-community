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

    private static final Set<String> CONTENT_TYPES = Set.of("ARTICLE", "COMMENT", "MOMENT");
    private static final Set<String> ACTIONS = Set.of("BLOCK", "MANUAL_REVIEW", "WARN");
    private static final int MAX_REASON_RULE_IDS = 30;

    private final ContentKeywordRuleMapper ruleMapper;

    public KeywordReviewOutcome review(String title, String summary, String plainText) {
        return review("ARTICLE", title, summary, plainText);
    }

    public KeywordReviewOutcome review(String contentType, String title, String summary, String plainText) {
        String type = normalizeContentType(contentType);
        String normalizedContent = normalizeForMatching(String.join(
                "\n", valueOrEmpty(title), valueOrEmpty(summary), valueOrEmpty(plainText)
        ));
        List<ContentKeywordRule> rules = ruleMapper.selectList(
                Wrappers.<ContentKeywordRule>lambdaQuery()
                        .eq(ContentKeywordRule::getStatus, "ACTIVE")
                        .isNull(ContentKeywordRule::getDeletedAt)
                        .orderByAsc(ContentKeywordRule::getSortOrder)
                        .orderByAsc(ContentKeywordRule::getId)
        );
        List<Long> matchedIds = new ArrayList<>();
        String strongestAction = null;
        String strongestRisk = "LOW";
        for (ContentKeywordRule rule : rules) {
            if (!appliesTo(rule.getContentScopes(), type)) continue;
            String keyword = normalizeForMatching(rule.getNormalizedKeyword() == null ? rule.getKeyword() : rule.getNormalizedKeyword());
            if (keyword.isBlank() || !normalizedContent.contains(keyword)) continue;
            matchedIds.add(rule.getId());
            String action = action(rule);
            if (actionPriority(action) > actionPriority(strongestAction)) strongestAction = action;
            String risk = risk(rule, action);
            if (riskPriority(risk) > riskPriority(strongestRisk)) strongestRisk = risk;
        }
        if ("BLOCK".equals(strongestAction)) return outcome(KeywordReviewOutcome.Decision.BLOCK, strongestRisk, "AUTO_BLOCKED_KEYWORD", matchedIds);
        if ("MANUAL_REVIEW".equals(strongestAction)) return outcome(KeywordReviewOutcome.Decision.MANUAL_REVIEW, strongestRisk, "AUTO_ESCALATED_KEYWORD", matchedIds);
        if ("WARN".equals(strongestAction)) return outcome(KeywordReviewOutcome.Decision.AUTO_APPROVE, strongestRisk, "AUTO_APPROVED_WITH_WARNING", matchedIds);
        return outcome(KeywordReviewOutcome.Decision.AUTO_APPROVE, "LOW", "AUTO_APPROVED", List.of());
    }

    static String normalizeForMatching(String raw) {
        String normalized = Normalizer.normalize(valueOrEmpty(raw), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private static boolean appliesTo(String scopes, String type) {
        String normalized = scopes == null || scopes.isBlank() ? "ARTICLE,COMMENT,MOMENT" : scopes.toUpperCase(Locale.ROOT);
        return normalized.equals("ALL") || List.of(normalized.split(",")).stream().map(String::trim).anyMatch(type::equals);
    }

    private static String normalizeContentType(String raw) {
        String type = raw == null ? "ARTICLE" : raw.trim().toUpperCase(Locale.ROOT);
        if (!CONTENT_TYPES.contains(type)) throw new IllegalArgumentException("Unsupported content type");
        return type;
    }

    private static String action(ContentKeywordRule rule) {
        String configured = rule.getHitAction() == null ? "" : rule.getHitAction().trim().toUpperCase(Locale.ROOT);
        if (ACTIONS.contains(configured)) return configured;
        return switch (String.valueOf(rule.getSeverity()).toUpperCase(Locale.ROOT)) {
            case "BLOCK" -> "BLOCK";
            case "REVIEW" -> "MANUAL_REVIEW";
            default -> "WARN";
        };
    }

    private static String risk(ContentKeywordRule rule, String action) {
        String configured = rule.getRiskLevel() == null ? "" : rule.getRiskLevel().trim().toUpperCase(Locale.ROOT);
        if (Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(configured)) return configured;
        return switch (action) { case "BLOCK" -> "CRITICAL"; case "MANUAL_REVIEW" -> "HIGH"; default -> "MEDIUM"; };
    }

    private static int actionPriority(String action) { return switch (action == null ? "" : action) { case "BLOCK" -> 3; case "MANUAL_REVIEW" -> 2; case "WARN" -> 1; default -> 0; }; }
    private static int riskPriority(String risk) { return switch (risk) { case "CRITICAL" -> 4; case "HIGH" -> 3; case "MEDIUM" -> 2; default -> 1; }; }
    private static KeywordReviewOutcome outcome(KeywordReviewOutcome.Decision decision, String risk, String code, List<Long> ids) { return new KeywordReviewOutcome(decision, risk, code, reason(ids), ids); }
    private static String reason(List<Long> ids) { return ids.isEmpty() ? "No active keyword rule matched" : "Matched keyword rules: " + ids.stream().limit(MAX_REASON_RULE_IDS).map(String::valueOf).reduce((a,b)->a+","+b).orElse(""); }
    private static String valueOrEmpty(String value) { return value == null ? "" : value; }
}
