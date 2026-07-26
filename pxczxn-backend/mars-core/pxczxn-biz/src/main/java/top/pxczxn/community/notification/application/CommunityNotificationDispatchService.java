package top.pxczxn.community.notification.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.application.ArticlePublishedEvent;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.notification.model.CommunityNotification;
import top.pxczxn.community.notification.model.CommunityNotificationRecipient;
import top.pxczxn.community.notification.persistence.CommunityNotificationMapper;
import top.pxczxn.community.notification.persistence.CommunityNotificationRecipientMapper;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommunityNotificationDispatchService {

    private static final long AGGREGATION_WINDOW_SECONDS = 10 * 60;
    private static final Set<String> ACTIVE_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");

    private final CommunityNotificationMapper notificationMapper;
    private final CommunityNotificationRecipientMapper recipientMapper;
    private final CommunityFollowMapper followMapper;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final ArticleMapper articleMapper;
    private final CommunityMomentMapper momentMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDirect(CommunityNotificationEvent event) {
        create(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createArticlePublished(ArticlePublishedEvent event) {
        if (event == null) {
            return;
        }
        Article article = articleMapper.selectById(event.articleId());
        Blog blog = blogMapper.selectById(event.blogId());
        if (article == null
                || article.getDeletedAt() != null
                || !"PUBLISHED".equals(article.getPublishStatus())
                || !Objects.equals(
                        article.getPublishedVersionId(),
                        event.publishedVersionId()
                )
                || blog == null
                || blog.getDeletedAt() != null
                || !"ACTIVE".equals(blog.getStatus())) {
            return;
        }
        createForFollowers(
                blog,
                event.authorUserId(),
                "ARTICLE_PUBLISHED",
                "INTERACTION",
                "ARTICLE",
                article.getId(),
                "你关注的博客发布了文章",
                "你关注的博客发布了新文章。",
                "published:article:" + blog.getId(),
                true
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createMomentPublished(MomentPublishedNotificationEvent event) {
        if (event == null
                || !Set.of("PUBLIC", "FOLLOWERS_ONLY")
                .contains(event.visibility())) {
            return;
        }
        CommunityMoment moment = momentMapper.selectById(event.momentId());
        Blog blog = blogMapper.selectById(event.blogId());
        if (moment == null
                || moment.getDeletedAt() != null
                || !"PUBLISHED".equals(moment.getStatus())
                || blog == null
                || blog.getDeletedAt() != null
                || !"ACTIVE".equals(blog.getStatus())) {
            return;
        }
        boolean important = Set.of("PROJECT_UPDATE", "TEAM_NOTICE")
                .contains(event.momentType());
        createForFollowers(
                blog,
                event.actorUserId(),
                "MOMENT_PUBLISHED",
                "INTERACTION",
                "MOMENT",
                moment.getId(),
                important
                        ? "你关注的博客发布了重要动态"
                        : "你关注的博客发布了动态",
                important
                        ? "你关注的博客发布了重要更新。"
                        : "你关注的博客发布了新动态。",
                "published:moment:" + blog.getId(),
                important
        );
    }

    private void createForFollowers(
            Blog blog,
            Long senderUserId,
            String notificationType,
            String category,
            String targetType,
            Long targetId,
            String title,
            String content,
            String aggregateKey,
            boolean importantContent
    ) {
        List<CommunityFollow> followers = followMapper.selectList(
                Wrappers.<CommunityFollow>lambdaQuery()
                        .eq(CommunityFollow::getTargetType, "BLOG")
                        .eq(CommunityFollow::getTargetId, blog.getId())
        );
        for (CommunityFollow follower : followers) {
            String level = follower.getNotificationLevel();
            boolean special = Integer.valueOf(1).equals(
                    follower.getSpecialFollow()
            );
            if ("MUTED".equals(level)
                    || ("IMPORTANT".equals(level)
                    && !importantContent
                    && !special)) {
                continue;
            }
            create(new CommunityNotificationEvent(
                    notificationType,
                    category,
                    senderUserId,
                    follower.getFollowerUserId(),
                    targetType,
                    targetId,
                    title,
                    content,
                    aggregateKey,
                    importantContent
                            || special
                            || "IMPORTANT".equals(level)
                            ? "HIGH"
                            : "NORMAL"
            ));
        }
    }

    private void create(CommunityNotificationEvent event) {
        if (!valid(event)
                || Objects.equals(
                        event.senderUserId(), event.recipientUserId()
                )) {
            return;
        }
        CommunityUser recipient = userMapper.selectById(
                event.recipientUserId()
        );
        if (recipient == null
                || !ACTIVE_USER_STATUSES.contains(recipient.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String deduplicationKey = deduplicationKey(event);
        CommunityNotification notification =
                notificationMapper.findByDeduplicationKey(
                        deduplicationKey
                );
        if (notification == null) {
            notification = insertNotification(
                    event, deduplicationKey, now
            );
            if (notification == null) {
                notification = notificationMapper.findByDeduplicationKey(
                        deduplicationKey
                );
            }
        } else {
            mergeNotification(notification.getId(), event, now);
        }
        if (notification == null) {
            throw new IllegalStateException(
                    "Failed to resolve notification after insert"
            );
        }
        ensureUnreadRecipient(
                notification.getId(), event.recipientUserId(), now
        );
    }

    private CommunityNotification insertNotification(
            CommunityNotificationEvent event,
            String deduplicationKey,
            LocalDateTime now
    ) {
        CommunityNotification notification = new CommunityNotification();
        notification.setId(IdWorker.getId());
        notification.setNotificationType(event.notificationType());
        notification.setCategory(event.category());
        notification.setImportance(event.importance());
        notification.setSenderUserId(event.senderUserId());
        notification.setTitle(truncate(event.title(), 160));
        notification.setContent(truncate(event.content(), 1_000));
        notification.setTargetType(event.targetType());
        notification.setTargetId(event.targetId());
        notification.setDeduplicationKey(deduplicationKey);
        notification.setAggregateCount(1);
        notification.setLastActivityAt(now);
        notification.setCreatedAt(now);
        try {
            if (notificationMapper.insert(notification) != 1) {
                throw new IllegalStateException(
                        "Failed to create community notification"
                );
            }
            return notification;
        } catch (DuplicateKeyException exception) {
            CommunityNotification concurrent =
                    notificationMapper.findByDeduplicationKey(
                            deduplicationKey
                    );
            if (concurrent == null) {
                throw exception;
            }
            mergeNotification(concurrent.getId(), event, now);
            return concurrent;
        }
    }

    private void mergeNotification(
            Long notificationId,
            CommunityNotificationEvent event,
            LocalDateTime now
    ) {
        if (notificationMapper.update(
                null,
                Wrappers.<CommunityNotification>update()
                        .eq("id", notificationId)
                        .set("sender_user_id", event.senderUserId())
                        .set("title", truncate(event.title(), 160))
                        .set("content", truncate(event.content(), 1_000))
                        .set("target_type", event.targetType())
                        .set("target_id", event.targetId())
                        .set(
                                "HIGH".equals(event.importance()),
                                "importance",
                                "HIGH"
                        )
                        .set("last_activity_at", now)
                        .setSql("aggregate_count = aggregate_count + 1")
        ) != 1) {
            throw new IllegalStateException(
                    "Failed to aggregate community notification"
            );
        }
    }

    private void ensureUnreadRecipient(
            Long notificationId,
            Long recipientUserId,
            LocalDateTime now
    ) {
        CommunityNotificationRecipient existing =
                recipientMapper.findRelation(
                        notificationId, recipientUserId
                );
        if (existing != null) {
            recipientMapper.update(
                    null,
                    Wrappers.<CommunityNotificationRecipient>update()
                            .eq("id", existing.getId())
                            .set("status", "UNREAD")
                            .set("read_at", null)
            );
            return;
        }
        CommunityNotificationRecipient recipient =
                new CommunityNotificationRecipient();
        recipient.setId(IdWorker.getId());
        recipient.setNotificationId(notificationId);
        recipient.setRecipientUserId(recipientUserId);
        recipient.setStatus("UNREAD");
        recipient.setCreatedAt(now);
        try {
            if (recipientMapper.insert(recipient) != 1) {
                throw new IllegalStateException(
                        "Failed to create notification recipient"
                );
            }
        } catch (DuplicateKeyException exception) {
            recipientMapper.update(
                    null,
                    Wrappers.<CommunityNotificationRecipient>update()
                            .eq("notification_id", notificationId)
                            .eq("recipient_user_id", recipientUserId)
                            .set("status", "UNREAD")
                            .set("read_at", null)
            );
        }
    }

    private static boolean valid(CommunityNotificationEvent event) {
        return event != null
                && event.notificationType() != null
                && event.category() != null
                && event.recipientUserId() != null
                && event.recipientUserId() > 0
                && event.title() != null
                && event.content() != null
                && event.aggregateKey() != null
                && event.importance() != null;
    }

    private static String deduplicationKey(
            CommunityNotificationEvent event
    ) {
        long bucket = Instant.now().getEpochSecond()
                / AGGREGATION_WINDOW_SECONDS;
        String key = event.aggregateKey()
                + ":" + event.recipientUserId()
                + ":" + bucket;
        if (key.length() > 120) {
            throw new IllegalArgumentException(
                    "Notification aggregation key is too long"
            );
        }
        return key;
    }

    private static String truncate(String value, int maxLength) {
        String normalized = value == null ? "" : value.strip();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
