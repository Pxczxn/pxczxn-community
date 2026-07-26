package top.pxczxn.community.social.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogSetting;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.blog.persistence.BlogSettingMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommentScopeServiceTest {

    private CommunityUserMapper userMapper;
    private BlogMapper blogMapper;
    private BlogSettingMapper settingMapper;
    private CommunityFollowMapper followMapper;
    private CommunityAuth auth;
    private TeamBlogMemberResolver teamMemberResolver;
    private CommentScopeService service;
    private CommunityUser actor;
    private Blog blog;
    private BlogSetting setting;

    @BeforeEach
    void setUp() {
        userMapper = mock(CommunityUserMapper.class);
        blogMapper = mock(BlogMapper.class);
        settingMapper = mock(BlogSettingMapper.class);
        followMapper = mock(CommunityFollowMapper.class);
        auth = mock(CommunityAuth.class);
        teamMemberResolver = mock(TeamBlogMemberResolver.class);
        service = new CommentScopeService(
                userMapper,
                blogMapper,
                settingMapper,
                followMapper,
                auth,
                List.of(teamMemberResolver)
        );
        actor = user(100L, 200L);
        blog = blog(300L, 101L, "PERSONAL");
        setting = setting("ALL_LOGGED_IN");
        when(auth.getLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L)).thenReturn(actor);
        when(blogMapper.selectById(300L)).thenReturn(blog);
        when(settingMapper.selectOne(any())).thenReturn(setting);
    }

    @Test
    void allLoggedInAllowsAnActiveUser() {
        CommentActorContext result =
                service.requireCanComment(target());

        assertThat(result.actor().getId()).isEqualTo(100L);
        assertThat(result.blog().getId()).isEqualTo(300L);
    }

    @Test
    void activeCommentRestrictionBlocksTheUser() {
        actor.setCommentRestrictedUntil(LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> service.requireCanComment(target()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前账号暂时不能发表评论");
    }

    @Test
    void followersOnlyRequiresFollowRelation() {
        setting.setCommentScope("FOLLOWERS_ONLY");

        assertThatThrownBy(() -> service.requireCanComment(target()))
                .isInstanceOf(BusinessException.class);

        when(followMapper.findRelation(100L, "BLOG", 300L))
                .thenReturn(follow(100L, 300L));
        assertThat(service.requireCanComment(target()).actor().getId())
                .isEqualTo(100L);
    }

    @Test
    void mutualOnlyRequiresBothPersonalBlogRelations() {
        setting.setCommentScope("MUTUAL_ONLY");
        when(followMapper.findRelation(100L, "BLOG", 300L))
                .thenReturn(follow(100L, 300L));

        assertThatThrownBy(() -> service.requireCanComment(target()))
                .isInstanceOf(BusinessException.class);

        when(followMapper.findRelation(101L, "BLOG", 200L))
                .thenReturn(follow(101L, 200L));
        assertThat(service.requireCanComment(target()).actor().getId())
                .isEqualTo(100L);
    }

    @Test
    void bloggerFollowingRequiresOwnerToFollowActorBlog() {
        setting.setCommentScope("BLOGGER_FOLLOWING");

        assertThatThrownBy(() -> service.requireCanComment(target()))
                .isInstanceOf(BusinessException.class);

        when(followMapper.findRelation(101L, "BLOG", 200L))
                .thenReturn(follow(101L, 200L));
        assertThat(service.requireCanComment(target()).actor().getId())
                .isEqualTo(100L);
    }

    @Test
    void disabledBlocksEvenTheBlogOwner() {
        actor.setId(101L);
        when(auth.getLoginUserId()).thenReturn(101L);
        when(userMapper.selectById(101L)).thenReturn(actor);
        setting.setCommentScope("DISABLED");

        assertThatThrownBy(() -> service.requireCanComment(target()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该博客已关闭评论");
    }

    @Test
    void teamMembersUsesTheExtensionResolver() {
        blog.setBlogType("TEAM");
        setting.setCommentScope("TEAM_MEMBERS");
        when(teamMemberResolver.isMember(100L, 300L)).thenReturn(true);

        assertThat(service.requireCanComment(target()).actor().getId())
                .isEqualTo(100L);
    }

    private static AccessibleContentTarget target() {
        return new AccessibleContentTarget(
                LikeTargetType.ARTICLE,
                400L,
                102L,
                300L,
                "文章",
                "摘要",
                null,
                "/article/400",
                0,
                0,
                0
        );
    }

    private static CommunityUser user(Long id, Long personalBlogId) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setDisplayName("用户" + id);
        user.setStatus("NORMAL");
        user.setPersonalBlogId(personalBlogId);
        return user;
    }

    private static Blog blog(
            Long id,
            Long ownerUserId,
            String type
    ) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setOwnerUserId(ownerUserId);
        blog.setBlogType(type);
        blog.setStatus("ACTIVE");
        return blog;
    }

    private static BlogSetting setting(String scope) {
        BlogSetting setting = new BlogSetting();
        setting.setCommentScope(scope);
        return setting;
    }

    private static CommunityFollow follow(
            Long userId,
            Long targetId
    ) {
        CommunityFollow follow = new CommunityFollow();
        follow.setFollowerUserId(userId);
        follow.setTargetType("BLOG");
        follow.setTargetId(targetId);
        return follow;
    }
}
