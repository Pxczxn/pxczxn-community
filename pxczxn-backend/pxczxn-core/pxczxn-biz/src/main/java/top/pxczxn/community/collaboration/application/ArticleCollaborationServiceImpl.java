package top.pxczxn.community.collaboration.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.collaboration.model.ArticleCollaborationAuditEvent;
import top.pxczxn.community.collaboration.model.ArticleCollaborationInvitation;
import top.pxczxn.community.collaboration.model.ArticleCollaborator;
import top.pxczxn.community.collaboration.persistence.ArticleCollaborationAuditEventMapper;
import top.pxczxn.community.collaboration.persistence.ArticleCollaborationInvitationMapper;
import top.pxczxn.community.collaboration.persistence.ArticleCollaboratorMapper;
import top.pxczxn.community.notification.application.CommunityNotificationEvent;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleCollaborationServiceImpl implements ArticleCollaborationService {
    private static final Set<String> TYPES = Set.of("CO_AUTHOR", "RESEARCH", "REVIEW", "ILLUSTRATION");
    private final ArticleCollaborationInvitationMapper invitationMapper;
    private final ArticleCollaboratorMapper collaboratorMapper;
    private final ArticleCollaborationAuditEventMapper auditMapper;
    private final ArticleMapper articleMapper;
    private final CommunityUserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override @Transactional
    public ArticleCollaborationInvitationView invite(Long actor, Long articleId, InviteArticleCollaboratorCommand command) {
        requireAuthor(actor, articleId); if (command == null || command.inviteeUserId() == null || actor.equals(command.inviteeUserId())) throw new BusinessException(400, "协作邀请信息无效");
        String key = key(command.idempotencyKey()); ArticleCollaborationInvitation same = invitationMapper.findByIdempotencyKey(key); if (same != null) { if (same.getArticleId().equals(articleId) && same.getInvitedByUserId().equals(actor)) return ArticleCollaborationInvitationView.from(same); throw new BusinessException(409, "幂等键已被使用"); }
        CommunityUser invitee = userMapper.selectById(command.inviteeUserId()); if (invitee == null || !Set.of("NORMAL", "LIMITED").contains(invitee.getStatus())) throw new BusinessException(404, "被邀请用户不可用");
        String type = normalizeType(command.contributionType()); int order = positive(command.attributionOrder(), "署名顺序"); LocalDateTime now = now(); ArticleCollaborationInvitation invitation = new ArticleCollaborationInvitation(); invitation.setId(IdWorker.getId()); invitation.setArticleId(articleId); invitation.setInviteeUserId(invitee.getId()); invitation.setInvitedByUserId(actor); invitation.setContributionType(type); invitation.setCanEdit(Boolean.TRUE.equals(command.canEdit())); invitation.setAttributionOrder(order); invitation.setMessage(trim(command.message(), 1000)); invitation.setStatus("PENDING"); invitation.setIdempotencyKey(key); invitation.setExpiresAt(now.plusDays(14)); invitation.setLockVersion(0); invitation.setCreatedAt(now); invitation.setUpdatedAt(now);
        try { if (invitationMapper.insert(invitation) != 1) throw new BusinessException(500, "创建协作邀请失败"); } catch (DuplicateKeyException e) { throw new BusinessException(409, "该用户已有待处理协作邀请或署名顺序冲突"); }
        audit(articleId, actor, "INVITED", invitee.getId(), null, "PENDING", now); eventPublisher.publishEvent(notification("ARTICLE_COLLABORATION_INVITE", actor, invitee.getId(), invitation.getId(), "收到文章共创邀请", "请查看并决定是否接受邀请")); return ArticleCollaborationInvitationView.from(invitation);
    }
    @Override @Transactional(readOnly = true) public List<ArticleCollaborationInvitationView> myPendingInvitations(Long actor) { return invitationMapper.findPendingByInvitee(actor).stream().map(ArticleCollaborationInvitationView::from).toList(); }
    @Override @Transactional(readOnly = true) public List<ArticleCollaboratorView> collaborators(Long actor, Long articleId) { requireArticleParticipant(actor, articleId); List<ArticleCollaborator> values = collaboratorMapper.findActiveByArticle(articleId); List<Long> userIds = values.stream().map(ArticleCollaborator::getUserId).toList(); Map<Long, CommunityUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(CommunityUser::getId, u -> u)); return values.stream().map(v -> { CommunityUser u = users.get(v.getUserId()); return ArticleCollaboratorView.from(v, u == null ? null : u.getUsername(), u == null ? null : u.getDisplayName()); }).toList(); }
    @Override @Transactional public ArticleCollaboratorView accept(Long actor, Long invitationId, RespondArticleCollaborationCommand command) { ArticleCollaborationInvitation invitation = invitation(invitationId); requireResponse(invitation, actor, command); LocalDateTime now = now(); if (invitation.getExpiresAt().isBefore(now)) throw new BusinessException(409, "协作邀请已过期"); if (invitationMapper.respond(invitationId, actor, invitation.getLockVersion(), "ACCEPTED", now) != 1) throw collision(); ArticleCollaborator c = new ArticleCollaborator(); c.setId(IdWorker.getId()); c.setArticleId(invitation.getArticleId()); c.setUserId(actor); c.setInvitationId(invitationId); c.setContributionType(invitation.getContributionType()); c.setCanEdit(invitation.getCanEdit()); c.setAttributionOrder(invitation.getAttributionOrder()); c.setAcceptedAt(now); c.setLockVersion(0); c.setCreatedAt(now); c.setUpdatedAt(now); try { if (collaboratorMapper.insert(c) != 1) throw new BusinessException(500, "创建协作者失败"); } catch (DuplicateKeyException e) { throw new BusinessException(409, "协作者或署名顺序已变化"); } audit(c.getArticleId(), actor, "ACCEPTED", actor, "PENDING", "ACCEPTED", now); eventPublisher.publishEvent(notification("ARTICLE_COLLABORATION_ACCEPTED", actor, invitation.getInvitedByUserId(), invitationId, "协作邀请已接受", "协作者已加入文章")); CommunityUser u = userMapper.selectById(actor); return ArticleCollaboratorView.from(c, u == null ? null : u.getUsername(), u == null ? null : u.getDisplayName()); }
    @Override @Transactional public void reject(Long actor, Long id, RespondArticleCollaborationCommand command) { respond(actor, id, command, "REJECTED", "ARTICLE_COLLABORATION_REJECTED", "协作邀请已拒绝"); }
    @Override @Transactional public void cancel(Long actor, Long id, RespondArticleCollaborationCommand command) { ArticleCollaborationInvitation invitation = invitation(id); if (!actor.equals(invitation.getInvitedByUserId()) || command == null || command.expectedLockVersion() == null || !command.expectedLockVersion().equals(invitation.getLockVersion())) throw collision(); LocalDateTime now = now(); if (invitationMapper.cancel(id, actor, invitation.getLockVersion(), now) != 1) throw collision(); audit(invitation.getArticleId(), actor, "CANCELLED", invitation.getInviteeUserId(), "PENDING", "CANCELLED", now); }
    @Override @Transactional public void revoke(Long actor, Long articleId, Long collaboratorId, Integer lock) { requireAuthor(actor, articleId); ArticleCollaborator c = collaboratorMapper.selectById(collaboratorId); if (c == null || !articleId.equals(c.getArticleId()) || lock == null || !lock.equals(c.getLockVersion())) throw collision(); LocalDateTime now = now(); if (collaboratorMapper.revoke(collaboratorId, articleId, lock, now) != 1) throw collision(); audit(articleId, actor, "REVOKED", c.getUserId(), "ACTIVE", "REVOKED", now); eventPublisher.publishEvent(notification("ARTICLE_COLLABORATION_REVOKED", actor, c.getUserId(), collaboratorId, "协作权限已撤销", "你不再是该文章的协作者")); }
    private void respond(Long actor, Long id, RespondArticleCollaborationCommand command, String next, String event, String content) { ArticleCollaborationInvitation invitation = invitation(id); requireResponse(invitation, actor, command); LocalDateTime now = now(); if (invitationMapper.respond(id, actor, invitation.getLockVersion(), next, now) != 1) throw collision(); audit(invitation.getArticleId(), actor, next, actor, "PENDING", next, now); eventPublisher.publishEvent(notification(event, actor, invitation.getInvitedByUserId(), id, "文章共创邀请结果", content)); }
    private void requireAuthor(Long actor, Long articleId) { Article a = articleMapper.selectById(articleId); if (a == null || a.getDeletedAt() != null) throw new BusinessException(404, "文章不存在"); if (!actor.equals(a.getAuthorUserId())) throw new BusinessException(403, "只有文章作者可以管理协作关系"); }
    private void requireArticleParticipant(Long actor, Long articleId) { Article a = articleMapper.selectById(articleId); if (a == null || a.getDeletedAt() != null) throw new BusinessException(404, "文章不存在"); if (!actor.equals(a.getAuthorUserId()) && collaboratorMapper.findActive(articleId, actor) == null) throw new BusinessException(403, "无权查看文章协作关系"); }
    private ArticleCollaborationInvitation invitation(Long id) { ArticleCollaborationInvitation value = invitationMapper.selectById(id); if (value == null) throw new BusinessException(404, "协作邀请不存在"); return value; }
    private static void requireResponse(ArticleCollaborationInvitation i, Long actor, RespondArticleCollaborationCommand c) { if (!actor.equals(i.getInviteeUserId()) || c == null || c.expectedLockVersion() == null || !c.expectedLockVersion().equals(i.getLockVersion())) throw collision(); }
    private void audit(Long article, Long actor, String event, Long target, String before, String after, LocalDateTime now) { ArticleCollaborationAuditEvent a = new ArticleCollaborationAuditEvent(); a.setId(IdWorker.getId()); a.setArticleId(article); a.setActorUserId(actor); a.setEventType(event); a.setTargetUserId(target); a.setBeforeSnapshot(before == null ? null : "{\"status\":\"" + before + "\"}"); a.setAfterSnapshot("{\"status\":\"" + after + "\"}"); a.setOccurredAt(now); auditMapper.insert(a); }
    private static CommunityNotificationEvent notification(String type, Long sender, Long recipient, Long target, String title, String content) { return new CommunityNotificationEvent(type, "ARTICLE", sender, recipient, "ARTICLE_COLLABORATION", target, title, content, type + ":" + target + ":" + recipient, "NORMAL"); }
    private static String key(String value) { if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,79}")) throw new BusinessException(400, "幂等键格式无效"); return value; }
    private static String normalizeType(String value) { String type = value == null ? "CO_AUTHOR" : value.trim().toUpperCase(); if (!TYPES.contains(type)) throw new BusinessException(400, "贡献类型无效"); return type; }
    private static int positive(Integer value, String label) { if (value == null || value < 1 || value > 1000) throw new BusinessException(400, label + "无效"); return value; }
    private static String trim(String value, int max) { if (value == null) return null; String result = value.trim(); return result.isEmpty() ? null : result.substring(0, Math.min(result.length(), max)); }
    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static BusinessException collision() { return new BusinessException(409, "协作关系已变化，请刷新后重试"); }
}
