package top.pxczxn.community.report.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.chat.persistence.CommunityChatMessageMapper;
import top.pxczxn.community.report.model.CommunityReport;
import top.pxczxn.community.report.model.CommunityReportEvent;
import top.pxczxn.community.report.persistence.CommunityReportEventMapper;
import top.pxczxn.community.report.persistence.CommunityReportMapper;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class CommunityReportServiceImpl implements CommunityReportService {
    private final CommunityReportMapper reportMapper; private final CommunityReportEventMapper eventMapper;
    private final ArticleMapper articleMapper; private final CommunityMomentMapper momentMapper; private final CommunityCommentMapper commentMapper;
    private final BlogMapper blogMapper; private final CommunityUserMapper userMapper; private final TeamMapper teamMapper; private final CommunityChatMessageMapper chatMapper;
    private final ObjectMapper objectMapper;
    private final CommunityAbuseGuard abuseGuard;

    @Override @Transactional public CommunityReportView create(Long reporter, CreateCommunityReportCommand command) {
        if (reporter == null || command == null) throw new BusinessException(400, "举报信息无效");
        abuseGuard.check("USER:" + reporter, "REPORT_CREATE", 6, 300);
        String type = targetType(command.targetType()); Long target = positive(command.targetId(), "target");
        if ("USER".equals(type) && reporter.equals(target)) throw new BusinessException(400, "不能举报自己");
        requireTarget(type, target); abuseGuard.check("REPORT_TARGET:" + type + ":" + target, "REPORT_TARGET_BURST", 12, 600); String reason = required(command.reasonCode(), 40, "reason"); String evidence = evidence(command.evidenceJson()); LocalDateTime now = now();
        CommunityReport report = new CommunityReport(); report.setId(IdWorker.getId()); report.setReporterUserId(reporter); report.setTargetType(type); report.setTargetId(target); report.setReasonCode(reason); report.setDescription(optional(command.description(), 1000)); report.setEvidenceJson(evidence); report.setStatus("PENDING"); report.setLockVersion(0); report.setCreatedAt(now); report.setUpdatedAt(now);
        try { if (reportMapper.insert(report) != 1) throw new BusinessException(500, "举报提交失败"); }
        catch (DuplicateKeyException exception) { throw new BusinessException(409, "已存在相同原因的举报"); }
        event(report.getId(), "USER", reporter, "CREATED", null, "PENDING", now); return CommunityReportView.from(report);
    }
    @Override @Transactional(readOnly = true) public List<CommunityReportView> mine(Long reporter) { return reportMapper.findByReporter(reporter).stream().map(CommunityReportView::from).toList(); }
    @Override @Transactional(readOnly = true) public List<CommunityReportView> queue(String status, String targetType, Long targetId) { String value = status == null || status.isBlank() ? null : queueStatus(status); String type = StringUtils.hasText(targetType) ? targetType.trim().toUpperCase(Locale.ROOT) : null; return reportMapper.findQueue(value, type, targetId).stream().map(CommunityReportView::from).toList(); }

    @Override @Transactional(readOnly = true)
    public List<CommunityReportView> search(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) return List.of();
        String value = keyword.trim();
        int max = Math.max(1, Math.min(limit, 20));
        return reportMapper.selectList(Wrappers.<CommunityReport>lambdaQuery()
                        .and(w -> w
                                .apply("CAST(id AS CHAR) LIKE {0}", "%" + value + "%")
                                .or()
                                .apply("CAST(target_id AS CHAR) LIKE {0}", "%" + value + "%")
                                .or()
                                .like(CommunityReport::getTargetType, value)
                                .or()
                                .like(CommunityReport::getReasonCode, value)
                                .or()
                                .like(CommunityReport::getDescription, value))
                        .orderByDesc(CommunityReport::getCreatedAt)
                        .last("LIMIT " + max))
                .stream().map(CommunityReportView::from).toList();
    }
    @Override @Transactional public CommunityReportView claim(Long admin, Long id, Integer lock) { CommunityReport report = report(id); LocalDateTime now = now(); if (lock == null || reportMapper.claim(id, admin, lock, now) != 1) throw collision(); report.setStatus("ASSIGNED"); report.setAssigneeAdminId(admin); report.setLockVersion(lock + 1); event(id, "ADMIN", admin, "CLAIMED", "PENDING", "ASSIGNED", now); return CommunityReportView.from(report); }
    @Override @Transactional public CommunityReportView resolve(Long admin, Long id, Integer lock, String code, String note, boolean dismiss) { CommunityReport report = report(id); LocalDateTime now = now(); String status = dismiss ? "DISMISSED" : "RESOLVED"; if (lock == null || reportMapper.resolve(id, admin, status, required(code, 40, "resolution"), optional(note, 1000), lock, now) != 1) throw collision(); report.setStatus(status); report.setResolutionCode(code.trim().toUpperCase(Locale.ROOT)); report.setResolutionNote(optional(note, 1000)); report.setResolvedAt(now); report.setLockVersion(lock + 1); event(id, "ADMIN", admin, status, "ASSIGNED", status, now); return CommunityReportView.from(report); }
    private CommunityReport report(Long id) { CommunityReport value = reportMapper.selectById(positive(id, "report")); if (value == null) throw new BusinessException(404, "举报记录不存在"); return value; }
    private void requireTarget(String type, Long id) { boolean found = switch (type) { case "ARTICLE" -> articleMapper.selectById(id) != null; case "MOMENT" -> momentMapper.selectById(id) != null; case "COMMENT" -> commentMapper.selectById(id) != null; case "BLOG" -> blogMapper.selectById(id) != null; case "USER" -> userMapper.selectById(id) != null; case "TEAM" -> teamMapper.selectById(id) != null; case "CHAT" -> chatMapper.selectById(id) != null; default -> false; }; if (!found) throw new BusinessException(404, "举报目标不存在"); }
    private void event(Long reportId, String actorType, Long actor, String type, String before, String after, LocalDateTime now) { CommunityReportEvent event = new CommunityReportEvent(); event.setReportId(reportId); event.setActorType(actorType); event.setActorId(actor); event.setEventType(type); event.setBeforeSnapshot(before == null ? null : "{\"status\":\"" + before + "\"}"); event.setAfterSnapshot("{\"status\":\"" + after + "\"}"); event.setOccurredAt(now); eventMapper.insert(event); }
    private String evidence(String value) { if (value == null || value.isBlank()) return null; if (value.length() > 4000) throw new BusinessException(400, "证据内容过长"); try { objectMapper.readTree(value); return value; } catch (Exception exception) { throw new BusinessException(400, "证据格式不正确"); } }
    private static String targetType(String value) { String type = required(value, 24, "target type").toUpperCase(Locale.ROOT); if (!List.of("ARTICLE","MOMENT","COMMENT","BLOG","USER","TEAM","CHAT").contains(type)) throw new BusinessException(400, "无效的举报目标类型"); return type; }
    private static String queueStatus(String value) { String status = value.trim().toUpperCase(Locale.ROOT); if (!List.of("PENDING","ASSIGNED").contains(status)) throw new BusinessException(400, "无效的队列状态"); return status; }
    private static String required(String value, int max, String label) { String result = optional(value, max); if (result == null) throw new BusinessException(400, "缺少必填参数"); return result.toUpperCase(Locale.ROOT); }
    private static String optional(String value, int max) { if (value == null) return null; String result = value.trim(); if (result.isEmpty()) return null; if (result.length() > max) throw new BusinessException(400, "输入内容过长"); return result; }
    private static Long positive(Long value, String label) { if (value == null || value <= 0) throw new BusinessException(400, "无效的" + label + "编号"); return value; }
    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static BusinessException collision() { return new BusinessException(409, "举报状态已变更，请刷新后重试"); }
}
