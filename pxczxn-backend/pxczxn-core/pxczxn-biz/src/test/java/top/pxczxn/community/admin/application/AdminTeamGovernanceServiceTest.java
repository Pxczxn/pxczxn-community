package top.pxczxn.community.admin.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.collaboration.model.ArticleCollaborator;
import top.pxczxn.community.collaboration.persistence.ArticleCollaboratorMapper;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminTeamGovernanceServiceTest {

    private ArticleCollaboratorMapper collaboratorMapper;
    private ArticleMapper articleMapper;
    private CommunityUserMapper userMapper;
    private AdminTeamGovernanceService service;

    @BeforeEach
    void setUp() {
        collaboratorMapper = mock(ArticleCollaboratorMapper.class);
        articleMapper = mock(ArticleMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        service = new AdminTeamGovernanceService(
                mock(TeamMapper.class),
                mock(TeamMemberMapper.class),
                mock(TeamAuditEventMapper.class),
                mock(BlogMapper.class),
                userMapper,
                collaboratorMapper,
                articleMapper
        );
    }

    @Test
    void collaborationsWithEmptyPageDoesNotBuildEmptyInClause() {
        Page<ArticleCollaborator> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(java.util.List.of());
        emptyPage.setTotal(0);
        when(collaboratorMapper.selectPage(any(), any())).thenReturn(emptyPage);

        AdminCommunityPageView<AdminArticleCollaborationView> result =
                service.collaborations(null, null, 1, 20);

        assertThat(result.list()).isEmpty();
        assertThat(result.total()).isZero();
        // 空集合时跳过批量查询，避免生成 "WHERE id IN ()" 导致 SQL 语法错误
        verify(articleMapper, never()).selectBatchIds(any());
        verify(userMapper, never()).selectBatchIds(any());
    }

    @Test
    void collaborationsWithRecordsQueriesRelatedData() {
        ArticleCollaborator collaborator = new ArticleCollaborator();
        collaborator.setArticleId(10L);
        collaborator.setUserId(20L);
        Page<ArticleCollaborator> page = new Page<>(1, 20);
        page.setRecords(java.util.List.of(collaborator));
        page.setTotal(1);
        when(collaboratorMapper.selectPage(any(), any())).thenReturn(page);
        when(articleMapper.selectBatchIds(java.util.List.of(10L))).thenReturn(java.util.List.of());
        when(userMapper.selectBatchIds(java.util.List.of(20L))).thenReturn(java.util.List.of());

        AdminCommunityPageView<AdminArticleCollaborationView> result =
                service.collaborations(null, null, 1, 20);

        assertThat(result.list()).hasSize(1);
        verify(articleMapper).selectBatchIds(java.util.List.of(10L));
        verify(userMapper).selectBatchIds(java.util.List.of(20L));
    }
}
