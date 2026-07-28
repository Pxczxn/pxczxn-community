package top.pxczxn.community.sanction.application;

import java.time.LocalDateTime;
import java.util.List;

public interface CommunitySanctionService {
    SanctionView issue(Long adminId, Long targetUserId, String sanctionType, String reasonCode, String reasonNote, Long sourceReportId, LocalDateTime expiresAt);
    SanctionView revoke(Long adminId, Long sanctionId, String note);
    List<SanctionView> mine(Long userId);
    List<SanctionView> activeFor(Long userId);
    void requireActionAllowed(Long userId, SanctionAction action);
    void expireDueSanctions();
}
