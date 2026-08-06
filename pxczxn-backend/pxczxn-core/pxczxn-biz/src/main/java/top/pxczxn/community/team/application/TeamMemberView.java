package top.pxczxn.community.team.application;

import java.time.LocalDateTime;

/**
 * Team member row for the workspace member list.
 *
 * <p>Carries the user profile inline so the member page does not have to call the workspace endpoint
 * as well and merge two payloads by user id.
 *
 * <p>{@code contributionCount} is the number of live articles this member authored inside the team
 * blog; {@code lastActiveAt} is the most recent update timestamp among those articles and is null
 * for members who have not written anything yet. Counts stay {@code Integer} so they serialise as
 * JSON numbers - only ids are turned into strings by the global Jackson config.
 */
public record TeamMemberView(
        Long userId,
        String displayName,
        String username,
        Long avatarFileId,
        String roleCode,
        LocalDateTime joinedAt,
        Integer contributionCount,
        LocalDateTime lastActiveAt
) {
}
