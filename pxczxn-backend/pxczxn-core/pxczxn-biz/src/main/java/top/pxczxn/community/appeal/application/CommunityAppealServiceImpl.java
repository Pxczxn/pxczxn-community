package top.pxczxn.community.appeal.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.appeal.model.CommunityAppeal;
import top.pxczxn.community.appeal.model.CommunityAppealEvent;
import top.pxczxn.community.appeal.persistence.CommunityAppealEventMapper;
import top.pxczxn.community.appeal.persistence.CommunityAppealMapper;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.chat.persistence.CommunityChatMessageMapper;
import top.pxczxn.community.report.model.CommunityReport;
import top.pxczxn.community.report.model.CommunityReportEvent;
import top.pxczxn.community.report.persistence.CommunityReportEventMapper;
import top.pxczxn.community.report.persistence.CommunityReportMapper;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service @RequiredArgsConstructor
public class CommunityAppealServiceImpl implements CommunityAppealService {
    private final CommunityAppealMapper appealMapper; private final CommunityAppealEventMapper eventMapper;
    private final CommunityReportMapper reportMapper; private final CommunityReportEventMapper reportEventMapper;
    private final ArticleMapper articleMapper; private final CommunityMomentMapper momentMapper; private final CommunityCommentMapper commentMapper;
    private final BlogMapper blogMapper; private final TeamMapper teamMapper; private final CommunityChatMessageMapper chatMapper;
    private final ObjectMapper objectMapper;

    @Override @Transactional public CommunityAppealView submit(Long appellant, Long reportId, String reason, String evidenceJson) {
        CommunityReport report = report(reportId); if (!"RESOLVED".equals(report.getStatus())) throw new BusinessException(409, "只有已解决的举报才能申诉");
        if (!owns(appellant, report)) throw new BusinessException(403, "只有被举报方可以申诉");
        CommunityAppeal appeal = new CommunityAppeal(); LocalDateTime now = now(); appeal.setId(IdWorker.getId()); appeal.setReportId(report.getId()); appeal.setAppellantUserId(appellant); appeal.setAppealReason(required(reason, 1000, "appeal reason")); appeal.setEvidenceJson(evidence(evidenceJson)); appeal.setStatus("PENDING"); appeal.setLockVersion(0); appeal.setCreatedAt(now); appeal.setUpdatedAt(now);
        try { if (appealMapper.insert(appeal) != 1) throw new BusinessException(500, "申诉提交失败"); } catch (DuplicateKeyException exception) { throw new BusinessException(409, "该举报已存在申诉"); }
        event(appeal.getId(), "USER", appellant, "CREATED", null, "PENDING", now); return CommunityAppealView.from(appeal);
    }
    @Override @Transactional(readOnly = true) public List<CommunityAppealView> mine(Long appellant) { return appealMapper.mine(appellant).stream().map(CommunityAppealView::from).toList(); }
    @Override @Transactional(readOnly = true) public AppealContextView context(Long appellant, Long reportId) { CommunityReport report = report(reportId); if (!"RESOLVED".equals(report.getStatus()) || !owns(appellant, report)) throw new BusinessException(404, "可申诉的举报不存在"); return new AppealContextView(report.getId(), report.getTargetType(), report.getTargetId(), report.getResolutionCode(), report.getResolutionNote(), report.getLockVersion()); }
    @Override @Transactional(readOnly = true) public List<CommunityAppealView> queue() { return appealMapper.queue().stream().map(CommunityAppealView::from).toList(); }
    @Override @Transactional public CommunityAppealView review(Long admin, Long appealId, Integer lock, boolean revoke, String note) {
        CommunityAppeal appeal = appeal(appealId); if (lock == null) throw conflict(); String status = revoke ? "REVOKED" : "UPHELD"; LocalDateTime now = now(); String review = required(note, 1000, "review note");
        CommunityReport report = report(appeal.getReportId()); if (revoke) { if (reportMapper.revokeForAppeal(report.getId(), report.getLockVersion(), review, now) != 1) throw conflict(); reportEvent(report.getId(), admin, "APPEAL_REVOKED", "RESOLVED", "DISMISSED", now); }
        if (appealMapper.review(appeal.getId(), admin, status, review, lock, now) != 1) throw conflict(); event(appeal.getId(), "ADMIN", admin, status, "PENDING", status, now);
        appeal.setStatus(status); appeal.setReviewerAdminId(admin); appeal.setReviewNote(review); appeal.setReviewedAt(now); appeal.setLockVersion(lock + 1); return CommunityAppealView.from(appeal);
    }
    private boolean owns(Long user, CommunityReport report) { if (user == null) return false; return switch (report.getTargetType()) { case "USER" -> Objects.equals(user, report.getTargetId()); case "ARTICLE" -> articleMapper.selectById(report.getTargetId()) != null && Objects.equals(user, articleMapper.selectById(report.getTargetId()).getAuthorUserId()); case "MOMENT" -> momentMapper.selectById(report.getTargetId()) != null && Objects.equals(user, momentMapper.selectById(report.getTargetId()).getActorUserId()); case "COMMENT" -> commentMapper.selectById(report.getTargetId()) != null && Objects.equals(user, commentMapper.selectById(report.getTargetId()).getAuthorUserId()); case "BLOG" -> blogMapper.selectById(report.getTargetId()) != null && Objects.equals(user, blogMapper.selectById(report.getTargetId()).getOwnerUserId()); case "TEAM" -> teamMapper.selectById(report.getTargetId()) != null && Objects.equals(user, teamMapper.selectById(report.getTargetId()).getOwnerUserId()); case "CHAT" -> chatMapper.selectById(report.getTargetId()) != null && (Objects.equals(user, chatMapper.selectById(report.getTargetId()).getSenderUserId()) || Objects.equals(user, chatMapper.selectById(report.getTargetId()).getRecipientUserId())); default -> false; }; }
    private CommunityReport report(Long id) { CommunityReport value = reportMapper.selectById(positive(id, "report")); if (value == null) throw new BusinessException(404, "举报记录不存在"); return value; }
    private CommunityAppeal appeal(Long id) { CommunityAppeal value = appealMapper.selectById(positive(id, "appeal")); if (value == null) throw new BusinessException(404, "申诉不存在"); return value; }
    private void event(Long id, String actorType, Long actor, String type, String before, String after, LocalDateTime now) { CommunityAppealEvent value = new CommunityAppealEvent(); value.setAppealId(id); value.setActorType(actorType); value.setActorId(actor); value.setEventType(type); value.setBeforeSnapshot(snapshot(before)); value.setAfterSnapshot(snapshot(after)); value.setOccurredAt(now); eventMapper.insert(value); }
    private void reportEvent(Long id, Long admin, String type, String before, String after, LocalDateTime now) { CommunityReportEvent value = new CommunityReportEvent(); value.setReportId(id); value.setActorType("ADMIN"); value.setActorId(admin); value.setEventType(type); value.setBeforeSnapshot(snapshot(before)); value.setAfterSnapshot(snapshot(after)); value.setOccurredAt(now); reportEventMapper.insert(value); }
    private String evidence(String value) { if (value == null || value.isBlank()) return null; if (value.length() > 4000) throw new BusinessException(400, "证据内容过长"); try { objectMapper.readTree(value); return value; } catch (Exception exception) { throw new BusinessException(400, "证据格式不正确"); } }
    private static String snapshot(String status) { return status == null ? null : "{\"status\":\"" + status + "\"}"; }
    private static String required(String value, int max, String label) { if (value == null || value.trim().isEmpty()) throw new BusinessException(400, "缺少必填参数"); String result = value.trim(); if (result.length() > max) throw new BusinessException(400, "输入内容过长"); return result; }
    private static Long positive(Long value, String label) { if (value == null || value <= 0) throw new BusinessException(400, "无效的" + label + "编号"); return value; }
    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static BusinessException conflict() { return new BusinessException(409, "申诉状态已变更，请刷新后重试"); }
}
