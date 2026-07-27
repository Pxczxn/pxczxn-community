package top.pxczxn.community.collaboration.application;

import top.pxczxn.community.collaboration.model.ArticleCollaborator;

import java.time.LocalDateTime;

public record ArticleCollaboratorView(Long id, Long articleId, Long userId, String username, String displayName, String contributionType, boolean canEdit, Integer attributionOrder, LocalDateTime acceptedAt, Integer lockVersion) {
    static ArticleCollaboratorView from(ArticleCollaborator value, String username, String displayName) { return new ArticleCollaboratorView(value.getId(), value.getArticleId(), value.getUserId(), username, displayName, value.getContributionType(), Boolean.TRUE.equals(value.getCanEdit()), value.getAttributionOrder(), value.getAcceptedAt(), value.getLockVersion()); }
}
