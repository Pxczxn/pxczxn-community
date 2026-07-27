package top.pxczxn.community.report.application;
import top.pxczxn.community.report.model.CommunityReport;
import java.time.LocalDateTime;
public record CommunityReportView(Long id, Long reporterUserId, String targetType, Long targetId, String reasonCode, String description, String evidenceJson, String status, Long assigneeAdminId, String resolutionCode, String resolutionNote, LocalDateTime resolvedAt, Integer lockVersion, LocalDateTime createdAt, LocalDateTime updatedAt) {
    static CommunityReportView from(CommunityReport value) { return new CommunityReportView(value.getId(), value.getReporterUserId(), value.getTargetType(), value.getTargetId(), value.getReasonCode(), value.getDescription(), value.getEvidenceJson(), value.getStatus(), value.getAssigneeAdminId(), value.getResolutionCode(), value.getResolutionNote(), value.getResolvedAt(), value.getLockVersion(), value.getCreatedAt(), value.getUpdatedAt()); }
}
