package top.pxczxn.community.team.submission.application;

public record CreateTeamSubmissionCommand(
        Long sourceArticleId,
        Long targetTeamId,
        Long supersedesSubmissionId,
        String idempotencyKey
) {
}
