package top.pxczxn.community.team.application;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * View model for team application.
 */
@Getter
@Setter
public class TeamApplicationView {

    private Long id;

    private Long applicantUserId;

    private String teamName;

    private String teamSlug;

    private String description;

    private String status;

    private Long reviewerUserId;

    private String reviewComment;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
