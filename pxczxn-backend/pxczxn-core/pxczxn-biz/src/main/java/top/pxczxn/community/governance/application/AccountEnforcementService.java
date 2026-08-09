package top.pxczxn.community.governance.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementCase;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementAppeal;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementEvent;
import top.pxczxn.community.governance.model.CommunityAccountEnforcementReview;
import top.pxczxn.community.governance.persistence.CommunityAccountEnforcementCaseMapper;
import top.pxczxn.community.governance.persistence.CommunityAccountEnforcementAppealMapper;
import top.pxczxn.community.governance.persistence.CommunityAccountEnforcementEventMapper;
import top.pxczxn.community.governance.persistence.CommunityAccountEnforcementReviewMapper;
import top.pxczxn.community.file.application.CommunityFileService;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.chat.persistence.CommunityChatMessageMapper;
import top.pxczxn.community.file.model.CommunityFileReference;
import top.pxczxn.community.file.persistence.CommunityFileReferenceMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.sanction.application.CommunitySanctionService;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.submission.model.TeamSubmission;
import top.pxczxn.community.team.submission.persistence.TeamSubmissionMapper;
import top.pxczxn.community.social.model.CommunityComment;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.model.CommunityUserLoginAccount;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.community.user.persistence.CommunityUserLoginAccountMapper;
import top.pxczxn.community.user.application.CommunitySessionService;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.file.entity.SysFile;
import top.pxczxn.platform.file.service.SysFileService;
import top.pxczxn.platform.system.helper.SystemConfigHelper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccountEnforcementService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> APPROVAL_MEASURES = Set.of("LONG_FREEZE", "DATA_CLEANUP", "ACCOUNT_DELETE");
    private static final int NORMAL_ADMIN_MAX_FREEZE_DAYS = 7;
    private static final int DELETE_APPEAL_WINDOW_DAYS = 30;

    private final CommunityAccountEnforcementCaseMapper cases;
    private final CommunityAccountEnforcementAppealMapper appeals;
    private final CommunityAccountEnforcementReviewMapper reviews;
    private final CommunityAccountEnforcementEventMapper events;
    private final CommunityFileService fileService;
    private final CommunityUserMapper users;
    private final CommunityAuth communityAuth;
    private final CommunitySessionService sessionService;
    private final CommunityUserLoginAccountMapper loginAccounts;
    private final CommunitySanctionService sanctions;
    private final TeamMapper teams;
    private final TeamSubmissionMapper submissions;
    private final ArticleMapper articles;
    private final CommunityMomentMapper moments;
    private final CommunityCommentMapper comments;
    private final CommunityChatMessageMapper chatMessages;
    private final CommunityFileReferenceMapper fileReferences;
    private final SysFileService evidenceFiles;
    private final SystemConfigHelper configHelper;

    @Transactional
    public void forceLogout(Long adminId, Long userId, String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot) {
        recordSecurityOperation(adminId, userId, "SECURITY_LOGOUT", reasonCode, userVisibleReason, internalReason, evidenceSnapshot);
        communityAuth.stpLogic().logout(userId);
    }

    @Transactional
    public void unlockAccount(Long adminId, Long userId, String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot) {
        CommunityUserLoginAccount account = loginAccounts.selectOne(Wrappers.<CommunityUserLoginAccount>lambdaQuery().eq(CommunityUserLoginAccount::getUserId, userId).eq(CommunityUserLoginAccount::getLoginType, "EMAIL").last("LIMIT 1"));
        if (account == null || loginAccounts.clearLoginFailures(account.getId()) != 1) throw new BusinessException(404, "登录账号不存在或解除锁定失败");
        recordSecurityOperation(adminId, userId, "ACCOUNT_UNLOCK", reasonCode, userVisibleReason, internalReason, evidenceSnapshot);
    }

    @Transactional
    public void lockAccount(Long adminId, Long userId, FreezeCommand command) {
        if (command.expiresAt() == null || !command.expiresAt().isAfter(now())) throw new BusinessException(400, "锁定期限必须晚于当前时间");
        CommunityUserLoginAccount account = loginAccounts.selectOne(Wrappers.<CommunityUserLoginAccount>lambdaQuery().eq(CommunityUserLoginAccount::getUserId, userId).eq(CommunityUserLoginAccount::getLoginType, "EMAIL").last("LIMIT 1"));
        if (account == null || loginAccounts.lockAccount(account.getId(), command.expiresAt()) != 1) throw new BusinessException(404, "登录账号不存在或锁定失败");
        communityAuth.stpLogic().logout(userId);
        recordSecurityOperation(adminId, userId, "ACCOUNT_LOCK", command.reasonCode(), command.userVisibleReason(), command.internalReason(), command.evidenceSnapshot());
    }

    @Transactional
    public String forcePasswordReset(Long adminId, Long userId, String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot) {
        String visibleReason = requireText(userVisibleReason, "用户可见理由", 1000);
        String internal = requireText(internalReason, "内部处理说明", 2000);
        String evidence = requireText(evidenceSnapshot, "证据快照", 50000);
        CommunityAccountEnforcementCase item = newCase(
                adminId, userId, "PASSWORD_RESET", requireText(reasonCode, "标准原因", 40),
                visibleReason, internal, evidence
        );
        item.setStatus("FINALIZED");
        item.setStartsAt(now());
        item.setFinalizedAt(now());
        item.setAppealAllowed(false);
        insert(item, adminId, "PASSWORD_RESET_EXECUTED");
        return sessionService.forcePasswordReset(userId);
    }

    @Transactional
    public CommunityAccountEnforcementCase freeze(Long adminId, FreezeCommand command, boolean superAdmin) {
        requireText(command.userVisibleReason(), "用户可见理由", 1000);
        requireText(command.internalReason(), "内部处理说明", 2000);
        requireText(command.evidenceSnapshot(), "证据快照", 50000);
        if (command.expiresAt() == null || !command.expiresAt().isAfter(now())) {
            throw new BusinessException(400, "冻结期限必须晚于当前时间");
        }
        if (!superAdmin && command.expiresAt().isAfter(now().plusDays(NORMAL_ADMIN_MAX_FREEZE_DAYS))) {
            throw new BusinessException(403, "普通管理员单次冻结最长不得超过 7 天");
        }
        CommunityAccountEnforcementCase item = newCase(adminId, command.targetUserId(), "TEMP_FREEZE", command.reasonCode(), command.userVisibleReason(), command.internalReason(), command.evidenceSnapshot());
        item.setStatus("ACTIVE");
        item.setStartsAt(now());
        item.setExpiresAt(command.expiresAt());
        insert(item, adminId, "FREEZE_EXECUTED");
        setFrozen(item.getTargetUserId());
        return item;
    }

    @Transactional
    public CommunityAccountEnforcementCase extendFreeze(Long adminId, FreezeCommand command, boolean superAdmin) {
        validateFreezeExpiry(command.expiresAt(), superAdmin);
        CommunityAccountEnforcementCase item = activeTemporaryFreeze(command.targetUserId());
        item.setExpiresAt(command.expiresAt());
        item.setLockVersion(item.getLockVersion() + 1);
        cases.updateById(item);
        recordOperation(adminId, command.targetUserId(), "FREEZE_EXTEND", command.reasonCode(), command.userVisibleReason(), command.internalReason(), command.evidenceSnapshot());
        event(item, adminId, "FREEZE_EXTENDED");
        return item;
    }

    @Transactional
    public void releaseFreeze(Long adminId, Long userId, OperationCommand command) {
        List<CommunityAccountEnforcementCase> active = cases.selectList(Wrappers.<CommunityAccountEnforcementCase>lambdaQuery()
                .eq(CommunityAccountEnforcementCase::getTargetUserId, userId)
                .eq(CommunityAccountEnforcementCase::getMeasureType, "TEMP_FREEZE")
                .eq(CommunityAccountEnforcementCase::getStatus, "ACTIVE"));
        if (active.isEmpty()) throw new BusinessException(409, "该账号不存在可提前解除的临时冻结");
        active.forEach(item -> { item.setStatus("REVOKED"); item.setFinalizedAt(now()); item.setLockVersion(item.getLockVersion() + 1); cases.updateById(item); event(item, adminId, "FREEZE_RELEASED"); });
        restoreIfNoActiveFreeze(userId, null);
        recordOperation(adminId, userId, "FREEZE_RELEASE", command.reasonCode(), command.userVisibleReason(), command.internalReason(), command.evidenceSnapshot());
    }

    @Transactional
    public void blockLogin(Long adminId, Long userId, FreezeCommand command, boolean superAdmin) {
        validateFreezeExpiry(command.expiresAt(), superAdmin);
        sanctions.issue(adminId, userId, "LOGIN_SUSPEND", command.reasonCode(), command.userVisibleReason(), null, command.expiresAt());
        communityAuth.stpLogic().logout(userId);
        recordOperation(adminId, userId, "LOGIN_BLOCK", command.reasonCode(), command.userVisibleReason(), command.internalReason(), command.evidenceSnapshot());
    }

    @Transactional
    public void restoreLogin(Long adminId, Long userId, OperationCommand command) {
        sanctions.revokeActiveType(adminId, userId, "LOGIN_SUSPEND", command.internalReason());
        recordOperation(adminId, userId, "LOGIN_RESTORE", command.reasonCode(), command.userVisibleReason(), command.internalReason(), command.evidenceSnapshot());
    }

    @Transactional
    public void deactivate(Long adminId, Long userId, OperationCommand command) {
        ensureNoBlockingCleanupRelation(userId);
        CommunityUser target = user(userId);
        if ("DELETED".equals(target.getStatus())) throw new BusinessException(409, "已删除账号不能停用");
        target.setStatus("DEACTIVATED"); users.updateById(target); communityAuth.stpLogic().logout(userId);
        recordOperation(adminId, userId, "ACCOUNT_DEACTIVATE", command.reasonCode(), command.userVisibleReason(), command.internalReason(), command.evidenceSnapshot());
    }

    @Transactional
    public void restore(Long adminId, Long userId, OperationCommand command) {
        CommunityUser target = user(userId);
        if (!"DEACTIVATED".equals(target.getStatus())) throw new BusinessException(409, "仅已停用账号可以恢复");
        target.setStatus("NORMAL"); users.updateById(target);
        recordOperation(adminId, userId, "ACCOUNT_RESTORE", command.reasonCode(), command.userVisibleReason(), command.internalReason(), command.evidenceSnapshot());
    }

    @Transactional
    public CommunityAccountEnforcementCase submit(Long adminId, SubmitCommand command) {
        if (!APPROVAL_MEASURES.contains(command.measureType())) throw new BusinessException(400, "仅长期冻结、数据清理或账号删除需要提交申请");
        if ("FROZEN".equals(user(command.targetUserId()).getStatus())) {
            throw new BusinessException(409, "该用户正在冻结中，不能重复发起申请");
        }
        if ("LONG_FREEZE".equals(command.measureType()) && (command.expiresAt() == null || !command.expiresAt().isAfter(now()))) {
            throw new BusinessException(400, "长期冻结必须设置晚于当前时间的到期时间");
        }
        if (!"LONG_FREEZE".equals(command.measureType())) ensureNoBlockingCleanupRelation(command.targetUserId());
        requireText(command.userVisibleReason(), "用户可见理由", 1000);
        requireText(command.internalReason(), "内部处理说明", 2000);
        requireText(command.evidenceSnapshot(), "证据快照", 50000);
        Set<String> cleanupScopes = "DATA_CLEANUP".equals(command.measureType()) ? cleanupScopes(command.cleanupScope()) : Set.of();
        CommunityAccountEnforcementCase item = newCase(adminId, command.targetUserId(), command.measureType(), command.reasonCode(), command.userVisibleReason(), command.internalReason(), command.evidenceSnapshot());
        item.setStatus("SUBMITTED");
        item.setCleanupScope(cleanupScopes.isEmpty() ? null : json(cleanupScopes));
        item.setSourceReportId(command.sourceReportId());
        item.setExpiresAt(command.expiresAt());
        insert(item, adminId, "SUBMITTED");
        return item;
    }

    @Transactional
    public CommunityAccountEnforcementCase review(Long adminId, Long caseId, ReviewCommand command, boolean superAdmin) {
        CommunityAccountEnforcementCase item = requireCase(caseId);
        if (!Set.of("SUBMITTED", "UNDER_REVIEW").contains(item.getStatus())) {
            throw new BusinessException(409, "当前申请状态不允许继续审核");
        }
        boolean allowSuperAdminSelfReview = configHelper.getBoolean(SystemConfigHelper.GROUP_SYSTEM, "allowSuperAdminSelfReview", true);
        if ((!superAdmin || !allowSuperAdminSelfReview) && adminId.equals(item.getRequestedByAdminId())) {
            throw new BusinessException(403, "发起人不能审核自己的申请");
        }
        requireText(command.reviewNote(), "审核意见", 2000);
        if (!Set.of("APPROVE", "REJECT", "RETURN_FOR_EVIDENCE").contains(command.decision())) throw new BusinessException(400, "审核决定无效");
        String stage = "SUBMITTED".equals(item.getStatus()) ? "INITIAL" : "SECONDARY";
        if ("SECONDARY".equals(stage) && !superAdmin) {
            throw new BusinessException(403, "复审只能由超级管理员完成");
        }
        if (reviews.selectCount(Wrappers.<CommunityAccountEnforcementReview>lambdaQuery().eq(CommunityAccountEnforcementReview::getCaseId, caseId).eq(CommunityAccountEnforcementReview::getReviewerAdminId, adminId)) > 0) {
            if (!superAdmin) {
                throw new BusinessException(409, "同一管理员不能重复审核同一申请");
            }
        }
        long previousApprovalCount = reviews.selectCount(Wrappers.<CommunityAccountEnforcementReview>lambdaQuery()
                .eq(CommunityAccountEnforcementReview::getCaseId, caseId)
                .eq(CommunityAccountEnforcementReview::getDecision, "APPROVE"));
        CommunityAccountEnforcementReview review = new CommunityAccountEnforcementReview();
        review.setCaseId(caseId); review.setStage(stage); review.setReviewerAdminId(adminId); review.setDecision(command.decision()); review.setReviewNote(command.reviewNote().trim()); review.setCreatedAt(now());
        reviews.insert(review);
        long approvalCount = Math.max("SECONDARY".equals(stage) ? 1 : 0, previousApprovalCount)
                + ("APPROVE".equals(command.decision()) ? 1 : 0);
        int minimumApprovalCount = minimumApprovalCount();
        // 超级管理员免初审：对 SUBMITTED 申请的一次通过审核直接视为终审批准
        boolean superAdminDirectApproval = superAdmin && "INITIAL".equals(stage) && "APPROVE".equals(command.decision());
        item.setStatus("REJECT".equals(command.decision()) ? "REJECTED" : "RETURN_FOR_EVIDENCE".equals(command.decision()) ? "SUBMITTED" : (superAdminDirectApproval || approvalCount >= minimumApprovalCount) ? "APPROVED" : "UNDER_REVIEW");
        item.setLockVersion(item.getLockVersion() + 1);
        cases.updateById(item);
        event(item, adminId, "REVIEW_" + command.decision());
        return item;
    }

    private int minimumApprovalCount() {
        int configured = configHelper.getInt(SystemConfigHelper.GROUP_SYSTEM, "minimumApprovalCount", 2);
        return Math.max(1, Math.min(configured, 10));
    }

    public List<CommunityAccountEnforcementReview> reviews(Long caseId) {
        requireCase(caseId);
        return reviews.selectList(Wrappers.<CommunityAccountEnforcementReview>lambdaQuery()
                .eq(CommunityAccountEnforcementReview::getCaseId, caseId)
                .orderByAsc(CommunityAccountEnforcementReview::getCreatedAt));
    }

    @Transactional
    public CommunityAccountEnforcementCase supplementEvidence(Long adminId, Long caseId, String evidenceSnapshot) {
        CommunityAccountEnforcementCase item = requireCase(caseId);
        if (!"SUBMITTED".equals(item.getStatus())) {
            throw new BusinessException(409, "当前申请状态不允许补充证据");
        }
        if (!adminId.equals(item.getRequestedByAdminId())) {
            throw new BusinessException(403, "仅申请发起人可以补充证据");
        }
        long returnCount = reviews.selectCount(Wrappers.<CommunityAccountEnforcementReview>lambdaQuery()
                .eq(CommunityAccountEnforcementReview::getCaseId, caseId)
                .eq(CommunityAccountEnforcementReview::getDecision, "RETURN_FOR_EVIDENCE"));
        if (returnCount == 0) {
            throw new BusinessException(409, "该申请尚未被要求补充证据");
        }
        String previousEvidence = item.getEvidenceSnapshot();
        String supplementedEvidence = evidenceSnapshot(evidenceSnapshot);
        Map<String, Object> combinedEvidence = new LinkedHashMap<>();
        combinedEvidence.put("previous", jsonNode(previousEvidence));
        combinedEvidence.put("supplement", jsonNode(supplementedEvidence));
        item.setEvidenceSnapshot(json(combinedEvidence));
        item.setLockVersion(item.getLockVersion() + 1);
        cases.updateById(item);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", item.getStatus());
        snapshot.put("measureType", item.getMeasureType());
        snapshot.put("previousEvidence", previousEvidence);
        snapshot.put("supplementedEvidence", item.getEvidenceSnapshot());
        event(item, adminId, "ADMIN", "EVIDENCE_SUPPLEMENTED", snapshot);
        return item;
    }

    @Transactional
    public CommunityAccountEnforcementCase execute(Long adminId, Long caseId, String confirmation) {
        CommunityAccountEnforcementCase item = requireCase(caseId);
        if (!"APPROVED".equals(item.getStatus())) throw new BusinessException(409, "该申请尚未完成多级审核");
        String expected = confirmationText(item);
        if (!expected.equals(confirmation == null ? "" : confirmation.trim())) throw new BusinessException(400, "最终确认文字不匹配");
        item.setStartsAt(now());
        item.setAppealDeadlineAt(now().plusDays(DELETE_APPEAL_WINDOW_DAYS));
        if ("LONG_FREEZE".equals(item.getMeasureType())) {
            item.setStatus("ACTIVE");
            item.setExecuteAfter(null);
        } else {
            item.setStatus("APPEAL_WINDOW");
            item.setExecuteAfter(item.getAppealDeadlineAt());
        }
        item.setLockVersion(item.getLockVersion() + 1);
        cases.updateById(item); setFrozen(item.getTargetUserId()); event(item, adminId, "EXECUTION_PENDING_APPEAL");
        return item;
    }

    public String confirmationText(Long caseId) {
        return confirmationText(requireCase(caseId));
    }

    private String confirmationText(CommunityAccountEnforcementCase item) {
        return "我已审核，确认对账号 " + user(item.getTargetUserId()).getUsername()
                + "（ID：" + item.getTargetUserId() + "）执行" + display(item.getMeasureType()) + "操作";
    }

    public List<CommunityAccountEnforcementCase> list(Long userId) {
        return cases.selectList(Wrappers.<CommunityAccountEnforcementCase>lambdaQuery().eq(userId != null, CommunityAccountEnforcementCase::getTargetUserId, userId).orderByDesc(CommunityAccountEnforcementCase::getCreatedAt));
    }

    @Transactional
    public CommunityAccountEnforcementAppeal appeal(Long userId, Long caseId, String statement, List<Long> evidenceFileIds) {
        CommunityAccountEnforcementCase item = requireCase(caseId);
        if (!userId.equals(item.getTargetUserId())) throw new BusinessException(403, "只能对本人账号措施提交申诉");
        if (!Boolean.TRUE.equals(item.getAppealAllowed()) || !("ACTIVE".equals(item.getStatus()) || "APPEAL_WINDOW".equals(item.getStatus()))) {
            throw new BusinessException(409, "当前措施不在可申诉状态");
        }
        if (item.getAppealDeadlineAt() != null && now().isAfter(item.getAppealDeadlineAt())) {
            throw new BusinessException(409, "该措施已超过申诉期限");
        }
        if (appeals.selectCount(Wrappers.<CommunityAccountEnforcementAppeal>lambdaQuery().eq(CommunityAccountEnforcementAppeal::getCaseId, caseId).eq(CommunityAccountEnforcementAppeal::getAppellantUserId, userId).in(CommunityAccountEnforcementAppeal::getStatus, "SUBMITTED", "UNDER_REVIEW")) > 0) {
            throw new BusinessException(409, "该措施已有待处理申诉");
        }
        CommunityAccountEnforcementAppeal appeal = new CommunityAccountEnforcementAppeal();
        appeal.setId(IdWorker.getId()); appeal.setCaseId(caseId); appeal.setAppellantUserId(userId); appeal.setStatement(requireText(statement, "申诉说明", 2000)); appeal.setEvidenceSnapshot(appealEvidenceSnapshot(evidenceFileIds)); appeal.setStatus("SUBMITTED"); appeal.setCreatedAt(now());
        appeals.insert(appeal);
        fileService.replaceOwnedFileReferences(evidenceFileIds, "ACCOUNT_ENFORCEMENT_APPEAL", appeal.getId(), "APPEAL_EVIDENCE");
        item.setStatus("APPEALED"); item.setLockVersion(item.getLockVersion() + 1); cases.updateById(item); event(item, userId, "APPEAL_SUBMITTED");
        return appeal;
    }

    public List<CommunityAccountEnforcementAppeal> myAppeals(Long userId) {
        return appeals.selectList(Wrappers.<CommunityAccountEnforcementAppeal>lambdaQuery().eq(CommunityAccountEnforcementAppeal::getAppellantUserId, userId).orderByDesc(CommunityAccountEnforcementAppeal::getCreatedAt));
    }

    public List<CommunityAccountEnforcementAppeal> appeals() {
        return appeals.selectList(Wrappers.<CommunityAccountEnforcementAppeal>lambdaQuery().orderByAsc(CommunityAccountEnforcementAppeal::getCreatedAt));
    }

    @Transactional
    public CommunityAccountEnforcementAppeal primaryReviewAppeal(Long adminId, Long appealId, String decision, String note, boolean superAdmin) {
        CommunityAccountEnforcementAppeal appeal = requireAppeal(appealId);
        CommunityAccountEnforcementCase item = requireCase(appeal.getCaseId());
        if (!"SUBMITTED".equals(appeal.getStatus())) throw new BusinessException(409, "申诉已进入审核阶段");
        if (adminId.equals(item.getRequestedByAdminId()) && !superAdmin) throw new BusinessException(403, "您是该措施的原处置人，不能初审自己的申诉，请由其他管理员处理");
        validateAppealDecision(decision, note, null);
        appeal.setPrimaryReviewedByAdminId(adminId); appeal.setPrimaryDecision(decision); appeal.setPrimaryReviewNote(note.trim()); appeal.setPrimaryReviewedAt(now()); appeal.setStatus("PRIMARY_REVIEWED");
        appeals.updateById(appeal); event(item, adminId, "APPEAL_PRIMARY_" + decision);
        return appeal;
    }

    @Transactional
    public CommunityAccountEnforcementAppeal finalReviewAppeal(Long adminId, Long appealId, String decision, String note, LocalDateTime modifiedExpiresAt) {
        CommunityAccountEnforcementAppeal appeal = requireAppeal(appealId);
        if (!"PRIMARY_REVIEWED".equals(appeal.getStatus())) throw new BusinessException(409, "该申诉尚未完成初审，不能终审");
        if (adminId.equals(appeal.getPrimaryReviewedByAdminId())) throw new BusinessException(403, "初审人与终审人不能是同一管理员，请由另一名超级管理员终审");
        validateAppealDecision(decision, note, modifiedExpiresAt);
        appeal.setFinalReviewedByAdminId(adminId); appeal.setFinalDecision(decision); appeal.setFinalReviewNote(note.trim()); appeal.setFinalReviewedAt(now());
        appeal.setStatus("UNDER_REVIEW"); appeals.updateById(appeal);
        CommunityAccountEnforcementAppeal finalized = reviewAppeal(adminId, appealId, decision, note, modifiedExpiresAt);
        finalized.setStatus("FINALIZED"); appeals.updateById(finalized);
        return finalized;
    }

    @Transactional
    public CommunityAccountEnforcementAppeal reviewAppeal(Long adminId, Long appealId, String decision, String note, LocalDateTime modifiedExpiresAt) {
        CommunityAccountEnforcementAppeal appeal = appeals.selectById(appealId);
        if (appeal == null) throw new BusinessException(404, "申诉不存在");
        if (!Set.of("SUBMITTED", "UNDER_REVIEW").contains(appeal.getStatus())) throw new BusinessException(409, "该申诉已处理");
        CommunityAccountEnforcementCase item = requireCase(appeal.getCaseId());
        if (adminId.equals(item.getRequestedByAdminId())) throw new BusinessException(403, "原处置人不能审核该申诉");
        if (!Set.of("UPHOLD", "MODIFY", "REVOKE").contains(decision)) throw new BusinessException(400, "申诉裁决无效");
        if ("MODIFY".equals(decision) && (modifiedExpiresAt == null || !modifiedExpiresAt.isAfter(now()))) {
            throw new BusinessException(400, "修改处理必须设置晚于当前时间的新期限");
        }
        appeal.setStatus(switch (decision) {
            case "UPHOLD" -> "UPHELD";
            case "MODIFY" -> "MODIFIED";
            case "REVOKE" -> "REVOKED";
            default -> throw new IllegalStateException("未处理的申诉裁决: " + decision);
        });
        appeal.setReviewedByAdminId(adminId); appeal.setReviewNote(requireText(note, "审核意见", 2000)); appeal.setReviewedAt(now()); appeals.updateById(appeal);
        if ("REVOKE".equals(decision)) { item.setStatus("REVOKED"); restoreIfNoActiveFreeze(item.getTargetUserId(), item.getId()); }
        else if (Set.of("TEMP_FREEZE", "LONG_FREEZE").contains(item.getMeasureType())) {
            if ("MODIFY".equals(decision)) item.setExpiresAt(modifiedExpiresAt);
            item.setStatus("ACTIVE");
        } else {
            item.setStatus("APPEAL_WINDOW");
            item.setExecuteAfter("MODIFY".equals(decision) ? modifiedExpiresAt : now().plusDays(7));
            item.setAppealDeadlineAt(item.getExecuteAfter());
        }
        item.setLockVersion(item.getLockVersion() + 1); cases.updateById(item); event(item, adminId, "APPEAL_" + decision);
        return appeal;
    }

    private CommunityAccountEnforcementAppeal requireAppeal(Long appealId) { CommunityAccountEnforcementAppeal appeal = appeals.selectById(appealId); if (appeal == null) throw new BusinessException(404, "申诉不存在"); return appeal; }
    private static String appealEvidenceSnapshot(List<Long> fileIds) { if (fileIds == null || fileIds.isEmpty()) return null; LinkedHashSet<Long> normalized = new LinkedHashSet<>(fileIds); normalized.remove(null); if (normalized.size() > 10) throw new BusinessException(400, "最多上传 10 个申诉附件"); return normalized.toString(); }
    private void validateAppealDecision(String decision, String note, LocalDateTime modifiedExpiresAt) { if (!Set.of("UPHOLD", "MODIFY", "REVOKE").contains(decision)) throw new BusinessException(400, "申诉裁决无效"); requireText(note, "审核意见", 2000); if ("MODIFY".equals(decision) && (modifiedExpiresAt == null || !modifiedExpiresAt.isAfter(now()))) throw new BusinessException(400, "修改后的到期时间必须晚于当前时间"); }

    public List<CommunityAccountEnforcementCase> mine(Long userId) {
        return list(userId);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void finalizeDue() {
        cases.selectList(Wrappers.<CommunityAccountEnforcementCase>lambdaQuery().eq(CommunityAccountEnforcementCase::getStatus, "ACTIVE").in(CommunityAccountEnforcementCase::getMeasureType, "TEMP_FREEZE", "LONG_FREEZE").isNotNull(CommunityAccountEnforcementCase::getExpiresAt).le(CommunityAccountEnforcementCase::getExpiresAt, now())).forEach(item -> {
            item.setStatus("FINALIZED"); item.setFinalizedAt(now()); item.setLockVersion(item.getLockVersion() + 1); cases.updateById(item); restoreIfNoActiveFreeze(item.getTargetUserId(), item.getId()); event(item, null, "FREEZE_EXPIRED");
        });
        cases.selectList(Wrappers.<CommunityAccountEnforcementCase>lambdaQuery().eq(CommunityAccountEnforcementCase::getStatus, "APPEAL_WINDOW").le(CommunityAccountEnforcementCase::getExecuteAfter, now())).forEach(item -> {
            CommunityUser user = user(item.getTargetUserId());
            if ("ACCOUNT_DELETE".equals(item.getMeasureType())) finalizeAccountDeletion(user);
            else if ("DATA_CLEANUP".equals(item.getMeasureType())) {
                cleanupUserData(user, cleanupScopes(item.getCleanupScope()));
            }
            else user.setStatus("FROZEN");
            users.updateById(user); communityAuth.stpLogic().logout(item.getTargetUserId());
            item.setStatus("FINALIZED"); item.setFinalizedAt(now()); item.setLockVersion(item.getLockVersion() + 1); cases.updateById(item); event(item, null, "FINALIZED");
        });
    }

    private CommunityAccountEnforcementCase newCase(Long adminId, Long userId, String type, String code, String visible, String internal, String evidence) {
        CommunityAccountEnforcementCase item = new CommunityAccountEnforcementCase(); item.setId(IdWorker.getId()); item.setTargetUserId(user(userId).getId()); item.setMeasureType(type); item.setReasonCode(requireText(code, "处置原因", 40)); item.setUserVisibleReason(visible.trim()); item.setInternalReason(internal.trim()); item.setEvidenceSnapshot(evidenceSnapshot(evidence)); item.setRequestedByAdminId(adminId); item.setRequestedAt(now()); item.setAppealAllowed(true); item.setLockVersion(0); return item;
    }
    private void recordSecurityOperation(Long adminId, Long userId, String type, String reasonCode, String visible, String internal, String evidence) { recordOperation(adminId, userId, type, reasonCode, visible, internal, evidence); }
    private void recordOperation(Long adminId, Long userId, String type, String reasonCode, String visible, String internal, String evidence) {
        CommunityAccountEnforcementCase item = newCase(adminId, userId, type, requireText(reasonCode, "标准原因", 40), requireText(visible, "用户可见理由", 1000), requireText(internal, "内部处理说明", 2000), requireText(evidence, "证据快照", 50000));
        item.setStatus("FINALIZED"); item.setStartsAt(now()); item.setFinalizedAt(now()); item.setAppealAllowed(false); insert(item, adminId, type + "_EXECUTED");
    }
    private void validateFreezeExpiry(LocalDateTime expiresAt, boolean superAdmin) { if (expiresAt == null || !expiresAt.isAfter(now())) throw new BusinessException(400, "冻结或禁止登录期限必须晚于当前时间"); if (!superAdmin && expiresAt.isAfter(now().plusDays(NORMAL_ADMIN_MAX_FREEZE_DAYS))) throw new BusinessException(403, "普通管理员单次操作最长不得超过 7 天"); }
    private CommunityAccountEnforcementCase activeTemporaryFreeze(Long userId) { CommunityAccountEnforcementCase item = cases.selectOne(Wrappers.<CommunityAccountEnforcementCase>lambdaQuery().eq(CommunityAccountEnforcementCase::getTargetUserId, userId).eq(CommunityAccountEnforcementCase::getMeasureType, "TEMP_FREEZE").eq(CommunityAccountEnforcementCase::getStatus, "ACTIVE").orderByDesc(CommunityAccountEnforcementCase::getExpiresAt).last("LIMIT 1")); if (item == null) throw new BusinessException(409, "该账号不存在可延长的临时冻结"); return item; }
    private void ensureNoBlockingCleanupRelation(Long userId) {
        long casesInProgress = cases.selectCount(Wrappers.<CommunityAccountEnforcementCase>lambdaQuery().eq(CommunityAccountEnforcementCase::getTargetUserId, userId).in(CommunityAccountEnforcementCase::getStatus, "SUBMITTED", "UNDER_REVIEW", "APPROVED", "APPEAL_WINDOW", "APPEALED"));
        if (casesInProgress > 0) throw new BusinessException(409, "账号存在待处理的处置或申诉，完成关联关系校验后才能清理");
        long ownedTeams = teams.selectCount(Wrappers.<Team>lambdaQuery().eq(Team::getOwnerUserId, userId).eq(Team::getStatus, "ACTIVE"));
        if (ownedTeams > 0) throw new BusinessException(409, "账号仍是活跃团队负责人，请先完成团队负责人移交或解散团队");
        long pendingSubmissions = submissions.selectCount(Wrappers.<TeamSubmission>lambdaQuery().eq(TeamSubmission::getSubmittedByUserId, userId).in(TeamSubmission::getStatus, "TEAM_PENDING", "PLATFORM_PENDING", "PLATFORM_PUBLISHING"));
        if (pendingSubmissions > 0) throw new BusinessException(409, "账号存在待处理投稿，请先完成投稿处理或撤回");
    }

    private Set<String> cleanupScopes(String value) {
        if (value == null || value.isBlank()) throw new BusinessException(400, "请至少选择一项清理范围");
        try {
            Set<String> scopes = JSON.readValue(value, new TypeReference<Set<String>>() { });
            Set<String> supported = Set.of("PROFILE", "ARTICLES", "MOMENTS", "COMMENTS", "CHAT_MESSAGES", "FILE_REFERENCES");
            if (scopes == null || scopes.isEmpty() || !supported.containsAll(scopes)) throw new BusinessException(400, "清理范围无效");
            return scopes;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(400, "清理范围必须是有效 JSON");
        }
    }

    private void cleanupUserData(CommunityUser user, Set<String> scopes) {
        LocalDateTime deletedAt = now();
        Long userId = user.getId();
        if (scopes.contains("PROFILE")) { user.setDisplayName("已注销用户"); user.setBio(null); user.setAvatarFileId(null); }
        if (scopes.contains("ARTICLES")) articles.update(null, Wrappers.<Article>lambdaUpdate().eq(Article::getAuthorUserId, userId).isNull(Article::getDeletedAt).set(Article::getDeletedAt, deletedAt));
        if (scopes.contains("MOMENTS")) moments.update(null, Wrappers.<CommunityMoment>lambdaUpdate().eq(CommunityMoment::getActorUserId, userId).isNull(CommunityMoment::getDeletedAt).set(CommunityMoment::getDeletedAt, deletedAt));
        if (scopes.contains("COMMENTS")) comments.update(null, Wrappers.<CommunityComment>lambdaUpdate().eq(CommunityComment::getAuthorUserId, userId).isNull(CommunityComment::getDeletedAt).set(CommunityComment::getDeletedAt, deletedAt));
        if (scopes.contains("CHAT_MESSAGES")) chatMessages.hideForUser(userId, deletedAt);
        if (scopes.contains("FILE_REFERENCES")) fileReferences.update(null, Wrappers.<CommunityFileReference>lambdaUpdate().eq(CommunityFileReference::getOwnerUserId, userId).isNull(CommunityFileReference::getDeletedAt).set(CommunityFileReference::getDeletedAt, deletedAt));
    }

    private void finalizeAccountDeletion(CommunityUser user) {
        Long userId = user.getId();
        user.setUsername("deleted_" + userId);
        user.setDisplayName("已注销用户");
        user.setBio(null);
        user.setAvatarFileId(null);
        user.setLastLoginAt(null);
        user.setStatus("DELETED");
        loginAccounts.update(null, Wrappers.<CommunityUserLoginAccount>lambdaUpdate()
                .eq(CommunityUserLoginAccount::getUserId, userId)
                .set(CommunityUserLoginAccount::getNormalizedIdentifier, "deleted-" + userId + "@invalid.local")
                .set(CommunityUserLoginAccount::getPasswordHash, null)
                .set(CommunityUserLoginAccount::getVerifiedAt, null)
                .set(CommunityUserLoginAccount::getFailedLoginCount, 0)
                .set(CommunityUserLoginAccount::getLockedUntil, null)
                .set(CommunityUserLoginAccount::getForcePasswordChange, false));
    }

    private String evidenceSnapshot(String value) {
        String evidence = requireText(value, "证据快照", 50000);
        JsonNode root;
        try { root = JSON.readTree(evidence); }
        catch (JsonProcessingException ignored) { return json(Map.of("description", requireText(evidence, "证据说明", 20000), "attachments", List.of())); }
        if (root.isTextual()) return json(Map.of("description", requireText(root.asText(), "证据说明", 20000), "attachments", List.of()));
        if (!root.isObject()) throw new BusinessException(400, "证据快照必须包含证据说明");
        String description = requireText(root.path("description").asText(null), "证据说明", 20000);
        JsonNode attachments = root.path("attachments");
        if (!attachments.isMissingNode() && !attachments.isArray()) throw new BusinessException(400, "证据附件格式无效");
        if (attachments.isArray() && attachments.size() > 10) throw new BusinessException(400, "证据附件最多 10 个");
        List<Map<String, Object>> sanitizedAttachments = new ArrayList<>();
        if (attachments.isArray()) {
            for (JsonNode attachment : attachments) {
                long fileId = attachment.path("id").asLong(0);
                if (fileId <= 0) throw new BusinessException(400, "证据附件 ID 无效");
                SysFile file = evidenceFiles.getById(fileId);
                if (file == null) throw new BusinessException(400, "证据附件不存在: " + fileId);
                Map<String, Object> sanitized = new LinkedHashMap<>();
                sanitized.put("id", fileId);
                sanitized.put("name", file.getOriginalName());
                sanitizedAttachments.add(sanitized);
            }
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("description", description);
        snapshot.put("attachments", sanitizedAttachments);
        return json(snapshot);
    }
    private String optionalEvidenceSnapshot(String value) { return value == null || value.isBlank() ? null : evidenceSnapshot(value); }
    private static String json(Object value) { try { return JSON.writeValueAsString(value); } catch (JsonProcessingException exception) { throw new BusinessException(500, "证据快照序列化失败"); } }
    private static JsonNode jsonNode(String value) { try { return JSON.readTree(value); } catch (JsonProcessingException exception) { throw new BusinessException(500, "证据快照解析失败"); } }
    private void insert(CommunityAccountEnforcementCase item, Long adminId, String type) { cases.insert(item); event(item, adminId, type); }
    private void setFrozen(Long userId) { CommunityUser user = user(userId); if ("NORMAL".equals(user.getStatus())) { user.setSanctionOriginalStatus("NORMAL"); user.setStatus("FROZEN"); users.updateById(user); } communityAuth.stpLogic().logout(userId); }
    private void restoreIfNoActiveFreeze(Long userId, Long excludedCaseId) { long active = cases.selectCount(Wrappers.<CommunityAccountEnforcementCase>lambdaQuery().eq(CommunityAccountEnforcementCase::getTargetUserId, userId).eq(CommunityAccountEnforcementCase::getStatus, "ACTIVE").in(CommunityAccountEnforcementCase::getMeasureType, "TEMP_FREEZE", "LONG_FREEZE").ne(excludedCaseId != null, CommunityAccountEnforcementCase::getId, excludedCaseId)); if (active == 0) { CommunityUser user = user(userId); if ("FROZEN".equals(user.getStatus())) { user.setStatus("NORMAL"); user.setSanctionOriginalStatus(null); users.updateById(user); } } }
    private CommunityAccountEnforcementCase requireCase(Long id) { CommunityAccountEnforcementCase item = cases.selectById(id); if (item == null) throw new BusinessException(404, "强制措施申请不存在"); return item; }
    private CommunityUser user(Long id) { CommunityUser user = users.selectById(id); if (user == null) throw new BusinessException(404, "社区用户不存在"); return user; }
    private void event(CommunityAccountEnforcementCase item, Long actor, String type) {
        String actorType = actor == null ? "SYSTEM" : "APPEAL_SUBMITTED".equals(type) ? "USER" : "ADMIN";
        event(item, actor, actorType, type, Map.of("status", item.getStatus(), "measureType", item.getMeasureType()));
    }
    private void event(CommunityAccountEnforcementCase item, Long actor, String actorType, String type, Object snapshot) {
        CommunityAccountEnforcementEvent event = new CommunityAccountEnforcementEvent();
        event.setCaseId(item.getId()); event.setActorType(actorType); event.setActorId(actor); event.setEventType(type);
        event.setSnapshot(json(snapshot)); event.setOccurredAt(now()); events.insert(event);
    }
    private static String display(String type) { return switch (type) { case "LONG_FREEZE" -> "强制冻结"; case "DATA_CLEANUP" -> "强制清理"; case "ACCOUNT_DELETE" -> "强制删除"; default -> "强制措施"; }; }
    private static String requireText(String value, String label, int max) { if (value == null || value.trim().isEmpty() || value.trim().length() > max) throw new BusinessException(400, label + "不能为空且长度必须有效"); return value.trim(); }
    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }

    public record FreezeCommand(Long targetUserId, String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot, LocalDateTime expiresAt) { }
    public record SubmitCommand(Long targetUserId, String measureType, String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot, String cleanupScope, Long sourceReportId, LocalDateTime expiresAt) { }
    public record ReviewCommand(String decision, String reviewNote) { }
    public record OperationCommand(String reasonCode, String userVisibleReason, String internalReason, String evidenceSnapshot) { }
}
