package top.pxczxn.community.taxonomy.application;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.taxonomy.model.PlatformTag;
import top.pxczxn.community.taxonomy.persistence.ArticleTagMapper;
import top.pxczxn.community.taxonomy.persistence.PlatformTagMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformTagServiceTest {

    private PlatformTagMapper tagMapper;
    private ArticleTagMapper articleTagMapper;
    private PlatformTagService service;

    @BeforeEach
    void setUp() {
        tagMapper = mock(PlatformTagMapper.class);
        articleTagMapper = mock(ArticleTagMapper.class);
        service = new PlatformTagService(tagMapper, articleTagMapper);
    }

    @Test
    void articleCannotHaveMoreThanFiveTags() {
        assertThatThrownBy(() -> service.replaceArticleTags(
                100L,
                List.of(1L, 2L, 3L, 4L, 5L, 6L)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("每篇文章最多选择 5 个标签");

        verify(articleTagMapper, never()).delete(any());
    }

    @Test
    void unavailableTagCannotBeAssigned() {
        when(articleTagMapper.selectList(any())).thenReturn(List.of());
        when(tagMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.replaceArticleTags(
                100L,
                List.of(1L, 2L)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可用");
    }

    @Test
    void usedTagCannotBeDeleted() {
        PlatformTag tag = new PlatformTag();
        tag.setId(1L);
        tag.setStatus("ACTIVE");
        when(tagMapper.selectById(1L)).thenReturn(tag);
        when(articleTagMapper.selectCount(any())).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仍被文章使用");

        verify(tagMapper, never()).update(any(), any());
    }

    @Test
    void defaultAdminListExcludesSoftDeletedTags() {
        when(tagMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.listAdmin(null, null);

        verify(tagMapper).selectList(argThat(query ->
                !((AbstractWrapper<?, ?, ?>) query)
                        .getExpression()
                        .getNormal()
                        .isEmpty()
        ));
    }
}
