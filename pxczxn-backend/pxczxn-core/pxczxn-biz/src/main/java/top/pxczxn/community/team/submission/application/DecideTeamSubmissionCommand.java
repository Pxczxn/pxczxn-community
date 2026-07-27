package top.pxczxn.community.team.submission.application;

public record DecideTeamSubmissionCommand(Integer expectedLockVersion, String comment) {
}
