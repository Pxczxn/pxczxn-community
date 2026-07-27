package top.pxczxn.community.team.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.model.BlogCategory;
import top.pxczxn.community.blog.model.BlogSetting;
import top.pxczxn.community.blog.persistence.BlogCategoryMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.blog.persistence.BlogSettingMapper;
import top.pxczxn.community.notification.application.TeamApplicationReviewDecisionEvent;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamApplication;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.persistence.TeamApplicationMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of team application review service.
 */
@Service
@RequiredArgsConstructor
public class TeamApplicationReviewServiceImpl implements TeamApplicationReviewService {

    private final TeamApplicationMapper teamApplicationMapper;
    private final BlogMapper blogMapper;
    private final BlogSettingMapper blogSettingMapper;
    private final BlogCategoryMapper blogCategoryMapper;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamAuthorityService teamAuthorityService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<TeamApplicationView> listPendingApplications() {
        List<TeamApplication> applications = teamApplicationMapper.listPending();
        return applications.stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public TeamApplicationView getApplicationForReview(Long applicationId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID is required");
        }

        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("Application not found");
        }

        return toView(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long approveApplication(Long applicationId, Long reviewerUserId, String reviewComment, String requestId) {
        if (applicationId == null || reviewerUserId == null) {
            throw new IllegalArgumentException("Application ID and reviewer user ID are required");
        }

        // Load application
        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("Application not found");
        }

        // Must be in PENDING status
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("Application is not in PENDING status");
        }

        // Update application status to APPROVED with conditional update (only one reviewer succeeds)
        int updated = teamApplicationMapper.approveWithOptimisticLock(
                applicationId,
                reviewerUserId,
                reviewComment,
                application.getLockVersion()
        );
        if (updated != 1) {
            throw new BusinessException("Failed to approve application due to concurrent modification or status change");
        }

        // Atomically create team blog, team, owner member, default settings/category, and audit event

        // 1. Create TEAM type blog
        Long blogId = IdWorker.getId();
        Blog blog = new Blog();
        blog.setId(blogId);
        blog.setBlogType("TEAM");
        blog.setOwnerUserId(application.getApplicantUserId());
        blog.setName(application.getTeamName());
        blog.setSlug(application.getTeamSlug());
        blog.setSummary(application.getDescription());
        blog.setStatus("ACTIVE");
        blog.setArticleCount(0L);
        blog.setFollowerCount(0L);
        blog.setLockVersion(0);
        blog.setCreatedAt(LocalDateTime.now());
        blog.setUpdatedAt(LocalDateTime.now());
        if (blogMapper.insert(blog) != 1) {
            throw new BusinessException("Failed to create team blog");
        }

        // 2. Create team record
        Team team = new Team();
        team.setBlogId(blogId);
        team.setOwnerUserId(application.getApplicantUserId());
        team.setStatus("ACTIVE");
        team.setLockVersion(0);
        team.setCreatedAt(LocalDateTime.now());
        team.setUpdatedAt(LocalDateTime.now());
        if (teamMapper.insert(team) != 1) {
            throw new BusinessException("Failed to create team record");
        }

        Long teamId = team.getId();

        // 3. Create OWNER member
        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeamId(teamId);
        ownerMember.setUserId(application.getApplicantUserId());
        ownerMember.setRoleCode("OWNER");
        ownerMember.setJoinedAt(LocalDateTime.now());
        ownerMember.setLockVersion(0);
        if (teamMemberMapper.insert(ownerMember) != 1) {
            throw new BusinessException("Failed to create team owner member");
        }

        // 4. Create default blog settings
        BlogSetting setting = new BlogSetting();
        setting.setId(IdWorker.getId());
        setting.setBlogId(blogId);
        setting.setCommentScope("ALL_LOGGED_IN");
        setting.setDefaultVisibility("PUBLIC");
        setting.setAllowRepost("ALLOW");
        setting.setThemeKey("light");
        if (blogSettingMapper.insert(setting) != 1) {
            throw new BusinessException("Failed to create blog settings");
        }

        // 5. Create default category
        BlogCategory category = new BlogCategory();
        category.setId(IdWorker.getId());
        category.setBlogId(blogId);
        category.setName("默认分类");
        category.setSlug("default");
        category.setDescription("团队默认文章分类");
        category.setSortOrder(1);
        category.setIsDefault(1);
        category.setArticleCount(0L);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        if (blogCategoryMapper.insert(category) != 1) {
            throw new BusinessException("Failed to create default blog category");
        }

        // 6. Record audit event
        teamAuthorityService.recordAuditEvent(
                teamId,
                null, // System action (platform approval)
                "TEAM_CREATED",
                "TEAM_APPLICATION",
                applicationId,
                requestId,
                "{\"applicationId\":" + applicationId + ",\"applicantUserId\":" + application.getApplicantUserId() + "}",
                "{\"teamId\":" + teamId + ",\"blogId\":" + blogId + ",\"reviewerUserId\":" + reviewerUserId + "}"
        );

        // 7. Publish notification event (after commit)
        eventPublisher.publishEvent(new TeamApplicationReviewDecisionEvent(
                applicationId,
                application.getApplicantUserId(),
                application.getTeamName(),
                "APPROVED",
                reviewComment,
                teamId
        ));

        return teamId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectApplication(Long applicationId, Long reviewerUserId, String reviewComment, String requestId) {
        if (applicationId == null || reviewerUserId == null) {
            throw new IllegalArgumentException("Application ID and reviewer user ID are required");
        }
        if (reviewComment == null || reviewComment.trim().isEmpty()) {
            throw new IllegalArgumentException("Review comment is required for rejection");
        }

        // Load application
        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("Application not found");
        }

        // Must be in PENDING status
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("Application is not in PENDING status");
        }

        // Update application status to REJECTED with conditional update (only one reviewer succeeds)
        int updated = teamApplicationMapper.rejectWithOptimisticLock(
                applicationId,
                reviewerUserId,
                reviewComment.trim(),
                application.getLockVersion()
        );
        if (updated != 1) {
            throw new BusinessException("Failed to reject application due to concurrent modification or status change");
        }

        // Publish notification event (after commit)
        eventPublisher.publishEvent(new TeamApplicationReviewDecisionEvent(
                applicationId,
                application.getApplicantUserId(),
                application.getTeamName(),
                "REJECTED",
                reviewComment,
                null
        ));
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
