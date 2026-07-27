package top.pxczxn.community.web.team;

import jakarta.validation.constraints.NotNull;

public record TeamSubmissionDecisionRequest(@NotNull Integer expectedLockVersion, String comment) {
}
