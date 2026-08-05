package top.pxczxn.community.team.application;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One membership card on the "我的团队" tab and one entry of the workspace team switcher.
 *
 * <p>Identifier fields stay {@code Long} on purpose: {@code JacksonConfig} serializes them through
 * {@code ToStringSerializer} so the frontend receives strings and never loses precision.
 * Counter fields are {@code Integer} for exactly the same reason inverted - they must stay real JSON
 * numbers so badges can render arithmetic without parsing quoted values.</p>
 */
public record MyTeamView(
        Long teamId,
        Long blogId,
        String name,
        String slug,
        String summary,
        Long avatarFileId,
        Long backgroundFileId,
        String viewerRole,
        List<String> capabilities,
        List<String> permissions,
        Integer memberCount,
        Integer articleCount,
        Integer seriesCount,
        Integer followerCount,
        Integer pendingSubmissionCount,
        Integer revisionRequiredCount,
        LocalDateTime joinedAt,
        LocalDateTime updatedAt) {
}
