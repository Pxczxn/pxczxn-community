package top.pxczxn.community.moderation.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.moderation.model.ContentKeywordRule;
import top.pxczxn.community.moderation.persistence.ContentKeywordRuleMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArticleKeywordReviewEngineTest {

    private ContentKeywordRuleMapper ruleMapper;
    private ArticleKeywordReviewEngine engine;

    @BeforeEach
    void setUp() {
        ruleMapper = mock(ContentKeywordRuleMapper.class);
        engine = new ArticleKeywordReviewEngine(ruleMapper);
    }

    @Test
    void noMatchIsAutomaticallyApprovedAtLowRisk() {
        when(ruleMapper.selectList(any())).thenReturn(List.of());

        KeywordReviewOutcome outcome =
                engine.review("普通标题", null, "普通正文");

        assertThat(outcome.decision())
                .isEqualTo(KeywordReviewOutcome.Decision.AUTO_APPROVE);
        assertThat(outcome.riskLevel()).isEqualTo("LOW");
        assertThat(outcome.resultCode()).isEqualTo("AUTO_APPROVED");
        assertThat(outcome.matchedRuleIds()).isEmpty();
    }

    @Test
    void warnMatchIsApprovedWithMediumRisk() {
        when(ruleMapper.selectList(any()))
                .thenReturn(List.of(rule(11L, "review notice", "WARN")));

        KeywordReviewOutcome outcome =
                engine.review("Review Notice", null, "正文");

        assertThat(outcome.decision())
                .isEqualTo(KeywordReviewOutcome.Decision.AUTO_APPROVE);
        assertThat(outcome.riskLevel()).isEqualTo("MEDIUM");
        assertThat(outcome.resultCode())
                .isEqualTo("AUTO_APPROVED_WITH_WARNING");
        assertThat(outcome.matchedRuleIds()).containsExactly(11L);
    }

    @Test
    void reviewMatchEscalatesToManualQueue() {
        when(ruleMapper.selectList(any()))
                .thenReturn(List.of(rule(12L, "manual phrase", "REVIEW")));

        KeywordReviewOutcome outcome =
                engine.review("标题", null, "Contains manual phrase here");

        assertThat(outcome.decision())
                .isEqualTo(KeywordReviewOutcome.Decision.MANUAL_REVIEW);
        assertThat(outcome.riskLevel()).isEqualTo("HIGH");
        assertThat(outcome.resultCode())
                .isEqualTo("AUTO_ESCALATED_KEYWORD");
    }

    @Test
    void blockWinsAndNfkcNormalizationPreventsWidthBypass() {
        when(ruleMapper.selectList(any())).thenReturn(List.of(
                rule(13L, "danger phrase", "REVIEW"),
                rule(14L, "danger phrase", "BLOCK")
        ));

        KeywordReviewOutcome outcome =
                engine.review("ＤＡＮＧＥＲ　ＰＨＲＡＳＥ", null, "正文");

        assertThat(outcome.decision())
                .isEqualTo(KeywordReviewOutcome.Decision.BLOCK);
        assertThat(outcome.riskLevel()).isEqualTo("CRITICAL");
        assertThat(outcome.resultCode()).isEqualTo("AUTO_BLOCKED_KEYWORD");
        assertThat(outcome.matchedRuleIds()).containsExactly(13L, 14L);
        assertThat(outcome.resultReason()).doesNotContain("danger phrase");
    }

    private static ContentKeywordRule rule(
            Long id,
            String keyword,
            String severity
    ) {
        ContentKeywordRule rule = new ContentKeywordRule();
        rule.setId(id);
        rule.setKeyword(keyword);
        rule.setNormalizedKeyword(keyword);
        rule.setSeverity(severity);
        rule.setStatus("ACTIVE");
        rule.setSortOrder(0);
        return rule;
    }
}
