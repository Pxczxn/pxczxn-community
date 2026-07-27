package top.pxczxn.community.team.application;

import lombok.Getter;
import lombok.Setter;

/**
 * Command for submitting a team application.
 */
@Getter
@Setter
public class SubmitTeamApplicationCommand {

    private Long applicantUserId;

    private String teamName;

    private String teamSlug;

    private String description;

    private String idempotencyKey;
}
