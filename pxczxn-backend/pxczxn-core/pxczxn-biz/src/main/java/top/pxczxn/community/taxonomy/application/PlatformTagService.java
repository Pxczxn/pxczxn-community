package top.pxczxn.community.taxonomy.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mars.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.taxonomy.model.ArticleTag;
import top.pxczxn.community.taxonomy.model.PlatformTag;
import top.pxczxn.community.taxonomy.persistence.ArticleTagMapper;
import top.pxczxn.community.taxonomy.persistence.PlatformTagMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PlatformTagService {

    private static final Pattern SLUG_PATTERN =
            Pattern.compile("[a-z0-9]+(?:[-_][a-z0-9]+)*");
    private static final Set<String> EDITABLE_STATUSES = Set.of("ACTIVE", "HIDDEN");

    private final PlatformTagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;

    @Transactional(readOnly = true)
    public List<PlatformTagView> listPublic(String keyword) {
        var query = Wrappers.<PlatformTag>lambdaQuery()
                .eq(PlatformTag::getStatus, "ACTIVE");
        if (keyword != null && !keyword.isBlank()) {
            String normalized = Normalizer.normalize(keyword.trim(), Normalizer.Form.NFKC);
            if (normalized.length() > 80) {
                throw new BusinessException(400, "标签搜索词不能超过 80 个字符");
            }
            query.and(wrapper -> wrapper
                    .like(PlatformTag::getName, normalized)
                    .or()
                    .like(PlatformTag::getSlug, normalized.toLowerCase(Locale.ROOT)));
        }
        query.orderByDesc(PlatformTag::getUsageCount)
                .orderByAsc(PlatformTag::getName)
                .last("LIMIT 100");
        return tagMapper.selectList(query).stream().map(PlatformTagService::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformTagView> listAdmin(String status, String keyword) {
        var query = Wrappers.<PlatformTag>lambdaQuery();
        if (status != null && !status.isBlank()) {
            query.eq(PlatformTag::getStatus, normalizeStatus(status, Set.of(
                    "ACTIVE", "HIDDEN", "MERGED", "DELETED"
            )));
        } else {
            query.ne(PlatformTag::getStatus, "DELETED");
        }
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(PlatformTag::getName, value)
                    .or()
                    .like(PlatformTag::getSlug, value));
        }
        query.orderByDesc(PlatformTag::getUsageCount)
                .orderByDesc(PlatformTag::getUpdatedAt)
                .last("LIMIT 500");
        return tagMapper.selectList(query).stream().map(PlatformTagService::toView).toList();
    }

    @Transactional
    public PlatformTagView create(Long adminId, CreatePlatformTagCommand command) {
        requireAdminId(adminId);
        if (command == null) {
            throw new BusinessException(400, "标签信息不能为空");
        }
        PlatformTag tag = new PlatformTag();
        tag.setId(IdWorker.getId());
        tag.setName(requiredText(command.name(), "标签名称", 80));
        tag.setSlug(normalizeSlug(command.slug()));
        tag.setDescription(optionalText(command.description(), "标签描述", 300));
        tag.setStatus("ACTIVE");
        tag.setUsageCount(0L);
        tag.setCreatedByAdminId(adminId);
        tag.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (tagMapper.insert(tag) != 1) {
                throw new BusinessException(500, "创建标签失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "标签名称或地址已存在");
        }
        return toView(tag);
    }

    @Transactional
    public PlatformTagView update(Long tagId, UpdatePlatformTagCommand command) {
        PlatformTag tag = requireTag(tagId);
        if (command == null) {
            throw new BusinessException(400, "标签信息不能为空");
        }
        var update = Wrappers.<PlatformTag>lambdaUpdate()
                .eq(PlatformTag::getId, tag.getId())
                .ne(PlatformTag::getStatus, "DELETED");
        boolean changed = false;
        if (command.name() != null) {
            update.set(PlatformTag::getName, requiredText(
                    command.name(), "标签名称", 80
            ));
            changed = true;
        }
        if (command.slug() != null) {
            update.set(PlatformTag::getSlug, normalizeSlug(command.slug()));
            changed = true;
        }
        if (command.description() != null) {
            update.set(PlatformTag::getDescription, optionalText(
                    command.description(), "标签描述", 300
            ));
            changed = true;
        }
        if (command.status() != null) {
            update.set(PlatformTag::getStatus, normalizeStatus(
                    command.status(), EDITABLE_STATUSES
            ));
            changed = true;
        }
        if (changed) {
            try {
                if (tagMapper.update(null, update) != 1) {
                    throw new BusinessException(409, "标签已发生变化，请刷新后重试");
                }
            } catch (DuplicateKeyException exception) {
                throw new BusinessException(409, "标签名称或地址已存在");
            }
        }
        return toView(tagMapper.selectById(tag.getId()));
    }

    @Transactional
    public void delete(Long tagId) {
        PlatformTag tag = requireTag(tagId);
        Long usages = articleTagMapper.selectCount(
                Wrappers.<ArticleTag>lambdaQuery().eq(ArticleTag::getTagId, tagId)
        );
        if (usages != null && usages > 0) {
            throw new BusinessException(409, "标签仍被文章使用，请先隐藏或合并标签");
        }
        if (tagMapper.update(
                null,
                Wrappers.<PlatformTag>lambdaUpdate()
                        .eq(PlatformTag::getId, tagId)
                        .ne(PlatformTag::getStatus, "DELETED")
                        .set(PlatformTag::getStatus, "DELETED")
        ) != 1) {
            throw new BusinessException(409, "标签已发生变化，请刷新后重试");
        }
    }

    @Transactional
    public List<PlatformTagView> replaceArticleTags(Long articleId, List<Long> requestedTagIds) {
        if (articleId == null || articleId <= 0) {
            throw new BusinessException(400, "文章 ID 无效");
        }
        LinkedHashSet<Long> tagIds = new LinkedHashSet<>(
                requestedTagIds == null ? List.of() : requestedTagIds
        );
        tagIds.remove(null);
        if (tagIds.size() > 5) {
            throw new BusinessException(400, "每篇文章最多选择 5 个标签");
        }
        List<ArticleTag> oldRelations = articleTagMapper.selectList(
                Wrappers.<ArticleTag>lambdaQuery().eq(ArticleTag::getArticleId, articleId)
        );
        if (!tagIds.isEmpty()) {
            Long activeCount = tagMapper.selectCount(
                    Wrappers.<PlatformTag>lambdaQuery()
                            .in(PlatformTag::getId, tagIds)
                            .eq(PlatformTag::getStatus, "ACTIVE")
            );
            if (activeCount == null || activeCount != tagIds.size()) {
                throw new BusinessException(400, "包含不存在或不可用的平台标签");
            }
        }
        articleTagMapper.delete(
                Wrappers.<ArticleTag>lambdaQuery().eq(ArticleTag::getArticleId, articleId)
        );
        int sort = 0;
        for (Long tagId : tagIds) {
            ArticleTag relation = new ArticleTag();
            relation.setArticleId(articleId);
            relation.setTagId(tagId);
            relation.setSortOrder(sort++);
            relation.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            if (articleTagMapper.insert(relation) != 1) {
                throw new BusinessException(500, "保存文章标签失败");
            }
        }
        LinkedHashSet<Long> affected = new LinkedHashSet<>(tagIds);
        oldRelations.forEach(relation -> affected.add(relation.getTagId()));
        affected.forEach(tagMapper::refreshUsageCount);
        if (tagIds.isEmpty()) {
            return List.of();
        }
        List<PlatformTag> tags = tagMapper.selectList(
                Wrappers.<PlatformTag>lambdaQuery().in(PlatformTag::getId, tagIds)
        );
        return tagIds.stream()
                .map(id -> tags.stream()
                        .filter(tag -> id.equals(tag.getId()))
                        .findFirst()
                        .map(PlatformTagService::toView)
                        .orElseThrow())
                .toList();
    }

    private PlatformTag requireTag(Long tagId) {
        if (tagId == null || tagId <= 0) {
            throw new BusinessException(400, "标签 ID 无效");
        }
        PlatformTag tag = tagMapper.selectById(tagId);
        if (tag == null || "DELETED".equals(tag.getStatus())) {
            throw new BusinessException(404, "标签不存在");
        }
        return tag;
    }

    private static PlatformTagView toView(PlatformTag tag) {
        return new PlatformTagView(
                tag.getId(),
                tag.getName(),
                tag.getSlug(),
                tag.getDescription(),
                tag.getStatus(),
                tag.getUsageCount() == null ? 0 : tag.getUsageCount()
        );
    }

    private static void requireAdminId(Long adminId) {
        if (adminId == null || adminId <= 0) {
            throw new BusinessException(401, "管理员未登录");
        }
    }

    private static String requiredText(String raw, String label, int max) {
        if (raw == null) {
            throw new BusinessException(400, label + "不能为空");
        }
        String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (value.isEmpty() || value.length() > max) {
            throw new BusinessException(400, label + "长度须为 1-" + max + " 个字符");
        }
        return value;
    }

    private static String optionalText(String raw, String label, int max) {
        if (raw == null) {
            return null;
        }
        String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (value.length() > max) {
            throw new BusinessException(400, label + "不能超过 " + max + " 个字符");
        }
        return value.isEmpty() ? null : value;
    }

    private static String normalizeSlug(String raw) {
        String slug = raw == null
                ? ""
                : Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
                        .toLowerCase(Locale.ROOT);
        if (slug.isEmpty() || slug.length() > 80 || !SLUG_PATTERN.matcher(slug).matches()) {
            throw new BusinessException(400, "标签地址须为 1-80 位小写字母、数字、下划线或连字符");
        }
        return slug;
    }

    private static String normalizeStatus(String raw, Set<String> allowed) {
        String status = raw.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(status)) {
            throw new BusinessException(400, "标签状态无效");
        }
        return status;
    }
}
