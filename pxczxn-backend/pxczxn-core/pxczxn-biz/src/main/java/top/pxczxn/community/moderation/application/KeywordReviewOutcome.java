package top.pxczxn.community.moderation.application;

import java.util.List;

public record KeywordReviewOutcome(
        Decision decision,
        String riskLevel,
        String resultCode,
        String resultReason,
        List<Long> matchedRuleIds
) {

    public enum Decision {
        AUTO_APPROVE,
        MANUAL_REVIEW,
        BLOCK
    }

    public KeywordReviewOutcome {
        matchedRuleIds = matchedRuleIds == null
                ? List.of()
                : List.copyOf(matchedRuleIds);
    }
}
