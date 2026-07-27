package top.pxczxn.community.collaboration.application;

import java.util.List;

public interface ArticleCollaborationService {
    ArticleCollaborationInvitationView invite(Long actorUserId, Long articleId, InviteArticleCollaboratorCommand command);
    List<ArticleCollaborationInvitationView> myPendingInvitations(Long actorUserId);
    List<ArticleCollaboratorView> collaborators(Long actorUserId, Long articleId);
    ArticleCollaboratorView accept(Long actorUserId, Long invitationId, RespondArticleCollaborationCommand command);
    void reject(Long actorUserId, Long invitationId, RespondArticleCollaborationCommand command);
    void cancel(Long actorUserId, Long invitationId, RespondArticleCollaborationCommand command);
    void revoke(Long actorUserId, Long articleId, Long collaboratorId, Integer expectedLockVersion);
}
