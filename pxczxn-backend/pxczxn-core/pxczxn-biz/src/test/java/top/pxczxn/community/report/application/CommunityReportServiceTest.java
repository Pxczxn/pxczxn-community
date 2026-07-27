package top.pxczxn.community.report.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.chat.persistence.CommunityChatMessageMapper;
import top.pxczxn.community.report.model.CommunityReport;
import top.pxczxn.community.report.persistence.CommunityReportEventMapper;
import top.pxczxn.community.report.persistence.CommunityReportMapper;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommunityReportServiceTest {
    private CommunityReportMapper reports; private CommunityReportEventMapper events; private CommunityUserMapper users;
    private CommunityReportService service;

    @BeforeEach void setUp() {
        reports = mock(CommunityReportMapper.class); events = mock(CommunityReportEventMapper.class); users = mock(CommunityUserMapper.class);
        service = new CommunityReportServiceImpl(reports, events, mock(ArticleMapper.class), mock(CommunityMomentMapper.class), mock(CommunityCommentMapper.class), mock(BlogMapper.class), users, mock(TeamMapper.class), mock(CommunityChatMessageMapper.class), new ObjectMapper());
    }

    @Test void rejectsSelfUserReport() {
        assertThatThrownBy(() -> service.create(7L, new CreateCommunityReportCommand("USER", 7L, "ABUSE", null, null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yourself");
        verifyNoInteractions(reports, events);
    }

    @Test void rejectsMissingTarget() {
        assertThatThrownBy(() -> service.create(7L, new CreateCommunityReportCommand("USER", 8L, "ABUSE", null, null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("does not exist");
        verify(reports, never()).insert(any());
    }

    @Test void claimsPendingReportWithOptimisticLockAndAuditEvent() {
        CommunityReport report = new CommunityReport(); report.setId(20L); report.setStatus("PENDING"); report.setLockVersion(3);
        when(reports.selectById(20L)).thenReturn(report); when(reports.claim(eq(20L), eq(99L), eq(3), any())).thenReturn(1);
        CommunityReportView result = service.claim(99L, 20L, 3);
        assertThat(result.status()).isEqualTo("ASSIGNED"); assertThat(result.assigneeAdminId()).isEqualTo(99L); assertThat(result.lockVersion()).isEqualTo(4);
        verify(events).insert(any());
    }
}
