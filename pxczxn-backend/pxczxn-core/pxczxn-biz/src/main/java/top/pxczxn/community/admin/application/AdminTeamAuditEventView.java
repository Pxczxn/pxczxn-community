package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;

public record AdminTeamAuditEventView(Long id, Long actorUserId, String eventType, String targetType,
                                      Long targetId, LocalDateTime occurredAt) { }
