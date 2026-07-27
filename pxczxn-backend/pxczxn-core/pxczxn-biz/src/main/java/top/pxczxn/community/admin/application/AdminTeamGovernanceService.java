package top.pxczxn.community.admin.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.collaboration.model.ArticleCollaborator;
import top.pxczxn.community.collaboration.persistence.ArticleCollaboratorMapper;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamAuditEvent;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTeamGovernanceService {
    private final TeamMapper teamMapper;
    private final TeamMemberMapper memberMapper;
    private final TeamAuditEventMapper auditMapper;
    private final BlogMapper blogMapper;
    private final CommunityUserMapper userMapper;
    private final ArticleCollaboratorMapper collaboratorMapper;
    private final ArticleMapper articleMapper;

    @Transactional(readOnly = true)
    public AdminCommunityPageView<AdminTeamView> teams(String status, Integer pageNum, Integer pageSize) {
        Page<Team> page = teamMapper.selectPage(new Page<>(positive(pageNum), boundedSize(pageSize)),
                Wrappers.<Team>lambdaQuery().eq(StringUtils.hasText(status), Team::getStatus, status)
                        .isNull(Team::getDeletedAt).orderByDesc(Team::getCreatedAt));
        return new AdminCommunityPageView<>(page.getRecords().stream().map(team -> view(team, false)).toList(),
                page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Transactional(readOnly = true)
    public AdminTeamView team(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null || team.getDeletedAt() != null) throw new BusinessException(404, "团队不存在");
        return view(team, true);
    }

    @Transactional(readOnly = true)
    public AdminCommunityPageView<AdminArticleCollaborationView> collaborations(Long articleId, Long userId, Integer pageNum, Integer pageSize) {
        Page<ArticleCollaborator> page = collaboratorMapper.selectPage(new Page<>(positive(pageNum), boundedSize(pageSize)),
                Wrappers.<ArticleCollaborator>lambdaQuery().eq(articleId != null, ArticleCollaborator::getArticleId, articleId)
                        .eq(userId != null, ArticleCollaborator::getUserId, userId).orderByDesc(ArticleCollaborator::getCreatedAt));
        Map<Long, Article> articles = indexed(articleMapper.selectBatchIds(page.getRecords().stream().map(ArticleCollaborator::getArticleId).toList()), Article::getId);
        Map<Long, CommunityUser> users = indexed(userMapper.selectBatchIds(page.getRecords().stream().map(ArticleCollaborator::getUserId).toList()), CommunityUser::getId);
        List<AdminArticleCollaborationView> list = page.getRecords().stream().map(value -> {
            Article article = articles.get(value.getArticleId()); CommunityUser user = users.get(value.getUserId());
            return new AdminArticleCollaborationView(value.getId(), value.getArticleId(), article == null ? null : article.getTitle(),
                    value.getUserId(), user == null ? null : user.getUsername(), user == null ? null : user.getDisplayName(),
                    value.getContributionType(), Boolean.TRUE.equals(value.getCanEdit()), value.getAttributionOrder(),
                    value.getRevokedAt() == null ? "ACTIVE" : "REVOKED", value.getAcceptedAt(), value.getRevokedAt(), value.getCreatedAt());
        }).toList();
        return new AdminCommunityPageView<>(list, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    private AdminTeamView view(Team team, boolean detail) {
        Blog blog = team.getBlogId() == null ? null : blogMapper.selectById(team.getBlogId());
        CommunityUser owner = team.getOwnerUserId() == null ? null : userMapper.selectById(team.getOwnerUserId());
        List<TeamMember> rows = memberMapper.findActiveMembers(team.getId());
        Map<Long, CommunityUser> users = indexed(userMapper.selectBatchIds(rows.stream().map(TeamMember::getUserId).toList()), CommunityUser::getId);
        List<AdminTeamMemberView> members = detail ? rows.stream().map(member -> memberView(member, users.get(member.getUserId()))).toList() : List.of();
        List<AdminTeamAuditEventView> events = detail ? auditMapper.selectList(Wrappers.<TeamAuditEvent>lambdaQuery()
                .eq(TeamAuditEvent::getTeamId, team.getId()).orderByDesc(TeamAuditEvent::getOccurredAt).last("LIMIT 50"))
                .stream().map(event -> new AdminTeamAuditEventView(event.getId(), event.getActorUserId(), event.getEventType(), event.getTargetType(), event.getTargetId(), event.getOccurredAt())).toList() : List.of();
        return new AdminTeamView(team.getId(), team.getBlogId(), blog == null ? null : blog.getName(), blog == null ? null : blog.getSlug(),
                team.getStatus(), team.getOwnerUserId(), owner == null ? null : owner.getUsername(), rows.size(), team.getLockVersion(),
                team.getCreatedAt(), team.getUpdatedAt(), members, events);
    }

    private static AdminTeamMemberView memberView(TeamMember member, CommunityUser user) {
        return new AdminTeamMemberView(member.getId(), member.getUserId(), user == null ? null : user.getUsername(),
                user == null ? null : user.getDisplayName(), member.getRoleCode(), member.getInvitedByUserId(), member.getJoinedAt(), member.getLockVersion());
    }

    private static int positive(Integer value) { return value == null || value < 1 ? 1 : value; }
    private static int boundedSize(Integer value) { return value == null ? 20 : Math.min(Math.max(value, 1), 100); }
    private static <T> Map<Long, T> indexed(Collection<T> values, Function<T, Long> id) { return values.stream().collect(Collectors.toMap(id, Function.identity(), (left, right) -> left)); }
}
