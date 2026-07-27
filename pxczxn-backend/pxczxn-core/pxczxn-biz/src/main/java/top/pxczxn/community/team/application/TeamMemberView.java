package top.pxczxn.community.team.application;

import java.time.LocalDateTime;

public record TeamMemberView(Long userId, String roleCode, LocalDateTime joinedAt) {
}
