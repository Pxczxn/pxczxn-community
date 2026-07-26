package top.pxczxn.community.social.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.notification.application.CommunityNotificationEvent;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityComment;
import top.pxczxn.community.social.model.CommunityContentLike;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityContentLikeMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentLikeService {

    private static final Set<String> ACTIVE_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");

    private final CommunityContentLikeMapper likeMapper;
    private final CommunityContentAccessService contentAccessService;
    private final ArticleMapper articleMapper;
    private final CommunityMomentMapper momentMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityAuth communityAuth;
    private final LikeListPrivacyService privacyService;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public ContentLikeRelationshipView like(
            String rawType,
            Long targetId
    ) {
        LikeTargetType type = LikeTargetType.parse(rawType);
        Long userId = requireActorId();
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(type, targetId);
        CommunityContentLike existing =
                likeMapper.findRelation(userId, type.name(), targetId);
        if (existing != null) {
            return relationship(target, true, target.likeCount());
        }

        CommunityContentLike relation = new CommunityContentLike();
        relation.setId(IdWorker.getId());
        relation.setUserId(userId);
        relation.setTargetType(type.name());
        relation.setTargetId(targetId);
        relation.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (likeMapper.insert(relation) != 1) {
                throw new BusinessException(500, "点赞失败");
            }
        } catch (DuplicateKeyException exception) {
            if (likeMapper.findRelation(userId, type.name(), targetId) == null) {
                throw new BusinessException(409, "点赞关系已发生变化，请重试");
            }
            AccessibleContentTarget current =
                    contentAccessService.requireAccessible(type, targetId);
            return relationship(current, true, current.likeCount());
        }
        if (incrementTarget(type, targetId) != 1) {
            throw new BusinessException(409, "内容状态已发生变化，请刷新后重试");
        }
        publishLikeNotification(userId, target);
        return relationship(target, true, target.likeCount() + 1);
    }

    private void publishLikeNotification(
            Long actorId,
            AccessibleContentTarget target
    ) {
        if (eventPublisher == null) {
            return;
        }
        eventPublisher.publishEvent(new CommunityNotificationEvent(
                "LIKE",
                "INTERACTION",
                actorId,
                target.authorUserId(),
                target.targetType().name(),
                target.targetId(),
                "有人点赞了你的内容",
                "有人点赞了你发布的内容。",
                "like:" + target.targetType().name()
                        + ":" + target.targetId(),
                "NORMAL"
        ));
    }

    @Transactional
    public ContentLikeRelationshipView unlike(
            String rawType,
            Long targetId
    ) {
        LikeTargetType type = LikeTargetType.parse(rawType);
        Long userId = requireActorId();
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(type, targetId);
        CommunityContentLike relation =
                likeMapper.findRelation(userId, type.name(), targetId);
        if (relation == null) {
            return relationship(target, false, target.likeCount());
        }
        if (likeMapper.deleteRelation(userId, type.name(), targetId) == 1) {
            decrementTarget(type, targetId);
            return relationship(
                    target, false, Math.max(0, target.likeCount() - 1)
            );
        }
        CommunityContentLike current =
                likeMapper.findRelation(userId, type.name(), targetId);
        AccessibleContentTarget refreshed =
                contentAccessService.requireAccessible(type, targetId);
        return relationship(
                refreshed, current != null, refreshed.likeCount()
        );
    }

    @Transactional(readOnly = true)
    public ContentLikeRelationshipView relationship(
            String rawType,
            Long targetId
    ) {
        LikeTargetType type = LikeTargetType.parse(rawType);
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(type, targetId);
        Long userId = communityAuth.getOptionalLoginUserId();
        boolean liked = userId != null
                && likeMapper.findRelation(userId, type.name(), targetId) != null;
        return relationship(target, liked, target.likeCount());
    }

    @Transactional(readOnly = true)
    public LikedContentPageView myLikes(
            String rawType,
            int pageNum,
            int pageSize
    ) {
        Long userId = requireActorId();
        return likesForUser(userId, rawType, pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public LikedContentPageView likesOf(
            Long ownerUserId,
            String rawType,
            int pageNum,
            int pageSize
    ) {
        privacyService.requireCanView(ownerUserId);
        return likesForUser(ownerUserId, rawType, pageNum, pageSize);
    }

    private LikedContentPageView likesForUser(
            Long userId,
            String rawType,
            int pageNum,
            int pageSize
    ) {
        LikeTargetType type = rawType == null || rawType.isBlank()
                ? null
                : LikeTargetType.parse(rawType);
        int validPageNum = validPage(pageNum);
        int validPageSize = validPageSize(pageSize);
        List<CommunityContentLike> relations = likeMapper.selectList(
                Wrappers.<CommunityContentLike>lambdaQuery()
                        .eq(CommunityContentLike::getUserId, userId)
                        .eq(type != null, CommunityContentLike::getTargetType,
                                type == null ? null : type.name())
                        .orderByDesc(CommunityContentLike::getCreatedAt)
                        .orderByDesc(CommunityContentLike::getId)
        );
        List<LikedContentView> accessible = new ArrayList<>();
        for (CommunityContentLike relation : relations) {
            AccessibleContentTarget target;
            try {
                target = contentAccessService.requireAccessible(
                        LikeTargetType.parse(relation.getTargetType()),
                        relation.getTargetId()
                );
            } catch (BusinessException exception) {
                if (Integer.valueOf(404).equals(exception.getCode())) {
                    continue;
                }
                throw exception;
            }
            accessible.add(toLikedContent(relation, target));
        }
        long fromLong = (long) (validPageNum - 1) * validPageSize;
        int from = (int) Math.min(fromLong, accessible.size());
        int to = Math.min(from + validPageSize, accessible.size());
        return new LikedContentPageView(
                List.copyOf(accessible.subList(from, to)),
                accessible.size(),
                validPageNum,
                validPageSize
        );
    }

    private Long requireActorId() {
        Long userId = communityAuth.getLoginUserId();
        CommunityUser user = userMapper.selectById(userId);
        if (user == null || !ACTIVE_USER_STATUSES.contains(user.getStatus())) {
            throw new BusinessException(403, "当前账号不能使用点赞功能");
        }
        return userId;
    }

    private int incrementTarget(LikeTargetType type, Long targetId) {
        return switch (type) {
            case ARTICLE -> articleMapper.update(
                    null,
                    Wrappers.<Article>lambdaUpdate()
                            .eq(Article::getId, targetId)
                            .isNull(Article::getDeletedAt)
                            .setSql("like_count = like_count + 1")
            );
            case MOMENT -> momentMapper.update(
                    null,
                    Wrappers.<CommunityMoment>lambdaUpdate()
                            .eq(CommunityMoment::getId, targetId)
                            .eq(CommunityMoment::getStatus, "PUBLISHED")
                            .isNull(CommunityMoment::getDeletedAt)
                            .setSql("like_count = like_count + 1")
            );
            case COMMENT -> commentMapper.update(
                    null,
                    Wrappers.<CommunityComment>lambdaUpdate()
                            .eq(CommunityComment::getId, targetId)
                            .eq(CommunityComment::getStatus, "PUBLISHED")
                            .isNull(CommunityComment::getDeletedAt)
                            .setSql("like_count = like_count + 1")
            );
        };
    }

    private void decrementTarget(LikeTargetType type, Long targetId) {
        switch (type) {
            case ARTICLE -> articleMapper.update(
                    null,
                    Wrappers.<Article>lambdaUpdate()
                            .eq(Article::getId, targetId)
                            .setSql(
                                    "like_count = GREATEST(like_count - 1, 0)"
                            )
            );
            case MOMENT -> momentMapper.update(
                    null,
                    Wrappers.<CommunityMoment>lambdaUpdate()
                            .eq(CommunityMoment::getId, targetId)
                            .setSql(
                                    "like_count = GREATEST(like_count - 1, 0)"
                            )
            );
            case COMMENT -> commentMapper.update(
                    null,
                    Wrappers.<CommunityComment>lambdaUpdate()
                            .eq(CommunityComment::getId, targetId)
                            .setSql(
                                    "like_count = GREATEST(like_count - 1, 0)"
                            )
            );
        }
    }

    private static ContentLikeRelationshipView relationship(
            AccessibleContentTarget target,
            boolean liked,
            long likeCount
    ) {
        return new ContentLikeRelationshipView(
                target.targetType().name(),
                target.targetId(),
                liked,
                Math.max(0, likeCount)
        );
    }

    private static LikedContentView toLikedContent(
            CommunityContentLike relation,
            AccessibleContentTarget target
    ) {
        return new LikedContentView(
                relation.getId(),
                target.targetType().name(),
                target.targetId(),
                target.authorUserId(),
                target.blogId(),
                target.title(),
                target.excerpt(),
                target.coverFileId(),
                target.canonicalPath(),
                target.likeCount(),
                relation.getCreatedAt()
        );
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
}
