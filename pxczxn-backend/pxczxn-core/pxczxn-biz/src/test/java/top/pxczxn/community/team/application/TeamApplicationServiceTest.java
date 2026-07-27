package top.pxczxn.community.team.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.team.model.TeamApplication;
import top.pxczxn.community.team.persistence.TeamApplicationMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeamApplicationServiceTest {

    private TeamApplicationMapper teamApplicationMapper;
    private CommunityUserMapper communityUserMapper;
    private BlogMapper blogMapper;
    private TeamApplicationService service;

    @BeforeEach
    void setUp() {
        teamApplicationMapper = mock(TeamApplicationMapper.class);
        communityUserMapper = mock(CommunityUserMapper.class);
        blogMapper = mock(BlogMapper.class);
        service = new TeamApplicationServiceImpl(
                teamApplicationMapper,
                communityUserMapper,
                blogMapper
        );
    }

    @Test
    void submitApplication_shouldCreateApplication_whenValidRequest() {
        // Given
        SubmitTeamApplicationCommand command = new SubmitTeamApplicationCommand();
        command.setApplicantUserId(1L);
        command.setTeamName("Test Team");
        command.setTeamSlug("test-team");
        command.setDescription("Test description");
        command.setIdempotencyKey("test-key");

        CommunityUser user = new CommunityUser();
        user.setId(1L);
        user.setStatus("NORMAL");

        when(communityUserMapper.selectById(1L)).thenReturn(user);
        when(teamApplicationMapper.findPendingByApplicant(1L)).thenReturn(null);
        when(teamApplicationMapper.findByIdempotencyKey("test-key")).thenReturn(null);
        when(blogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(teamApplicationMapper.countBySlugInPendingOrApproved("test-team")).thenReturn(0);
        when(teamApplicationMapper.insert(any(TeamApplication.class))).thenAnswer(invocation -> {
            TeamApplication app = invocation.getArgument(0);
            app.setId(100L);
            return 1;
        });

        // When
        Long applicationId = service.submitApplication(command);

        // Then
        assertThat(applicationId).isEqualTo(100L);
        verify(teamApplicationMapper).insert(any(TeamApplication.class));
    }

    @Test
    void submitApplication_shouldThrowException_whenUserNotActive() {
        // Given
        SubmitTeamApplicationCommand command = new SubmitTeamApplicationCommand();
        command.setApplicantUserId(1L);
        command.setTeamName("Test Team");
        command.setTeamSlug("test-team");

        CommunityUser user = new CommunityUser();
        user.setId(1L);
        user.setStatus("BANNED");

        when(communityUserMapper.selectById(1L)).thenReturn(user);

        // When & Then
        assertThatThrownBy(() -> service.submitApplication(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");

        verify(teamApplicationMapper, never()).insert(any(TeamApplication.class));
    }

    @Test
    void submitApplication_shouldThrowException_whenUserHasPendingApplication() {
        // Given
        SubmitTeamApplicationCommand command = new SubmitTeamApplicationCommand();
        command.setApplicantUserId(1L);
        command.setTeamName("Test Team");
        command.setTeamSlug("test-team");

        CommunityUser user = new CommunityUser();
        user.setId(1L);
        user.setStatus("NORMAL");

        TeamApplication existingApp = new TeamApplication();
        existingApp.setId(99L);
        existingApp.setStatus("PENDING");

        when(communityUserMapper.selectById(1L)).thenReturn(user);
        when(teamApplicationMapper.findPendingByApplicant(1L)).thenReturn(existingApp);

        // When & Then
        assertThatThrownBy(() -> service.submitApplication(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending");

        verify(teamApplicationMapper, never()).insert(any(TeamApplication.class));
    }

    @Test
    void submitApplication_shouldReturnExistingId_whenIdempotencyKeyExists() {
        // Given
        SubmitTeamApplicationCommand command = new SubmitTeamApplicationCommand();
        command.setApplicantUserId(1L);
        command.setTeamName("Test Team");
        command.setTeamSlug("test-team");
        command.setIdempotencyKey("existing-key");

        CommunityUser user = new CommunityUser();
        user.setId(1L);
        user.setStatus("NORMAL");

        TeamApplication existingApp = new TeamApplication();
        existingApp.setId(99L);
        existingApp.setIdempotencyKey("existing-key");

        when(communityUserMapper.selectById(1L)).thenReturn(user);
        when(teamApplicationMapper.findPendingByApplicant(1L)).thenReturn(existingApp);
        when(teamApplicationMapper.findByIdempotencyKey("existing-key")).thenReturn(existingApp);

        // When
        Long applicationId = service.submitApplication(command);

        // Then
        assertThat(applicationId).isEqualTo(99L);
        verify(teamApplicationMapper, never()).insert(any(TeamApplication.class));
    }

    @Test
    void submitApplication_shouldThrowException_whenSlugAlreadyTaken() {
        // Given
        SubmitTeamApplicationCommand command = new SubmitTeamApplicationCommand();
        command.setApplicantUserId(1L);
        command.setTeamName("Test Team");
        command.setTeamSlug("test-team");

        CommunityUser user = new CommunityUser();
        user.setId(1L);
        user.setStatus("NORMAL");

        Blog existingBlog = new Blog();
        existingBlog.setId(1L);
        existingBlog.setSlug("test-team");

        when(communityUserMapper.selectById(1L)).thenReturn(user);
        when(teamApplicationMapper.findPendingByApplicant(1L)).thenReturn(null);
        when(teamApplicationMapper.findByIdempotencyKey(null)).thenReturn(null);
        when(blogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingBlog);

        // When & Then
        assertThatThrownBy(() -> service.submitApplication(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("slug");

        verify(teamApplicationMapper, never()).insert(any(TeamApplication.class));
    }

    @Test
    void cancelApplication_shouldUpdateStatus_whenPendingApplication() {
        // Given
        Long applicationId = 1L;
        Long applicantUserId = 100L;

        TeamApplication application = new TeamApplication();
        application.setId(applicationId);
        application.setApplicantUserId(applicantUserId);
        application.setStatus("PENDING");
        application.setLockVersion(0);

        when(teamApplicationMapper.selectById(applicationId)).thenReturn(application);
        when(teamApplicationMapper.cancelWithOptimisticLock(applicationId, 0)).thenReturn(1);

        // When
        service.cancelApplication(applicationId, applicantUserId);

        // Then
        verify(teamApplicationMapper).cancelWithOptimisticLock(applicationId, 0);
    }

    @Test
    void cancelApplication_shouldThrowException_whenNotApplicant() {
        // Given
        Long applicationId = 1L;
        Long applicantUserId = 100L;
        Long otherUserId = 200L;

        TeamApplication application = new TeamApplication();
        application.setId(applicationId);
        application.setApplicantUserId(applicantUserId);
        application.setStatus("PENDING");

        when(teamApplicationMapper.selectById(applicationId)).thenReturn(application);

        // When & Then
        assertThatThrownBy(() -> service.cancelApplication(applicationId, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not authorized");

        verify(teamApplicationMapper, never()).cancelWithOptimisticLock(any(), any());
    }

    @Test
    void cancelApplication_shouldThrowException_whenNotPending() {
        // Given
        Long applicationId = 1L;
        Long applicantUserId = 100L;

        TeamApplication application = new TeamApplication();
        application.setId(applicationId);
        application.setApplicantUserId(applicantUserId);
        application.setStatus("APPROVED");

        when(teamApplicationMapper.selectById(applicationId)).thenReturn(application);

        // When & Then
        assertThatThrownBy(() -> service.cancelApplication(applicationId, applicantUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending");

        verify(teamApplicationMapper, never()).cancelWithOptimisticLock(any(), any());
    }
}
