package top.pxczxn.community.social.application;

import com.mars.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.permission.ArticlePublicAccess;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityComment;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommunityContentAccessService {

    private static final Set<String> ACTIVE_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");
    private static final int MAX_REPOST_DEPTH = 16;

    private final ArticlePermissionService articlePermissionService;
    private final CommunityMomentMapper momentMapper;
    private final CommunityCommentMapper commentMapper;
    private final BlogMapper blogMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityFollowMapper followMapper;
    private final CommunityAuth communityAuth;

    @Transactional(readOnly = true)
    public AccessibleContentTarget requireAccessible(
            String rawType,
            Long targetId
    ) {
        return requireAccessible(LikeTargetType.parse(rawType), targetId);
    }

    @Transactional(readOnly = true)
    public AccessibleContentTarget requireAccessible(
            LikeTargetType type,
            Long targetId
    ) {
        requireValidTargetId(targetId);
        return switch (type) {
            case ARTICLE -> requireArticle(targetId);
            case MOMENT -> requireMoment(targetId, new HashSet<>(), 0);
            case COMMENT -> requireComment(targetId);
        };
    }

    @Transactional(readOnly = true)
    public AccessibleContentTarget findAccessible(
            LikeTargetType type,
            Long targetId
    ) {
        try {
            return requireAccessible(type, targetId);
        } catch (BusinessException exception) {
            if (Objects.equals(exception.getCode(), 404)) {
                return null;
            }
            throw exception;
        }
    }

    private AccessibleContentTarget requireArticle(Long articleId) {
        ArticlePublicAccess access = articlePermissionService.requirePublicArticle(
                articleId, ArticleAction.VIEW_DETAIL
        );
        return new AccessibleContentTarget(
                LikeTargetType.ARTICLE,
                access.article().getId(),
                access.article().getAuthorUserId(),
                access.article().getBlogId(),
                access.article().getTitle(),
                access.article().getSummary(),
                access.article().getCoverFileId(),
                access.article().getCanonicalPath(),
                safeLong(access.article().getLikeCount()),
                safeLong(access.article().getFavoriteCount()),
                safeLong(access.article().getCommentCount())
        );
    }

    private AccessibleContentTarget requireMoment(
            Long momentId,
            Set<Long> visited,
            int depth
    ) {
        if (depth > MAX_REPOST_DEPTH || !visited.add(momentId)) {
            throw notFound();
        }
        CommunityMoment moment = momentMapper.selectById(momentId);
        if (moment == null
                || moment.getDeletedAt() != null
                || !"PUBLISHED".equals(moment.getStatus())) {
            throw notFound();
        }
        Blog blog = blogMapper.selectById(moment.getBlogId());
        CommunityUser author = userMapper.selectById(moment.getActorUserId());
        if (blog == null
                || blog.getDeletedAt() != null
                || !"ACTIVE".equals(blog.getStatus())
                || author == null
                || !ACTIVE_USER_STATUSES.contains(author.getStatus())) {
            throw notFound();
        }
        Long viewerId = communityAuth.getOptionalLoginUserId();
        boolean privileged = viewerId != null
                && (Objects.equals(viewerId, moment.getActorUserId())
                || Objects.equals(viewerId, blog.getOwnerUserId()));
        boolean visible = switch (moment.getVisibility()) {
            case "PUBLIC", "UNLISTED" -> true;
            case "FOLLOWERS_ONLY" -> viewerId != null
                    && followMapper.findRelation(
                            viewerId, "BLOG", moment.getBlogId()
                    ) != null;
            case "PRIVATE" -> privileged;
            default -> false;
        };
        if (!visible) {
            throw notFound();
        }
        if (moment.getArticleId() != null) {
            requireArticle(moment.getArticleId());
        }
        if (moment.getRepostMomentId() != null) {
            requireMoment(
                    moment.getRepostMomentId(), visited, depth + 1
            );
        }
        return new AccessibleContentTarget(
                LikeTargetType.MOMENT,
                moment.getId(),
                moment.getActorUserId(),
                moment.getBlogId(),
                momentTitle(moment),
                excerpt(moment.getTextContent()),
                null,
                "/moments/" + moment.getId(),
                safeLong(moment.getLikeCount()),
                safeLong(moment.getFavoriteCount()),
                safeLong(moment.getCommentCount())
        );
    }

    private AccessibleContentTarget requireComment(Long commentId) {
        CommunityComment comment = publishedComment(commentId);
        AccessibleContentTarget parentTarget = switch (comment.getTargetType()) {
            case "ARTICLE" -> requireArticle(comment.getTargetId());
            case "MOMENT" ->
                    requireMoment(comment.getTargetId(), new HashSet<>(), 0);
            default -> throw notFound();
        };
        requireVisibleAnchor(comment, comment.getRootCommentId());
        requireVisibleAnchor(comment, comment.getParentCommentId());
        CommunityUser author = userMapper.selectById(comment.getAuthorUserId());
        if (author == null || !ACTIVE_USER_STATUSES.contains(author.getStatus())) {
            throw notFound();
        }
        return new AccessibleContentTarget(
                LikeTargetType.COMMENT,
                comment.getId(),
                comment.getAuthorUserId(),
                parentTarget.blogId(),
                parentTarget.title(),
                excerpt(comment.getContentText()),
                parentTarget.coverFileId(),
                parentTarget.canonicalPath(),
                safeLong(comment.getLikeCount()),
                0,
                0
        );
    }

    private void requireVisibleAnchor(
            CommunityComment comment,
            Long anchorId
    ) {
        if (anchorId == null) {
            return;
        }
        if (Objects.equals(anchorId, comment.getId())) {
            throw notFound();
        }
        CommunityComment anchor = publishedComment(anchorId);
        if (!Objects.equals(anchor.getTargetType(), comment.getTargetType())
                || !Objects.equals(anchor.getTargetId(), comment.getTargetId())) {
            throw notFound();
        }
    }

    private CommunityComment publishedComment(Long commentId) {
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null
                || comment.getDeletedAt() != null
                || !"PUBLISHED".equals(comment.getStatus())) {
            throw notFound();
        }
        return comment;
    }

    private static String momentTitle(CommunityMoment moment) {
        return switch (moment.getMomentType()) {
            case "ARTICLE_SHARE" -> "分享了一篇文章";
            case "REPOST" -> "转发了一条动态";
            case "QUOTE" -> "引用了一条动态";
            case "TEAM_NOTICE" -> "团队公告";
            case "PROJECT_UPDATE" -> "项目进展";
            default -> "动态";
        };
    }

    private static String excerpt(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        return normalized.length() <= 180
                ? normalized
                : normalized.substring(0, 180) + "…";
    }

    private static void requireValidTargetId(Long targetId) {
        if (targetId == null || targetId <= 0) {
            throw new BusinessException(400, "点赞目标 ID 无效");
        }
    }

    private static BusinessException notFound() {
        return new BusinessException(404, "内容不存在");
    }

    private static long safeLong(Long value) {
        return value == null ? 0 : value;
    }
}
