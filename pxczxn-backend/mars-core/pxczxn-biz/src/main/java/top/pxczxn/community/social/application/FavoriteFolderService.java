package top.pxczxn.community.social.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mars.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.FavoriteFolder;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.social.persistence.FavoriteFolderItemMapper;
import top.pxczxn.community.social.persistence.FavoriteFolderMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteFolderService {

    public static final String DEFAULT_FOLDER_NAME = "全部收藏";

    private static final Set<String> ACTIVE_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");
    private static final Set<String> VISIBILITIES = Set.of(
            "PRIVATE", "PUBLIC", "FOLLOWERS_ONLY", "MUTUAL_ONLY"
    );
    private static final int MAX_CUSTOM_FOLDERS = 50;
    private static final int MAX_SORT_ORDER = 100_000;

    private final FavoriteFolderMapper folderMapper;
    private final FavoriteFolderItemMapper folderItemMapper;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final CommunityFollowMapper followMapper;
    private final CommunityAuth communityAuth;

    @Transactional
    public List<FavoriteFolderView> mine() {
        Long actorId = requireActorId();
        ensureDefaultFolder(actorId);
        return ownedFolders(actorId).stream()
                .map(FavoriteFolderService::view)
                .toList();
    }

    @Transactional
    public FavoriteFolderView create(CreateFavoriteFolderCommand command) {
        Long actorId = requireActorId();
        if (command == null) {
            throw new BusinessException(400, "收藏夹信息不能为空");
        }
        ensureDefaultFolder(actorId);
        if (folderMapper.countCustomFolders(actorId) >= MAX_CUSTOM_FOLDERS) {
            throw new BusinessException(400, "自定义收藏夹最多 50 个");
        }
        FavoriteFolder folder = new FavoriteFolder();
        folder.setId(IdWorker.getId());
        folder.setOwnerUserId(actorId);
        folder.setName(normalizeName(command.name()));
        folder.setDescription(normalizeDescription(command.description()));
        folder.setVisibility(normalizeVisibility(command.visibility(), "PRIVATE"));
        folder.setIsDefault(0);
        folder.setItemCount(0L);
        folder.setSortOrder(normalizeSortOrder(command.sortOrder(), 100));
        folder.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (folderMapper.insert(folder) != 1) {
                throw new BusinessException(500, "创建收藏夹失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "收藏夹名称已存在");
        }
        return view(folder);
    }

    @Transactional
    public FavoriteFolderView update(
            Long folderId,
            UpdateFavoriteFolderCommand command
    ) {
        Long actorId = requireActorId();
        if (command == null) {
            throw new BusinessException(400, "收藏夹信息不能为空");
        }
        FavoriteFolder folder = requireOwnedFolder(actorId, folderId);
        String name = command.name() == null
                ? folder.getName()
                : normalizeName(command.name());
        if (isDefault(folder) && !DEFAULT_FOLDER_NAME.equals(name)) {
            throw new BusinessException(400, "默认收藏夹不能重命名");
        }
        String description = Boolean.TRUE.equals(command.clearDescription())
                ? null
                : command.description() == null
                        ? folder.getDescription()
                        : normalizeDescription(command.description());
        String visibility = command.visibility() == null
                ? folder.getVisibility()
                : normalizeVisibility(command.visibility(), null);
        int sortOrder = normalizeSortOrder(
                command.sortOrder(),
                folder.getSortOrder() == null ? 0 : folder.getSortOrder()
        );
        try {
            if (folderMapper.update(
                    null,
                    Wrappers.<FavoriteFolder>update()
                            .eq("id", folderId)
                            .eq("owner_user_id", actorId)
                            .isNull("deleted_at")
                            .set("name", name)
                            .set("description", description)
                            .set("visibility", visibility)
                            .set("sort_order", sortOrder)
            ) != 1) {
                throw new BusinessException(
                        409, "收藏夹状态已发生变化，请刷新后重试"
                );
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "收藏夹名称已存在");
        }
        folder.setName(name);
        folder.setDescription(description);
        folder.setVisibility(visibility);
        folder.setSortOrder(sortOrder);
        return view(folder);
    }

    @Transactional
    public void delete(Long folderId) {
        Long actorId = requireActorId();
        FavoriteFolder folder = requireOwnedFolder(actorId, folderId);
        if (isDefault(folder)) {
            throw new BusinessException(400, "默认收藏夹不能删除");
        }
        folderItemMapper.deleteByFolder(folderId);
        if (folderMapper.update(
                null,
                Wrappers.<FavoriteFolder>update()
                        .eq("id", folderId)
                        .eq("owner_user_id", actorId)
                        .eq("is_default", 0)
                        .isNull("deleted_at")
                        .set("item_count", 0)
                        .set("deleted_at", LocalDateTime.now(ZoneOffset.UTC))
        ) != 1) {
            throw new BusinessException(409, "收藏夹状态已发生变化，请重试");
        }
    }

    @Transactional(readOnly = true)
    public List<FavoriteFolderView> visibleFolders(Long ownerUserId) {
        CommunityUser owner = requirePublicOwner(ownerUserId);
        Long viewerId = communityAuth.getOptionalLoginUserId();
        return ownedFolders(ownerUserId).stream()
                .filter(folder -> canView(folder, owner, viewerId))
                .map(FavoriteFolderService::view)
                .toList();
    }

    @Transactional
    public FavoriteFolder ensureDefaultFolder(Long ownerUserId) {
        FavoriteFolder existing = folderMapper.findDefault(ownerUserId);
        if (existing != null) {
            return existing;
        }
        FavoriteFolder folder = new FavoriteFolder();
        folder.setId(IdWorker.getId());
        folder.setOwnerUserId(ownerUserId);
        folder.setName(DEFAULT_FOLDER_NAME);
        folder.setDescription("所有收藏内容");
        folder.setVisibility("PRIVATE");
        folder.setIsDefault(1);
        folder.setItemCount(0L);
        folder.setSortOrder(0);
        folder.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (folderMapper.insert(folder) != 1) {
                throw new BusinessException(500, "创建默认收藏夹失败");
            }
            return folder;
        } catch (DuplicateKeyException exception) {
            FavoriteFolder concurrent = folderMapper.findDefault(ownerUserId);
            if (concurrent == null) {
                throw new BusinessException(409, "默认收藏夹创建冲突，请重试");
            }
            return concurrent;
        }
    }

    @Transactional(readOnly = true)
    public FavoriteFolder requireOwnedFolder(
            Long ownerUserId,
            Long folderId
    ) {
        requireValidFolderId(folderId);
        FavoriteFolder folder = folderMapper.selectById(folderId);
        if (folder == null
                || folder.getDeletedAt() != null
                || !Objects.equals(folder.getOwnerUserId(), ownerUserId)) {
            throw new BusinessException(404, "收藏夹不存在");
        }
        return folder;
    }

    @Transactional(readOnly = true)
    public FavoriteFolder requireCanView(Long folderId) {
        requireValidFolderId(folderId);
        FavoriteFolder folder = folderMapper.selectById(folderId);
        if (folder == null || folder.getDeletedAt() != null) {
            throw new BusinessException(404, "收藏夹不存在");
        }
        CommunityUser owner = requirePublicOwner(folder.getOwnerUserId());
        Long viewerId = communityAuth.getOptionalLoginUserId();
        if (!canView(folder, owner, viewerId)) {
            throw new BusinessException(404, "收藏夹不存在");
        }
        return folder;
    }

    @Transactional(readOnly = true)
    public Map<Long, FavoriteFolder> requireOwnedFolders(
            Long ownerUserId,
            Collection<Long> folderIds
    ) {
        if (folderIds == null || folderIds.isEmpty()) {
            return Map.of();
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long folderId : folderIds) {
            requireValidFolderId(folderId);
            unique.add(folderId);
        }
        if (unique.size() > MAX_CUSTOM_FOLDERS + 1) {
            throw new BusinessException(400, "一次最多选择 51 个收藏夹");
        }
        Map<Long, FavoriteFolder> folders =
                folderMapper.selectBatchIds(unique).stream()
                        .filter(folder -> folder.getDeletedAt() == null)
                        .filter(folder -> Objects.equals(
                                folder.getOwnerUserId(), ownerUserId
                        ))
                        .collect(Collectors.toMap(
                                FavoriteFolder::getId,
                                Function.identity(),
                                (left, right) -> left,
                                LinkedHashMap::new
                        ));
        if (folders.size() != unique.size()) {
            throw new BusinessException(404, "收藏夹不存在");
        }
        return folders;
    }

    @Transactional
    public void incrementItemCount(Long folderId) {
        if (folderMapper.update(
                null,
                Wrappers.<FavoriteFolder>update()
                        .eq("id", folderId)
                        .isNull("deleted_at")
                        .setSql("item_count = item_count + 1")
        ) != 1) {
            throw new BusinessException(409, "收藏夹状态已发生变化，请重试");
        }
    }

    @Transactional
    public void decrementItemCount(Long folderId) {
        folderMapper.update(
                null,
                Wrappers.<FavoriteFolder>update()
                        .eq("id", folderId)
                        .setSql(
                                "item_count = GREATEST(item_count - 1, 0)"
                        )
        );
    }

    private List<FavoriteFolder> ownedFolders(Long ownerUserId) {
        return folderMapper.selectList(
                Wrappers.<FavoriteFolder>query()
                        .eq("owner_user_id", ownerUserId)
                        .isNull("deleted_at")
                        .orderByAsc("sort_order")
                        .orderByAsc("id")
        );
    }

    private boolean canView(
            FavoriteFolder folder,
            CommunityUser owner,
            Long viewerId
    ) {
        if (Objects.equals(viewerId, owner.getId())) {
            return true;
        }
        String visibility = normalizeVisibility(
                folder.getVisibility(), "PRIVATE"
        );
        if ("PUBLIC".equals(visibility)) {
            return true;
        }
        if (viewerId == null) {
            return false;
        }
        CommunityUser viewer = userMapper.selectById(viewerId);
        if (viewer == null || !ACTIVE_USER_STATUSES.contains(viewer.getStatus())) {
            return false;
        }
        Blog ownerBlog = publicPersonalBlog(owner);
        boolean follower = followMapper.findRelation(
                viewerId, "BLOG", ownerBlog.getId()
        ) != null;
        if ("FOLLOWERS_ONLY".equals(visibility)) {
            return follower;
        }
        return "MUTUAL_ONLY".equals(visibility)
                && follower
                && viewer.getPersonalBlogId() != null
                && followMapper.findRelation(
                        owner.getId(), "BLOG", viewer.getPersonalBlogId()
                ) != null;
    }

    private CommunityUser requirePublicOwner(Long ownerUserId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new BusinessException(404, "收藏夹不存在");
        }
        CommunityUser owner = userMapper.selectById(ownerUserId);
        if (owner == null || !ACTIVE_USER_STATUSES.contains(owner.getStatus())) {
            throw new BusinessException(404, "收藏夹不存在");
        }
        return owner;
    }

    private Blog publicPersonalBlog(CommunityUser owner) {
        if (owner.getPersonalBlogId() == null) {
            throw new BusinessException(404, "收藏夹不存在");
        }
        Blog blog = blogMapper.selectById(owner.getPersonalBlogId());
        if (blog == null
                || blog.getDeletedAt() != null
                || !"PERSONAL".equals(blog.getBlogType())
                || !"ACTIVE".equals(blog.getStatus())
                || !Objects.equals(blog.getOwnerUserId(), owner.getId())) {
            throw new BusinessException(404, "收藏夹不存在");
        }
        return blog;
    }

    private Long requireActorId() {
        Long actorId = communityAuth.getLoginUserId();
        CommunityUser actor = userMapper.selectById(actorId);
        if (actor == null || !ACTIVE_USER_STATUSES.contains(actor.getStatus())) {
            throw new BusinessException(403, "当前账号不能管理收藏夹");
        }
        return actorId;
    }

    private static FavoriteFolderView view(FavoriteFolder folder) {
        return new FavoriteFolderView(
                folder.getId(),
                folder.getOwnerUserId(),
                folder.getName(),
                folder.getDescription(),
                normalizeVisibility(folder.getVisibility(), "PRIVATE"),
                isDefault(folder),
                folder.getItemCount() == null ? 0 : folder.getItemCount(),
                folder.getSortOrder() == null ? 0 : folder.getSortOrder(),
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }

    private static boolean isDefault(FavoriteFolder folder) {
        return Integer.valueOf(1).equals(folder.getIsDefault());
    }

    private static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "收藏夹名称不能为空");
        }
        String normalized = Normalizer.normalize(
                value.strip(), Normalizer.Form.NFKC
        );
        if (normalized.length() > 80) {
            throw new BusinessException(400, "收藏夹名称不能超过 80 个字符");
        }
        return normalized;
    }

    private static String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(
                value.strip(), Normalizer.Form.NFKC
        );
        if (normalized.length() > 300) {
            throw new BusinessException(400, "收藏夹说明不能超过 300 个字符");
        }
        return normalized;
    }

    private static String normalizeVisibility(
            String value,
            String defaultValue
    ) {
        String normalized = value == null || value.isBlank()
                ? defaultValue
                : value.strip().toUpperCase(Locale.ROOT);
        if (normalized == null || !VISIBILITIES.contains(normalized)) {
            throw new BusinessException(400, "收藏夹公开范围无效");
        }
        return normalized;
    }

    private static int normalizeSortOrder(
            Integer value,
            int defaultValue
    ) {
        int normalized = value == null ? defaultValue : value;
        if (normalized < 0 || normalized > MAX_SORT_ORDER) {
            throw new BusinessException(
                    400, "收藏夹排序须为 0-" + MAX_SORT_ORDER
            );
        }
        return normalized;
    }

    private static void requireValidFolderId(Long folderId) {
        if (folderId == null || folderId <= 0) {
            throw new BusinessException(400, "收藏夹 ID 无效");
        }
    }
}
