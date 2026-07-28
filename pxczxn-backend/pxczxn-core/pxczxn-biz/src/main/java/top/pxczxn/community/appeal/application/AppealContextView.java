package top.pxczxn.community.appeal.application;

public record AppealContextView(Long reportId, String targetType, Long targetId, String resolutionCode, String resolutionNote, Integer lockVersion) {
}
