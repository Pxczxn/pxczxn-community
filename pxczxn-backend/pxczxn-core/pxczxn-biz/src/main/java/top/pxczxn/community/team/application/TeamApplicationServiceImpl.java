package top.pxczxn.community.team.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.team.model.TeamApplication;
import top.pxczxn.community.team.persistence.TeamApplicationMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Implementation of team application service.
 */
@Service
@RequiredArgsConstructor
public class TeamApplicationServiceImpl implements TeamApplicationService {

    private static final Set<String> ACTIVE_USER_STATUSES = Set.of("NORMAL", "LIMITED");

    private final TeamApplicationMapper teamApplicationMapper;
    private final CommunityUserMapper communityUserMapper;
    private final BlogMapper blogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitApplication(SubmitTeamApplicationCommand command) {
        // Validate input
        if (command.getApplicantUserId() == null) {
            throw new IllegalArgumentException("Applicant user ID is required");
        }
        if (command.getTeamName() == null || command.getTeamName().trim().isEmpty()) {
            throw new IllegalArgumentException("Team name is required");
        }
        if (command.getTeamSlug() == null || command.getTeamSlug().trim().isEmpty()) {
            throw new IllegalArgumentException("Team slug is required");
        }

        // Check if user exists and is active
        CommunityUser user = communityUserMapper.selectById(command.getApplicantUserId());
        if (user == null) {
            throw new BusinessException("User not found");
        }
        if (!ACTIVE_USER_STATUSES.contains(user.getStatus())) {
            throw new BusinessException("User is not active and cannot submit team application");
        }

        // Check for duplicate pending application by this user
        TeamApplication existingPending = teamApplicationMapper.findPendingByApplicant(command.getApplicantUserId());
        if (existingPending != null) {
            throw new BusinessException("User already has a pending team application");
        }

        // Check if idempotency key already exists
        if (command.getIdempotencyKey() != null && !command.getIdempotencyKey().trim().isEmpty()) {
            TeamApplication existingIdempotent = teamApplicationMapper.findByIdempotencyKey(command.getIdempotencyKey());
            if (existingIdempotent != null) {
                // Return existing application ID (idempotent)
                return existingIdempotent.getId();
            }
        }

        // Check if slug is already taken by an existing blog
        LambdaQueryWrapper<Blog> blogQuery = new LambdaQueryWrapper<>();
        blogQuery.eq(Blog::getSlug, command.getTeamSlug())
                .isNull(Blog::getDeletedAt);
        Blog existingBlog = blogMapper.selectOne(blogQuery);
        if (existingBlog != null) {
            throw new BusinessException("Team slug is already taken");
        }

        // Check if slug is in another pending/approved application
        int slugCount = teamApplicationMapper.countBySlugInPendingOrApproved(command.getTeamSlug());
        if (slugCount > 0) {
            throw new BusinessException("Team slug is already requested by another application");
        }

        // Create application
        TeamApplication application = new TeamApplication();
        application.setApplicantUserId(command.getApplicantUserId());
        application.setTeamName(command.getTeamName().trim());
        application.setTeamSlug(command.getTeamSlug().trim());
        application.setDescription(command.getDescription());
        application.setIdempotencyKey(command.getIdempotencyKey());
        application.setStatus("PENDING");
        application.setLockVersion(0);
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());

        teamApplicationMapper.insert(application);

        return application.getId();
    }

    @Override
    public TeamApplicationView getApplication(Long applicationId, Long applicantUserId) {
        if (applicationId == null || applicantUserId == null) {
            throw new IllegalArgumentException("Application ID and applicant user ID are required");
        }

        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("Application not found");
        }

        // Only applicant can view their own application
        if (!application.getApplicantUserId().equals(applicantUserId)) {
            throw new BusinessException(403, "You are not authorized to view this application");
        }

        return toView(application);
    }

    @Override
    public TeamApplicationView getMyApplication(Long applicantUserId) {
        if (applicantUserId == null) {
            throw new IllegalArgumentException("Applicant user ID is required");
        }

        TeamApplication application = teamApplicationMapper.findPendingByApplicant(applicantUserId);
        return application != null ? toView(application) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelApplication(Long applicationId, Long applicantUserId) {
        if (applicationId == null || applicantUserId == null) {
            throw new IllegalArgumentException("Application ID and applicant user ID are required");
        }

        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("Application not found");
        }

        // Only applicant can cancel their own application
        if (!application.getApplicantUserId().equals(applicantUserId)) {
            throw new BusinessException(403, "You are not authorized to cancel this application");
        }

        // Can only cancel PENDING applications
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("Only pending applications can be cancelled");
        }

        // Update status to CANCELLED with optimistic lock
        int updated = teamApplicationMapper.cancelWithOptimisticLock(applicationId, application.getLockVersion());
        if (updated != 1) {
            throw new BusinessException("Failed to cancel application due to concurrent modification");
        }
    }

    private TeamApplicationView toView(TeamApplication application) {
        TeamApplicationView view = new TeamApplicationView();
        view.setId(application.getId());
        view.setApplicantUserId(application.getApplicantUserId());
        view.setTeamName(application.getTeamName());
        view.setTeamSlug(application.getTeamSlug());
        view.setDescription(application.getDescription());
        view.setStatus(application.getStatus());
        view.setReviewerUserId(application.getReviewerUserId());
        view.setReviewComment(application.getReviewComment());
        view.setReviewedAt(application.getReviewedAt());
        view.setCreatedAt(application.getCreatedAt());
        view.setUpdatedAt(application.getUpdatedAt());
        return view;
    }
}
