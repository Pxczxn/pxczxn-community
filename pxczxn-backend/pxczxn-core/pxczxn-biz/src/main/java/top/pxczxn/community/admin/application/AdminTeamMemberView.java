package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;

public record AdminTeamMemberView(Long id, Long userId, String username, String displayName,
                                  String roleCode, Long invitedByUserId, LocalDateTime joinedAt,
                                  Integer lockVersion) { }
