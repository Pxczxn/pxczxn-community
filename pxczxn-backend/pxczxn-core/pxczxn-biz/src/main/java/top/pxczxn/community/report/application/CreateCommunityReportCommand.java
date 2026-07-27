package top.pxczxn.community.report.application;
public record CreateCommunityReportCommand(String targetType, Long targetId, String reasonCode, String description, String evidenceJson) { }
