package top.pxczxn.community.web.article;

import top.pxczxn.community.collaboration.application.ArticleCollaborationInvitationView;
import top.pxczxn.community.collaboration.application.ArticleCollaboratorView;
import java.time.LocalDateTime;

public record ArticleCollaborationResponse(String id, String articleId, String userId, String username, String displayName, String contributionType, boolean canEdit, Integer attributionOrder, String status, Integer lockVersion, LocalDateTime createdAt, LocalDateTime expiresAt) {
    static ArticleCollaborationResponse invitation(ArticleCollaborationInvitationView v) { return new ArticleCollaborationResponse(id(v.id()), id(v.articleId()), id(v.inviteeUserId()), null, null, v.contributionType(), v.canEdit(), v.attributionOrder(), v.status(), v.lockVersion(), v.createdAt(), v.expiresAt()); }
    static ArticleCollaborationResponse collaborator(ArticleCollaboratorView v) { return new ArticleCollaborationResponse(id(v.id()), id(v.articleId()), id(v.userId()), v.username(), v.displayName(), v.contributionType(), v.canEdit(), v.attributionOrder(), "ACCEPTED", v.lockVersion(), v.acceptedAt(), null); }
    private static String id(Long value) { return value == null ? null : value.toString(); }
}
