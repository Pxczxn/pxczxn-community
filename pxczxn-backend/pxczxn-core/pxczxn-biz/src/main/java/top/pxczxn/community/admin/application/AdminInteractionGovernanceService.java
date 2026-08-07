package top.pxczxn.community.admin.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.notification.application.CommunityNotificationEvent;
import top.pxczxn.community.notification.application.MomentPublishedNotificationEvent;
import top.pxczxn.community.report.model.CommunityReport;
import top.pxczxn.community.report.persistence.CommunityReportMapper;
import top.pxczxn.community.social.model.CommunityComment;
import top.pxczxn.community.social.model.CommunityCommentModerationEvent;
import top.pxczxn.community.social.model.CommunityContentLike;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.model.CommunityMomentModerationEvent;
import top.pxczxn.community.social.model.FavoriteItem;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityCommentModerationEventMapper;
import top.pxczxn.community.social.persistence.CommunityContentLikeMapper;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.social.persistence.CommunityMomentModerationEventMapper;
import top.pxczxn.community.social.persistence.FavoriteItemMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminInteractionGovernanceService {

    private static final int MAX_BATCH_SIZE = 100;
    private static final Set<String> COMMENT_STATUSES = Set.of(
            "PENDING_REVIEW", "PUBLISHED", "HIDDEN_BY_AUTHOR",
            "HIDDEN_BY_BLOG", "DELETED_BY_USER", "TAKEN_DOWN", "SPAM"
    );
    private static final Set<String> MOMENT_STATUSES = Set.of(
            "PENDING_REVIEW", "PUBLISHED", "HIDDEN", "TAKEN_DOWN", "DELETED"
    );
    // 与 MomentType 枚举对齐：当前用户端仅支持这 8 种动态类型，
    // IMAGE / POLL / TEAM_NOTICE 暂未实现，待用户端真实支持后再同步扩展。
    private static final Set<String> MOMENT_TYPES = Set.of(
            "TEXT", "LINK", "ARTICLE_SHARE", "PROJECT_UPDATE",
            "CODE", "REPOST", "QUOTE", "VIDEO_LINK"
    );
    private static final Set<String> VISIBILITIES = Set.of(
            "PUBLIC", "FOLLOWERS_ONLY", "PRIVATE", "UNLISTED"
    );
    private static final Set<String> INTERACTION_TYPES =
            Set.of("LIKE", "FAVORITE", "FOLLOW");

    private final CommunityCommentMapper commentMapper;
    private final CommunityCommentModerationEventMapper commentEventMapper;
    private final CommunityMomentMapper momentMapper;
    private final CommunityMomentModerationEventMapper momentEventMapper;
    private final CommunityContentLikeMapper likeMapper;
    private final FavoriteItemMapper favoriteItemMapper;
    private final CommunityFollowMapper followMapper;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final ArticleMapper articleMapper;
    private final CommunityReportMapper reportMapper;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public AdminCommunityPageView<AdminCommunityCommentView> comments(
            String keyword,
            String status,
            String targetType,
            Long targetId,
            Long authorUserId,
            Integer pageNum,
            Integer pageSize
    ) {
        String normalizedStatus = optionalEnum(
                status, COMMENT_STATUSES, "评论状态"
        );
        String normalizedTargetType = optionalEnum(
                targetType, Set.of("ARTICLE", "MOMENT"), "评论目标类型"
        );
        Page<CommunityComment> page = new Page<>(
                page(pageNum), size(pageSize)
        );
        var query = Wrappers.<CommunityComment>lambdaQuery()
                .like(
                        StringUtils.hasText(keyword),
                        CommunityComment::getContentText,
                        trim(keyword)
                )
                .eq(
                        normalizedStatus != null,
                        CommunityComment::getStatus,
                        normalizedStatus
                )
                .eq(
                        normalizedTargetType != null,
                        CommunityComment::getTargetType,
                        normalizedTargetType
                )
                .eq(
                        targetId != null,
                        CommunityComment::getTargetId,
                        targetId
                )
                .eq(
                        authorUserId != null,
                        CommunityComment::getAuthorUserId,
                        authorUserId
                )
                .orderByDesc(CommunityComment::getCreatedAt)
                .orderByDesc(CommunityComment::getId);
        Page<CommunityComment> result = commentMapper.selectPage(page, query);
        return new AdminCommunityPageView<>(
                result.getRecords().stream()
                        .map(this::commentView)
                        .toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminCommunityCommentDetailView comment(Long commentId) {
        requireId(commentId, "评论");
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        List<AdminGovernanceEventView> events =
                commentEventMapper.selectList(
                        Wrappers.<CommunityCommentModerationEvent>lambdaQuery()
                                .eq(
                                        CommunityCommentModerationEvent::getCommentId,
                                        commentId
                                )
                                .orderByDesc(
                                        CommunityCommentModerationEvent::getCreatedAt
                                )
                                .orderByDesc(
                                        CommunityCommentModerationEvent::getId
                                )
                ).stream().map(this::commentEventView).toList();
        return new AdminCommunityCommentDetailView(
                commentView(comment, events.size()), events
        );
    }

    @Transactional(readOnly = true)
    public AdminCommunityPageView<AdminCommunityMomentView> moments(
            String keyword,
            String status,
            String visibility,
            String momentType,
            Long blogId,
            Long actorUserId,
            Integer pageNum,
            Integer pageSize
    ) {
        String normalizedStatus = optionalEnum(
                status, MOMENT_STATUSES, "动态状态"
        );
        String normalizedVisibility = optionalEnum(
                visibility, VISIBILITIES, "动态可见范围"
        );
        String normalizedMomentType = optionalEnum(
                momentType, MOMENT_TYPES, "动态类型"
        );
        Page<CommunityMoment> page = new Page<>(
                page(pageNum), size(pageSize)
        );
        var query = Wrappers.<CommunityMoment>lambdaQuery()
                .and(
                        StringUtils.hasText(keyword),
                        nested -> nested
                                .like(
                                        CommunityMoment::getTextContent,
                                        trim(keyword)
                                )
                                .or()
                                .like(
                                        CommunityMoment::getLinkUrl,
                                        trim(keyword)
                                )
                )
                .eq(
                        normalizedStatus != null,
                        CommunityMoment::getStatus,
                        normalizedStatus
                )
                .eq(
                        normalizedVisibility != null,
                        CommunityMoment::getVisibility,
                        normalizedVisibility
                )
                .eq(
                        normalizedMomentType != null,
                        CommunityMoment::getMomentType,
                        normalizedMomentType
                )
                .eq(
                        blogId != null,
                        CommunityMoment::getBlogId,
                        blogId
                )
                .eq(
                        actorUserId != null,
                        CommunityMoment::getActorUserId,
                        actorUserId
                )
                .orderByDesc(CommunityMoment::getCreatedAt)
                .orderByDesc(CommunityMoment::getId);
        Page<CommunityMoment> result = momentMapper.selectPage(page, query);
        return new AdminCommunityPageView<>(
                result.getRecords().stream()
                        .map(this::momentView)
                        .toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminCommunityMomentDetailView moment(Long momentId) {
        requireId(momentId, "动态");
        CommunityMoment moment = momentMapper.selectById(momentId);
        if (moment == null) {
            throw new BusinessException(404, "动态不存在");
        }
        List<AdminGovernanceEventView> events =
                momentEventMapper.selectList(
                        Wrappers.<CommunityMomentModerationEvent>lambdaQuery()
                                .eq(
                                        CommunityMomentModerationEvent::getMomentId,
                                        momentId
                                )
                                .orderByDesc(
                                        CommunityMomentModerationEvent::getCreatedAt
                                )
                                .orderByDesc(
                                        CommunityMomentModerationEvent::getId
                                )
                ).stream().map(this::momentEventView).toList();
        return new AdminCommunityMomentDetailView(
                momentView(moment, events.size()), events
        );
    }

    @Transactional(readOnly = true)
    public AdminMomentOverviewView overview() {
        LocalDateTime startOfDay = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay();
        long totalMoments = safeLong(momentMapper.selectCount(
                Wrappers.<CommunityMoment>lambdaQuery()
                        .isNull(CommunityMoment::getDeletedAt)
        ));
        long todayNew = safeLong(momentMapper.selectCount(
                Wrappers.<CommunityMoment>lambdaQuery()
                        .ge(CommunityMoment::getCreatedAt, startOfDay)
                        .isNull(CommunityMoment::getDeletedAt)
        ));
        long pendingReview = safeLong(momentMapper.selectCount(
                Wrappers.<CommunityMoment>lambdaQuery()
                        .eq(CommunityMoment::getStatus, "PENDING_REVIEW")
                        .isNull(CommunityMoment::getDeletedAt)
        ));
        long takenDown = safeLong(momentMapper.selectCount(
                Wrappers.<CommunityMoment>lambdaQuery()
                        .eq(CommunityMoment::getStatus, "TAKEN_DOWN")
                        .isNull(CommunityMoment::getDeletedAt)
        ));
        long todayLikes = safeLong(likeMapper.selectCount(
                Wrappers.<CommunityContentLike>lambdaQuery()
                        .eq(CommunityContentLike::getTargetType, "MOMENT")
                        .ge(CommunityContentLike::getCreatedAt, startOfDay)
        ));
        long todayFavorites = safeLong(favoriteItemMapper.selectCount(
                Wrappers.<FavoriteItem>lambdaQuery()
                        .eq(FavoriteItem::getTargetType, "MOMENT")
                        .ge(FavoriteItem::getCreatedAt, startOfDay)
        ));
        long todayComments = safeLong(commentMapper.selectCount(
                Wrappers.<CommunityComment>lambdaQuery()
                        .eq(CommunityComment::getTargetType, "MOMENT")
                        .ge(CommunityComment::getCreatedAt, startOfDay)
        ));
        return new AdminMomentOverviewView(
                totalMoments,
                todayNew,
                pendingReview,
                takenDown,
                todayLikes + todayFavorites + todayComments
        );
    }

    @Transactional(readOnly = true)
    public AdminCommunityPageView<AdminCommunityInteractionView> interactions(
            String interactionType,
            String targetType,
            Long targetId,
            Long actorUserId,
            Integer pageNum,
            Integer pageSize
    ) {
        String type = requiredEnum(
                interactionType, INTERACTION_TYPES, "互动类型"
        );
        String normalizedTargetType = StringUtils.hasText(targetType)
                ? normalize(targetType)
                : null;
        return switch (type) {
            case "LIKE" -> likeInteractions(
                    normalizedTargetType, targetId, actorUserId,
                    pageNum, pageSize
            );
            case "FAVORITE" -> favoriteInteractions(
                    normalizedTargetType, targetId, actorUserId,
                    pageNum, pageSize
            );
            case "FOLLOW" -> followInteractions(
                    normalizedTargetType, targetId, actorUserId,
                    pageNum, pageSize
            );
            default -> throw new BusinessException(400, "互动类型无效");
        };
    }

    @Transactional
    public AdminGovernanceResultView moderateComment(
            Long commentId,
            Long adminId,
            String rawAction,
            Integer expectedLockVersion,
            String reason
    ) {
        return moderateCommentInternal(
                commentId,
                adminId,
                GovernanceAction.parse(rawAction),
                expectedLockVersion,
                reason
        );
    }

    @Transactional
    public List<AdminGovernanceResultView> moderateComments(
            List<AdminGovernanceTarget> targets,
            Long adminId,
            String rawAction,
            String reason
    ) {
        GovernanceAction action = GovernanceAction.parse(rawAction);
        return validTargets(targets).stream()
                .map(target -> moderateCommentInternal(
                        target.id(),
                        adminId,
                        action,
                        target.expectedLockVersion(),
                        reason
                ))
                .toList();
    }

    @Transactional
    public AdminGovernanceResultView moderateMoment(
            Long momentId,
            Long adminId,
            String rawAction,
            Integer expectedLockVersion,
            String reason
    ) {
        return moderateMomentInternal(
                momentId,
                adminId,
                GovernanceAction.parse(rawAction),
                expectedLockVersion,
                reason
        );
    }

    @Transactional
    public List<AdminGovernanceResultView> moderateMoments(
            List<AdminGovernanceTarget> targets,
            Long adminId,
            String rawAction,
            String reason
    ) {
        GovernanceAction action = GovernanceAction.parse(rawAction);
        return validTargets(targets).stream()
                .map(target -> moderateMomentInternal(
                        target.id(),
                        adminId,
                        action,
                        target.expectedLockVersion(),
                        reason
                ))
                .toList();
    }

    private AdminGovernanceResultView moderateCommentInternal(
            Long commentId,
            Long adminId,
            GovernanceAction action,
            Integer expectedLockVersion,
            String rawReason
    ) {
        requireId(commentId, "评论");
        requireId(adminId, "管理员");
        requireLockVersion(expectedLockVersion);
        String reason = normalizeReason(rawReason, action);
        CommunityComment primary = commentMapper.selectById(commentId);
        if (primary == null || primary.getDeletedAt() != null) {
            throw new BusinessException(404, "评论不存在");
        }
        String eventAction = action.eventAction();
        String newStatus = action.newStatus();
        if (Objects.equals(primary.getStatus(), newStatus)
                && hasCommentEvent(commentId, eventAction, newStatus)) {
            return new AdminGovernanceResultView(
                    commentId,
                    newStatus,
                    safeInt(primary.getLockVersion()),
                    true,
                    0,
                    List.of()
            );
        }
        action.requireSourceStatus(primary.getStatus(), "评论");
        if (action == GovernanceAction.APPROVE) {
            requireReplyRootPublished(primary);
        }
        List<CommunityComment> candidates = commentCandidates(
                primary, action
        );
        String previousStatus = action.sourceStatus();
        String batchId = IdWorker.getIdStr();
        String metadata = "{\"batchId\":\"" + batchId + "\"}";
        List<Long> affectedIds = new ArrayList<>();
        List<CommunityComment> affectedComments = new ArrayList<>();
        for (CommunityComment candidate : candidates) {
            var update = Wrappers.<CommunityComment>update()
                    .eq("id", candidate.getId())
                    .eq("status", previousStatus)
                    .isNull("deleted_at")
                    .eq(
                            Objects.equals(candidate.getId(), commentId),
                            "lock_version",
                            expectedLockVersion
                    )
                    .set("status", newStatus)
                    .set("updated_at", LocalDateTime.now(ZoneOffset.UTC))
                    .setSql("lock_version = lock_version + 1");
            int changed = commentMapper.update(null, update);
            if (changed != 1) {
                if (Objects.equals(candidate.getId(), commentId)) {
                    throw conflict("评论");
                }
                continue;
            }
            affectedIds.add(candidate.getId());
            affectedComments.add(candidate);
            insertCommentEvent(
                    candidate.getId(),
                    eventAction,
                    adminId,
                    previousStatus,
                    newStatus,
                    reason,
                    metadata
            );
        }
        if (affectedIds.isEmpty()) {
            throw conflict("评论");
        }
        int delta = newStatus.equals("PUBLISHED")
                ? affectedIds.size()
                : -affectedIds.size();
        updateTargetCommentCount(
                primary.getTargetType(), primary.getTargetId(), delta
        );
        affectedComments.forEach(comment -> publishReviewNotification(
                "COMMENT",
                comment.getId(),
                comment.getAuthorUserId(),
                action,
                reason
        ));
        CommunityComment updated = commentMapper.selectById(commentId);
        return new AdminGovernanceResultView(
                commentId,
                newStatus,
                updated == null
                        ? expectedLockVersion + 1
                        : safeInt(updated.getLockVersion()),
                false,
                affectedIds.size(),
                List.copyOf(affectedIds)
        );
    }

    private AdminGovernanceResultView moderateMomentInternal(
            Long momentId,
            Long adminId,
            GovernanceAction action,
            Integer expectedLockVersion,
            String rawReason
    ) {
        requireId(momentId, "动态");
        requireId(adminId, "管理员");
        requireLockVersion(expectedLockVersion);
        String reason = normalizeReason(rawReason, action);
        CommunityMoment moment = momentMapper.selectById(momentId);
        if (moment == null || moment.getDeletedAt() != null) {
            throw new BusinessException(404, "动态不存在");
        }
        String eventAction = action.eventAction();
        String newStatus = action.newStatus();
        if (Objects.equals(moment.getStatus(), newStatus)
                && hasMomentEvent(momentId, eventAction, newStatus)) {
            return new AdminGovernanceResultView(
                    momentId,
                    newStatus,
                    safeInt(moment.getLockVersion()),
                    true,
                    0,
                    List.of()
            );
        }
        action.requireSourceStatus(moment.getStatus(), "动态");
        String previousStatus = moment.getStatus();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (momentMapper.update(
                null,
                Wrappers.<CommunityMoment>update()
                        .eq("id", momentId)
                        .eq("status", previousStatus)
                        .eq("lock_version", expectedLockVersion)
                        .isNull("deleted_at")
                        .set("status", newStatus)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        ) != 1) {
            throw conflict("动态");
        }
        updateSourceRepostCount(moment, previousStatus, newStatus);
        insertMomentEvent(
                momentId,
                eventAction,
                adminId,
                previousStatus,
                newStatus,
                reason
        );
        publishReviewNotification(
                "MOMENT",
                momentId,
                moment.getActorUserId(),
                action,
                reason
        );
        if ("PUBLISHED".equals(newStatus)) {
            publishMomentPublished(moment);
        }
        return new AdminGovernanceResultView(
                momentId,
                newStatus,
                expectedLockVersion + 1,
                false,
                1,
                List.of(momentId)
        );
    }

    private AdminCommunityPageView<AdminCommunityInteractionView>
    likeInteractions(
            String targetType,
            Long targetId,
            Long actorUserId,
            Integer pageNum,
            Integer pageSize
    ) {
        Page<CommunityContentLike> page = new Page<>(
                page(pageNum), size(pageSize)
        );
        Page<CommunityContentLike> result = likeMapper.selectPage(
                page,
                Wrappers.<CommunityContentLike>lambdaQuery()
                        .eq(
                                targetType != null,
                                CommunityContentLike::getTargetType,
                                targetType
                        )
                        .eq(
                                targetId != null,
                                CommunityContentLike::getTargetId,
                                targetId
                        )
                        .eq(
                                actorUserId != null,
                                CommunityContentLike::getUserId,
                                actorUserId
                        )
                        .orderByDesc(CommunityContentLike::getCreatedAt)
                        .orderByDesc(CommunityContentLike::getId)
        );
        return new AdminCommunityPageView<>(
                result.getRecords().stream()
                        .map(value -> interactionView(
                                value.getId(),
                                "LIKE",
                                value.getUserId(),
                                value.getTargetType(),
                                value.getTargetId(),
                                null,
                                false,
                                value.getCreatedAt(),
                                null
                        ))
                        .toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    private AdminCommunityPageView<AdminCommunityInteractionView>
    favoriteInteractions(
            String targetType,
            Long targetId,
            Long actorUserId,
            Integer pageNum,
            Integer pageSize
    ) {
        Page<FavoriteItem> page = new Page<>(
                page(pageNum), size(pageSize)
        );
        Page<FavoriteItem> result = favoriteItemMapper.selectPage(
                page,
                Wrappers.<FavoriteItem>lambdaQuery()
                        .eq(
                                targetType != null,
                                FavoriteItem::getTargetType,
                                targetType
                        )
                        .eq(
                                targetId != null,
                                FavoriteItem::getTargetId,
                                targetId
                        )
                        .eq(
                                actorUserId != null,
                                FavoriteItem::getOwnerUserId,
                                actorUserId
                        )
                        .orderByDesc(FavoriteItem::getCreatedAt)
                        .orderByDesc(FavoriteItem::getId)
        );
        return new AdminCommunityPageView<>(
                result.getRecords().stream()
                        .map(value -> interactionView(
                                value.getId(),
                                "FAVORITE",
                                value.getOwnerUserId(),
                                value.getTargetType(),
                                value.getTargetId(),
                                null,
                                false,
                                value.getCreatedAt(),
                                null
                        ))
                        .toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    private AdminCommunityPageView<AdminCommunityInteractionView>
    followInteractions(
            String targetType,
            Long targetId,
            Long actorUserId,
            Integer pageNum,
            Integer pageSize
    ) {
        Page<CommunityFollow> page = new Page<>(
                page(pageNum), size(pageSize)
        );
        Page<CommunityFollow> result = followMapper.selectPage(
                page,
                Wrappers.<CommunityFollow>lambdaQuery()
                        .eq(
                                targetType != null,
                                CommunityFollow::getTargetType,
                                targetType
                        )
                        .eq(
                                targetId != null,
                                CommunityFollow::getTargetId,
                                targetId
                        )
                        .eq(
                                actorUserId != null,
                                CommunityFollow::getFollowerUserId,
                                actorUserId
                        )
                        .orderByDesc(CommunityFollow::getCreatedAt)
                        .orderByDesc(CommunityFollow::getId)
        );
        return new AdminCommunityPageView<>(
                result.getRecords().stream()
                        .map(value -> interactionView(
                                value.getId(),
                                "FOLLOW",
                                value.getFollowerUserId(),
                                value.getTargetType(),
                                value.getTargetId(),
                                value.getNotificationLevel(),
                                Objects.equals(value.getSpecialFollow(), 1),
                                value.getCreatedAt(),
                                value.getUpdatedAt()
                        ))
                        .toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    private AdminCommunityCommentView commentView(
            CommunityComment comment
    ) {
        long eventCount = safeLong(commentEventMapper.selectCount(
                Wrappers.<CommunityCommentModerationEvent>lambdaQuery()
                        .eq(
                                CommunityCommentModerationEvent::getCommentId,
                                comment.getId()
                        )
        ));
        return commentView(comment, eventCount);
    }

    private AdminCommunityCommentView commentView(
            CommunityComment comment,
            long eventCount
    ) {
        CommunityUser author = userMapper.selectById(
                comment.getAuthorUserId()
        );
        return new AdminCommunityCommentView(
                comment.getId(),
                comment.getAuthorUserId(),
                author == null ? null : author.getUsername(),
                author == null ? null : author.getDisplayName(),
                comment.getTargetType(),
                comment.getTargetId(),
                targetTitle(
                        comment.getTargetType(), comment.getTargetId()
                ),
                comment.getRootCommentId(),
                comment.getParentCommentId(),
                comment.getReplyToUserId(),
                comment.getContentText(),
                comment.getRenderedHtml(),
                comment.getStatus(),
                safeLong(comment.getLikeCount()),
                safeInt(comment.getLockVersion()),
                eventCount,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getDeletedAt()
        );
    }

    private AdminCommunityMomentView momentView(CommunityMoment moment) {
        long eventCount = safeLong(momentEventMapper.selectCount(
                Wrappers.<CommunityMomentModerationEvent>lambdaQuery()
                        .eq(
                                CommunityMomentModerationEvent::getMomentId,
                                moment.getId()
                        )
        ));
        return momentView(moment, eventCount);
    }

    private AdminCommunityMomentView momentView(
            CommunityMoment moment,
            long eventCount
    ) {
        CommunityUser actor = userMapper.selectById(
                moment.getActorUserId()
        );
        Blog blog = blogMapper.selectById(moment.getBlogId());
        long reportCount = safeLong(reportMapper.selectCount(
                Wrappers.<CommunityReport>lambdaQuery()
                        .eq(CommunityReport::getTargetType, "MOMENT")
                        .eq(CommunityReport::getTargetId, moment.getId())
        ));
        return new AdminCommunityMomentView(
                moment.getId(),
                moment.getActorUserId(),
                actor == null ? null : actor.getUsername(),
                actor == null ? null : actor.getDisplayName(),
                moment.getBlogId(),
                blog == null ? null : blog.getName(),
                blog == null ? null : blog.getSlug(),
                moment.getMomentType(),
                moment.getTextContent(),
                moment.getRenderedHtml(),
                moment.getLinkUrl(),
                moment.getArticleId(),
                moment.getRepostMomentId(),
                moment.getVisibility(),
                moment.getStatus(),
                safeLong(moment.getLikeCount()),
                safeLong(moment.getFavoriteCount()),
                safeLong(moment.getCommentCount()),
                safeLong(moment.getRepostCount()),
                safeInt(moment.getLockVersion()),
                eventCount,
                reportCount,
                moment.getCreatedAt(),
                moment.getUpdatedAt(),
                moment.getDeletedAt()
        );
    }

    private AdminCommunityInteractionView interactionView(
            Long id,
            String interactionType,
            Long actorUserId,
            String targetType,
            Long targetId,
            String notificationLevel,
            boolean specialFollow,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        CommunityUser actor = userMapper.selectById(actorUserId);
        return new AdminCommunityInteractionView(
                id,
                interactionType,
                actorUserId,
                actor == null ? null : actor.getUsername(),
                actor == null ? null : actor.getDisplayName(),
                targetType,
                targetId,
                targetTitle(targetType, targetId),
                notificationLevel,
                specialFollow,
                createdAt,
                updatedAt
        );
    }

    private AdminGovernanceEventView commentEventView(
            CommunityCommentModerationEvent event
    ) {
        return new AdminGovernanceEventView(
                event.getId(),
                "COMMENT",
                event.getCommentId(),
                event.getAction(),
                event.getActorType(),
                event.getActorUserId(),
                event.getActorAdminId(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getReason(),
                event.getMetadataJson(),
                event.getCreatedAt()
        );
    }

    private AdminGovernanceEventView momentEventView(
            CommunityMomentModerationEvent event
    ) {
        return new AdminGovernanceEventView(
                event.getId(),
                "MOMENT",
                event.getMomentId(),
                event.getAction(),
                "ADMIN",
                null,
                event.getActorAdminId(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getReason(),
                event.getMetadataJson(),
                event.getCreatedAt()
        );
    }

    private List<CommunityComment> commentCandidates(
            CommunityComment comment,
            GovernanceAction action
    ) {
        if (comment.getRootCommentId() != null) {
            return List.of(comment);
        }
        if (action == GovernanceAction.TAKE_DOWN) {
            return commentMapper.findPublishedThread(comment.getId());
        }
        if (action == GovernanceAction.RESTORE) {
            return commentMapper.findTakenDownThread(comment.getId());
        }
        return List.of(comment);
    }

    private void requireReplyRootPublished(CommunityComment comment) {
        if (comment.getRootCommentId() == null) {
            return;
        }
        CommunityComment root = commentMapper.selectById(
                comment.getRootCommentId()
        );
        if (root == null
                || !"PUBLISHED".equals(root.getStatus())
                || root.getDeletedAt() != null) {
            throw new BusinessException(
                    409, "一级评论不可见，不能通过该回复"
            );
        }
    }

    private void updateTargetCommentCount(
            String targetType,
            Long targetId,
            int delta
    ) {
        if (delta == 0) {
            return;
        }
        String sql = delta > 0
                ? "comment_count = comment_count + " + delta
                : "comment_count = GREATEST(comment_count - "
                        + (-delta) + ", 0)";
        int changed;
        if ("ARTICLE".equals(targetType)) {
            changed = articleMapper.update(
                    null,
                    Wrappers.<Article>update()
                            .eq("id", targetId)
                            .isNull("deleted_at")
                            .setSql(sql)
            );
        } else if ("MOMENT".equals(targetType)) {
            changed = momentMapper.update(
                    null,
                    Wrappers.<CommunityMoment>update()
                            .eq("id", targetId)
                            .isNull("deleted_at")
                            .setSql(sql)
            );
        } else {
            throw new BusinessException(409, "评论目标类型无效");
        }
        if (changed != 1) {
            throw new BusinessException(
                    409, "评论目标状态已发生变化，请刷新后重试"
            );
        }
    }

    private void updateSourceRepostCount(
            CommunityMoment moment,
            String previousStatus,
            String newStatus
    ) {
        if (moment.getRepostMomentId() == null
                || Objects.equals(
                        "PUBLISHED".equals(previousStatus),
                        "PUBLISHED".equals(newStatus)
                )) {
            return;
        }
        String sql = "PUBLISHED".equals(newStatus)
                ? "repost_count = repost_count + 1"
                : "repost_count = GREATEST(repost_count - 1, 0)";
        var update = Wrappers.<CommunityMoment>update()
                .eq("id", moment.getRepostMomentId())
                .isNull("deleted_at")
                .setSql(sql);
        if ("PUBLISHED".equals(newStatus)) {
            update.eq("status", "PUBLISHED");
        }
        if (momentMapper.update(null, update) != 1) {
            throw new BusinessException(
                    409, "转发源动态已不可用，不能恢复或通过审核"
            );
        }
    }

    private void insertCommentEvent(
            Long commentId,
            String action,
            Long adminId,
            String previousStatus,
            String newStatus,
            String reason,
            String metadata
    ) {
        CommunityCommentModerationEvent event =
                new CommunityCommentModerationEvent();
        event.setId(IdWorker.getId());
        event.setCommentId(commentId);
        event.setAction(action);
        event.setActorType("ADMIN");
        event.setActorAdminId(adminId);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setReason(reason);
        event.setMetadataJson(metadata);
        event.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        if (commentEventMapper.insert(event) != 1) {
            throw new BusinessException(500, "记录评论治理事件失败");
        }
    }

    private void insertMomentEvent(
            Long momentId,
            String action,
            Long adminId,
            String previousStatus,
            String newStatus,
            String reason
    ) {
        CommunityMomentModerationEvent event =
                new CommunityMomentModerationEvent();
        event.setId(IdWorker.getId());
        event.setMomentId(momentId);
        event.setAction(action);
        event.setActorAdminId(adminId);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setReason(reason);
        event.setMetadataJson(null);
        event.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        if (momentEventMapper.insert(event) != 1) {
            throw new BusinessException(500, "记录动态治理事件失败");
        }
    }

    private boolean hasCommentEvent(
            Long commentId,
            String action,
            String newStatus
    ) {
        return safeLong(commentEventMapper.selectCount(
                Wrappers.<CommunityCommentModerationEvent>lambdaQuery()
                        .eq(
                                CommunityCommentModerationEvent::getCommentId,
                                commentId
                        )
                        .eq(
                                CommunityCommentModerationEvent::getAction,
                                action
                        )
                        .eq(
                                CommunityCommentModerationEvent::getNewStatus,
                                newStatus
                        )
        )) > 0;
    }

    private boolean hasMomentEvent(
            Long momentId,
            String action,
            String newStatus
    ) {
        return safeLong(momentEventMapper.selectCount(
                Wrappers.<CommunityMomentModerationEvent>lambdaQuery()
                        .eq(
                                CommunityMomentModerationEvent::getMomentId,
                                momentId
                        )
                        .eq(
                                CommunityMomentModerationEvent::getAction,
                                action
                        )
                        .eq(
                                CommunityMomentModerationEvent::getNewStatus,
                                newStatus
                        )
        )) > 0;
    }

    private void publishReviewNotification(
            String subjectType,
            Long subjectId,
            Long recipientUserId,
            GovernanceAction action,
            String reason
    ) {
        if (eventPublisher == null || recipientUserId == null) {
            return;
        }
        String subject = "COMMENT".equals(subjectType) ? "评论" : "动态";
        String title = subject + action.notificationTitle();
        String content = reason == null || reason.isBlank()
                ? title + "。"
                : title + "。审核说明：" + reason;
        eventPublisher.publishEvent(new CommunityNotificationEvent(
                subjectType + "_REVIEW",
                "REVIEW",
                null,
                recipientUserId,
                subjectType,
                subjectId,
                title,
                content,
                "governance:" + subjectType + ":" + subjectId
                        + ":" + action.name(),
                "HIGH"
        ));
    }

    private void publishMomentPublished(CommunityMoment moment) {
        if (eventPublisher == null) {
            return;
        }
        eventPublisher.publishEvent(new MomentPublishedNotificationEvent(
                moment.getId(),
                moment.getBlogId(),
                moment.getActorUserId(),
                moment.getMomentType(),
                moment.getVisibility()
        ));
    }

    private String targetTitle(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return null;
        }
        return switch (targetType) {
            case "ARTICLE" -> {
                Article article = articleMapper.selectById(targetId);
                yield article == null ? "已删除文章" : article.getTitle();
            }
            case "MOMENT" -> {
                CommunityMoment moment = momentMapper.selectById(targetId);
                yield moment == null
                        ? "已删除动态"
                        : summarize(moment.getTextContent(), "动态");
            }
            case "COMMENT" -> {
                CommunityComment comment = commentMapper.selectById(targetId);
                yield comment == null
                        ? "已删除评论"
                        : summarize(comment.getContentText(), "评论");
            }
            case "BLOG" -> {
                Blog blog = blogMapper.selectById(targetId);
                yield blog == null ? "已删除博客" : blog.getName();
            }
            default -> targetType + " #" + targetId;
        };
    }

    private static List<AdminGovernanceTarget> validTargets(
            List<AdminGovernanceTarget> targets
    ) {
        if (targets == null || targets.isEmpty()) {
            throw new BusinessException(400, "请选择治理目标");
        }
        if (targets.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(400, "单次最多治理 100 条内容");
        }
        Map<Long, AdminGovernanceTarget> unique = new LinkedHashMap<>();
        for (AdminGovernanceTarget target : targets) {
            if (target == null) {
                throw new BusinessException(400, "治理目标无效");
            }
            requireId(target.id(), "治理目标");
            requireLockVersion(target.expectedLockVersion());
            if (unique.putIfAbsent(target.id(), target) != null) {
                throw new BusinessException(400, "治理目标不能重复");
            }
        }
        return List.copyOf(unique.values());
    }

    private static String normalizeReason(
            String rawReason,
            GovernanceAction action
    ) {
        String reason = rawReason == null
                ? ""
                : Normalizer.normalize(
                        rawReason.strip(), Normalizer.Form.NFKC
                );
        if (action.reasonRequired() && reason.isBlank()) {
            throw new BusinessException(400, "请填写治理原因");
        }
        if (reason.length() > 500) {
            throw new BusinessException(400, "治理原因不能超过 500 个字符");
        }
        return reason.isBlank() ? null : reason;
    }

    private static String optionalEnum(
            String raw,
            Set<String> values,
            String label
    ) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return requiredEnum(raw, values, label);
    }

    private static String requiredEnum(
            String raw,
            Set<String> values,
            String label
    ) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(400, label + "不能为空");
        }
        String value = normalize(raw);
        if (!values.contains(value)) {
            throw new BusinessException(400, label + "无效");
        }
        return value;
    }

    private static String normalize(String value) {
        return value == null
                ? null
                : value.strip().toUpperCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String summarize(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        return normalized.length() <= 80
                ? normalized
                : normalized.substring(0, 80) + "…";
    }

    private static long page(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private static long size(Integer value) {
        return value == null ? 20 : Math.max(1, Math.min(value, 100));
    }

    private static void requireId(Long id, String label) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, label + " ID 无效");
        }
    }

    private static void requireLockVersion(Integer value) {
        if (value == null || value < 0) {
            throw new BusinessException(400, "锁版本无效");
        }
    }

    private static BusinessException conflict(String label) {
        return new BusinessException(
                409, label + "状态已发生变化，请刷新后重试"
        );
    }

    private static long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private enum GovernanceAction {
        APPROVE(
                "PENDING_REVIEW",
                "PUBLISHED",
                "PLATFORM_APPROVED",
                "审核通过",
                false
        ),
        REJECT(
                "PENDING_REVIEW",
                "TAKEN_DOWN",
                "PLATFORM_REJECTED",
                "审核未通过",
                true
        ),
        TAKE_DOWN(
                "PUBLISHED",
                "TAKEN_DOWN",
                "PLATFORM_TAKEN_DOWN",
                "已由平台下架",
                true
        ),
        RESTORE(
                "TAKEN_DOWN",
                "PUBLISHED",
                "PLATFORM_RESTORED",
                "已由平台恢复",
                false
        );

        private final String sourceStatus;
        private final String newStatus;
        private final String eventAction;
        private final String notificationTitle;
        private final boolean reasonRequired;

        GovernanceAction(
                String sourceStatus,
                String newStatus,
                String eventAction,
                String notificationTitle,
                boolean reasonRequired
        ) {
            this.sourceStatus = sourceStatus;
            this.newStatus = newStatus;
            this.eventAction = eventAction;
            this.notificationTitle = notificationTitle;
            this.reasonRequired = reasonRequired;
        }

        static GovernanceAction parse(String raw) {
            if (!StringUtils.hasText(raw)) {
                throw new BusinessException(400, "治理动作不能为空");
            }
            try {
                return valueOf(normalize(raw));
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(400, "治理动作无效");
            }
        }

        void requireSourceStatus(String actual, String label) {
            if (!Objects.equals(sourceStatus, actual)) {
                throw new BusinessException(
                        409, label + "当前状态不能执行该治理动作"
                );
            }
        }

        String sourceStatus() {
            return sourceStatus;
        }

        String newStatus() {
            return newStatus;
        }

        String eventAction() {
            return eventAction;
        }

        String notificationTitle() {
            return notificationTitle;
        }

        boolean reasonRequired() {
            return reasonRequired;
        }
    }
}
