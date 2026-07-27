package top.pxczxn.community.series.application;

public record SeriesReviewDecisionCommand(Integer expectedLockVersion, String comment) { }
