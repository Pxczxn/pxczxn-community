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
import top.pxczxn.community.user.model.CommunityUserPreference;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.community.user.persistence.CommunityUserPreferenceMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LikeListPrivacyServiceTest {

    private CommunityUserPreferenceMapper preferenceMapper;
    private CommunityUserMapper userMapper;
    private BlogMapper blogMapper;
    private CommunityFollowMapper followMapper;
    private CommunityAuth communityAuth;
    private LikeListPrivacyService service;

    @BeforeEach
    void setUp() {
        preferenceMapper = mock(CommunityUserPreferenceMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        blogMapper = mock(BlogMapper.class);
        followMapper = mock(CommunityFollowMapper.class);
        communityAuth = mock(CommunityAuth.class);
        service = new LikeListPrivacyService(
                preferenceMapper,
                userMapper,
                blogMapper,
                followMapper,
                communityAuth
        );
    }

    @Test
    void missingLegacyValueDefaultsToPrivate() {
        when(communityAuth.getLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L)).thenReturn(user(100L, 200L));
        when(preferenceMapper.selectOne(any()))
                .thenReturn(preference(100L, null));

        LikeListPrivacyView result = service.mine();

        assertThat(result.visibility()).isEqualTo("PRIVATE");
    }

    @Test
    void ownerCanUpdateVisibility() {
        when(communityAuth.getLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L)).thenReturn(user(100L, 200L));
        when(preferenceMapper.selectOne(any()))
                .thenReturn(preference(100L, "PRIVATE"));
        when(preferenceMapper.update(isNull(), any())).thenReturn(1);

        LikeListPrivacyView result = service.update("followers_only");

        assertThat(result.visibility()).isEqualTo("FOLLOWERS_ONLY");
        verify(preferenceMapper).update(isNull(), any());
    }

    @Test
    void publicListIsVisibleWithoutLogin() {
        when(userMapper.selectById(100L)).thenReturn(user(100L, 200L));
        when(preferenceMapper.selectOne(any()))
                .thenReturn(preference(100L, "PUBLIC"));
        when(communityAuth.getOptionalLoginUserId()).thenReturn(null);

        LikeListPrivacyView result = service.requireCanView(100L);

        assertThat(result.visibility()).isEqualTo("PUBLIC");
    }

    @Test
    void followerOnlyListUsesBlogFollowRelation() {
        CommunityUser owner = user(100L, 200L);
        when(userMapper.selectById(100L)).thenReturn(owner);
        when(userMapper.selectById(101L)).thenReturn(user(101L, 201L));
        when(preferenceMapper.selectOne(any()))
                .thenReturn(preference(100L, "FOLLOWERS_ONLY"));
        when(communityAuth.getOptionalLoginUserId()).thenReturn(101L);
        when(blogMapper.selectById(200L)).thenReturn(blog(200L, 100L));
        when(followMapper.findRelation(101L, "BLOG", 200L))
                .thenReturn(new CommunityFollow());

        LikeListPrivacyView result = service.requireCanView(100L);

        assertThat(result.visibility()).isEqualTo("FOLLOWERS_ONLY");
    }

    @Test
    void mutualListRequiresBothDirections() {
        CommunityUser owner = user(100L, 200L);
        CommunityUser viewer = user(101L, 201L);
        when(userMapper.selectById(100L)).thenReturn(owner);
        when(userMapper.selectById(101L)).thenReturn(viewer);
        when(preferenceMapper.selectOne(any()))
                .thenReturn(preference(100L, "MUTUAL_ONLY"));
        when(communityAuth.getOptionalLoginUserId()).thenReturn(101L);
        when(blogMapper.selectById(200L)).thenReturn(blog(200L, 100L));
        when(followMapper.findRelation(101L, "BLOG", 200L))
                .thenReturn(new CommunityFollow());
        when(followMapper.findRelation(100L, "BLOG", 201L))
                .thenReturn(new CommunityFollow());

        LikeListPrivacyView result = service.requireCanView(100L);

        assertThat(result.visibility()).isEqualTo("MUTUAL_ONLY");
    }

    @Test
    void privateListReturnsNotFoundToOtherUsers() {
        when(userMapper.selectById(100L)).thenReturn(user(100L, 200L));
        when(preferenceMapper.selectOne(any()))
                .thenReturn(preference(100L, "PRIVATE"));
        when(communityAuth.getOptionalLoginUserId()).thenReturn(101L);
        when(userMapper.selectById(101L)).thenReturn(user(101L, 201L));

        assertThatThrownBy(() -> service.requireCanView(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("喜欢列表不存在");
    }

    private static CommunityUser user(Long id, Long personalBlogId) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setPersonalBlogId(personalBlogId);
        user.setStatus("NORMAL");
        return user;
    }

    private static Blog blog(Long id, Long ownerId) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setOwnerUserId(ownerId);
        blog.setBlogType("PERSONAL");
        blog.setStatus("ACTIVE");
        return blog;
    }

    private static CommunityUserPreference preference(
            Long userId,
            String visibility
    ) {
        CommunityUserPreference preference = new CommunityUserPreference();
        preference.setId(userId + 1000);
        preference.setUserId(userId);
        preference.setLikesVisibility(visibility);
        return preference;
    }
}
