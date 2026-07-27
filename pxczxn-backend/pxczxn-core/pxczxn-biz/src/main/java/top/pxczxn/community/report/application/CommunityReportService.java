package top.pxczxn.community.report.application;
import java.util.List;
public interface CommunityReportService {
    CommunityReportView create(Long reporterUserId, CreateCommunityReportCommand command);
    List<CommunityReportView> mine(Long reporterUserId);
    List<CommunityReportView> queue(String status);
    CommunityReportView claim(Long adminId, Long reportId, Integer expectedLockVersion);
    CommunityReportView resolve(Long adminId, Long reportId, Integer expectedLockVersion, String resolutionCode, String resolutionNote, boolean dismiss);
}
