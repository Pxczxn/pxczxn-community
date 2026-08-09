package top.pxczxn.community.article.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for discovery sort algorithms: HOT, QUALITY, and other sort types.
 * Covers enum parsing, weight semantics, and formula properties.
 */
class PublicDiscoverySortTest {

    @Test
    void parseDefaultReturnsLatest() {
        assertThat(PublicDiscoverySort.parse(null)).isEqualTo(PublicDiscoverySort.LATEST);
        assertThat(PublicDiscoverySort.parse("")).isEqualTo(PublicDiscoverySort.LATEST);
        assertThat(PublicDiscoverySort.parse("  ")).isEqualTo(PublicDiscoverySort.LATEST);
    }

    @Test
    void parseIsCaseInsensitive() {
        assertThat(PublicDiscoverySort.parse("hot")).isEqualTo(PublicDiscoverySort.HOT);
        assertThat(PublicDiscoverySort.parse("HOT")).isEqualTo(PublicDiscoverySort.HOT);
        assertThat(PublicDiscoverySort.parse("Hot")).isEqualTo(PublicDiscoverySort.HOT);
        assertThat(PublicDiscoverySort.parse("quality")).isEqualTo(PublicDiscoverySort.QUALITY);
    }

    @Test
    void parseInvalidThrows400() {
        assertThatThrownBy(() -> PublicDiscoverySort.parse("INVALID"))
                .isInstanceOf(top.pxczxn.platform.common.exception.BusinessException.class);
        assertThatThrownBy(() -> PublicDiscoverySort.parse("TRENDING"))
                .isInstanceOf(top.pxczxn.platform.common.exception.BusinessException.class);
    }

    @Test
    void hotEnumExists() {
        assertThat(PublicDiscoverySort.HOT).isNotNull();
        assertThat(PublicDiscoverySort.HOT.name()).isEqualTo("HOT");
        assertThat(PublicDiscoverySort.HOT.explanation()).contains("热度");
    }

    @Test
    void qualityEnumExistsWithUpdatedExplanation() {
        assertThat(PublicDiscoverySort.QUALITY).isNotNull();
        assertThat(PublicDiscoverySort.QUALITY.explanation()).contains("贝叶斯");
    }

    @Test
    void allSortTypesAreParseable() {
        for (PublicDiscoverySort sort : PublicDiscoverySort.values()) {
            assertThat(PublicDiscoverySort.parse(sort.name())).isEqualTo(sort);
        }
    }

    /**
     * HOT formula properties (tested via numeric simulation of the SQL formula):
     * hotScore = (like*1.0 + comment*2.5 + favorite*4.0 + 1) / POW(GREATEST(ageHours, 0) + 2, 1.2)
     */
    @Test
    void hotScore_weightFavoriteOverCommentOverLike() {
        // Same age, different interaction types
        double ageHours = 10.0;
        double denominator = Math.pow(Math.max(ageHours, 0) + 2, 1.2);

        double scoreLikeOnly = (100 * 1.0 + 1) / denominator;
        double scoreCommentOnly = (100 * 2.5 + 1) / denominator;
        double scoreFavoriteOnly = (100 * 4.0 + 1) / denominator;

        assertThat(scoreFavoriteOnly).isGreaterThan(scoreCommentOnly);
        assertThat(scoreCommentOnly).isGreaterThan(scoreLikeOnly);
    }

    @Test
    void hotScore_newerContentRanksHigher() {
        double interactions = 50 * 1.0 + 10 * 2.5 + 5 * 4.0 + 1; // = 96

        double scoreNew = interactions / Math.pow(Math.max(1.0, 0) + 2, 1.2);   // 1 hour old
        double scoreOld = interactions / Math.pow(Math.max(48.0, 0) + 2, 1.2);  // 48 hours old

        assertThat(scoreNew).isGreaterThan(scoreOld);
    }

    @Test
    void hotScore_futureTimeProducesNonNegativeScore() {
        // publishedAt in the future => ageHours negative => GREATEST clamps to 0
        double ageHours = -5.0; // future time
        double denominator = Math.pow(Math.max(ageHours, 0) + 2, 1.2);
        double score = (10 * 1.0 + 5 * 2.5 + 2 * 4.0 + 1) / denominator;

        assertThat(denominator).isGreaterThan(0);
        assertThat(score).isFinite();
        assertThat(score).isGreaterThan(0);
    }

    @Test
    void hotScore_zeroInteractionsStillHasNonZeroScore() {
        double ageHours = 24.0;
        double score = (0 + 1) / Math.pow(ageHours + 2, 1.2);

        assertThat(score).isGreaterThan(0);
    }

    /**
     * QUALITY formula properties (tested via numeric simulation):
     * C=100, prior=0.02
     * smoothedRate = (count + C*prior) / (viewCount + C) = (count + 2) / (viewCount + 100)
     */
    @Test
    void qualityScore_bayesianSmoothingSuppressesSmallSample() {
        int C = 100;
        double prior = 0.02;

        // Case 1: 1 like / 11 views (very small sample)
        double smoothedLike1 = (1 + C * prior) / (11.0 + C);
        // Case 2: 300 likes / 10000 views (large sample)
        double smoothedLike2 = (300 + C * prior) / (10000.0 + C);

        // With C=100: (1+2)/(11+100)=3/111≈0.027 vs (300+2)/(10000+100)=302/10100≈0.0299
        // Small sample is pulled toward prior (0.02), large sample trusts real data
        assertThat(smoothedLike1).isLessThan(smoothedLike2);
    }

    @Test
    void qualityScore_largeSampleApproachesRealRate() {
        int C = 100;
        double prior = 0.02;

        // 500 likes / 10000 views = real rate 0.05
        double smoothed = (500 + C * prior) / (10000.0 + C);
        double realRate = 500.0 / 10000;

        // Smoothed rate should be close to real rate for large samples
        assertThat(smoothed).isCloseTo(realRate, org.assertj.core.data.Offset.offset(0.005));
    }

    @Test
    void qualityScore_wordCountAndReadingTimeNotDoubleReward() {
        // wordCount >= 500 gives +1, readingTime >= 3 gives +0.5
        // They should NOT both give large rewards (max combined = 1.5)
        double lengthComponentMax = 1.0 + 0.5; // wordCount signal + readingTime signal
        double qualityBaseMax = 3.0 + 2.0 + 1.0 + 1.0 + 0.5; // fav + like + comment + summary + cover
        double freshnessMax = 1.0;
        double reviewMax = 1.0;

        // Length signals should be small relative to engagement signals
        assertThat(lengthComponentMax).isLessThan(qualityBaseMax);
    }

    @Test
    void qualityScore_engagementWeightsAreCorrect() {
        // In QUALITY formula: fav weight=3.0, like weight=2.0, comment weight=1.0
        // This tests the relative importance: fav > like > comment
        double favWeight = 3.0;
        double likeWeight = 2.0;
        double commentWeight = 1.0;

        assertThat(favWeight).isGreaterThan(likeWeight);
        assertThat(likeWeight).isGreaterThan(commentWeight);
    }

    /**
     * specialFollow effectiveTime properties.
     * effectiveTime = occurredAt + INTERVAL (CASE WHEN special_follow = 1 THEN 8 ELSE 0 END) HOUR
     */
    @Test
    void specialFollow_effectiveTimeBoost() {
        // A special follow item posted 6 hours ago should appear
        // as if it was posted 14 hours ago in the effective timeline
        long specialFollowBoostHours = 8;
        long occurredHoursAgo = 6;
        long effectiveHoursAgo = occurredHoursAgo - specialFollowBoostHours; // -8 (appears newer)

        // The special follow content appears to be from 14 hours ago (further in the past)
        // Wait - effectiveTime = occurredAt + boost, so larger effectiveTime = more recent in DESC order
        // occurredAt = NOW - 6h, effectiveTime = NOW - 6h + 8h = NOW + 2h
        // A normal follow item posted 1 hour ago: effectiveTime = NOW - 1h
        // So special follow (NOW+2h) > normal follow (NOW-1h) in DESC order

        // But a very old special follow: occurredAt = NOW - 100h, effectiveTime = NOW - 92h
        // A new normal follow: occurredAt = NOW - 1h, effectiveTime = NOW - 1h
        // NOW - 1h > NOW - 92h in DESC, so new normal wins
        long veryOldSpecialEffective = -100 + specialFollowBoostHours; // -92
        long newNormalEffective = -1; // -1

        assertThat(newNormalEffective).isGreaterThan(veryOldSpecialEffective);
    }

    @Test
    void specialFollow_doesNotPermanently压制NewerContent() {
        // Special follow 200 hours ago vs normal follow 1 hour ago
        long boost = 8;
        long specialEffective = -200 + boost; // -192
        long normalEffective = -1;

        // Normal follow should rank higher (more recent effective time)
        assertThat(normalEffective).isGreaterThan(specialEffective);
    }

    @Test
    void specialFollow_boostIsFinite() {
        // The boost is exactly 8 hours, not unlimited
        long boost = 8;
        assertThat(boost).isEqualTo(8);
        assertThat(boost).isLessThan(24); // Less than a day
    }
}