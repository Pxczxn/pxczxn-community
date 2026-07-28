package top.pxczxn.community.appeal.application;

import top.pxczxn.community.appeal.model.CommunityAppeal;

import java.time.LocalDateTime;

public record CommunityAppealView(Long id, Long reportId, Long appellantUserId, String appealReason, String evidenceJson, String status, Long reviewerAdminId, String reviewNote, Integer lockVersion, LocalDateTime createdAt, LocalDateTime reviewedAt) {
    static CommunityAppealView from(CommunityAppeal value) {
        return new CommunityAppealView(value.getId(), value.getReportId(), value.getAppellantUserId(), value.getAppealReason(), value.getEvidenceJson(), value.getStatus(), value.getReviewerAdminId(), value.getReviewNote(), value.getLockVersion(), value.getCreatedAt(), value.getReviewedAt());
    }
}
