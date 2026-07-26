package top.pxczxn.community.social.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogSetting;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.blog.persistence.BlogSettingMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class CommentScopeService {

    private static final Set<String> ACTIVE_USER_STATUSES =
            Set.of("NORMAL", "LIMITED");

    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final BlogSettingMapper settingMapper;
    private final CommunityFollowMapper followMapper;
    private final CommunityAuth communityAuth;
    private final List<TeamBlogMemberResolver> teamMemberResolvers;

    public CommentScopeService(
            CommunityUserMapper userMapper,
            BlogMapper blogMapper,
            BlogSettingMapper settingMapper,
            CommunityFollowMapper followMapper,
            CommunityAuth communityAuth,
            List<TeamBlogMemberResolver> teamMemberResolvers
    ) {
        this.userMapper = userMapper;
        this.blogMapper = blogMapper;
        this.settingMapper = settingMapper;
        this.followMapper = followMapper;
        this.communityAuth = communityAuth;
        this.teamMemberResolvers = teamMemberResolvers == null
                ? List.of()
                : List.copyOf(teamMemberResolvers);
    }

    @Transactional(readOnly = true)
    public CommentActorContext requireCanComment(
            AccessibleContentTarget target
    ) {
        Long actorId = communityAuth.getLoginUserId();
        CommunityUser actor = userMapper.selectById(actorId);
        if (actor == null || !ACTIVE_USER_STATUSES.contains(actor.getStatus())) {
            throw new BusinessException(403, "当前账号不能发表评论");
        }
        if (actor.getCommentRestrictedUntil() != null
                && actor.getCommentRestrictedUntil().isAfter(
                        LocalDateTime.now(ZoneOffset.UTC)
                )) {
            throw new BusinessException(403, "当前账号暂时不能发表评论");
        }
        Blog blog = blogMapper.selectById(target.blogId());
        if (blog == null
                || blog.getDeletedAt() != null
                || !"ACTIVE".equals(blog.getStatus())) {
            throw new BusinessException(404, "内容不存在");
        }
        BlogSetting setting = settingMapper.selectOne(
                Wrappers.<BlogSetting>query()
                        .eq("blog_id", blog.getId())
                        .last("LIMIT 1")
        );
        if (setting == null) {
            throw new BusinessException(500, "博客评论设置不存在");
        }
        String scope = setting.getCommentScope();
        if ("DISABLED".equals(scope)) {
            throw new BusinessException(403, "该博客已关闭评论");
        }
        if (Objects.equals(actorId, blog.getOwnerUserId())) {
            return new CommentActorContext(actor, blog, setting);
        }

        boolean followsBlog = followMapper.findRelation(
                actorId, "BLOG", blog.getId()
        ) != null;
        boolean blogOwnerFollowsActor =
                actor.getPersonalBlogId() != null
                && followMapper.findRelation(
                        blog.getOwnerUserId(),
                        "BLOG",
                        actor.getPersonalBlogId()
                ) != null;
        boolean allowed = switch (scope) {
            case "ALL_LOGGED_IN" -> true;
            case "FOLLOWERS_ONLY" -> followsBlog;
            case "MUTUAL_ONLY" -> "PERSONAL".equals(blog.getBlogType())
                    && followsBlog
                    && blogOwnerFollowsActor;
            case "BLOGGER_FOLLOWING" -> "PERSONAL".equals(blog.getBlogType())
                    && blogOwnerFollowsActor;
            case "TEAM_FOLLOWERS" -> "TEAM".equals(blog.getBlogType())
                    && followsBlog;
            case "TEAM_MEMBERS" -> "TEAM".equals(blog.getBlogType())
                    && teamMemberResolvers.stream().anyMatch(
                            resolver -> resolver.isMember(
                                    actorId, blog.getId()
                            )
                    );
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException(403, "当前账号不在该博客的评论范围内");
        }
        return new CommentActorContext(actor, blog, setting);
    }
}
