package top.pxczxn.community.collaboration.application;

import top.pxczxn.community.collaboration.model.ArticleCollaborationInvitation;
import java.time.LocalDateTime;

public record ArticleCollaborationInvitationView(Long id, Long articleId, Long inviteeUserId, Long invitedByUserId, String contributionType, boolean canEdit, Integer attributionOrder, String message, String status, LocalDateTime expiresAt, LocalDateTime respondedAt, Integer lockVersion, LocalDateTime createdAt) {
    static ArticleCollaborationInvitationView from(ArticleCollaborationInvitation value) { return new ArticleCollaborationInvitationView(value.getId(), value.getArticleId(), value.getInviteeUserId(), value.getInvitedByUserId(), value.getContributionType(), Boolean.TRUE.equals(value.getCanEdit()), value.getAttributionOrder(), value.getMessage(), value.getStatus(), value.getExpiresAt(), value.getRespondedAt(), value.getLockVersion(), value.getCreatedAt()); }
}
