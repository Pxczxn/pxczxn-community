package top.pxczxn.community.notification.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.notification.model.CommunityNotification;
import top.pxczxn.community.notification.model.CommunityNotificationRecipient;
import top.pxczxn.community.notification.persistence.CommunityNotificationMapper;
import top.pxczxn.community.notification.persistence.CommunityNotificationRecipientMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.application.AccessibleContentTarget;
import top.pxczxn.community.social.application.CommunityContentAccessService;
import top.pxczxn.community.social.application.LikeTargetType;
import top.pxczxn.community.social.model.CommunityComment;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityNotificationInboxService {

    private static final Set<String> ACTIVE_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");
    private static final Set<String> CATEGORIES = Set.of(
            "INTERACTION", "FOLLOW", "COMMENT", "COAUTHOR",
            "SUBMISSION", "TEAM", "REVIEW", "SYSTEM"
    );
    private static final Set<String> STATUSES = Set.of("UNREAD", "READ");

    private final CommunityNotificationMapper notificationMapper;
    private final CommunityNotificationRecipientMapper recipientMapper;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final ArticleMapper articleMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityMomentMapper momentMapper;
    private final CommunityContentAccessService contentAccessService;
    private final CommunityAuth communityAuth;

    @Transactional(readOnly = true)
    public NotificationPageView page(
            String rawCategory,
            String rawStatus,
            int pageNum,
            int pageSize
    ) {
        Long userId = requireUserId();
        String category = category(rawCategory, true);
        String status = status(rawStatus);
        int validPageNum = validPage(pageNum);
        int validPageSize = validPageSize(pageSize);
        long offset = (long) (validPageNum - 1) * validPageSize;
        List<CommunityNotificationRecipient> recipients =
                recipientMapper.selectInbox(
                        userId,
                        category,
                        status,
                        offset,
                        validPageSize
                );
        long total = recipientMapper.countInbox(
                userId, category, status
        );
        if (recipients.isEmpty()) {
            return new NotificationPageView(
                    List.of(), total, validPageNum, validPageSize
            );
        }
        Map<Long, CommunityNotification> notifications = byId(
                notificationMapper.selectBatchIds(
                        recipients.stream()
                                .map(
                                        CommunityNotificationRecipient
                                                ::getNotificationId
                                )
                                .toList()
                ),
                CommunityNotification::getId
        );
        List<Long> senderIds = notifications.values().stream()
                .map(CommunityNotification::getSenderUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, CommunityUser> senders = byId(
                senderIds.isEmpty() ? List.of() : userMapper.selectBatchIds(senderIds),
                CommunityUser::getId
        );
        List<NotificationView> records = new ArrayList<>();
        for (CommunityNotificationRecipient recipient : recipients) {
            CommunityNotification notification = notifications.get(
                    recipient.getNotificationId()
            );
            if (notification != null) {
                records.add(toView(
                        notification,
                        recipient,
                        senders.get(notification.getSenderUserId()),
                        userId
                ));
            }
        }
        return new NotificationPageView(
                records, total, validPageNum, validPageSize
        );
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountView unreadCount() {
        Long userId = requireUserId();
        LinkedHashMap<String, Long> categories = new LinkedHashMap<>();
        List.of(
                "INTERACTION", "FOLLOW", "COMMENT", "COAUTHOR",
                "SUBMISSION", "TEAM", "REVIEW", "SYSTEM"
        ).forEach(value -> categories.put(value, 0L));
        long total = 0;
        for (Map<String, Object> row
                : recipientMapper.countUnreadByCategory(userId)) {
            String category = String.valueOf(row.get("category"));
            long count = number(row.get("count"));
            if (categories.containsKey(category)) {
                categories.put(category, count);
                total += count;
            }
        }
        return new UnreadNotificationCountView(
                total, Map.copyOf(categories)
        );
    }

    @Transactional
    public NotificationReadView read(Long notificationId) {
        requireNotificationId(notificationId);
        Long userId = requireUserId();
        CommunityNotificationRecipient recipient =
                recipientMapper.findRelation(notificationId, userId);
        if (recipient == null
                || "ARCHIVED".equals(recipient.getStatus())) {
            throw new BusinessException(404, "通知不存在");
        }
        boolean replay = "READ".equals(recipient.getStatus());
        LocalDateTime readAt = recipient.getReadAt();
        if (!replay) {
            readAt = LocalDateTime.now(ZoneOffset.UTC);
            if (recipientMapper.update(
                    null,
                    Wrappers.<CommunityNotificationRecipient>update()
                            .eq("id", recipient.getId())
                            .eq("recipient_user_id", userId)
                            .eq("status", "UNREAD")
                            .set("status", "READ")
                            .set("read_at", readAt)
            ) != 1) {
                CommunityNotificationRecipient current =
                        recipientMapper.findRelation(
                                notificationId, userId
                        );
                if (current == null
                        || !"READ".equals(current.getStatus())) {
                    throw new BusinessException(
                            409, "通知状态已发生变化，请重试"
                    );
                }
                replay = true;
                readAt = current.getReadAt();
            }
        }
        return new NotificationReadView(
                notificationId,
                "READ",
                readAt,
                replay,
                unreadCount().total()
        );
    }

    @Transactional
    public NotificationReadAllView readAll(String rawCategory) {
        Long userId = requireUserId();
        String category = category(rawCategory, true);
        int affected = recipientMapper.markAllRead(
                userId,
                category,
                LocalDateTime.now(ZoneOffset.UTC)
        );
        return new NotificationReadAllView(
                category,
                affected,
                unreadCount().total()
        );
    }

    private NotificationView toView(
            CommunityNotification notification,
            CommunityNotificationRecipient recipient,
            CommunityUser sender,
            Long viewerId
    ) {
        TargetResolution target = resolveTarget(
                notification, viewerId
        );
        boolean available = target.available();
        return new NotificationView(
                notification.getId(),
                notification.getNotificationType(),
                notification.getCategory(),
                notification.getImportance(),
                available ? sender(sender) : null,
                available
                        ? notification.getTitle()
                        : "通知内容已不可用",
                available
                        ? notification.getContent()
                        : "关联内容已删除、隐藏或你已无权访问。",
                available ? notification.getTargetType() : null,
                available ? notification.getTargetId() : null,
                available,
                available ? target.canonicalPath() : null,
                safeInt(notification.getAggregateCount(), 1),
                recipient.getStatus(),
                recipient.getReadAt(),
                notification.getCreatedAt(),
                notification.getLastActivityAt()
        );
    }

    private TargetResolution resolveTarget(
            CommunityNotification notification,
            Long viewerId
    ) {
        String type = notification.getTargetType();
        Long id = notification.getTargetId();
        if (type == null || id == null) {
            return new TargetResolution(true, null);
        }
        if ("BLOG".equals(type)) {
            Blog blog = blogMapper.selectById(id);
            if (blog == null
                    || blog.getDeletedAt() != null
                    || !"ACTIVE".equals(blog.getStatus())) {
                return TargetResolution.unavailable();
            }
            return new TargetResolution(
                    true, "/blogs/" + blog.getSlug()
            );
        }
        if ("REVIEW".equals(notification.getCategory())
                && "ARTICLE".equals(type)) {
            Article article = articleMapper.selectById(id);
            if (article == null
                    || article.getDeletedAt() != null
                    || !Objects.equals(
                            article.getAuthorUserId(), viewerId
                    )) {
                return TargetResolution.unavailable();
            }
            return new TargetResolution(
                    true, "/editor/" + article.getId()
            );
        }
        if ("REVIEW".equals(notification.getCategory())
                && "COMMENT".equals(type)) {
            CommunityComment comment = commentMapper.selectById(id);
            if (comment == null
                    || !Objects.equals(
                            comment.getAuthorUserId(), viewerId
                    )) {
                return TargetResolution.unavailable();
            }
            return new TargetResolution(
                    true,
                    "MOMENT".equals(comment.getTargetType())
                            ? "/moments/" + comment.getTargetId()
                            : null
            );
        }
        if ("REVIEW".equals(notification.getCategory())
                && "MOMENT".equals(type)) {
            CommunityMoment moment = momentMapper.selectById(id);
            if (moment == null
                    || !Objects.equals(
                            moment.getActorUserId(), viewerId
                    )) {
                return TargetResolution.unavailable();
            }
            return new TargetResolution(
                    true,
                    "PUBLISHED".equals(moment.getStatus())
                            ? "/moments/" + moment.getId()
                            : null
            );
        }
        AccessibleContentTarget target =
                contentAccessService.findAccessible(
                        LikeTargetType.parse(type), id
                );
        if (target == null) {
            return TargetResolution.unavailable();
        }
        String path = target.canonicalPath();
        if (path == null && target.targetType()
                == LikeTargetType.MOMENT) {
            path = "/moments/" + target.targetId();
        }
        return new TargetResolution(true, path);
    }

    private static NotificationSenderView sender(CommunityUser sender) {
        if (sender == null
                || !ACTIVE_USER_STATUSES.contains(sender.getStatus())) {
            return null;
        }
        return new NotificationSenderView(
                sender.getId(),
                sender.getUsername(),
                sender.getDisplayName(),
                sender.getAvatarFileId()
        );
    }

    private Long requireUserId() {
        return communityAuth.getLoginUserId();
    }

    private static String category(String raw, boolean optional) {
        if (raw == null || raw.isBlank()) {
            return optional ? null : "SYSTEM";
        }
        String value = raw.strip().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(value)) {
            throw new BusinessException(400, "通知分类无效");
        }
        return value;
    }

    private static String status(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.strip().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(value)) {
            throw new BusinessException(400, "通知状态筛选无效");
        }
        return value;
    }

    private static int validPage(int pageNum) {
        if (pageNum < 1) {
            throw new BusinessException(400, "页码必须大于 0");
        }
        return pageNum;
    }

    private static int validPageSize(int pageSize) {
        if (pageSize < 1 || pageSize > 50) {
            throw new BusinessException(400, "每页数量必须在 1 到 50 之间");
        }
        return pageSize;
    }

    private static void requireNotificationId(Long notificationId) {
        if (notificationId == null || notificationId <= 0) {
            throw new BusinessException(400, "通知 ID 无效");
        }
    }

    private static long number(Object value) {
        return value instanceof Number number
                ? number.longValue()
                : Long.parseLong(String.valueOf(value));
    }

    private static int safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static <T> Map<Long, T> byId(
            Collection<T> values,
            Function<T, Long> id
    ) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return values.stream().collect(Collectors.toMap(
                id,
                Function.identity(),
                (left, right) -> left
        ));
    }

    private record TargetResolution(
            boolean available,
            String canonicalPath
    ) {
        private static TargetResolution unavailable() {
            return new TargetResolution(false, null);
        }
    }
}
