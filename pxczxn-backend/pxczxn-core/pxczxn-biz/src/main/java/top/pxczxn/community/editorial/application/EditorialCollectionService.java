package top.pxczxn.community.editorial.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.editorial.model.EditorialCollection;
import top.pxczxn.community.editorial.model.EditorialCollectionItem;
import top.pxczxn.community.editorial.persistence.EditorialCollectionItemMapper;
import top.pxczxn.community.editorial.persistence.EditorialCollectionMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EditorialCollectionService {

    private static final Set<String> KINDS = Set.of("TOPIC", "EVENT", "ANNOUNCEMENT", "FEATURED", "COLLECTION");
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");
    private static final Set<String> TARGETS = Set.of("ARTICLE", "SERIES");

    private final EditorialCollectionMapper collectionMapper;
    private final EditorialCollectionItemMapper itemMapper;

    @Transactional
    @CacheEvict(cacheNames = "publicEditorial", allEntries = true)
    public EditorialCollectionView create(Long adminId, EditorialCollectionCommand command) {
        EditorialCollection c = build(command);
        c.setId(IdWorker.getId());
        c.setCreatedByAdminId(adminId);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        if ("PUBLISHED".equals(c.getStatus())) {
            c.setPublishedAt(LocalDateTime.now());
        }
        collectionMapper.insert(c);
        replaceItems(c.getId(), command.items());
        return view(c);
    }

    @Transactional
    @CacheEvict(cacheNames = "publicEditorial", allEntries = true)
    public EditorialCollectionView update(Long id, EditorialCollectionCommand command) {
        EditorialCollection c = require(id);
        EditorialCollection next = build(command);
        next.setId(id);
        next.setCreatedByAdminId(c.getCreatedByAdminId());
        next.setCreatedAt(c.getCreatedAt());
        next.setPublishedAt("PUBLISHED".equals(next.getStatus())
                ? (c.getPublishedAt() == null ? LocalDateTime.now() : c.getPublishedAt())
                : c.getPublishedAt());
        next.setUpdatedAt(LocalDateTime.now());
        collectionMapper.updateById(next);
        replaceItems(id, command.items());
        return view(next);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "publicEditorial")
    public List<EditorialCollectionView> publicList() {
        LocalDateTime now = LocalDateTime.now();
        List<EditorialCollection> collections = collectionMapper.selectList(
                Wrappers.<EditorialCollection>lambdaQuery()
                        .eq(EditorialCollection::getStatus, "PUBLISHED")
                        .and(q -> q.isNull(EditorialCollection::getStartsAt)
                                .or().le(EditorialCollection::getStartsAt, now))
                        .and(q -> q.isNull(EditorialCollection::getEndsAt)
                                .or().gt(EditorialCollection::getEndsAt, now))
                        .orderByAsc(EditorialCollection::getDisplayOrder)
                        .orderByDesc(EditorialCollection::getPublishedAt)
        );
        if (collections.isEmpty()) {
            return List.of();
        }
        List<Long> collectionIds = collections.stream()
                .map(EditorialCollection::getId).toList();
        List<EditorialCollectionItem> allItems = itemMapper.selectList(
                Wrappers.<EditorialCollectionItem>lambdaQuery()
                        .in(EditorialCollectionItem::getCollectionId, collectionIds)
                        .orderByAsc(EditorialCollectionItem::getDisplayOrder)
                        .orderByAsc(EditorialCollectionItem::getId)
        );
        Map<Long, List<EditorialCollectionItem>> itemsByCollection = allItems.stream()
                .collect(Collectors.groupingBy(EditorialCollectionItem::getCollectionId));
        return collections.stream()
                .map(c -> EditorialCollectionView.from(c,
                        itemsByCollection.getOrDefault(c.getId(), List.of()).stream()
                                .map(i -> new EditorialItemView(i.getTargetType(), i.getTargetId(), i.getDisplayOrder()))
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EditorialCollectionView> adminList() {
        return collectionMapper.selectList(
                Wrappers.<EditorialCollection>lambdaQuery()
                        .orderByDesc(EditorialCollection::getUpdatedAt)
        ).stream().map(this::view).toList();
    }

    private EditorialCollection build(EditorialCollectionCommand x) {
        if (x == null || x.title() == null || x.title().isBlank() || x.title().trim().length() > 160) {
            throw new BusinessException(400, "合集标题无效");
        }
        String kind = upper(x.kind(), KINDS, "合集类型");
        String status = upper(x.status() == null ? "DRAFT" : x.status(), STATUSES, "合集状态");
        if (x.startsAt() != null && x.endsAt() != null && !x.endsAt().isAfter(x.startsAt())) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }
        EditorialCollection c = new EditorialCollection();
        c.setKind(kind);
        c.setTitle(x.title().trim());
        c.setSlug(slug(x.slug(), x.title()));
        c.setSummary(x.summary() == null ? null : x.summary().trim());
        c.setCoverFileId(x.coverFileId());
        c.setStatus(status);
        c.setStartsAt(x.startsAt());
        c.setEndsAt(x.endsAt());
        c.setDisplayOrder(x.displayOrder() == null ? 0 : x.displayOrder());
        return c;
    }

    private void replaceItems(Long id, List<EditorialItemCommand> items) {
        itemMapper.delete(Wrappers.<EditorialCollectionItem>lambdaQuery()
                .eq(EditorialCollectionItem::getCollectionId, id));
        if (items == null) return;
        Set<String> seen = new HashSet<>();
        for (EditorialItemCommand x : items) {
            String t = upper(x.targetType(), TARGETS, "内容类型");
            if (x.targetId() == null || x.targetId() <= 0 || !seen.add(t + ":" + x.targetId())) {
                throw new BusinessException(400, "合集内容无效或重复");
            }
            EditorialCollectionItem item = new EditorialCollectionItem();
            item.setId(IdWorker.getId());
            item.setCollectionId(id);
            item.setTargetType(t);
            item.setTargetId(x.targetId());
            item.setDisplayOrder(x.displayOrder() == null ? 0 : x.displayOrder());
            item.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(item);
        }
    }

    private EditorialCollectionView view(EditorialCollection c) {
        List<EditorialItemView> items = itemMapper.selectList(
                Wrappers.<EditorialCollectionItem>lambdaQuery()
                        .eq(EditorialCollectionItem::getCollectionId, c.getId())
                        .orderByAsc(EditorialCollectionItem::getDisplayOrder)
                        .orderByAsc(EditorialCollectionItem::getId)
        ).stream().map(i -> new EditorialItemView(i.getTargetType(), i.getTargetId(), i.getDisplayOrder())).toList();
        return EditorialCollectionView.from(c, items);
    }

    private EditorialCollection require(Long id) {
        EditorialCollection c = collectionMapper.selectById(id);
        if (c == null) throw new BusinessException(404, "运营合集不存在");
        return c;
    }

    private static String upper(String value, Set<String> allowed, String label) {
        String n = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(n)) throw new BusinessException(400, label + "无效");
        return n;
    }

    private static String slug(String raw, String title) {
        String value = (raw == null || raw.isBlank() ? title : raw).trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("(^-|-$)", "");
        if (value.isBlank() || value.length() > 160) {
            throw new BusinessException(400, "合集地址无效");
        }
        return value;
    }
}