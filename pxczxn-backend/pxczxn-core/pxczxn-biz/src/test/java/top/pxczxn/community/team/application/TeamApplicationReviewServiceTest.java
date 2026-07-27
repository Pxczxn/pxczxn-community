package top.pxczxn.community.team.application;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogCategory;
import top.pxczxn.community.blog.model.BlogSetting;
import top.pxczxn.community.blog.persistence.BlogCategoryMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.blog.persistence.BlogSettingMapper;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamApplication;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.persistence.TeamApplicationMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeamApplicationReviewServiceTest {

    private TeamApplicationMapper teamApplicationMapper;
    private BlogMapper blogMapper;
    private BlogSettingMapper blogSettingMapper;
    private BlogCategoryMapper blogCategoryMapper;
    private TeamMapper teamMapper;
    private TeamMemberMapper teamMemberMapper;
    private TeamAuthorityService teamAuthorityService;
    private ApplicationEventPublisher eventPublisher;
    private TeamApplicationReviewService service;

    @BeforeEach
    void setUp() {
        teamApplicationMapper = mock(TeamApplicationMapper.class);
        blogMapper = mock(BlogMapper.class);
        blogSettingMapper = mock(BlogSettingMapper.class);
        blogCategoryMapper = mock(BlogCategoryMapper.class);
        teamMapper = mock(TeamMapper.class);
        teamMemberMapper = mock(TeamMemberMapper.class);
        teamAuthorityService = mock(TeamAuthorityService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new TeamApplicationReviewServiceImpl(
                teamApplicationMapper,
                blogMapper,
                blogSettingMapper,
                blogCategoryMapper,
                teamMapper,
                teamMemberMapper,
                teamAuthorityService,
                eventPublisher
        );
    }

    @Test
    void approveApplication_shouldCreateTeamAndRelatedEntities_whenPendingApplication() {
        // Given
        Long applicationId = 1L;
        Long reviewerUserId = 100L;
        String reviewComment = "Approved";
        String requestId = "request-123";

        TeamApplication application = new TeamApplication();
        application.setId(applicationId);
        application.setApplicantUserId(200L);
        application.setTeamName("Test Team");
        application.setTeamSlug("test-team");
        application.setDescription("Test description");
        application.setStatus("PENDING");
        application.setLockVersion(0);

        when(teamApplicationMapper.selectById(applicationId)).thenReturn(application);
        when(teamApplicationMapper.approveWithOptimisticLock(applicationId, reviewerUserId, reviewComment, 0)).thenReturn(1);
        when(blogMapper.insert(any(Blog.class))).thenReturn(1);
        when(teamMapper.insert(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(1000L);
            return 1;
        });
        when(teamMemberMapper.insert(any(TeamMember.class))).thenReturn(1);
        when(blogSettingMapper.insert(any(BlogSetting.class))).thenReturn(1);
        when(blogCategoryMapper.insert(any(BlogCategory.class))).thenReturn(1);

        // When
        Long teamId = service.approveApplication(applicationId, reviewerUserId, reviewComment, requestId);

        // Then
        assertThat(teamId).isNotNull();
        verify(teamApplicationMapper).approveWithOptimisticLock(applicationId, reviewerUserId, reviewComment, 0);
        verify(blogMapper).insert(any(Blog.class));
        verify(teamMapper).insert(any(Team.class));
        verify(teamMemberMapper).insert(any(TeamMember.class));
        verify(blogSettingMapper).insert(any(BlogSetting.class));
        verify(blogCategoryMapper).insert(any(BlogCategory.class));
        verify(teamAuthorityService).recordAuditEvent(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void approveApplication_shouldThrowException_whenNotPending() {
        // Given
        Long applicationId = 1L;
        Long reviewerUserId = 100L;

        TeamApplication application = new TeamApplication();
        application.setId(applicationId);
        application.setStatus("APPROVED");

        when(teamApplicationMapper.selectById(applicationId)).thenReturn(application);

        // When & Then
        assertThatThrownBy(() -> service.approveApplication(applicationId, reviewerUserId, "comment", "req-123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not in PENDING");

        verify(blogMapper, never()).insert(any(Blog.class));
        verify(teamMapper, never()).insert(any(Team.class));
    }

    @Test
    void approveApplication_shouldThrowException_whenConcurrentApproval() {
        // Given
        Long applicationId = 1L;
        Long reviewerUserId = 100L;

        TeamApplication application = new TeamApplication();
        application.setId(applicationId);
        application.setApplicantUserId(200L);
        application.setStatus("PENDING");
        application.setLockVersion(0);

        when(teamApplicationMapper.selectById(applicationId)).thenReturn(application);
        when(teamApplicationMapper.approveWithOptimisticLock(applicationId, reviewerUserId, "comment", 0)).thenReturn(0);

        // When & Then
        assertThatThrownBy(() -> service.approveApplication(applicationId, reviewerUserId, "comment", "req-123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("concurrent modification");

        verify(blogMapper, never()).insert(any(Blog.class));
        verify(teamMapper, never()).insert(any(Team.class));
    }

    @Test
    void rejectApplication_shouldUpdateStatus_whenPendingApplication() {
        // Given
        Long applicationId = 1L;
        Long reviewerUserId = 100L;
        String reviewComment = "Rejected for reasons";
        String requestId = "request-123";

        TeamApplication application = new TeamApplication();
        application.setId(applicationId);
        application.setApplicantUserId(200L);
        application.setStatus("PENDING");
        application.setLockVersion(0);

        when(teamApplicationMapper.selectById(applicationId)).thenReturn(application);
        when(teamApplicationMapper.rejectWithOptimisticLock(applicationId, reviewerUserId, reviewComment, 0)).thenReturn(1);

        // When
        service.rejectApplication(applicationId, reviewerUserId, reviewComment, requestId);

        // Then
        verify(teamApplicationMapper).rejectWithOptimisticLock(applicationId, reviewerUserId, reviewComment, 0);
        verify(blogMapper, never()).insert(any(Blog.class));
        verify(teamMapper, never()).insert(any(Team.class));
    }

    @Test
    void rejectApplication_shouldThrowException_whenReviewCommentIsEmpty() {
        // Given
        Long applicationId = 1L;
        Long reviewerUserId = 100L;

        // When & Then
        assertThatThrownBy(() -> service.rejectApplication(applicationId, reviewerUserId, "", "req-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");

        verify(teamApplicationMapper, never()).update(isNull(), any());
    }

    @Test
    void rejectApplication_shouldThrowException_whenNotPending() {
        // Given
        Long applicationId = 1L;
        Long reviewerUserId = 100L;

        TeamApplication application = new TeamApplication();
        application.setId(applicationId);
        application.setStatus("REJECTED");

        when(teamApplicationMapper.selectById(applicationId)).thenReturn(application);

        // When & Then
        assertThatThrownBy(() -> service.rejectApplication(applicationId, reviewerUserId, "comment", "req-123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not in PENDING");

        verify(teamApplicationMapper, never()).update(isNull(), any());
    }

    @Test
    void rejectApplication_shouldThrowException_whenConcurrentRejection() {
        // Given
        Long applicationId = 1L;
        Long reviewerUserId = 100L;

        TeamApplication application = new TeamApplication();
        application.setId(applicationId);
        application.setStatus("PENDING");
        application.setLockVersion(0);

        when(teamApplicationMapper.selectById(applicationId)).thenReturn(application);
        when(teamApplicationMapper.rejectWithOptimisticLock(applicationId, reviewerUserId, "comment", 0)).thenReturn(0);

        // When & Then
        assertThatThrownBy(() -> service.rejectApplication(applicationId, reviewerUserId, "comment", "req-123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("concurrent modification");
    }
}
