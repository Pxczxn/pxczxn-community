package top.pxczxn.community.article.permission;

import top.pxczxn.platform.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static top.pxczxn.community.article.permission.ArticlePermissionFailure.CONFLICT;
import static top.pxczxn.community.article.permission.ArticlePermissionFailure.FORBIDDEN;
import static top.pxczxn.community.article.permission.ArticlePermissionFailure.NOT_FOUND;
import static top.pxczxn.community.article.permission.ArticlePermissionFailure.UNAUTHENTICATED;

@Slf4j
@Service
public class ArticlePermissionService {

    public static final String PLATFORM_REVIEW_PERMISSION = "community:article:review";

    private static final Set<String> EDITABLE_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");
    private static final Set<String> PUBLIC_AUTHOR_STATUSES =
            Set.of("NORMAL", "LIMITED");
    private static final Set<String> EDITABLE_BLOG_STATUSES =
            Set.of("ACTIVE", "HIDDEN");
    private static final Set<String> EDIT_CONFLICT_STATUSES =
            Set.of("PENDING_REVIEW", "SCHEDULED");
    private static final Set<String> SUBMITTABLE_PUBLISH_STATUSES =
            Set.of("DRAFT", "PUBLISHED", "HIDDEN", "APPROVED", "PUBLISH_FAILED");
    private static final Set<String> SUBMITTABLE_REVIEW_STATUSES =
            Set.of(
                    "NOT_SUBMITTED",
                    "REVISION_REQUIRED",
                    "REJECTED",
                    "CANCELLED",
                    "EXPIRED"
            );
    private static final Set<String> REVIEWABLE_REVIEW_STATUSES =
            Set.of("QUEUED", "AUTO_REVIEWING", "MANUAL_REVIEWING");
    private static final Set<String> ACTIVE_REVIEW_STATUSES =
            Set.of("QUEUED", "AUTO_REVIEWING", "MANUAL_REVIEWING");
    private static final Set<String> NON_PUBLIC_STATUSES =
            Set.of("HIDDEN", "TAKEN_DOWN", "DELETED");

    private final ArticleMapper articleMapper;
    private final BlogMapper blogMapper;
    private final CommunityUserMapper userMapper;
    private final CommunityAuth communityAuth;
    private final List<BlogArticleRoleResolver> roleResolvers;
    private final List<ArticleCollaborationEditResolver> collaborationEditResolvers;
    private final List<BlogFollowerResolver> followerResolvers;
    private final PlatformArticleAuthority platformAuthority;

    public ArticlePermissionService(
            ArticleMapper articleMapper,
            BlogMapper blogMapper,
            CommunityUserMapper userMapper,
            CommunityAuth communityAuth,
            List<BlogArticleRoleResolver> roleResolvers,
            List<ArticleCollaborationEditResolver> collaborationEditResolvers,
            List<BlogFollowerResolver> followerResolvers,
            PlatformArticleAuthority platformAuthority
    ) {
        this.articleMapper = articleMapper;
        this.blogMapper = blogMapper;
        this.userMapper = userMapper;
        this.communityAuth = communityAuth;
        this.roleResolvers = roleResolvers == null ? List.of() : List.copyOf(roleResolvers);
        this.collaborationEditResolvers = collaborationEditResolvers == null ? List.of() : List.copyOf(collaborationEditResolvers);
        this.followerResolvers =
                followerResolvers == null ? List.of() : List.copyOf(followerResolvers);
        this.platformAuthority = platformAuthority;
    }

    /**
     * Authoring context for creating an article directly inside a team blog.
     * Any active team member may create drafts; publish permissions stay with the publish flow.
     */
    @Transactional(readOnly = true)
    public ArticleAuthoringContext requireTeamAuthoringContext(Long blogId) {
        Long actorId = communityAuth.getOptionalLoginUserId();
        if (actorId == null) {
            reject(null, null, UNAUTHENTICATED, "请先登录后再创作文章");
        }
        CommunityUser actor = userMapper.selectById(actorId);
        if (actor == null) {
            reject(null, actorId, UNAUTHENTICATED, "登录用户不存在");
        }
        if (!EDITABLE_USER_STATUSES.contains(actor.getStatus())) {
            reject(null, actorId, FORBIDDEN, "当前账号不能编辑文章");
        }
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null || blog.getDeletedAt() != null || !"ACTIVE".equals(blog.getStatus())) {
            reject(null, actorId, NOT_FOUND, "团队博客不存在或不可用");
        }
        if (!"TEAM".equals(blog.getBlogType())) {
            reject(null, actorId, FORBIDDEN, "仅团队博客支持团队直接创作");
        }
        boolean member = roleResolvers.stream()
                .map(resolver -> resolver.resolve(actorId, blog))
                .flatMap(Optional::stream)
                .anyMatch(role -> role != null);
        if (!member) {
            reject(null, actorId, FORBIDDEN, "仅团队成员可以在团队博客创作");
        }
        return new ArticleAuthoringContext(actor, blog);
    }

    @Transactional(readOnly = true)
    public ArticleAuthoringContext requirePersonalAuthoringContext() {        Long actorId = communityAuth.getOptionalLoginUserId();
        if (actorId == null) {
            reject(null, null, UNAUTHENTICATED, "请先登录后再创作文章");
        }
        CommunityUser actor = userMapper.selectById(actorId);
        if (actor == null) {
            reject(null, actorId, UNAUTHENTICATED, "登录用户不存在");
        }
        if (!EDITABLE_USER_STATUSES.contains(actor.getStatus())) {
            reject(null, actorId, FORBIDDEN, "当前账号不能编辑文章");
        }
        if (actor.getPersonalBlogId() == null) {
            reject(null, actorId, NOT_FOUND, "个人博客不存在");
        }
        Blog blog = blogMapper.selectById(actor.getPersonalBlogId());
        if (blog == null || blog.getDeletedAt() != null || "DELETED".equals(blog.getStatus())) {
            reject(null, actorId, NOT_FOUND, "个人博客不存在");
        }
        if (!"PERSONAL".equals(blog.getBlogType())
                || !actorId.equals(blog.getOwnerUserId())) {
            reject(null, actorId, FORBIDDEN, "无权管理该博客文章");
        }
        assertBlogCanBeManaged(blog, ArticleAction.EDIT, null, actorId);
        return new ArticleAuthoringContext(actor, blog);
    }

    @Transactional(readOnly = true)
    public ArticleCommunityAccess requireCommunityArticle(
            Long articleId,
            ArticleAction action
    ) {
        requireValidArticleId(articleId);
        requireCommunityAction(action);
        Long actorId = communityAuth.getOptionalLoginUserId();
        if (actorId == null) {
            reject(action, articleId, null, UNAUTHENTICATED, "请先登录");
        }
        CommunityUser actor = userMapper.selectById(actorId);
        if (actor == null) {
            reject(action, articleId, actorId, UNAUTHENTICATED, "登录用户不存在");
        }
        Article article = articleMapper.selectById(articleId);
        Blog blog = article == null ? null : blogMapper.selectById(article.getBlogId());
        BlogArticleRole role = resolveRole(actorId, blog).orElse(null);
        ArticlePermissionDecision decision =
                decideCommunity(action, actor, blog, article, role);
        assertDecision(decision, action, articleId, actorId);
        return new ArticleCommunityAccess(actor, blog, article, role);
    }

    @Transactional(readOnly = true)
    public ArticlePublicAccess requirePublicArticle(
            Long articleId,
            ArticleAction action
    ) {
        requireValidArticleId(articleId);
        if (action != ArticleAction.VIEW_DETAIL
                && action != ArticleAction.LIST_PUBLIC) {
            throw new BusinessException(400, "公开文章权限动作无效");
        }
        Long viewerId = communityAuth.getOptionalLoginUserId();
        CommunityUser viewer =
                viewerId == null ? null : userMapper.selectById(viewerId);
        Article article = articleMapper.selectById(articleId);
        Blog blog = article == null ? null : blogMapper.selectById(article.getBlogId());
        CommunityUser author = article == null
                ? null
                : userMapper.selectById(article.getAuthorUserId());
        BlogArticleRole role = resolveRole(viewerId, blog).orElse(null);
        boolean follower = viewerId != null
                && blog != null
                && followerResolvers.stream()
                        .anyMatch(resolver -> resolver.isFollower(viewerId, blog.getId()));
        ArticlePermissionDecision decision =
                decidePublic(action, viewer, author, blog, article, role, follower);
        assertDecision(decision, action, articleId, viewerId);
        return new ArticlePublicAccess(viewer, author, blog, article, role);
    }

    @Transactional(readOnly = true)
    public ArticlePlatformAccess requirePlatformArticle(
            Long articleId,
            ArticleAction action
    ) {
        requireValidArticleId(articleId);
        if (action != ArticleAction.PLATFORM_REVIEW) {
            throw new BusinessException(400, "平台文章权限动作无效");
        }
        if (!platformAuthority.isAuthenticated()) {
            reject(action, articleId, null, UNAUTHENTICATED, "管理员未登录");
        }
        if (!platformAuthority.hasPermission(PLATFORM_REVIEW_PERMISSION)) {
            reject(action, articleId, null, FORBIDDEN, "无文章审核权限");
        }
        Article article = articleMapper.selectById(articleId);
        Blog blog = article == null ? null : blogMapper.selectById(article.getBlogId());
        ArticlePermissionDecision decision = decidePlatformReview(blog, article);
        assertDecision(decision, action, articleId, null);
        return new ArticlePlatformAccess(blog, article);
    }

    public ArticlePermissionDecision decideCommunity(
            ArticleAction action,
            CommunityUser actor,
            Blog blog,
            Article article,
            BlogArticleRole role
    ) {
        if (actor == null) {
            return deny(UNAUTHENTICATED, "请先登录");
        }
        if (!EDITABLE_USER_STATUSES.contains(actor.getStatus())) {
            return deny(FORBIDDEN, "当前账号不能执行该文章操作");
        }
        ArticlePermissionDecision resourceDecision =
                validateManagedResource(blog, article);
        if (!resourceDecision.allowed()) {
            return resourceDecision;
        }
        ArticlePermissionDecision blogDecision = decideManagedBlog(blog, action);
        if (!blogDecision.allowed()) {
            return blogDecision;
        }

        boolean author = Objects.equals(actor.getId(), article.getAuthorUserId());
        boolean owner = role == BlogArticleRole.OWNER;
        boolean administrator = role == BlogArticleRole.ADMIN;
        boolean editor = role == BlogArticleRole.EDITOR;
        boolean collaboratorCanEdit = (action == ArticleAction.VIEW_EDITOR || action == ArticleAction.EDIT)
                && collaborationEditResolvers.stream().anyMatch(resolver -> resolver.canEdit(actor, article));
        boolean roleAllowed = switch (action) {
            case VIEW_EDITOR, EDIT -> author || owner || administrator || editor || collaboratorCanEdit;
            case SUBMIT_REVIEW, WITHDRAW_REVIEW -> author || owner || administrator || editor;
            case DELETE -> author || owner || administrator;
            case PUBLISH -> "PERSONAL".equals(blog.getBlogType())
                    ? author || owner
                    : owner || administrator || editor;
            default -> false;
        };
        if (!roleAllowed) {
            return deny(FORBIDDEN, "无权执行该文章操作");
        }
        if ("LIMITED".equals(actor.getStatus())
                && (action == ArticleAction.SUBMIT_REVIEW
                || action == ArticleAction.PUBLISH)) {
            return deny(FORBIDDEN, "当前账号受到发布限制");
        }
        return decideWorkflowState(action, article);
    }

    public ArticlePermissionDecision decidePublic(
            ArticleAction action,
            CommunityUser viewer,
            CommunityUser author,
            Blog blog,
            Article article,
            BlogArticleRole viewerRole,
            boolean follower
    ) {
        if (article == null
                || article.getDeletedAt() != null
                || "DELETED".equals(article.getPublishStatus())
                || blog == null
                || blog.getDeletedAt() != null
                || "DELETED".equals(blog.getStatus())
                || author == null
                || !PUBLIC_AUTHOR_STATUSES.contains(author.getStatus())) {
            return deny(NOT_FOUND, "文章不存在");
        }
        if (article.getPublishedVersionId() == null
                || "TAKEN_DOWN".equals(article.getPublishStatus())) {
            return deny(NOT_FOUND, "文章不存在");
        }

        boolean validViewer = viewer != null
                && EDITABLE_USER_STATUSES.contains(viewer.getStatus());
        boolean privileged = validViewer
                && (Objects.equals(viewer.getId(), article.getAuthorUserId())
                || viewerRole == BlogArticleRole.OWNER
                || viewerRole == BlogArticleRole.ADMIN
                || viewerRole == BlogArticleRole.EDITOR);

        if (action == ArticleAction.LIST_PUBLIC) {
            if (!"ACTIVE".equals(blog.getStatus())
                    || NON_PUBLIC_STATUSES.contains(article.getPublishStatus())
                    || !"PUBLIC".equals(article.getVisibility())) {
                return deny(NOT_FOUND, "文章不存在");
            }
            return ArticlePermissionDecision.grant();
        }

        if (privileged
                && EDITABLE_BLOG_STATUSES.contains(blog.getStatus())
                && !"TAKEN_DOWN".equals(article.getPublishStatus())) {
            return ArticlePermissionDecision.grant();
        }
        if (!"ACTIVE".equals(blog.getStatus())
                || NON_PUBLIC_STATUSES.contains(article.getPublishStatus())) {
            return deny(NOT_FOUND, "文章不存在");
        }
        return switch (article.getVisibility()) {
            case "PUBLIC", "UNLISTED" -> ArticlePermissionDecision.grant();
            case "FOLLOWERS_ONLY" -> validViewer && follower
                    ? ArticlePermissionDecision.grant()
                    : deny(NOT_FOUND, "文章不存在");
            case "PRIVATE" -> deny(NOT_FOUND, "文章不存在");
            default -> deny(NOT_FOUND, "文章不存在");
        };
    }

    public ArticlePermissionDecision decidePlatformReview(
            Blog blog,
            Article article
    ) {
        ArticlePermissionDecision resourceDecision =
                validateManagedResource(blog, article);
        if (!resourceDecision.allowed()) {
            return resourceDecision;
        }
        if (article.getReviewVersionId() == null
                || !REVIEWABLE_REVIEW_STATUSES.contains(article.getReviewStatus())) {
            return deny(CONFLICT, "文章当前不处于可审核状态");
        }
        return ArticlePermissionDecision.grant();
    }

    private ArticlePermissionDecision decideWorkflowState(
            ArticleAction action,
            Article article
    ) {
        if (action == ArticleAction.EDIT || action == ArticleAction.DELETE) {
            if (EDIT_CONFLICT_STATUSES.contains(article.getPublishStatus())
                    || ACTIVE_REVIEW_STATUSES.contains(article.getReviewStatus())) {
                return deny(
                        CONFLICT,
                        action == ArticleAction.EDIT
                                ? "文章当前状态不能编辑，请先撤回审核或取消定时发布"
                                : "文章当前状态不能删除，请先撤回审核或取消定时发布"
                );
            }
            if ("TAKEN_DOWN".equals(article.getPublishStatus())) {
                return deny(FORBIDDEN, "文章已被平台下架，不能执行该操作");
            }
        }
        if (action == ArticleAction.SUBMIT_REVIEW) {
            if (article.getCurrentVersionId() == null
                    || !SUBMITTABLE_PUBLISH_STATUSES.contains(article.getPublishStatus())
                    || !SUBMITTABLE_REVIEW_STATUSES.contains(article.getReviewStatus())) {
                return deny(CONFLICT, "文章当前状态不能提交审核");
            }
        }
        if (action == ArticleAction.WITHDRAW_REVIEW) {
            if (article.getReviewVersionId() == null
                    || !ACTIVE_REVIEW_STATUSES.contains(article.getReviewStatus())) {
                return deny(CONFLICT, "文章当前没有可撤回的审核");
            }
        }
        if (action == ArticleAction.PUBLISH) {
            boolean publishStateAllowed =
                    "APPROVED".equals(article.getPublishStatus())
                    || "SCHEDULED".equals(article.getPublishStatus())
                    || "PUBLISH_FAILED".equals(article.getPublishStatus())
                    || (article.getPublishedVersionId() != null
                    && Set.of("PUBLISHED", "HIDDEN").contains(
                            article.getPublishStatus()
                    ));
            if (!publishStateAllowed
                    || !"APPROVED".equals(article.getReviewStatus())
                    || article.getReviewVersionId() == null) {
                return deny(CONFLICT, "文章尚未通过审核，不能发布");
            }
        }
        return ArticlePermissionDecision.grant();
    }

    private ArticlePermissionDecision validateManagedResource(
            Blog blog,
            Article article
    ) {
        if (article == null
                || article.getDeletedAt() != null
                || "DELETED".equals(article.getPublishStatus())
                || blog == null
                || blog.getDeletedAt() != null
                || "DELETED".equals(blog.getStatus())) {
            return deny(NOT_FOUND, "文章不存在");
        }
        return ArticlePermissionDecision.grant();
    }

    private ArticlePermissionDecision decideManagedBlog(
            Blog blog,
            ArticleAction action
    ) {
        if ("FROZEN".equals(blog.getStatus())) {
            return deny(FORBIDDEN, "当前博客已被冻结");
        }
        if ("CLOSED".equals(blog.getStatus())) {
            return deny(CONFLICT, "当前博客已关闭");
        }
        if (!EDITABLE_BLOG_STATUSES.contains(blog.getStatus())) {
            return deny(FORBIDDEN, "当前博客状态不允许执行该操作");
        }
        if ("HIDDEN".equals(blog.getStatus())
                && (action == ArticleAction.SUBMIT_REVIEW
                || action == ArticleAction.PUBLISH)) {
            return deny(CONFLICT, "隐藏博客不能提交审核或发布文章");
        }
        return ArticlePermissionDecision.grant();
    }

    private void assertBlogCanBeManaged(
            Blog blog,
            ArticleAction action,
            Long articleId,
            Long actorId
    ) {
        assertDecision(decideManagedBlog(blog, action), action, articleId, actorId);
    }

    private Optional<BlogArticleRole> resolveRole(Long userId, Blog blog) {
        if (userId == null || blog == null) {
            return Optional.empty();
        }
        return roleResolvers.stream()
                .map(resolver -> resolver.resolve(userId, blog))
                .flatMap(Optional::stream)
                .min(Comparator.comparingInt(ArticlePermissionService::roleRank));
    }

    private static int roleRank(BlogArticleRole role) {
        return switch (role) {
            case OWNER -> 0;
            case ADMIN -> 1;
            case EDITOR -> 2;
            case AUTHOR -> 3;
        };
    }

    private static ArticlePermissionDecision deny(
            ArticlePermissionFailure failure,
            String message
    ) {
        return ArticlePermissionDecision.deny(failure, message);
    }

    private void assertDecision(
            ArticlePermissionDecision decision,
            ArticleAction action,
            Long articleId,
            Long actorId
    ) {
        if (!decision.allowed()) {
            log.warn(
                    "Article permission denied: action={}, articleId={}, actorId={}, reason={}",
                    action,
                    articleId,
                    actorId,
                    decision.failure()
            );
        }
        decision.assertAllowed();
    }

    private void reject(
            ArticleAction action,
            Long actorId,
            ArticlePermissionFailure failure,
            String message
    ) {
        reject(action, null, actorId, failure, message);
    }

    private void reject(
            ArticleAction action,
            Long articleId,
            Long actorId,
            ArticlePermissionFailure failure,
            String message
    ) {
        assertDecision(deny(failure, message), action, articleId, actorId);
    }

    private static void requireValidArticleId(Long articleId) {
        if (articleId == null || articleId <= 0) {
            throw new BusinessException(400, "文章 ID 无效");
        }
    }

    private static void requireCommunityAction(ArticleAction action) {
        if (action != ArticleAction.VIEW_EDITOR
                && action != ArticleAction.EDIT
                && action != ArticleAction.DELETE
                && action != ArticleAction.SUBMIT_REVIEW
                && action != ArticleAction.WITHDRAW_REVIEW
                && action != ArticleAction.PUBLISH) {
            throw new BusinessException(400, "社区文章权限动作无效");
        }
    }
}
