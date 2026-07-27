package top.pxczxn.community.web.team;

/**
 * Request for submitting a team application.
 */
public record SubmitTeamApplicationRequest(
        String teamName,
        String teamSlug,
        String description,
        String idempotencyKey
) {
}
