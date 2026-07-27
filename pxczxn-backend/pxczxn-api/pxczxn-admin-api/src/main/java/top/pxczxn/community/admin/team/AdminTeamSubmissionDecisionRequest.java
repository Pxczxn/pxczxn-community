package top.pxczxn.community.admin.team;

import jakarta.validation.constraints.NotNull;

public record AdminTeamSubmissionDecisionRequest(@NotNull Integer expectedLockVersion, String comment) {
}
