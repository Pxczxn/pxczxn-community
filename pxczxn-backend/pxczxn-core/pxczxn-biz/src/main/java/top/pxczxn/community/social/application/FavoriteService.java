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
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.notification.application.CommunityNotificationEvent;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.model.FavoriteFolder;
import top.pxczxn.community.social.model.FavoriteFolderItem;
import top.pxczxn.community.social.model.FavoriteItem;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.social.persistence.FavoriteFolderItemMapper;
import top.pxczxn.community.social.persistence.FavoriteItemMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final Set<String> ACTIVE_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");

    private final FavoriteItemMapper itemMapper;
    private final FavoriteFolderItemMapper folderItemMapper;
    private final FavoriteFolderService folderService;
    private final CommunityContentAccessService contentAccessService;
    private final ArticleMapper articleMapper;
    private final CommunityMomentMapper momentMapper;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final CommunityAuth communityAuth;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public FavoriteRelationshipView favorite(
            String rawType,
            Long targetId,
            Collection<Long> requestedFolderIds
    ) {
        LikeTargetType type = favoriteType(rawType);
        Long actorId = requireActorId();
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(type, targetId);
        FavoriteFolder defaultFolder =
                folderService.ensureDefaultFolder(actorId);
        Map<Long, FavoriteFolder> requested =
                folderService.requireOwnedFolders(
                        actorId, requestedFolderIds
                );
        LinkedHashSet<Long> desiredFolderIds =
                new LinkedHashSet<>(requested.keySet());
        desiredFolderIds.add(defaultFolder.getId());

        FavoriteItem item = itemMapper.findRelation(
                actorId, type.name(), targetId
        );
        boolean created = false;
        if (item == null) {
            item = new FavoriteItem();
            item.setId(IdWorker.getId());
            item.setOwnerUserId(actorId);
            item.setTargetType(type.name());
            item.setTargetId(targetId);
            item.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            try {
                if (itemMapper.insert(item) != 1) {
                    throw new BusinessException(500, "收藏失败");
                }
                created = true;
            } catch (DuplicateKeyException exception) {
                item = itemMapper.findRelation(
                        actorId, type.name(), targetId
                );
                if (item == null) {
                    throw new BusinessException(409, "收藏关系已发生变化，请重试");
                }
            }
        }
        if (created && incrementTarget(type, targetId) != 1) {
            throw new BusinessException(409, "内容状态已发生变化，请刷新后重试");
        }
        for (Long folderId : desiredFolderIds) {
            ensureMapping(folderId, item.getId());
        }
        if (created) {
            publishFavoriteNotification(actorId, target);
        }
        return relationship(
                target,
                true,
                created ? targetFavoriteCount(target) + 1
                        : targetFavoriteCount(target),
                folderItemMapper.findFolderIds(item.getId())
        );
    }

    private void publishFavoriteNotification(
            Long actorId,
            AccessibleContentTarget target
    ) {
        if (eventPublisher == null) {
            return;
        }
        eventPublisher.publishEvent(new CommunityNotificationEvent(
                "FAVORITE",
                "INTERACTION",
                actorId,
                target.authorUserId(),
                target.targetType().name(),
                target.targetId(),
                "有人收藏了你的内容",
                "有人收藏了你发布的内容。",
                "favorite:" + target.targetType().name()
                        + ":" + target.targetId(),
                "NORMAL"
        ));
    }

    @Transactional
    public FavoriteRelationshipView updateFolders(
            String rawType,
            Long targetId,
            Collection<Long> requestedFolderIds
    ) {
        LikeTargetType type = favoriteType(rawType);
        Long actorId = requireActorId();
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(type, targetId);
        FavoriteItem item = itemMapper.findRelation(
                actorId, type.name(), targetId
        );
        if (item == null) {
            throw new BusinessException(404, "尚未收藏该内容");
        }
        FavoriteFolder defaultFolder =
                folderService.ensureDefaultFolder(actorId);
        Map<Long, FavoriteFolder> requested =
                folderService.requireOwnedFolders(
                        actorId, requestedFolderIds
                );
        LinkedHashSet<Long> desired =
                new LinkedHashSet<>(requested.keySet());
        desired.add(defaultFolder.getId());
        LinkedHashSet<Long> existing = new LinkedHashSet<>(
                folderItemMapper.findFolderIds(item.getId())
        );
        for (Long folderId : desired) {
            if (!existing.contains(folderId)) {
                ensureMapping(folderId, item.getId());
            }
        }
        for (Long folderId : existing) {
            if (!desired.contains(folderId)
                    && folderItemMapper.deleteMapping(
                            folderId, item.getId()
                    ) == 1) {
                folderService.decrementItemCount(folderId);
            }
        }
        return relationship(
                target,
                true,
                targetFavoriteCount(target),
                folderItemMapper.findFolderIds(item.getId())
        );
    }

    @Transactional
    public FavoriteRelationshipView unfavorite(
            String rawType,
            Long targetId
    ) {
        LikeTargetType type = favoriteType(rawType);
        Long actorId = requireActorId();
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(type, targetId);
        FavoriteItem item = itemMapper.findRelation(
                actorId, type.name(), targetId
        );
        if (item == null) {
            return relationship(
                    target, false, targetFavoriteCount(target), List.of()
            );
        }
        List<Long> folderIds = folderItemMapper.findFolderIds(item.getId());
        folderItemMapper.deleteByFavoriteItem(item.getId());
        if (itemMapper.deleteRelation(
                actorId, type.name(), targetId
        ) != 1) {
            throw new BusinessException(409, "收藏关系已发生变化，请重试");
        }
        for (Long folderId : folderIds) {
            folderService.decrementItemCount(folderId);
        }
        decrementTarget(type, targetId);
        return relationship(
                target,
                false,
                Math.max(0, targetFavoriteCount(target) - 1),
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public FavoriteRelationshipView relationship(
            String rawType,
            Long targetId
    ) {
        LikeTargetType type = favoriteType(rawType);
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(type, targetId);
        Long actorId = communityAuth.getOptionalLoginUserId();
        if (actorId == null) {
            return relationship(
                    target, false, targetFavoriteCount(target), List.of()
            );
        }
        FavoriteItem item = itemMapper.findRelation(
                actorId, type.name(), targetId
        );
        return relationship(
                target,
                item != null,
                targetFavoriteCount(target),
                item == null
                        ? List.of()
                        : folderItemMapper.findFolderIds(item.getId())
        );
    }

    @Transactional(readOnly = true)
    public FavoriteContentPageView folderItems(
            Long folderId,
            int pageNum,
            int pageSize
    ) {
        FavoriteFolder folder = folderService.requireCanView(folderId);
        List<FavoriteContentView> accessible = new ArrayList<>();
        for (FavoriteItem item : folderItemMapper.findItems(folderId)) {
            AccessibleContentTarget target;
            try {
                target = contentAccessService.requireAccessible(
                        favoriteType(item.getTargetType()),
                        item.getTargetId()
                );
            } catch (BusinessException exception) {
                if (Integer.valueOf(404).equals(exception.getCode())) {
                    continue;
                }
                throw exception;
            }
            accessible.add(content(item, target));
        }
        int validPageNum = validPage(pageNum);
        int validPageSize = validPageSize(pageSize);
        long fromLong = (long) (validPageNum - 1) * validPageSize;
        int from = (int) Math.min(fromLong, accessible.size());
        int to = Math.min(from + validPageSize, accessible.size());
        return new FavoriteContentPageView(
                folder.getId(),
                List.copyOf(accessible.subList(from, to)),
                accessible.size(),
                validPageNum,
                validPageSize
        );
    }

    @Transactional(readOnly = true)
    public ContentFavoritorPageView favoritors(
            String rawType,
            Long targetId,
            int pageNum,
            int pageSize
    ) {
        LikeTargetType type = favoriteType(rawType);
        Long actorId = requireActorId();
        AccessibleContentTarget target =
                contentAccessService.requireAccessible(type, targetId);
        if (!Objects.equals(actorId, target.authorUserId())) {
            Blog blog = blogMapper.selectById(target.blogId());
            if (blog == null
                    || !Objects.equals(actorId, blog.getOwnerUserId())) {
                throw new BusinessException(403, "无权查看该内容的收藏用户");
            }
        }
        List<FavoriteItem> items =
                itemMapper.findByTarget(type.name(), targetId);
        Map<Long, CommunityUser> users = usersById(
                items.stream().map(FavoriteItem::getOwnerUserId).toList()
        );
        List<ContentFavoritorView> records = items.stream()
                .map(item -> favoritor(item, users.get(item.getOwnerUserId())))
                .filter(Objects::nonNull)
                .toList();
        int validPageNum = validPage(pageNum);
        int validPageSize = validPageSize(pageSize);
        long fromLong = (long) (validPageNum - 1) * validPageSize;
        int from = (int) Math.min(fromLong, records.size());
        int to = Math.min(from + validPageSize, records.size());
        return new ContentFavoritorPageView(
                List.copyOf(records.subList(from, to)),
                records.size(),
                validPageNum,
                validPageSize
        );
    }

    private void ensureMapping(Long folderId, Long favoriteItemId) {
        if (folderItemMapper.countMapping(folderId, favoriteItemId) > 0) {
            return;
        }
        FavoriteFolderItem mapping = new FavoriteFolderItem();
        mapping.setId(IdWorker.getId());
        mapping.setFolderId(folderId);
        mapping.setFavoriteItemId(favoriteItemId);
        mapping.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (folderItemMapper.insert(mapping) != 1) {
                throw new BusinessException(500, "加入收藏夹失败");
            }
            folderService.incrementItemCount(folderId);
        } catch (DuplicateKeyException exception) {
            if (folderItemMapper.countMapping(
                    folderId, favoriteItemId
            ) == 0) {
                throw new BusinessException(409, "收藏夹映射已发生变化，请重试");
            }
        }
    }

    private int incrementTarget(LikeTargetType type, Long targetId) {
        return switch (type) {
            case ARTICLE -> articleMapper.update(
                    null,
                    Wrappers.<Article>update()
                            .eq("id", targetId)
                            .isNull("deleted_at")
                            .setSql("favorite_count = favorite_count + 1")
            );
            case MOMENT -> momentMapper.update(
                    null,
                    Wrappers.<CommunityMoment>update()
                            .eq("id", targetId)
                            .eq("status", "PUBLISHED")
                            .isNull("deleted_at")
                            .setSql("favorite_count = favorite_count + 1")
            );
            case COMMENT -> throw new BusinessException(
                    400, "评论不支持收藏"
            );
        };
    }

    private void decrementTarget(LikeTargetType type, Long targetId) {
        switch (type) {
            case ARTICLE -> articleMapper.update(
                    null,
                    Wrappers.<Article>update()
                            .eq("id", targetId)
                            .setSql(
                                    "favorite_count = "
                                            + "GREATEST(favorite_count - 1, 0)"
                            )
            );
            case MOMENT -> momentMapper.update(
                    null,
                    Wrappers.<CommunityMoment>update()
                            .eq("id", targetId)
                            .setSql(
                                    "favorite_count = "
                                            + "GREATEST(favorite_count - 1, 0)"
                            )
            );
            case COMMENT -> throw new BusinessException(
                    400, "评论不支持收藏"
            );
        }
    }

    private Long requireActorId() {
        Long actorId = communityAuth.getLoginUserId();
        CommunityUser actor = userMapper.selectById(actorId);
        if (actor == null || !ACTIVE_USER_STATUSES.contains(actor.getStatus())) {
            throw new BusinessException(403, "当前账号不能使用收藏功能");
        }
        return actorId;
    }

    private Map<Long, CommunityUser> usersById(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(new LinkedHashSet<>(ids)).stream()
                .filter(user -> ACTIVE_USER_STATUSES.contains(user.getStatus()))
                .collect(Collectors.toMap(
                        CommunityUser::getId,
                        Function.identity()
                ));
    }

    private static FavoriteRelationshipView relationship(
            AccessibleContentTarget target,
            boolean favorited,
            long favoriteCount,
            Collection<Long> folderIds
    ) {
        return new FavoriteRelationshipView(
                target.targetType().name(),
                target.targetId(),
                favorited,
                Math.max(0, favoriteCount),
                folderIds == null
                        ? List.of()
                        : folderIds.stream().distinct().sorted().toList()
        );
    }

    private static FavoriteContentView content(
            FavoriteItem item,
            AccessibleContentTarget target
    ) {
        return new FavoriteContentView(
                item.getId(),
                target.targetType().name(),
                target.targetId(),
                target.authorUserId(),
                target.blogId(),
                target.title(),
                target.excerpt(),
                target.coverFileId(),
                target.canonicalPath(),
                targetFavoriteCount(target),
                item.getCreatedAt()
        );
    }

    private static ContentFavoritorView favoritor(
            FavoriteItem item,
            CommunityUser user
    ) {
        if (user == null) {
            return null;
        }
        return new ContentFavoritorView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarFileId(),
                item.getCreatedAt()
        );
    }

    private static LikeTargetType favoriteType(String rawType) {
        LikeTargetType type = LikeTargetType.parse(rawType);
        if (type == LikeTargetType.COMMENT) {
            throw new BusinessException(400, "评论不支持收藏");
        }
        return type;
    }

    private static long targetFavoriteCount(
            AccessibleContentTarget target
    ) {
        return target.favoriteCount();
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
