package top.pxczxn.community.search.application;

import org.junit.jupiter.api.Test;
import top.pxczxn.community.search.persistence.UnifiedSearchMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnifiedSearchServiceTest {

    @Test
    void highlightsOnlyEscapedSourceText() {
        assertThat(UnifiedSearchService.highlight("<script>alpha</script>", "alpha"))
                .isEqualTo("&lt;script&gt;<mark>alpha</mark>&lt;/script&gt;");
    }

    @Test
    void escapesLikeWildcardsBeforeCallingMysql() {
        UnifiedSearchMapper mapper = mock(UnifiedSearchMapper.class);
        when(mapper.count("%100\\%\\_%", "TAG")).thenReturn(0L);
        when(mapper.selectPage(eq("%100\\%\\_%"), eq("TAG"), anyLong(), anyInt())).thenReturn(java.util.List.of());

        new UnifiedSearchService(mapper).search(new UnifiedSearchQuery("100%_", "tag", 1, 20));

        verify(mapper).count("%100\\%\\_%", "TAG");
        verify(mapper).selectPage("%100\\%\\_%", "TAG", 0L, 20);
    }

    @Test
    void rejectsUnknownSearchType() {
        UnifiedSearchService service = new UnifiedSearchService(mock(UnifiedSearchMapper.class));

        assertThatThrownBy(() -> service.search(new UnifiedSearchQuery("java", "internal", 1, 20)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("搜索类型");
    }
}
