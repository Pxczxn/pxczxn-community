package top.pxczxn.community.team.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamPortalServiceImpl implements TeamPortalService {
    private static final Map<String, List<String>> CAPABILITIES = Map.of(
            "OWNER", List.of("OVERVIEW", "ARTICLES", "MEMBERS", "CATEGORIES", "SETTINGS"),
            "ADMIN", List.of("OVERVIEW", "ARTICLES", "MEMBERS", "CATEGORIES"),
            "EDITOR", List.of("OVERVIEW", "ARTICLES", "CATEGORIES"),
            "AUTHOR", List.of("OVERVIEW", "ARTICLES"));
    private final TeamMapper teamMapper;
    private final TeamMemberMapper memberMapper;
    private final BlogMapper blogMapper;
    private final CommunityUserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TeamSummaryView> listPublicTeams() {
        List<Team> teams = teamMapper.selectList(Wrappers.<Team>lambdaQuery()
                .eq(Team::getStatus, "ACTIVE").isNull(Team::getDeletedAt));
        if (teams.isEmpty()) return List.of();
        Map<Long, Blog> blogs = blogMapper.selectBatchIds(teams.stream().map(Team::getBlogId).toList()).stream()
                .filter(blog -> "TEAM".equals(blog.getBlogType()) && "ACTIVE".equals(blog.getStatus()) && blog.getDeletedAt() == null)
                .collect(Collectors.toMap(Blog::getId, Function.identity()));
        return teams.stream().map(team -> summary(team, blogs.get(team.getBlogId()))).filter(Objects::nonNull).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamPortalView publicTeam(String teamSlug) {
        Blog blog = blogMapper.selectOne(Wrappers.<Blog>lambdaQuery().eq(Blog::getSlug, normalizeSlug(teamSlug))
                .eq(Blog::getBlogType, "TEAM").eq(Blog::getStatus, "ACTIVE").isNull(Blog::getDeletedAt).last("LIMIT 1"));
        if (blog == null) throw new BusinessException(404, "Team does not exist");
        Team team = teamMapper.selectOne(Wrappers.<Team>lambdaQuery().eq(Team::getBlogId, blog.getId())
                .eq(Team::getStatus, "ACTIVE").isNull(Team::getDeletedAt).last("LIMIT 1"));
        if (team == null) throw new BusinessException(404, "Team does not exist");
        return portal(team, blog);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamWorkspaceView workspace(Long viewerUserId, Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null || team.getDeletedAt() != null || !"ACTIVE".equals(team.getStatus())) throw new BusinessException(404, "Team does not exist");
        TeamMember member = memberMapper.findActiveMember(teamId, viewerUserId);
        if (member == null) throw new BusinessException(403, "Not allowed to access team workspace");
        Blog blog = blogMapper.selectById(team.getBlogId());
        if (blog == null || !"TEAM".equals(blog.getBlogType())) throw new BusinessException(404, "Team does not exist");
        return new TeamWorkspaceView(portal(team, blog), member.getRoleCode(), CAPABILITIES.getOrDefault(member.getRoleCode(), List.of()));
    }

    private TeamPortalView portal(Team team, Blog blog) {
        List<TeamMember> members = memberMapper.findActiveMembers(team.getId());
        Map<Long, CommunityUser> users = userMapper.selectBatchIds(members.stream().map(TeamMember::getUserId).toList()).stream()
                .collect(Collectors.toMap(CommunityUser::getId, Function.identity()));
        List<TeamPortalMemberView> views = members.stream().map(member -> {
            CommunityUser user = users.get(member.getUserId());
            return user == null ? null : new TeamPortalMemberView(user.getId(), user.getDisplayName(), user.getUsername(), user.getAvatarFileId(), member.getRoleCode());
        }).filter(Objects::nonNull).toList();
        CommunityUser owner = users.get(team.getOwnerUserId());
        return new TeamPortalView(summary(team, blog), owner == null ? null : owner.getDisplayName(), views);
    }

    private static TeamSummaryView summary(Team team, Blog blog) {
        if (blog == null) return null;
        return new TeamSummaryView(team.getId(), blog.getId(), blog.getName(), blog.getSlug(), blog.getSummary(),
                blog.getAvatarFileId(), blog.getBackgroundFileId(), blog.getArticleCount() == null ? 0 : blog.getArticleCount(),
                blog.getFollowerCount() == null ? 0 : blog.getFollowerCount());
    }

    private static String normalizeSlug(String value) {
        if (value == null || !value.matches("[a-z0-9]+(?:[-_][a-z0-9]+)*")) throw new BusinessException(400, "Invalid team slug");
        return value;
    }
}
