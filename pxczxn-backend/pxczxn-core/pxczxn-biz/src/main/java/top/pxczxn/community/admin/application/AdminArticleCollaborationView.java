package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;

public record AdminArticleCollaborationView(Long id, Long articleId, String articleTitle,
                                            Long userId, String username, String displayName,
                                            String contributionType, boolean canEdit,
                                            Integer attributionOrder, String status,
                                            LocalDateTime acceptedAt, LocalDateTime revokedAt,
                                            LocalDateTime createdAt) { }
