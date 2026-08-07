package top.pxczxn.community.social.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.moderation.application.ArticleKeywordReviewEngine;
import top.pxczxn.community.moderation.application.KeywordReviewOutcome;
import top.pxczxn.community.notification.application.CommunityNotificationEvent;
import top.pxczxn.community.notification.application.MomentPublishedNotificationEvent;
import top.pxczxn.community.sanction.application.CommunitySanctionService;
import top.pxczxn.community.sanction.application.SanctionAction;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityContentLikeMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.social.persistence.FavoriteItemMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class MomentService {

    private static final Set<String> ACTIVE_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");
    private static final Set<String> VISIBILITIES =
            Set.of("PUBLIC", "FOLLOWERS_ONLY", "PRIVATE", "UNLISTED");

    private final CommunityMomentMapper momentMapper;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final CommunityContentLikeMapper likeMapper;
    private final FavoriteItemMapper favoriteItemMapper;
    private final CommunityContentAccessService contentAccessService;
    private final MomentContentRenderer contentRenderer;
    private final ArticleKeywordReviewEngine keywordReviewEngine;
    private final CommunityAuth communityAuth;
    private final List<MomentBlogPublisherResolver> publisherResolvers;
    private final CommunitySanctionService sanctionService;
    private final CommunityAbuseGuard abuseGuard;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    public MomentService(
            CommunityMomentMapper momentMapper,
            CommunityUserMapper userMapper,
            BlogMapper blogMapper,
            CommunityContentLikeMapper likeMapper,
            FavoriteItemMapper favoriteItemMapper,
            CommunityContentAccessService contentAccessService,
            MomentContentRenderer contentRenderer,
            ArticleKeywordReviewEngine keywordReviewEngine,
            CommunityAuth communityAuth,
            List<MomentBlogPublisherResolver> publisherResolvers,
            CommunitySanctionService sanctionService,
            CommunityAbuseGuard abuseGuard
    ) {
        this.momentMapper = momentMapper;
        this.userMapper = userMapper;
        this.blogMapper = blogMapper;
        this.likeMapper = likeMapper;
        this.favoriteItemMapper = favoriteItemMapper;
        this.contentAccessService = contentAccessService;
        this.contentRenderer = contentRenderer;
        this.keywordReviewEngine = keywordReviewEngine;
        this.communityAuth = communityAuth;
        this.publisherResolvers = publisherResolvers == null
                ? List.of()
                : List.copyOf(publisherResolvers);
        this.sanctionService = sanctionService;
        this.abuseGuard = abuseGuard;
    }

    @Transactional
    public MomentPublishView publish(PublishMomentCommand command) {
        if (command == null) {
            throw new BusinessException(400, "动态信息不能为空");
        }
        CommunityUser actor = requirePublishingActor();
        Blog blog = requirePublishingBlog(actor, command.blogId());
        MomentType type = MomentType.parse(command.momentType());
        String visibility = normalizeVisibility(command.visibility());
        validateShape(type, command);
        RenderedMomentContent rendered = contentRenderer.render(
                command.textContent(),
                type.requiresText(),
                type == MomentType.CODE
        );
        String linkUrl = type.requiresLink()
                ? normalizeHttpUrl(command.linkUrl())
                : null;
        if (type.referencesArticle()) {
            contentAccessService.requireAccessible(
                    LikeTargetType.ARTICLE, command.articleId()
            );
        }
        CommunityMoment source = null;
        if (type.referencesMoment()) {
            contentAccessService.requireAccessible(
                    LikeTargetType.MOMENT, command.repostMomentId()
            );
            source = momentMapper.selectById(command.repostMomentId());
            if (source == null) {
                throw notFound();
            }
        }
        String reviewText = String.join(
                "\n",
                valueOrEmpty(rendered.textContent()),
                valueOrEmpty(linkUrl)
        );
        abuseGuard.rejectDuplicateContent(actor.getId(), "MOMENT", reviewText);
        KeywordReviewOutcome outcome = keywordReviewEngine.review(
                "MOMENT", null, null, reviewText
        );
        if (outcome.decision() == KeywordReviewOutcome.Decision.BLOCK) {
            throw new BusinessException(400, "动态包含禁止内容，无法发布");
        }
        boolean pending = outcome.decision()
                == KeywordReviewOutcome.Decision.MANUAL_REVIEW;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        CommunityMoment moment = new CommunityMoment();
        moment.setId(IdWorker.getId());
        moment.setActorUserId(actor.getId());
        moment.setBlogId(blog.getId());
        moment.setMomentType(type.name());
        moment.setTextContent(rendered.textContent());
        moment.setRenderedHtml(rendered.renderedHtml());
        moment.setLinkUrl(linkUrl);
        moment.setArticleId(type.referencesArticle()
                ? command.articleId()
                : null);
        moment.setRepostMomentId(type.referencesMoment()
                ? command.repostMomentId()
                : null);
        moment.setVisibility(visibility);
        moment.setStatus(pending ? "PENDING_REVIEW" : "PUBLISHED");
        moment.setLikeCount(0L);
        moment.setFavoriteCount(0L);
        moment.setCommentCount(0L);
        moment.setRepostCount(0L);
        moment.setLockVersion(0);
        moment.setCreatedAt(now);
        if (momentMapper.insert(moment) != 1) {
            throw new BusinessException(500, "发布动态失败");
        }
        if (!pending && source != null && incrementRepostCount(source.getId()) != 1) {
            throw new BusinessException(409, "转发源状态已发生变化，请刷新后重试");
        }
        if (!pending) {
            publishNotifications(moment, source);
        }
        return new MomentPublishView(
                toView(moment, actor, blog),
                "AUTO_APPROVED_WITH_WARNING".equals(outcome.resultCode()),
                outcome.resultCode()
        );
    }

    private void publishNotifications(
            CommunityMoment moment,
            CommunityMoment source
    ) {
        if (eventPublisher == null) {
            return;
        }
        eventPublisher.publishEvent(
                new MomentPublishedNotificationEvent(
                        moment.getId(),
                        moment.getBlogId(),
                        moment.getActorUserId(),
                        moment.getMomentType(),
                        moment.getVisibility()
                )
        );
        if (source != null) {
            eventPublisher.publishEvent(new CommunityNotificationEvent(
                    "REPOST",
                    "INTERACTION",
                    moment.getActorUserId(),
                    source.getActorUserId(),
                    "MOMENT",
                    moment.getId(),
                    "有人转发了你的动态",
                    "有人转发或引用了你发布的动态。",
                    "repost:moment:" + source.getId(),
                    "NORMAL"
            ));
        }
    }

    @Transactional(readOnly = true)
    public MomentView detail(Long momentId) {
        requireValidMomentId(momentId);
        contentAccessService.requireAccessible(
                LikeTargetType.MOMENT, momentId
        );
        CommunityMoment moment = momentMapper.selectById(momentId);
        if (moment == null) {
            throw notFound();
        }
        CommunityUser author = userMapper.selectById(
                moment.getActorUserId()
        );
        Blog blog = blogMapper.selectById(moment.getBlogId());
        return toView(moment, author, blog);
    }

    @Transactional(readOnly = true)
    public MomentPageView publicPage(
            Long blogId,
            int pageNum,
            int pageSize,
            Set<MomentType> momentTypes
    ) {
        if (blogId != null) {
            requirePublicBlog(blogId);
        }
        List<String> typeNames = momentTypes == null || momentTypes.isEmpty()
                ? List.of()
                : momentTypes.stream().map(Enum::name).toList();
        List<CommunityMoment> candidates;
        if (typeNames.isEmpty()) {
            candidates = momentMapper.selectList(
                    Wrappers.<CommunityMoment>lambdaQuery()
                            .eq(CommunityMoment::getStatus, "PUBLISHED")
                            .isNull(CommunityMoment::getDeletedAt)
                            .eq(blogId != null, CommunityMoment::getBlogId, blogId)
                            .orderByDesc(CommunityMoment::getCreatedAt)
                            .orderByDesc(CommunityMoment::getId)
            );
        } else {
            candidates = momentMapper.selectList(
                    Wrappers.<CommunityMoment>lambdaQuery()
                            .eq(CommunityMoment::getStatus, "PUBLISHED")
                            .isNull(CommunityMoment::getDeletedAt)
                            .eq(blogId != null, CommunityMoment::getBlogId, blogId)
                            .in(CommunityMoment::getMomentType, typeNames)
                            .orderByDesc(CommunityMoment::getCreatedAt)
                            .orderByDesc(CommunityMoment::getId)
            );
        }
        List<MomentView> visible = new ArrayList<>();
        for (CommunityMoment candidate : candidates) {
            if (!canView(candidate.getId())) {
                continue;
            }
            CommunityUser author = userMapper.selectById(
                    candidate.getActorUserId()
            );
            Blog blog = blogMapper.selectById(candidate.getBlogId());
            visible.add(toView(candidate, author, blog));
        }
        int validPageNum = validPage(pageNum);
        int validPageSize = validPageSize(pageSize);
        return new MomentPageView(
                slice(visible, validPageNum, validPageSize),
                visible.size(),
                validPageNum,
                validPageSize
        );
    }

    @Transactional(readOnly = true)
    public MomentPageView mine(int pageNum, int pageSize) {
        CommunityUser actor = requireActiveActor();
        List<CommunityMoment> moments = momentMapper.selectList(
                Wrappers.<CommunityMoment>lambdaQuery()
                        .eq(CommunityMoment::getActorUserId, actor.getId())
                        .ne(CommunityMoment::getStatus, "DELETED")
                        .isNull(CommunityMoment::getDeletedAt)
                        .orderByDesc(CommunityMoment::getCreatedAt)
                        .orderByDesc(CommunityMoment::getId)
        );
        int validPageNum = validPage(pageNum);
        int validPageSize = validPageSize(pageSize);
        List<MomentView> views = moments.stream()
                .map(moment -> toView(
                        moment,
                        actor,
                        blogMapper.selectById(moment.getBlogId())
                ))
                .toList();
        return new MomentPageView(
                slice(views, validPageNum, validPageSize),
                views.size(),
                validPageNum,
                validPageSize
        );
    }

    @Transactional
    public MomentDeletionView delete(
            Long momentId,
            Integer expectedLockVersion
    ) {
        requireValidMomentId(momentId);
        if (expectedLockVersion == null || expectedLockVersion < 0) {
            throw new BusinessException(400, "动态锁版本无效");
        }
        CommunityUser actor = requireActiveActor();
        CommunityMoment moment = momentMapper.selectById(momentId);
        if (moment == null) {
            throw notFound();
        }
        Blog blog = blogMapper.selectById(moment.getBlogId());
        if (!canManage(actor.getId(), blog, moment)) {
            throw notFound();
        }
        if ("TAKEN_DOWN".equals(moment.getStatus())) {
            throw new BusinessException(409, "平台下架的动态不能由用户删除或恢复");
        }
        if ("DELETED".equals(moment.getStatus())
                || moment.getDeletedAt() != null) {
            return deletion(moment, true);
        }
        if (!Set.of("PUBLISHED", "PENDING_REVIEW", "HIDDEN")
                .contains(moment.getStatus())) {
            throw new BusinessException(409, "动态当前状态不能删除");
        }
        if (!Objects.equals(
                safeInt(moment.getLockVersion()),
                expectedLockVersion
        )) {
            throw new BusinessException(409, "动态已在其他窗口更新，请刷新后重试");
        }
        String previousStatus = moment.getStatus();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (momentMapper.update(
                null,
                Wrappers.<CommunityMoment>update()
                        .eq("id", momentId)
                        .eq("lock_version", expectedLockVersion)
                        .in("status", "PUBLISHED", "PENDING_REVIEW", "HIDDEN")
                        .isNull("deleted_at")
                        .set("status", "DELETED")
                        .set("deleted_at", now)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        ) != 1) {
            throw new BusinessException(409, "动态状态已发生变化，请重试");
        }
        moment.setStatus("DELETED");
        moment.setDeletedAt(now);
        moment.setLockVersion(expectedLockVersion + 1);
        if ("PUBLISHED".equals(previousStatus)
                && moment.getRepostMomentId() != null) {
            decrementRepostCount(moment.getRepostMomentId());
        }
        return deletion(moment, false);
    }

    @Transactional(readOnly = true)
    public MomentShareLinkView shareLink(Long momentId) {
        MomentView moment = detail(momentId);
        return new MomentShareLinkView(
                moment.momentId(), moment.canonicalPath()
        );
    }

    private MomentView toView(
            CommunityMoment moment,
            CommunityUser author,
            Blog blog
    ) {
        Long viewerId = communityAuth.getOptionalLoginUserId();
        boolean published = "PUBLISHED".equals(moment.getStatus())
                && moment.getDeletedAt() == null;
        boolean liked = published
                && viewerId != null
                && likeMapper.findRelation(
                        viewerId, "MOMENT", moment.getId()
                ) != null;
        boolean favorited = published
                && viewerId != null
                && favoriteItemMapper.findRelation(
                        viewerId, "MOMENT", moment.getId()
                ) != null;
        return new MomentView(
                moment.getId(),
                moment.getMomentType(),
                author(author),
                blog(blog),
                moment.getTextContent(),
                moment.getRenderedHtml(),
                moment.getLinkUrl(),
                article(moment.getArticleId()),
                source(moment.getRepostMomentId()),
                moment.getVisibility(),
                moment.getStatus(),
                safeLong(moment.getLikeCount()),
                safeLong(moment.getFavoriteCount()),
                safeLong(moment.getCommentCount()),
                safeLong(moment.getRepostCount()),
                liked,
                favorited,
                canonicalPath(moment.getId()),
                safeInt(moment.getLockVersion()),
                moment.getCreatedAt(),
                moment.getUpdatedAt()
        );
    }

    private MomentArticleView article(Long articleId) {
        if (articleId == null) {
            return null;
        }
        try {
            AccessibleContentTarget target =
                    contentAccessService.requireAccessible(
                            LikeTargetType.ARTICLE, articleId
                    );
            return new MomentArticleView(
                    articleId,
                    true,
                    target.title(),
                    target.excerpt(),
                    target.coverFileId(),
                    target.canonicalPath()
            );
        } catch (BusinessException exception) {
            if (!Objects.equals(exception.getCode(), 404)) {
                throw exception;
            }
            return new MomentArticleView(
                    articleId, false, null, null, null, null
            );
        }
    }

    private MomentSourceView source(Long sourceMomentId) {
        if (sourceMomentId == null) {
            return null;
        }
        try {
            contentAccessService.requireAccessible(
                    LikeTargetType.MOMENT, sourceMomentId
            );
            CommunityMoment source = momentMapper.selectById(sourceMomentId);
            if (source == null) {
                return unavailableSource(sourceMomentId);
            }
            CommunityUser author = userMapper.selectById(
                    source.getActorUserId()
            );
            Blog blog = blogMapper.selectById(source.getBlogId());
            return new MomentSourceView(
                    source.getId(),
                    true,
                    source.getMomentType(),
                    author(author),
                    blog(blog),
                    source.getTextContent(),
                    source.getRenderedHtml(),
                    source.getLinkUrl(),
                    article(source.getArticleId()),
                    source.getRepostMomentId(),
                    source.getCreatedAt()
            );
        } catch (BusinessException exception) {
            if (!Objects.equals(exception.getCode(), 404)) {
                throw exception;
            }
            return unavailableSource(sourceMomentId);
        }
    }

    private boolean canView(Long momentId) {
        try {
            return contentAccessService.findAccessible(
                    LikeTargetType.MOMENT, momentId
            ) != null;
        } catch (BusinessException exception) {
            if (Objects.equals(exception.getCode(), 404)) {
                return false;
            }
            throw exception;
        }
    }

    private CommunityUser requirePublishingActor() {
        CommunityUser actor = requireActiveActor();
        abuseGuard.check("USER:" + actor.getId(), "MOMENT_PUBLISH", 8, 60);
        sanctionService.requireActionAllowed(actor.getId(), SanctionAction.PUBLISH);
        if (actor.getPublishRestrictedUntil() != null
                && actor.getPublishRestrictedUntil().isAfter(
                        LocalDateTime.now(ZoneOffset.UTC)
                )) {
            throw new BusinessException(403, "当前账号暂时不能发布动态");
        }
        return actor;
    }

    private CommunityUser requireActiveActor() {
        Long actorId = communityAuth.getLoginUserId();
        CommunityUser actor = userMapper.selectById(actorId);
        if (actor == null || !ACTIVE_USER_STATUSES.contains(actor.getStatus())) {
            throw new BusinessException(403, "当前账号不能管理动态");
        }
        return actor;
    }

    private Blog requirePublishingBlog(
            CommunityUser actor,
            Long requestedBlogId
    ) {
        Long blogId = requestedBlogId == null
                ? actor.getPersonalBlogId()
                : requestedBlogId;
        if (blogId == null || blogId <= 0) {
            throw new BusinessException(404, "发布博客不存在");
        }
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null
                || blog.getDeletedAt() != null
                || !"ACTIVE".equals(blog.getStatus())) {
            throw new BusinessException(404, "发布博客不存在");
        }
        boolean personalOwner = "PERSONAL".equals(blog.getBlogType())
                && Objects.equals(actor.getId(), blog.getOwnerUserId());
        boolean teamPublisher = "TEAM".equals(blog.getBlogType())
                && publisherResolvers.stream().anyMatch(
                        resolver -> resolver.canPublish(actor.getId(), blog)
                );
        if (!personalOwner && !teamPublisher) {
            throw new BusinessException(403, "无权使用该博客身份发布动态");
        }
        return blog;
    }

    private Blog requirePublicBlog(Long blogId) {
        if (blogId <= 0) {
            throw new BusinessException(400, "博客 ID 无效");
        }
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null
                || blog.getDeletedAt() != null
                || !"ACTIVE".equals(blog.getStatus())) {
            throw new BusinessException(404, "博客不存在");
        }
        return blog;
    }

    private boolean canManage(
            Long actorId,
            Blog blog,
            CommunityMoment moment
    ) {
        if (Objects.equals(actorId, moment.getActorUserId())) {
            return true;
        }
        if (blog == null) {
            return false;
        }
        if (Objects.equals(actorId, blog.getOwnerUserId())) {
            return true;
        }
        return false;
    }

    private MomentDeletionView deletion(
            CommunityMoment moment,
            boolean replay
    ) {
        long sourceCount = 0;
        if (moment.getRepostMomentId() != null) {
            CommunityMoment source = momentMapper.selectById(
                    moment.getRepostMomentId()
            );
            sourceCount = source == null
                    ? 0
                    : safeLong(source.getRepostCount());
        }
        return new MomentDeletionView(
                moment.getId(),
                "DELETED",
                safeInt(moment.getLockVersion()),
                replay,
                sourceCount
        );
    }

    private int incrementRepostCount(Long sourceMomentId) {
        return momentMapper.update(
                null,
                Wrappers.<CommunityMoment>update()
                        .eq("id", sourceMomentId)
                        .eq("status", "PUBLISHED")
                        .isNull("deleted_at")
                        .setSql("repost_count = repost_count + 1")
        );
    }

    private void decrementRepostCount(Long sourceMomentId) {
        momentMapper.update(
                null,
                Wrappers.<CommunityMoment>update()
                        .eq("id", sourceMomentId)
                        .setSql(
                                "repost_count = "
                                + "GREATEST(repost_count - 1, 0)"
                        )
        );
    }

    private static void validateShape(
            MomentType type,
            PublishMomentCommand command
    ) {
        if (type == MomentType.REPOST
                && command.textContent() != null
                && !command.textContent().isBlank()) {
            throw new BusinessException(400, "纯转发不能填写引用观点");
        }
        if (type.referencesArticle()) {
            requireId(command.articleId(), "文章");
        } else if (command.articleId() != null) {
            throw new BusinessException(400, "当前动态类型不能关联文章");
        }
        if (type.referencesMoment()) {
            requireId(command.repostMomentId(), "转发源动态");
        } else if (command.repostMomentId() != null) {
            throw new BusinessException(400, "当前动态类型不能关联转发源");
        }
        if (!type.requiresLink()
                && command.linkUrl() != null
                && !command.linkUrl().isBlank()) {
            throw new BusinessException(400, "当前动态类型不能设置外链");
        }
    }

    private static String normalizeVisibility(String raw) {
        String value = raw == null || raw.isBlank()
                ? "PUBLIC"
                : raw.strip().toUpperCase(Locale.ROOT);
        if (!VISIBILITIES.contains(value)) {
            throw new BusinessException(400, "动态可见范围无效");
        }
        return value;
    }

    private static String normalizeHttpUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(400, "动态链接不能为空");
        }
        String normalized = Normalizer.normalize(
                raw.strip(), Normalizer.Form.NFKC
        );
        if (normalized.length() > 2_048) {
            throw new BusinessException(400, "动态链接不能超过 2048 个字符");
        }
        try {
            URI uri = new URI(normalized).normalize();
            String scheme = uri.getScheme() == null
                    ? null
                    : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!Set.of("http", "https").contains(scheme)
                    || uri.getHost() == null
                    || uri.getHost().isBlank()
                    || uri.getUserInfo() != null) {
                throw new URISyntaxException(normalized, "unsafe URL");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new BusinessException(400, "动态链接必须是有效的 HTTP(S) 地址");
        }
    }

    private static MomentAuthorView author(CommunityUser user) {
        if (user == null) {
            return new MomentAuthorView(
                    null, null, "社区用户", null
            );
        }
        return new MomentAuthorView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarFileId()
        );
    }

    private static MomentBlogView blog(Blog blog) {
        if (blog == null) {
            return null;
        }
        return new MomentBlogView(
                blog.getId(),
                blog.getBlogType(),
                blog.getName(),
                blog.getSlug(),
                blog.getAvatarFileId()
        );
    }

    private static MomentSourceView unavailableSource(Long sourceMomentId) {
        return new MomentSourceView(
                sourceMomentId,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static String canonicalPath(Long momentId) {
        return "/moments/" + momentId;
    }

    private static void requireValidMomentId(Long momentId) {
        requireId(momentId, "动态");
    }

    private static void requireId(Long id, String label) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, label + " ID 无效");
        }
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

    private static <T> List<T> slice(
            List<T> source,
            int pageNum,
            int pageSize
    ) {
        long fromLong = (long) (pageNum - 1) * pageSize;
        int from = (int) Math.min(fromLong, source.size());
        int to = Math.min(from + pageSize, source.size());
        return List.copyOf(source.subList(from, to));
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static BusinessException notFound() {
        return new BusinessException(404, "动态不存在");
    }
}
