package top.pxczxn.community.social.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.notification.application.CommunityNotificationEvent;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogFollowService {

    private static final String TARGET_BLOG = "BLOG";
    private static final Set<String> ACTIVE_USER_STATUSES = Set.of("NORMAL", "LIMITED");
    private static final Set<String> NOTIFICATION_LEVELS =
            Set.of("ALL", "IMPORTANT", "MUTED");

    private final CommunityFollowMapper followMapper;
    private final BlogMapper blogMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityAuth communityAuth;

    private final CommunityAbuseGuard abuseGuard;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public BlogFollowRelationshipView follow(
            Long blogId,
            UpdateBlogFollowCommand command
    ) {
        ActorContext actor = requireActor();
        TargetBlog target = requireTargetBlog(blogId);
        assertNotSelf(actor, target);
        CommunityFollow existing = followMapper.findRelation(
                actor.user().getId(), TARGET_BLOG, target.blog().getId()
        );
        if (existing != null) {
            applySettings(existing, settings(command, target.blog(), existing));
            return relationship(actor, target, existing);
        }
        abuseGuard.check(
                "USER:" + actor.user().getId(), "INTERACTION_CREATE", 40, 60
        );
        FollowSettings settings = settings(command, target.blog(), null);

        CommunityFollow relation = new CommunityFollow();
        relation.setId(IdWorker.getId());
        relation.setFollowerUserId(actor.user().getId());
        relation.setTargetType(TARGET_BLOG);
        relation.setTargetId(target.blog().getId());
        relation.setNotificationLevel(settings.notificationLevel());
        relation.setSpecialFollow(settings.specialFollow() ? 1 : 0);
        relation.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (followMapper.insert(relation) != 1) {
                throw new BusinessException(500, "关注失败");
            }
        } catch (DuplicateKeyException exception) {
            CommunityFollow concurrent = followMapper.findRelation(
                    actor.user().getId(), TARGET_BLOG, target.blog().getId()
            );
            if (concurrent == null) {
                throw new BusinessException(409, "关注关系已发生变化，请重试");
            }
            applySettings(concurrent, settings);
            return relationship(actor, target, concurrent);
        }
        if (incrementFollowerCount(target.blog().getId()) != 1) {
            throw new BusinessException(409, "博客状态已发生变化，请刷新后重试");
        }
        target.blog().setFollowerCount(longValue(target.blog().getFollowerCount()) + 1);
        publishFollowNotification(actor, target);
        return relationship(actor, target, relation);
    }

    private void publishFollowNotification(
            ActorContext actor,
            TargetBlog target
    ) {
        if (eventPublisher == null
                || target.blog().getOwnerUserId() == null) {
            return;
        }
        eventPublisher.publishEvent(new CommunityNotificationEvent(
                "FOLLOW",
                "FOLLOW",
                actor.user().getId(),
                target.blog().getOwnerUserId(),
                "BLOG",
                target.blog().getId(),
                "有新的关注者",
                "有人关注了你的博客。",
                "follow:blog:" + target.blog().getId(),
                "NORMAL"
        ));
    }

    @Transactional
    public BlogFollowRelationshipView update(
            Long blogId,
            UpdateBlogFollowCommand command
    ) {
        ActorContext actor = requireActor();
        TargetBlog target = requireTargetBlog(blogId);
        assertNotSelf(actor, target);
        CommunityFollow relation = followMapper.findRelation(
                actor.user().getId(), TARGET_BLOG, target.blog().getId()
        );
        if (relation == null) {
            throw new BusinessException(404, "尚未关注该博客");
        }
        applySettings(relation, settings(command, target.blog(), relation));
        return relationship(actor, target, relation);
    }

    @Transactional
    public BlogFollowRelationshipView unfollow(Long blogId) {
        ActorContext actor = requireActor();
        TargetBlog target = requireTargetBlog(blogId);
        assertNotSelf(actor, target);
        CommunityFollow relation = followMapper.findRelation(
                actor.user().getId(), TARGET_BLOG, target.blog().getId()
        );
        if (relation == null) {
            return relationship(actor, target, null);
        }
        if (followMapper.deleteRelation(
                actor.user().getId(), TARGET_BLOG, target.blog().getId()
        ) == 1) {
            decrementFollowerCount(target.blog().getId());
            target.blog().setFollowerCount(
                    Math.max(0, longValue(target.blog().getFollowerCount()) - 1)
            );
        }
        return relationship(actor, target, null);
    }

    @Transactional(readOnly = true)
    public BlogFollowRelationshipView relationship(Long blogId) {
        TargetBlog target = requireTargetBlog(blogId);
        Long actorId = communityAuth.getOptionalLoginUserId();
        if (actorId == null) {
            return anonymousRelationship(target.blog());
        }
        ActorContext actor = requireActor();
        if (actor.user().getId().equals(target.owner().getId())) {
            return anonymousRelationship(target.blog());
        }
        CommunityFollow relation = followMapper.findRelation(
                actor.user().getId(), TARGET_BLOG, target.blog().getId()
        );
        return relationship(actor, target, relation);
    }

    @Transactional(readOnly = true)
    public SocialCountsView myCounts() {
        ActorContext actor = requireActor();
        return new SocialCountsView(
                followMapper.countFollowing(actor.user().getId(), TARGET_BLOG),
                followMapper.countFollowers(TARGET_BLOG, actor.blog().getId()),
                followMapper.countMutualBlogs(
                        actor.user().getId(), actor.blog().getId()
                )
        );
    }

    @Transactional(readOnly = true)
    public SocialProfilePageView myFollowing(int pageNum, int pageSize) {
        ActorContext actor = requireActor();
        Page<CommunityFollow> page = followMapper.selectPage(
                new Page<>(validPage(pageNum), validPageSize(pageSize)),
                Wrappers.<CommunityFollow>lambdaQuery()
                        .eq(CommunityFollow::getFollowerUserId, actor.user().getId())
                        .eq(CommunityFollow::getTargetType, TARGET_BLOG)
                        .orderByDesc(CommunityFollow::getCreatedAt)
                        .orderByDesc(CommunityFollow::getId)
        );
        Map<Long, Blog> blogs = blogsById(
                page.getRecords().stream()
                        .map(CommunityFollow::getTargetId)
                        .toList()
        );
        Map<Long, CommunityUser> owners = usersById(
                blogs.values().stream().map(Blog::getOwnerUserId).toList()
        );
        Map<Long, CommunityFollow> reverse = reverseByUser(
                actor.blog().getId(), owners.keySet()
        );
        List<SocialProfileView> records = new ArrayList<>();
        for (CommunityFollow relation : page.getRecords()) {
            Blog blog = blogs.get(relation.getTargetId());
            CommunityUser owner = blog == null ? null : owners.get(blog.getOwnerUserId());
            if (!isPublicTarget(blog, owner)) {
                continue;
            }
            records.add(profile(
                    owner,
                    blog,
                    relation,
                    reverse.get(owner.getId()) != null
            ));
        }
        return new SocialProfilePageView(
                records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize()
        );
    }

    @Transactional(readOnly = true)
    public SocialProfilePageView myFollowers(int pageNum, int pageSize) {
        ActorContext actor = requireActor();
        Page<CommunityFollow> page = followMapper.selectPage(
                new Page<>(validPage(pageNum), validPageSize(pageSize)),
                Wrappers.<CommunityFollow>lambdaQuery()
                        .eq(CommunityFollow::getTargetType, TARGET_BLOG)
                        .eq(CommunityFollow::getTargetId, actor.blog().getId())
                        .orderByDesc(CommunityFollow::getCreatedAt)
                        .orderByDesc(CommunityFollow::getId)
        );
        Map<Long, CommunityUser> users = usersById(
                page.getRecords().stream()
                        .map(CommunityFollow::getFollowerUserId)
                        .toList()
        );
        Map<Long, Blog> personalBlogs = personalBlogs(
                users.values().stream()
                        .map(CommunityUser::getPersonalBlogId)
                        .toList()
        );
        Map<Long, CommunityFollow> outbound = followingByBlog(
                actor.user().getId(), personalBlogs.keySet()
        );
        List<SocialProfileView> records = new ArrayList<>();
        for (CommunityFollow inbound : page.getRecords()) {
            CommunityUser user = users.get(inbound.getFollowerUserId());
            Blog blog = user == null ? null : personalBlogs.get(user.getPersonalBlogId());
            if (!isPublicTarget(blog, user)) {
                continue;
            }
            CommunityFollow following = outbound.get(blog.getId());
            records.add(profile(user, blog, following, true, inbound.getCreatedAt()));
        }
        return new SocialProfilePageView(
                records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize()
        );
    }

    private void applySettings(
            CommunityFollow relation,
            FollowSettings settings
    ) {
        boolean changed =
                !settings.notificationLevel().equals(relation.getNotificationLevel())
                || settings.specialFollow()
                != Integer.valueOf(1).equals(relation.getSpecialFollow());
        if (!changed) {
            return;
        }
        if (followMapper.update(
                null,
                Wrappers.<CommunityFollow>lambdaUpdate()
                        .eq(CommunityFollow::getId, relation.getId())
                        .eq(CommunityFollow::getFollowerUserId, relation.getFollowerUserId())
                        .set(
                                CommunityFollow::getNotificationLevel,
                                settings.notificationLevel()
                        )
                        .set(
                                CommunityFollow::getSpecialFollow,
                                settings.specialFollow() ? 1 : 0
                        )
        ) != 1) {
            throw new BusinessException(409, "关注设置已发生变化，请刷新后重试");
        }
        relation.setNotificationLevel(settings.notificationLevel());
        relation.setSpecialFollow(settings.specialFollow() ? 1 : 0);
    }

    private BlogFollowRelationshipView relationship(
            ActorContext actor,
            TargetBlog target,
            CommunityFollow relation
    ) {
        CommunityFollow reverse = "PERSONAL".equals(target.blog().getBlogType())
                ? followMapper.findRelation(
                        target.owner().getId(), TARGET_BLOG, actor.blog().getId()
                )
                : null;
        boolean following = relation != null;
        boolean followedBy = reverse != null;
        return new BlogFollowRelationshipView(
                target.blog().getId(),
                following,
                followedBy,
                following && followedBy,
                following && Integer.valueOf(1).equals(relation.getSpecialFollow()),
                following ? relation.getNotificationLevel() : null,
                longValue(target.blog().getFollowerCount())
        );
    }

    private static BlogFollowRelationshipView anonymousRelationship(Blog blog) {
        return new BlogFollowRelationshipView(
                blog.getId(), false, false, false, false, null,
                longValue(blog.getFollowerCount())
        );
    }

    private SocialProfileView profile(
            CommunityUser user,
            Blog blog,
            CommunityFollow following,
            boolean followedBy
    ) {
        return profile(user, blog, following, followedBy,
                following == null ? null : following.getCreatedAt());
    }

    private SocialProfileView profile(
            CommunityUser user,
            Blog blog,
            CommunityFollow following,
            boolean followedBy,
            LocalDateTime followedAt
    ) {
        boolean isFollowing = following != null;
        return new SocialProfileView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarFileId(),
                blog.getId(),
                blog.getName(),
                blog.getSlug(),
                isFollowing,
                followedBy,
                isFollowing && followedBy,
                isFollowing && Integer.valueOf(1).equals(following.getSpecialFollow()),
                isFollowing ? following.getNotificationLevel() : null,
                followedAt
        );
    }

    private ActorContext requireActor() {
        Long userId = communityAuth.getLoginUserId();
        CommunityUser user = userMapper.selectById(userId);
        if (user == null || !ACTIVE_USER_STATUSES.contains(user.getStatus())) {
            throw new BusinessException(403, "当前账号不能使用关注功能");
        }
        if (user.getPersonalBlogId() == null) {
            throw new BusinessException(404, "个人博客不存在");
        }
        Blog blog = blogMapper.selectById(user.getPersonalBlogId());
        if (blog == null
                || blog.getDeletedAt() != null
                || !"PERSONAL".equals(blog.getBlogType())
                || !userId.equals(blog.getOwnerUserId())) {
            throw new BusinessException(404, "个人博客不存在");
        }
        return new ActorContext(user, blog);
    }

    private TargetBlog requireTargetBlog(Long blogId) {
        if (blogId == null || blogId <= 0) {
            throw new BusinessException(400, "博客 ID 无效");
        }
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null
                || blog.getDeletedAt() != null
                || !"ACTIVE".equals(blog.getStatus())) {
            throw new BusinessException(404, "博客不存在");
        }
        CommunityUser owner = userMapper.selectById(blog.getOwnerUserId());
        if (owner == null || !ACTIVE_USER_STATUSES.contains(owner.getStatus())) {
            throw new BusinessException(404, "博客不存在");
        }
        return new TargetBlog(blog, owner);
    }

    private static void assertNotSelf(ActorContext actor, TargetBlog target) {
        if (actor.user().getId().equals(target.owner().getId())
                && "PERSONAL".equals(target.blog().getBlogType())) {
            throw new BusinessException(400, "不能关注自己的个人博客");
        }
    }

    private static FollowSettings settings(
            UpdateBlogFollowCommand command,
            Blog blog,
            CommunityFollow current
    ) {
        String notification = command == null || command.notificationLevel() == null
                ? current == null || current.getNotificationLevel() == null
                        ? "ALL"
                        : current.getNotificationLevel()
                : command.notificationLevel().trim().toUpperCase(Locale.ROOT);
        if (!NOTIFICATION_LEVELS.contains(notification)) {
            throw new BusinessException(400, "关注通知等级无效");
        }
        boolean special = command == null || command.specialFollow() == null
                ? current != null
                && Integer.valueOf(1).equals(current.getSpecialFollow())
                : Boolean.TRUE.equals(command.specialFollow());
        if (special && !"PERSONAL".equals(blog.getBlogType())) {
            throw new BusinessException(400, "只有个人博客支持特别关注");
        }
        return new FollowSettings(notification, special);
    }

    private int incrementFollowerCount(Long blogId) {
        return blogMapper.update(
                null,
                Wrappers.<Blog>lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .eq(Blog::getStatus, "ACTIVE")
                        .isNull(Blog::getDeletedAt)
                        .setSql("follower_count = follower_count + 1")
        );
    }

    private void decrementFollowerCount(Long blogId) {
        blogMapper.update(
                null,
                Wrappers.<Blog>lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("follower_count = GREATEST(follower_count - 1, 0)")
        );
    }

    private Map<Long, Blog> blogsById(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return blogMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(
                        Blog::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, Blog> personalBlogs(Collection<Long> ids) {
        return blogsById(ids).values().stream()
                .filter(blog -> "PERSONAL".equals(blog.getBlogType()))
                .collect(Collectors.toMap(Blog::getId, Function.identity()));
    }

    private Map<Long, CommunityUser> usersById(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(
                        CommunityUser::getId,
                        Function.identity(),
                        (left, right) -> left,
                        HashMap::new
                ));
    }

    private Map<Long, CommunityFollow> reverseByUser(
            Long targetBlogId,
            Collection<Long> followerUserIds
    ) {
        if (followerUserIds == null || followerUserIds.isEmpty()) {
            return Map.of();
        }
        return followMapper.selectList(
                Wrappers.<CommunityFollow>lambdaQuery()
                        .in(CommunityFollow::getFollowerUserId, followerUserIds)
                        .eq(CommunityFollow::getTargetType, TARGET_BLOG)
                        .eq(CommunityFollow::getTargetId, targetBlogId)
        ).stream().collect(Collectors.toMap(
                CommunityFollow::getFollowerUserId, Function.identity()
        ));
    }

    private Map<Long, CommunityFollow> followingByBlog(
            Long followerUserId,
            Collection<Long> targetBlogIds
    ) {
        if (targetBlogIds == null || targetBlogIds.isEmpty()) {
            return Map.of();
        }
        return followMapper.selectList(
                Wrappers.<CommunityFollow>lambdaQuery()
                        .eq(CommunityFollow::getFollowerUserId, followerUserId)
                        .eq(CommunityFollow::getTargetType, TARGET_BLOG)
                        .in(CommunityFollow::getTargetId, targetBlogIds)
        ).stream().collect(Collectors.toMap(
                CommunityFollow::getTargetId, Function.identity()
        ));
    }

    private static boolean isPublicTarget(Blog blog, CommunityUser owner) {
        return blog != null
                && blog.getDeletedAt() == null
                && "ACTIVE".equals(blog.getStatus())
                && owner != null
                && ACTIVE_USER_STATUSES.contains(owner.getStatus());
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

    private static long longValue(Long value) {
        return value == null ? 0 : value;
    }

    private record ActorContext(CommunityUser user, Blog blog) {
    }

    private record TargetBlog(Blog blog, CommunityUser owner) {
    }

    private record FollowSettings(String notificationLevel, boolean specialFollow) {
    }
}
