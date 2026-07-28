package top.pxczxn.community.appeal.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.appeal.model.CommunityAppeal;
import top.pxczxn.community.appeal.persistence.CommunityAppealEventMapper;
import top.pxczxn.community.appeal.persistence.CommunityAppealMapper;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.chat.persistence.CommunityChatMessageMapper;
import top.pxczxn.community.report.model.CommunityReport;
import top.pxczxn.community.report.persistence.CommunityReportEventMapper;
import top.pxczxn.community.report.persistence.CommunityReportMapper;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityAppealServiceTest {
    private CommunityAppealMapper appeals; private CommunityAppealEventMapper events; private CommunityReportMapper reports; private CommunityReportEventMapper reportEvents; private CommunityAppealService service;
    @BeforeEach void setUp() { appeals=mock(CommunityAppealMapper.class); events=mock(CommunityAppealEventMapper.class); reports=mock(CommunityReportMapper.class); reportEvents=mock(CommunityReportEventMapper.class); service=new CommunityAppealServiceImpl(appeals,events,reports,reportEvents,mock(ArticleMapper.class),mock(CommunityMomentMapper.class),mock(CommunityCommentMapper.class),mock(BlogMapper.class),mock(TeamMapper.class),mock(CommunityChatMessageMapper.class),new ObjectMapper()); }
    @Test void rejectsAppealFromNonOwner() { when(reports.selectById(50L)).thenReturn(report(50L,10L,"RESOLVED",2)); assertThatThrownBy(() -> service.submit(11L,50L,"not mine",null)).isInstanceOf(BusinessException.class).hasMessageContaining("owner"); }
    @Test void revokingAppealRevertsResolvedReportAndWritesBothAudits() { CommunityAppeal appeal=new CommunityAppeal(); appeal.setId(60L); appeal.setReportId(50L); appeal.setStatus("PENDING"); appeal.setLockVersion(4); when(appeals.selectById(60L)).thenReturn(appeal); when(reports.selectById(50L)).thenReturn(report(50L,10L,"RESOLVED",2)); when(reports.revokeForAppeal(eq(50L),eq(2),any(),any())).thenReturn(1); when(appeals.review(eq(60L),eq(1L),eq("REVOKED"),any(),eq(4),any())).thenReturn(1); CommunityAppealView result=service.review(1L,60L,4,true,"reversal confirmed"); assertThat(result.status()).isEqualTo("REVOKED"); verify(events).insert(any()); verify(reportEvents).insert(any()); }
    private static CommunityReport report(Long id,Long target,String status,int lock) { CommunityReport value=new CommunityReport(); value.setId(id); value.setTargetType("USER"); value.setTargetId(target); value.setStatus(status); value.setLockVersion(lock); return value; }
}
