package top.pxczxn.community.web.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TeamSubmissionRequest(
        @NotNull Long sourceArticleId,
        @NotNull Long targetTeamId,
        Long supersedesSubmissionId,
        @NotBlank String idempotencyKey
) {
}
