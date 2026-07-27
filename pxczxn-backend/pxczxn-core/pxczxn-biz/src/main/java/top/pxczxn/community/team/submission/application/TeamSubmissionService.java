package top.pxczxn.community.team.submission.application;

import java.util.List;

public interface TeamSubmissionService {
    TeamSubmissionView submit(Long actorUserId, CreateTeamSubmissionCommand command);
    List<TeamSubmissionView> mine(Long actorUserId);
    List<TeamSubmissionView> teamQueue(Long actorUserId, Long teamId);
    List<TeamSubmissionView> platformQueue();
    TeamSubmissionView teamApprove(Long actorUserId, Long submissionId, DecideTeamSubmissionCommand command);
    TeamSubmissionView teamRequestRevision(Long actorUserId, Long submissionId, DecideTeamSubmissionCommand command);
    TeamSubmissionView teamReject(Long actorUserId, Long submissionId, DecideTeamSubmissionCommand command);
    TeamSubmissionView platformApprove(Long adminId, Long submissionId, DecideTeamSubmissionCommand command);
    TeamSubmissionView platformRequestRevision(Long adminId, Long submissionId, DecideTeamSubmissionCommand command);
    TeamSubmissionView platformReject(Long adminId, Long submissionId, DecideTeamSubmissionCommand command);
}
