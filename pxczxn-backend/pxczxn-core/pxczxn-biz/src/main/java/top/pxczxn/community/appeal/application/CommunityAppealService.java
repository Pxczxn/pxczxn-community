package top.pxczxn.community.appeal.application;

import java.util.List;

public interface CommunityAppealService {
    CommunityAppealView submit(Long appellantUserId, Long reportId, String reason, String evidenceJson);
    AppealContextView context(Long appellantUserId, Long reportId);
    List<CommunityAppealView> mine(Long appellantUserId);
    List<CommunityAppealView> queue();
    CommunityAppealView review(Long adminId, Long appealId, Integer expectedLockVersion, boolean revoke, String note);
}
