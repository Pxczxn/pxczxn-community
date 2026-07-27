package top.pxczxn.community.collaboration.application;

public record InviteArticleCollaboratorCommand(Long inviteeUserId, String contributionType, Boolean canEdit, Integer attributionOrder, String message, String idempotencyKey) {
}
