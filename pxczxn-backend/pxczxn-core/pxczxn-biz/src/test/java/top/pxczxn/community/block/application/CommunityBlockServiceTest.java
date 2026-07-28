package top.pxczxn.community.block.application;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.block.model.CommunityBlock;
import top.pxczxn.community.block.persistence.CommunityBlockMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.taxonomy.model.ArticleTag;
import top.pxczxn.community.taxonomy.persistence.ArticleTagMapper;
import top.pxczxn.community.taxonomy.persistence.PlatformTagMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityBlockServiceTest {

    private CommunityBlockMapper blockMapper;
    private CommunityUserMapper userMapper;
    private ArticleTagMapper articleTagMapper;
    private CommunityBlockService service;

    @BeforeEach
    void setUp() {
        blockMapper = mock(CommunityBlockMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        BlogMapper blogMapper = mock(BlogMapper.class);
        PlatformTagMapper tagMapper = mock(PlatformTagMapper.class);
        articleTagMapper = mock(ArticleTagMapper.class);
        when(userMapper.selectById(any())).thenReturn(new CommunityUser());
        service = new CommunityBlockServiceImpl(
                blockMapper, userMapper, blogMapper, tagMapper, articleTagMapper
        );
    }

    @Test
    void createsUserBlockAndRejectsSelfBlock() {
        when(blockMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(blockMapper.insert(any(CommunityBlock.class))).thenReturn(1);

        CommunityBlockView created = service.block(10L, "user", 20L);

        assertThat(created.targetType()).isEqualTo("USER");
        assertThat(created.targetId()).isEqualTo(20L);
        assertThatThrownBy(() -> service.block(10L, "CHAT", 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("yourself");
        verify(blockMapper, never()).delete(any());
    }

    @Test
    void tagBlockHidesTaggedArticle() {
        when(blockMapper.selectList(any())).thenReturn(List.of(block(10L, "TAG", 77L)));
        ArticleTag tag = new ArticleTag();
        tag.setArticleId(200L);
        tag.setTagId(77L);
        when(articleTagMapper.selectList(any())).thenReturn(List.of(tag));

        assertThat(service.isContentBlocked(10L, 20L, 30L, "ARTICLE", 200L)).isTrue();
    }

    @Test
    void chatBlockRestrictsBothConversationDirections() {
        when(blockMapper.selectList(any())).thenReturn(List.of(block(10L, "CHAT", 20L)));

        assertThat(service.isChatRestricted(10L, 20L)).isTrue();
        assertThat(service.isChatRestricted(20L, 10L)).isTrue();
    }

    private static CommunityBlock block(Long blocker, String type, Long target) {
        CommunityBlock value = new CommunityBlock();
        value.setBlockerUserId(blocker);
        value.setTargetType(type);
        value.setTargetId(target);
        return value;
    }
}
