package top.pxczxn.community.social.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mars.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.moderation.application.ArticleKeywordReviewEngine;
import top.pxczxn.community.moderation.application.KeywordReviewOutcome;
import top.pxczxn.community.notification.application.CommunityNotificationEvent;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityComment;
import top.pxczxn.community.social.model.CommunityCommentModerationEvent;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityCommentModerationEventMapper;
import top.pxczxn.community.social.persistence.CommunityContentLikeMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int REPLY_PREVIEW_SIZE = 3;
    private static final int MAX_MENTION_RECIPIENTS = 20;
    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "(?<![a-z0-9_-])@([a-z0-9][a-z0-9_-]{2,31})",
            Pattern.CASE_INSENSITIVE
    );

    private final CommunityCommentMapper commentMapper;
    private final CommunityCommentModerationEventMapper eventMapper;
    private final CommunityContentLikeMapper likeMapper;
    private final CommunityContentAccessService contentAccessService;
    private final CommentScopeService scopeService;
    private final CommentContentRenderer contentRenderer;
    private final ArticleKeywordReviewEngine keywordReviewEngine;
    private final ArticleMapper articleMapper;
    private final CommunityMomentMapper momentMapper;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final CommunityAuth communityAuth;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommentView create(
            String rawType,
            Long targetId,
            String content
    ) {
        LikeTargetType type = commentTargetType(rawType);
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(type, targetId);
        CommentActorContext context =
                scopeService.requireCanComment(target);
        return createInternal(
                target,
                context.actor(),
                null,
                null,
                null,
                content
        );
    }

    @Transactional
    public CommentView reply(Long parentCommentId, String content) {
        requireValidCommentId(parentCommentId);
        CommunityComment parent = commentMapper.selectById(parentCommentId);
        if (!isPublished(parent)) {
            throw new BusinessException(404, "评论不存在");
        }
        LikeTargetType type = commentTargetType(parent.getTargetType());
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(
                        type, parent.getTargetId()
                );
        Long rootId = parent.getRootCommentId() == null
                ? parent.getId()
                : parent.getRootCommentId();
        CommunityComment root = commentMapper.selectById(rootId);
        if (!isPublished(root)
                || !Objects.equals(root.getTargetType(), parent.getTargetType())
                || !Objects.equals(root.getTargetId(), parent.getTargetId())) {
            throw new BusinessException(404, "评论不存在");
        }
        CommentActorContext context =
                scopeService.requireCanComment(target);
        return createInternal(
                target,
                context.actor(),
                rootId,
                parent.getId(),
                parent.getAuthorUserId(),
                content
        );
    }

    @Transactional(readOnly = true)
    public CommentPageView page(
            String rawType,
            Long targetId,
            int pageNum,
            int pageSize
    ) {
        LikeTargetType type = commentTargetType(rawType);
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(type, targetId);
        List<CommunityComment> roots = commentMapper.findPublicRoots(
                type.name(), targetId
        );
        int validPageNum = validPage(pageNum);
        int validPageSize = validPageSize(pageSize);
        List<CommunityComment> pageRoots = slice(
                roots, validPageNum, validPageSize
        );
        List<CommentThreadView> records = new ArrayList<>();
        for (CommunityComment root : pageRoots) {
            List<CommunityComment> replies =
                    commentMapper.findPublicReplies(root.getId());
            records.add(new CommentThreadView(
                    publicView(root),
                    replies.stream()
                            .limit(REPLY_PREVIEW_SIZE)
                            .map(this::publicView)
                            .toList(),
                    replies.size()
            ));
        }
        return new CommentPageView(
                type.name(),
                targetId,
                records,
                roots.size(),
                target.commentCount(),
                validPageNum,
                validPageSize
        );
    }

    @Transactional(readOnly = true)
    public CommentReplyPageView replies(
            Long rootCommentId,
            int pageNum,
            int pageSize
    ) {
        requireValidCommentId(rootCommentId);
        CommunityComment root = commentMapper.selectById(rootCommentId);
        if (!isPublicRoot(root)) {
            throw new BusinessException(404, "评论不存在");
        }
        contentAccessService.requireAccessible(
                commentTargetType(root.getTargetType()),
                root.getTargetId()
        );
        List<CommunityComment> replies =
                commentMapper.findPublicReplies(rootCommentId);
        int validPageNum = validPage(pageNum);
        int validPageSize = validPageSize(pageSize);
        return new CommentReplyPageView(
                rootCommentId,
                slice(replies, validPageNum, validPageSize).stream()
                        .map(this::publicView)
                        .toList(),
                replies.size(),
                validPageNum,
                validPageSize
        );
    }

    @Transactional
    public CommentModerationView delete(Long commentId) {
        requireValidCommentId(commentId);
        Long actorId = communityAuth.getLoginUserId();
        CommunityUser actor = requireActiveUser(actorId);
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null
                || !Objects.equals(comment.getAuthorUserId(), actor.getId())) {
            throw new BusinessException(404, "评论不存在");
        }
        LikeTargetType type = commentTargetType(comment.getTargetType());
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(
                        type, comment.getTargetId()
                );
        if ("DELETED_BY_USER".equals(comment.getStatus())) {
            return new CommentModerationView(
                    commentId,
                    comment.getStatus(),
                    0,
                    target.commentCount()
            );
        }
        if (!"PUBLISHED".equals(comment.getStatus())
                && !"PENDING_REVIEW".equals(comment.getStatus())) {
            throw new BusinessException(409, "评论当前状态不能删除");
        }
        String previous = comment.getStatus();
        if (commentMapper.update(
                null,
                Wrappers.<CommunityComment>update()
                        .eq("id", commentId)
                        .eq("author_user_id", actorId)
                        .eq("status", previous)
                        .set("status", "DELETED_BY_USER")
                        .set("deleted_at", LocalDateTime.now(ZoneOffset.UTC))
        ) != 1) {
            throw new BusinessException(409, "评论状态已发生变化，请重试");
        }
        insertEvent(
                commentId,
                "USER_DELETED",
                "USER",
                actorId,
                previous,
                "DELETED_BY_USER",
                "用户删除自己的评论",
                null
        );
        int affected = "PUBLISHED".equals(previous) ? 1 : 0;
        if (affected > 0) {
            decrementTarget(type, comment.getTargetId(), affected);
        }
        return new CommentModerationView(
                commentId,
                "DELETED_BY_USER",
                affected,
                Math.max(0, target.commentCount() - affected)
        );
    }

    @Transactional
    public CommentModerationView hide(
            Long commentId,
            String reason
    ) {
        requireValidCommentId(commentId);
        Long actorId = communityAuth.getLoginUserId();
        requireActiveUser(actorId);
        CommunityComment comment = commentMapper.selectById(commentId);
        if (!isPublished(comment)) {
            throw new BusinessException(404, "评论不存在");
        }
        LikeTargetType type = commentTargetType(comment.getTargetType());
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(
                        type, comment.getTargetId()
                );
        if (Objects.equals(actorId, comment.getAuthorUserId())) {
            throw new BusinessException(400, "请使用删除功能处理自己的评论");
        }
        Blog blog = blogMapper.selectById(target.blogId());
        boolean contentAuthor =
                Objects.equals(actorId, target.authorUserId());
        boolean blogOwner = blog != null
                && Objects.equals(actorId, blog.getOwnerUserId());
        if (!contentAuthor && !blogOwner) {
            throw new BusinessException(403, "无权隐藏该评论");
        }
        String status = contentAuthor
                ? "HIDDEN_BY_AUTHOR"
                : "HIDDEN_BY_BLOG";
        String action = contentAuthor
                ? "AUTHOR_HIDDEN"
                : "BLOG_HIDDEN";
        String normalizedReason = normalizeReason(reason);
        List<CommunityComment> affectedComments =
                comment.getRootCommentId() == null
                        ? commentMapper.findPublishedThread(comment.getId())
                        : List.of(comment);
        int affected = 0;
        for (CommunityComment affectedComment : affectedComments) {
            if (commentMapper.update(
                    null,
                    Wrappers.<CommunityComment>update()
                            .eq("id", affectedComment.getId())
                            .eq("status", "PUBLISHED")
                            .set("status", status)
            ) == 1) {
                affected++;
                insertEvent(
                        affectedComment.getId(),
                        action,
                        "USER",
                        actorId,
                        "PUBLISHED",
                        status,
                        normalizedReason,
                        null
                );
            }
        }
        if (affected == 0) {
            throw new BusinessException(409, "评论状态已发生变化，请重试");
        }
        decrementTarget(type, comment.getTargetId(), affected);
        return new CommentModerationView(
                commentId,
                status,
                affected,
                Math.max(0, target.commentCount() - affected)
        );
    }

    private CommentView createInternal(
            AccessibleContentTarget target,
            CommunityUser actor,
            Long rootCommentId,
            Long parentCommentId,
            Long replyToUserId,
            String content
    ) {
        RenderedCommentContent rendered =
                contentRenderer.render(content);
        KeywordReviewOutcome outcome = keywordReviewEngine.review(
                null, null, rendered.contentText()
        );
        if (outcome.decision() == KeywordReviewOutcome.Decision.BLOCK) {
            throw new BusinessException(400, "评论包含禁止内容，无法发布");
        }
        boolean pending = outcome.decision()
                == KeywordReviewOutcome.Decision.MANUAL_REVIEW;
        String status = pending ? "PENDING_REVIEW" : "PUBLISHED";
        CommunityComment comment = new CommunityComment();
        comment.setId(IdWorker.getId());
        comment.setAuthorUserId(actor.getId());
        comment.setTargetType(target.targetType().name());
        comment.setTargetId(target.targetId());
        comment.setRootCommentId(rootCommentId);
        comment.setParentCommentId(parentCommentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setContentText(rendered.contentText());
        comment.setRenderedHtml(rendered.renderedHtml());
        comment.setStatus(status);
        comment.setLikeCount(0L);
        comment.setLockVersion(0);
        comment.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        if (commentMapper.insert(comment) != 1) {
            throw new BusinessException(500, "发表评论失败");
        }
        insertEvent(
                comment.getId(),
                pending ? "AUTO_REVIEW_QUEUED" : "AUTO_PUBLISHED",
                "SYSTEM",
                null,
                null,
                status,
                outcome.resultReason(),
                keywordMetadata(outcome)
        );
        if (!pending && incrementTarget(
                target.targetType(), target.targetId()
        ) != 1) {
            throw new BusinessException(409, "内容状态已发生变化，请刷新后重试");
        }
        if (!pending) {
            publishCommentNotifications(
                    comment, target, actor, replyToUserId
            );
        }
        boolean warning = "AUTO_APPROVED_WITH_WARNING".equals(
                outcome.resultCode()
        );
        return view(
                comment,
                actor,
                false,
                false,
                warning,
                outcome.resultCode()
        );
    }

    private void publishCommentNotifications(
            CommunityComment comment,
            AccessibleContentTarget target,
            CommunityUser actor,
            Long replyToUserId
    ) {
        if (eventPublisher == null) {
            return;
        }
        LinkedHashSet<Long> notified = new LinkedHashSet<>();
        if (replyToUserId != null
                && !Objects.equals(replyToUserId, actor.getId())) {
            publishCommentEvent(
                    "REPLY",
                    actor.getId(),
                    replyToUserId,
                    comment,
                    "有人回复了你的评论",
                    "有人回复了你的评论。",
                    "reply:comment:" + comment.getParentCommentId()
            );
            notified.add(replyToUserId);
        }
        if (target.authorUserId() != null
                && !Objects.equals(target.authorUserId(), actor.getId())
                && notified.add(target.authorUserId())) {
            publishCommentEvent(
                    "COMMENT",
                    actor.getId(),
                    target.authorUserId(),
                    comment,
                    "有人评论了你的内容",
                    "有人评论了你发布的内容。",
                    "comment:" + target.targetType().name()
                            + ":" + target.targetId()
            );
        }
        for (CommunityUser mentioned : mentionedUsers(
                comment.getContentText()
        )) {
            if (Objects.equals(mentioned.getId(), actor.getId())
                    || !notified.add(mentioned.getId())) {
                continue;
            }
            publishCommentEvent(
                    "MENTION",
                    actor.getId(),
                    mentioned.getId(),
                    comment,
                    "有人在评论中提到了你",
                    "有人在一条评论中提到了你。",
                    "mention:" + target.targetType().name()
                            + ":" + target.targetId()
            );
        }
    }

    private List<CommunityUser> mentionedUsers(String content) {
        LinkedHashSet<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(
                content == null ? "" : content
        );
        while (matcher.find()
                && usernames.size() < MAX_MENTION_RECIPIENTS) {
            usernames.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        if (usernames.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(
                Wrappers.<CommunityUser>lambdaQuery()
                        .in(CommunityUser::getUsername, usernames)
                        .in(
                                CommunityUser::getStatus,
                                "NORMAL",
                                "LIMITED"
                        )
        );
    }

    private void publishCommentEvent(
            String notificationType,
            Long senderUserId,
            Long recipientUserId,
            CommunityComment comment,
            String title,
            String content,
            String aggregateKey
    ) {
        eventPublisher.publishEvent(new CommunityNotificationEvent(
                notificationType,
                "COMMENT",
                senderUserId,
                recipientUserId,
                "COMMENT",
                comment.getId(),
                title,
                content,
                aggregateKey,
                "NORMAL"
        ));
    }

    private CommentView publicView(CommunityComment comment) {
        boolean deleted = "DELETED_BY_USER".equals(comment.getStatus());
        CommunityUser author = deleted
                ? null
                : userMapper.selectById(comment.getAuthorUserId());
        Long viewerId = communityAuth.getOptionalLoginUserId();
        boolean liked = !deleted
                && viewerId != null
                && likeMapper.findRelation(
                        viewerId, "COMMENT", comment.getId()
                ) != null;
        return view(comment, author, liked, deleted, false, null);
    }

    private static CommentView view(
            CommunityComment comment,
            CommunityUser author,
            boolean liked,
            boolean deleted,
            boolean warning,
            String moderationResult
    ) {
        return new CommentView(
                comment.getId(),
                comment.getTargetType(),
                comment.getTargetId(),
                comment.getRootCommentId(),
                comment.getParentCommentId(),
                comment.getReplyToUserId(),
                deleted ? null : author(author),
                deleted ? "该评论已删除" : comment.getContentText(),
                deleted ? "<p>该评论已删除</p>" : comment.getRenderedHtml(),
                comment.getStatus(),
                deleted,
                deleted ? 0 : safeLong(comment.getLikeCount()),
                !deleted && liked,
                warning,
                moderationResult,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private static CommentAuthorView author(CommunityUser user) {
        if (user == null) {
            return new CommentAuthorView(
                    null, null, "社区用户", null
            );
        }
        return new CommentAuthorView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarFileId()
        );
    }

    private int incrementTarget(LikeTargetType type, Long targetId) {
        return switch (type) {
            case ARTICLE -> articleMapper.update(
                    null,
                    Wrappers.<Article>update()
                            .eq("id", targetId)
                            .isNull("deleted_at")
                            .setSql("comment_count = comment_count + 1")
            );
            case MOMENT -> momentMapper.update(
                    null,
                    Wrappers.<CommunityMoment>update()
                            .eq("id", targetId)
                            .eq("status", "PUBLISHED")
                            .isNull("deleted_at")
                            .setSql("comment_count = comment_count + 1")
            );
            case COMMENT -> throw new BusinessException(
                    400, "评论目标类型无效"
            );
        };
    }

    private void decrementTarget(
            LikeTargetType type,
            Long targetId,
            int amount
    ) {
        if (amount <= 0) {
            return;
        }
        String sql = "comment_count = GREATEST(comment_count - "
                + amount + ", 0)";
        switch (type) {
            case ARTICLE -> articleMapper.update(
                    null,
                    Wrappers.<Article>update()
                            .eq("id", targetId)
                            .setSql(sql)
            );
            case MOMENT -> momentMapper.update(
                    null,
                    Wrappers.<CommunityMoment>update()
                            .eq("id", targetId)
                            .setSql(sql)
            );
            case COMMENT -> throw new BusinessException(
                    400, "评论目标类型无效"
            );
        }
    }

    private void insertEvent(
            Long commentId,
            String action,
            String actorType,
            Long actorUserId,
            String previousStatus,
            String newStatus,
            String reason,
            String metadataJson
    ) {
        CommunityCommentModerationEvent event =
                new CommunityCommentModerationEvent();
        event.setId(IdWorker.getId());
        event.setCommentId(commentId);
        event.setAction(action);
        event.setActorType(actorType);
        event.setActorUserId(actorUserId);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setReason(truncate(reason, 500));
        event.setMetadataJson(metadataJson);
        event.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        if (eventMapper.insert(event) != 1) {
            throw new BusinessException(500, "记录评论治理事件失败");
        }
    }

    private CommunityUser requireActiveUser(Long userId) {
        CommunityUser user = userMapper.selectById(userId);
        if (user == null
                || !List.of("NORMAL", "LIMITED").contains(user.getStatus())) {
            throw new BusinessException(403, "当前账号不能管理评论");
        }
        return user;
    }

    private static String keywordMetadata(KeywordReviewOutcome outcome) {
        String ids = outcome.matchedRuleIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return "{\"resultCode\":\""
                + outcome.resultCode()
                + "\",\"ruleIds\":["
                + ids
                + "]}";
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "内容作者隐藏评论";
        }
        String normalized = Normalizer.normalize(
                reason.strip(), Normalizer.Form.NFKC
        );
        if (normalized.length() > 500) {
            throw new BusinessException(400, "隐藏原因不能超过 500 个字符");
        }
        return normalized;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static boolean isPublished(CommunityComment comment) {
        return comment != null
                && comment.getDeletedAt() == null
                && "PUBLISHED".equals(comment.getStatus());
    }

    private static boolean isPublicRoot(CommunityComment comment) {
        return comment != null
                && comment.getRootCommentId() == null
                && comment.getParentCommentId() == null
                && ("PUBLISHED".equals(comment.getStatus())
                || "DELETED_BY_USER".equals(comment.getStatus()));
    }

    private static LikeTargetType commentTargetType(String rawType) {
        LikeTargetType type = LikeTargetType.parse(rawType);
        if (type == LikeTargetType.COMMENT) {
            throw new BusinessException(400, "评论目标类型无效");
        }
        return type;
    }

    private static void requireValidCommentId(Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(400, "评论 ID 无效");
        }
    }

    private static <T> List<T> slice(
            List<T> source,
            int pageNum,
            int pageSize
    ) {
        long fromLong = (long) (pageNum - 1) * pageSize;
        int from = (int) Math.min(fromLong, source.size());
        int to = Math.min(from + pageSize, source.size());
        return List.copyOf(source.subList(from, to));
    }

    private static int validPage(int value) {
        if (value < 1) {
            throw new BusinessException(400, "页码须大于 0");
        }
        return value;
    }

    private static int validPageSize(int value) {
        if (value < 1 || value > 100) {
            throw new BusinessException(400, "每页数量须为 1-100");
        }
        return value;
    }

    private static long safeLong(Long value) {
        return value == null ? 0 : value;
    }
}
