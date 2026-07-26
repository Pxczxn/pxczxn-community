package top.pxczxn.community.social.application;

import org.junit.jupiter.api.Test;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommunityFollowBlogFollowerResolverTest {

    @Test
    void resolvesExistingBlogFollow() {
        CommunityFollowMapper mapper = mock(CommunityFollowMapper.class);
        CommunityFollow relation = new CommunityFollow();
        when(mapper.findRelation(10L, "BLOG", 20L)).thenReturn(relation);

        CommunityFollowBlogFollowerResolver resolver =
                new CommunityFollowBlogFollowerResolver(mapper);

        assertThat(resolver.isFollower(10L, 20L)).isTrue();
        assertThat(resolver.isFollower(10L, 21L)).isFalse();
    }

    @Test
    void nullIdentifiersAreNeverFollowers() {
        CommunityFollowMapper mapper = mock(CommunityFollowMapper.class);
        CommunityFollowBlogFollowerResolver resolver =
                new CommunityFollowBlogFollowerResolver(mapper);

        assertThat(resolver.isFollower(null, 20L)).isFalse();
        assertThat(resolver.isFollower(10L, null)).isFalse();
        verifyNoInteractions(mapper);
    }
}
