package top.pxczxn.community.block.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.block.model.CommunityBlock;
import top.pxczxn.community.block.persistence.CommunityBlockMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.taxonomy.model.ArticleTag;
import top.pxczxn.community.taxonomy.model.PlatformTag;
import top.pxczxn.community.taxonomy.persistence.ArticleTagMapper;
import top.pxczxn.community.taxonomy.persistence.PlatformTagMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityBlockServiceImpl implements CommunityBlockService {

    private static final Set<String> TYPES = Set.of("USER", "BLOG", "TAG", "CHAT");

    private final CommunityBlockMapper blockMapper;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final PlatformTagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;

    @Override
    @Transactional
    public CommunityBlockView block(Long blockerUserId, String rawType, Long targetId) {
        Long blocker = user(blockerUserId, "blocker");
        String type = type(rawType);
        Long target = target(targetId);
        requireTarget(type, target);
        if (("USER".equals(type) || "CHAT".equals(type)) && blocker.equals(target)) {
            throw new BusinessException(400, "不能屏蔽自己");
        }
        CommunityBlock existing = find(blocker, type, target);
        if (existing != null) {
            return CommunityBlockView.from(existing);
        }
        CommunityBlock block = new CommunityBlock();
        block.setId(IdWorker.getId());
        block.setBlockerUserId(blocker);
        block.setTargetType(type);
        block.setTargetId(target);
        block.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            if (blockMapper.insert(block) != 1) {
                throw new BusinessException(500, "屏蔽操作失败");
            }
        } catch (DuplicateKeyException exception) {
            CommunityBlock replay = find(blocker, type, target);
            if (replay != null) {
                return CommunityBlockView.from(replay);
            }
            throw exception;
        }
        return CommunityBlockView.from(block);
    }

    @Override
    @Transactional
    public void unblock(Long blockerUserId, String rawType, Long targetId) {
        Long blocker = user(blockerUserId, "blocker");
        String type = type(rawType);
        Long target = target(targetId);
        blockMapper.delete(Wrappers.<CommunityBlock>lambdaQuery()
                .eq(CommunityBlock::getBlockerUserId, blocker)
                .eq(CommunityBlock::getTargetType, type)
                .eq(CommunityBlock::getTargetId, target));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityBlockView> mine(Long blockerUserId) {
        Long blocker = user(blockerUserId, "blocker");
        return blockMapper.selectList(Wrappers.<CommunityBlock>lambdaQuery()
                        .eq(CommunityBlock::getBlockerUserId, blocker)
                        .orderByDesc(CommunityBlock::getCreatedAt)
                        .orderByDesc(CommunityBlock::getId))
                .stream().map(CommunityBlockView::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isContentBlocked(Long viewerUserId, Long authorUserId, Long blogId, String contentType, Long contentId) {
        Scope scope = scope(viewerUserId);
        if (scope.isEmpty()) {
            return false;
        }
        if (scope.users().contains(authorUserId) || scope.blogs().contains(blogId)) {
            return true;
        }
        if (!scope.tags().isEmpty() && "ARTICLE".equalsIgnoreCase(contentType) && contentId != null) {
            return articleTagMapper.selectList(Wrappers.<ArticleTag>lambdaQuery()
                            .eq(ArticleTag::getArticleId, contentId))
                    .stream().map(ArticleTag::getTagId).anyMatch(scope.tags()::contains);
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNotificationBlocked(Long viewerUserId, Long senderUserId, String targetType, Long targetId) {
        Scope scope = scope(viewerUserId);
        if (scope.isEmpty()) {
            return false;
        }
        return scope.users().contains(senderUserId)
                || ("BLOG".equalsIgnoreCase(targetType) && scope.blogs().contains(targetId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isChatRestricted(Long firstUserId, Long secondUserId) {
        if (firstUserId == null || secondUserId == null) {
            return true;
        }
        return chatBlocked(scope(firstUserId), secondUserId)
                || chatBlocked(scope(secondUserId), firstUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserBlocked(Long viewerUserId, Long otherUserId) {
        return scope(viewerUserId).users().contains(otherUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlogBlocked(Long viewerUserId, Long blogId) {
        return scope(viewerUserId).blogs().contains(blogId);
    }

    private Scope scope(Long viewerUserId) {
        if (viewerUserId == null || viewerUserId <= 0) {
            return Scope.EMPTY;
        }
        Map<Type, Set<Long>> values = new EnumMap<>(Type.class);
        for (Type value : Type.values()) {
            values.put(value, Set.of());
        }
        blockMapper.selectList(Wrappers.<CommunityBlock>lambdaQuery()
                        .eq(CommunityBlock::getBlockerUserId, viewerUserId))
                .stream().collect(Collectors.groupingBy(
                        value -> Type.valueOf(value.getTargetType()),
                        () -> new EnumMap<>(Type.class),
                        Collectors.mapping(CommunityBlock::getTargetId, Collectors.toSet())
                )).forEach(values::put);
        return new Scope(values.get(Type.USER), values.get(Type.BLOG), values.get(Type.TAG), values.get(Type.CHAT));
    }

    private CommunityBlock find(Long blocker, String type, Long target) {
        return blockMapper.selectOne(Wrappers.<CommunityBlock>lambdaQuery()
                .eq(CommunityBlock::getBlockerUserId, blocker)
                .eq(CommunityBlock::getTargetType, type)
                .eq(CommunityBlock::getTargetId, target)
                .last("LIMIT 1"));
    }

    private void requireTarget(String type, Long target) {
        boolean exists = switch (type) {
            case "USER", "CHAT" -> userMapper.selectById(target) != null;
            case "BLOG" -> {
                Blog blog = blogMapper.selectById(target);
                yield blog != null && blog.getDeletedAt() == null && "ACTIVE".equals(blog.getStatus());
            }
            case "TAG" -> {
                PlatformTag tag = tagMapper.selectById(target);
                yield tag != null && "ACTIVE".equals(tag.getStatus());
            }
            default -> false;
        };
        if (!exists) {
            throw new BusinessException(404, "屏蔽目标不存在");
        }
    }

    private Long user(Long value, String label) {
        if (value == null || value <= 0 || userMapper.selectById(value) == null) {
            throw new BusinessException(401, "未知的" + label);
        }
        return value;
    }

    private static String type(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(400, "缺少屏蔽目标类型");
        }
        String value = raw.strip().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(value)) {
            throw new BusinessException(400, "无效的屏蔽目标类型");
        }
        return value;
    }

    private static Long target(Long value) {
        if (value == null || value <= 0) {
            throw new BusinessException(400, "无效的屏蔽目标编号");
        }
        return value;
    }

    private static boolean chatBlocked(Scope scope, Long peerUserId) {
        return scope.users().contains(peerUserId) || scope.chats().contains(peerUserId);
    }

    private enum Type { USER, BLOG, TAG, CHAT }

    private record Scope(Set<Long> users, Set<Long> blogs, Set<Long> tags, Set<Long> chats) {
        private static final Scope EMPTY = new Scope(Set.of(), Set.of(), Set.of(), Set.of());
        private boolean isEmpty() { return users.isEmpty() && blogs.isEmpty() && tags.isEmpty() && chats.isEmpty(); }
    }
}
