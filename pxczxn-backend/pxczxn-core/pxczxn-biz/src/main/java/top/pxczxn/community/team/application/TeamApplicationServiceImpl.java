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
            throw new IllegalArgumentException("申请用户不能为空");
        }
        if (command.getTeamName() == null || command.getTeamName().trim().isEmpty()) {
            throw new IllegalArgumentException("团队名称不能为空");
        }
        if (command.getTeamSlug() == null || command.getTeamSlug().trim().isEmpty()) {
            throw new IllegalArgumentException("团队地址不能为空");
        }

        // Check if user exists and is active
        CommunityUser user = communityUserMapper.selectById(command.getApplicantUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!ACTIVE_USER_STATUSES.contains(user.getStatus())) {
            throw new BusinessException("账号状态异常，暂无法提交团队申请");
        }

        // Check if idempotency key already exists
        if (command.getIdempotencyKey() != null && !command.getIdempotencyKey().trim().isEmpty()) {
            TeamApplication existingIdempotent = teamApplicationMapper.findByIdempotencyKey(command.getIdempotencyKey());
            if (existingIdempotent != null) {
                // Return existing application ID (idempotent)
                return existingIdempotent.getId();
            }
        }

        // Reject a distinct request while preserving a same-key replay above.
        TeamApplication existingPending = teamApplicationMapper.findPendingByApplicant(command.getApplicantUserId());
        if (existingPending != null) {
            throw new BusinessException("您已有待审核的团队申请");
        }

        // Check if slug is already taken by an existing blog
        LambdaQueryWrapper<Blog> blogQuery = new LambdaQueryWrapper<>();
        blogQuery.eq(Blog::getSlug, command.getTeamSlug())
                .isNull(Blog::getDeletedAt);
        Blog existingBlog = blogMapper.selectOne(blogQuery);
        if (existingBlog != null) {
            throw new BusinessException("该团队地址已被占用");
        }

        // Check if slug is in another pending/approved application
        int slugCount = teamApplicationMapper.countBySlugInPendingOrApproved(command.getTeamSlug());
        if (slugCount > 0) {
            throw new BusinessException("该团队地址已被其他申请使用");
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
            throw new IllegalArgumentException("申请编号不能为空");
        }

        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("申请不存在");
        }

        // Only applicant can view their own application
        if (!application.getApplicantUserId().equals(applicantUserId)) {
            throw new BusinessException(403, "无权查看该申请");
        }

        return toView(application);
    }

    @Override
    public TeamApplicationView getMyApplication(Long applicantUserId) {
        if (applicantUserId == null) {
            throw new IllegalArgumentException("申请用户不能为空");
        }

        TeamApplication application = teamApplicationMapper.findPendingByApplicant(applicantUserId);
        return application != null ? toView(application) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelApplication(Long applicationId, Long applicantUserId) {
        if (applicationId == null || applicantUserId == null) {
            throw new IllegalArgumentException("申请编号不能为空");
        }

        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("申请不存在");
        }

        // Only applicant can cancel their own application
        if (!application.getApplicantUserId().equals(applicantUserId)) {
            throw new BusinessException(403, "无权取消该申请");
        }

        // Can only cancel PENDING applications
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException("仅待审核状态的申请可以取消");
        }

        // Update status to CANCELLED with optimistic lock
        int updated = teamApplicationMapper.cancelWithOptimisticLock(applicationId, application.getLockVersion());
        if (updated != 1) {
            throw new BusinessException("取消失败，请刷新后重试");
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
