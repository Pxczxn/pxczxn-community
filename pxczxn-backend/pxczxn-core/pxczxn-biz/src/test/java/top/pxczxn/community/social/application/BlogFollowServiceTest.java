package top.pxczxn.community.social.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogFollowServiceTest {

    private CommunityFollowMapper followMapper;
    private BlogMapper blogMapper;
    private CommunityUserMapper userMapper;
    private CommunityAuth communityAuth;
    private BlogFollowService service;
    private CommunityUser actor;
    private CommunityUser targetOwner;
    private Blog actorBlog;
    private Blog targetBlog;

    @BeforeEach
    void setUp() {
        followMapper = mock(CommunityFollowMapper.class);
        blogMapper = mock(BlogMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        communityAuth = mock(CommunityAuth.class);
        service = new BlogFollowService(
                followMapper, blogMapper, userMapper, communityAuth
        );

        actor = user(100L, 200L);
        targetOwner = user(101L, 300L);
        actorBlog = blog(200L, 100L, "PERSONAL", 2L);
        targetBlog = blog(300L, 101L, "PERSONAL", 7L);

        when(communityAuth.getLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L)).thenReturn(actor);
        when(userMapper.selectById(101L)).thenReturn(targetOwner);
        when(blogMapper.selectById(200L)).thenReturn(actorBlog);
        when(blogMapper.selectById(300L)).thenReturn(targetBlog);
    }

    @Test
    void firstFollowCreatesRelationAndIncrementsCounter() {
        when(followMapper.findRelation(100L, "BLOG", 300L)).thenReturn(null);
        when(followMapper.findRelation(101L, "BLOG", 200L)).thenReturn(null);
        when(followMapper.insert(any())).thenReturn(1);
        when(blogMapper.update(isNull(), any())).thenReturn(1);

        BlogFollowRelationshipView result = service.follow(
                300L, new UpdateBlogFollowCommand("important", true)
        );

        assertThat(result.following()).isTrue();
        assertThat(result.followedBy()).isFalse();
        assertThat(result.specialFollow()).isTrue();
        assertThat(result.notificationLevel()).isEqualTo("IMPORTANT");
        assertThat(result.followerCount()).isEqualTo(8);
        verify(followMapper).insert(any());
        verify(blogMapper).update(isNull(), any());
    }

    @Test
    void repeatedFollowKeepsExistingSettingsAndDoesNotIncrement() {
        CommunityFollow existing = relation(400L, 100L, 300L, "MUTED", true);
        when(followMapper.findRelation(100L, "BLOG", 300L)).thenReturn(existing);
        when(followMapper.findRelation(101L, "BLOG", 200L)).thenReturn(null);

        BlogFollowRelationshipView result = service.follow(300L, null);

        assertThat(result.notificationLevel()).isEqualTo("MUTED");
        assertThat(result.specialFollow()).isTrue();
        assertThat(result.followerCount()).isEqualTo(7);
        verify(followMapper, never()).insert(any());
        verify(blogMapper, never()).update(any(), any());
    }

    @Test
    void unfollowIsIdempotentAndDecrementsOnlyAfterDelete() {
        CommunityFollow existing = relation(400L, 100L, 300L, "ALL", false);
        when(followMapper.findRelation(100L, "BLOG", 300L)).thenReturn(existing);
        when(followMapper.findRelation(101L, "BLOG", 200L)).thenReturn(null);
        when(followMapper.deleteRelation(100L, "BLOG", 300L)).thenReturn(1);
        when(blogMapper.update(isNull(), any())).thenReturn(1);

        BlogFollowRelationshipView result = service.unfollow(300L);

        assertThat(result.following()).isFalse();
        assertThat(result.followerCount()).isEqualTo(6);
        verify(followMapper).deleteRelation(100L, "BLOG", 300L);
        verify(blogMapper).update(isNull(), any());
    }

    @Test
    void personalBlogCannotFollowItself() {
        assertThatThrownBy(() -> service.follow(200L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能关注自己的个人博客");

        verify(followMapper, never()).insert(any());
    }

    @Test
    void teamBlogDoesNotSupportSpecialFollow() {
        targetBlog.setBlogType("TEAM");

        assertThatThrownBy(() -> service.follow(
                300L, new UpdateBlogFollowCommand("ALL", true)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有个人博客支持特别关注");
    }

    @Test
    void twoPersonalBlogRelationsAreReportedAsMutual() {
        CommunityFollow outbound = relation(400L, 100L, 300L, "ALL", false);
        CommunityFollow inbound = relation(401L, 101L, 200L, "IMPORTANT", false);
        when(followMapper.findRelation(100L, "BLOG", 300L)).thenReturn(outbound);
        when(followMapper.findRelation(101L, "BLOG", 200L)).thenReturn(inbound);
        when(communityAuth.getOptionalLoginUserId()).thenReturn(100L);

        BlogFollowRelationshipView result = service.relationship(300L);

        assertThat(result.following()).isTrue();
        assertThat(result.followedBy()).isTrue();
        assertThat(result.mutual()).isTrue();
    }

    @Test
    void teamBlogNeverUsesPersonalMutualSemantics() {
        targetBlog.setBlogType("TEAM");
        CommunityFollow outbound = relation(400L, 100L, 300L, "ALL", false);
        when(followMapper.findRelation(100L, "BLOG", 300L)).thenReturn(outbound);
        when(communityAuth.getOptionalLoginUserId()).thenReturn(100L);

        BlogFollowRelationshipView result = service.relationship(300L);

        assertThat(result.following()).isTrue();
        assertThat(result.followedBy()).isFalse();
        assertThat(result.mutual()).isFalse();
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
            Long ownerId,
            String type,
            Long followerCount
    ) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setOwnerUserId(ownerId);
        blog.setBlogType(type);
        blog.setName("博客" + id);
        blog.setSlug("blog-" + id);
        blog.setStatus("ACTIVE");
        blog.setFollowerCount(followerCount);
        return blog;
    }

    private static CommunityFollow relation(
            Long id,
            Long followerId,
            Long targetId,
            String notificationLevel,
            boolean special
    ) {
        CommunityFollow relation = new CommunityFollow();
        relation.setId(id);
        relation.setFollowerUserId(followerId);
        relation.setTargetType("BLOG");
        relation.setTargetId(targetId);
        relation.setNotificationLevel(notificationLevel);
        relation.setSpecialFollow(special ? 1 : 0);
        return relation;
    }
}
