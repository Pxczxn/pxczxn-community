package top.pxczxn.community.team.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleStatusCountRow;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.series.model.TeamSeries;
import top.pxczxn.community.series.model.TeamSeriesArticle;
import top.pxczxn.community.series.persistence.TeamSeriesArticleMapper;
import top.pxczxn.community.series.persistence.TeamSeriesMapper;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamAuditEvent;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamCountRow;
import top.pxczxn.community.team.persistence.TeamInvitationMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.community.team.submission.persistence.TeamSubmissionMapper;
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
            "OWNER", List.of("OVERVIEW", "ARTICLES", "MEMBERS", "CATEGORIES", "SERIES", "SETTINGS"),
            "ADMIN", List.of("OVERVIEW", "ARTICLES", "MEMBERS", "CATEGORIES", "SERIES", "SETTINGS"),
            "EDITOR", List.of("OVERVIEW", "ARTICLES", "CATEGORIES", "SERIES"),
            "AUTHOR", List.of("OVERVIEW", "ARTICLES"));
    private static final int DEFAULT_ACTIVITY_LIMIT = 20;
    private static final int MAX_ACTIVITY_LIMIT = 50;
    private static final int RECENT_ARTICLE_LIMIT = 6;
    private static final int CONTENT_LIST_LIMIT = 200;

    private final TeamMapper teamMapper;
    private final TeamMemberMapper memberMapper;
    private final BlogMapper blogMapper;
    private final CommunityUserMapper userMapper;
    private final TeamSubmissionMapper submissionMapper;
    private final TeamSeriesMapper seriesMapper;
    private final TeamSeriesArticleMapper seriesArticleMapper;
    private final TeamInvitationMapper invitationMapper;
    private final TeamAuditEventMapper auditEventMapper;
    private final ArticleMapper articleMapper;
    private final TeamAuthorityService authorityService;

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
        Team team = requireActiveTeam(teamId);
        TeamMember member = memberMapper.findActiveMember(teamId, viewerUserId);
        if (member == null) throw new BusinessException(403, "Not allowed to access team workspace");
        Blog blog = blogMapper.selectById(team.getBlogId());
        if (blog == null || !"TEAM".equals(blog.getBlogType())) throw new BusinessException(404, "Team does not exist");
        return new TeamWorkspaceView(portal(team, blog), member.getRoleCode(),
                CAPABILITIES.getOrDefault(member.getRoleCode(), List.of()),
                authorityService.getPermissions(viewerUserId, teamId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyTeamView> myTeams(Long viewerUserId) {
        List<TeamMember> memberships = memberMapper.findActiveMembershipsByUser(viewerUserId);
        if (memberships.isEmpty()) return List.of();
        List<Long> teamIds = memberships.stream().map(TeamMember::getTeamId).distinct().toList();
        Map<Long, Team> teams = teamMapper.selectBatchIds(teamIds).stream()
                .filter(team -> team.getDeletedAt() == null && "ACTIVE".equals(team.getStatus()))
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        if (teams.isEmpty()) return List.of();
        Map<Long, Blog> blogs = blogMapper.selectBatchIds(teams.values().stream().map(Team::getBlogId).toList()).stream()
                .filter(blog -> "TEAM".equals(blog.getBlogType()) && "ACTIVE".equals(blog.getStatus()) && blog.getDeletedAt() == null)
                .collect(Collectors.toMap(Blog::getId, Function.identity()));
        List<Long> activeTeamIds = teams.values().stream()
                .filter(team -> blogs.containsKey(team.getBlogId())).map(Team::getId).toList();
        if (activeTeamIds.isEmpty()) return List.of();

        Map<Long, Integer> memberCounts = toCountMap(memberMapper.countActiveMembersByTeams(activeTeamIds));
        Map<Long, Integer> seriesCounts = toCountMap(seriesMapper.countByTeams(activeTeamIds));
        Map<Long, Integer> pendingSubmissions = toCountMap(submissionMapper.countPendingByTeams(activeTeamIds));

        return memberships.stream()
                .filter(membership -> activeTeamIds.contains(membership.getTeamId()))
                .map(membership -> {
                    Team team = teams.get(membership.getTeamId());
                    Blog blog = blogs.get(team.getBlogId());
                    String role = membership.getRoleCode();
                    return new MyTeamView(
                            team.getId(), blog.getId(), blog.getName(), blog.getSlug(), blog.getSummary(),
                            blog.getAvatarFileId(), blog.getBackgroundFileId(),
                            role, CAPABILITIES.getOrDefault(role, List.of()), authorityService.getPermissions(viewerUserId, membership.getTeamId()),
                            memberCounts.getOrDefault(team.getId(), 0),
                            blog.getArticleCount() == null ? 0 : blog.getArticleCount().intValue(),
                            seriesCounts.getOrDefault(team.getId(), 0),
                            blog.getFollowerCount() == null ? 0 : blog.getFollowerCount().intValue(),
                            pendingSubmissions.getOrDefault(team.getId(), 0),
                            submissionMapper.countRevisionRequiredForAuthor(team.getId(), viewerUserId),
                            membership.getJoinedAt(),
                            team.getUpdatedAt() != null ? team.getUpdatedAt() : blog.getUpdatedAt());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamDashboardView dashboard(Long viewerUserId, Long teamId) {
        Team team = requireActiveTeam(teamId);
        TeamMember member = memberMapper.findActiveMember(teamId, viewerUserId);
        if (member == null) throw new BusinessException(403, "Not allowed to access team dashboard");
        Blog blog = blogMapper.selectById(team.getBlogId());
        if (blog == null || !"TEAM".equals(blog.getBlogType())) throw new BusinessException(404, "Team does not exist");

        String role = member.getRoleCode();
        List<String> capabilities = CAPABILITIES.getOrDefault(role, List.of());
        List<String> permissions = authorityService.getPermissions(viewerUserId, teamId);

        Map<String, Integer> statusCounts = articleMapper.countByBlogGroupedByPublishStatus(blog.getId()).stream()
                .collect(Collectors.toMap(ArticleStatusCountRow::getStatus, ArticleStatusCountRow::getTotal, Integer::sum));
        long totalViews = articleMapper.sumViewCountByBlog(blog.getId());
        long totalInteractions = articleMapper.sumInteractionCountByBlog(blog.getId());
        TeamStatsView stats = new TeamStatsView(
                statusCounts.getOrDefault("PUBLISHED", 0),
                statusCounts.getOrDefault("DRAFT", 0),
                statusCounts.getOrDefault("PENDING_REVIEW", 0),
                seriesCount(teamId),
                memberMapper.findActiveMembers(teamId).size(),
                blog.getFollowerCount() == null ? 0 : blog.getFollowerCount().intValue(),
                totalViews > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalViews,
                totalInteractions > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalInteractions);

        List<Article> recent = articleMapper.findRecentByBlog(blog.getId(), RECENT_ARTICLE_LIMIT);
        Map<Long, CommunityUser> recentAuthors = resolveUsers(recent.stream().map(Article::getAuthorUserId).toList());
        Map<Long, SeriesRef> recentSeries = resolveSeriesForArticles(recent);

        TeamTodoView todos = new TeamTodoView(
                submissionMapper.countByTeamAndStatus(teamId, "TEAM_PENDING"),
                submissionMapper.countRevisionRequiredForAuthor(teamId, viewerUserId),
                invitationMapper.countPendingByTeam(teamId),
                seriesMapper.countByTeamAndReviewStatus(teamId, "PENDING_REVIEW"),
                articleMapper.countRiskByBlog(blog.getId()));

        List<TeamAuditEvent> events = auditEventMapper.findRecentByTeam(teamId, DEFAULT_ACTIVITY_LIMIT);
        Map<Long, CommunityUser> eventActors = resolveUsers(events.stream()
                .map(TeamAuditEvent::getActorUserId).filter(Objects::nonNull).toList());

        return new TeamDashboardView(summary(team, blog), role, capabilities, permissions, stats,
                recent.stream().map(article -> articleBrief(article, recentAuthors, recentSeries.get(article.getId()))).toList(),
                todos, events.stream().map(event -> activity(event, eventActors)).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamActivityView> activities(Long viewerUserId, Long teamId, int limit) {
        Team team = requireActiveTeam(teamId);
        if (memberMapper.findActiveMember(teamId, viewerUserId) == null) {
            throw new BusinessException(403, "Not allowed to view team activities");
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? DEFAULT_ACTIVITY_LIMIT : limit, MAX_ACTIVITY_LIMIT));
        List<TeamAuditEvent> events = auditEventMapper.findRecentByTeam(teamId, capped);
        Map<Long, CommunityUser> actors = resolveUsers(events.stream()
                .map(TeamAuditEvent::getActorUserId).filter(Objects::nonNull).toList());
        return events.stream().map(event -> activity(event, actors)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamArticleBriefView> teamArticles(Long viewerUserId, Long teamId, String publishStatus) {
        Team team = requireActiveTeam(teamId);
        if (memberMapper.findActiveMember(teamId, viewerUserId) == null) {
            throw new BusinessException(403, "Not allowed to view team content");
        }
        Blog blog = blogMapper.selectById(team.getBlogId());
        if (blog == null || !"TEAM".equals(blog.getBlogType())) throw new BusinessException(404, "Team does not exist");
        String status = publishStatus == null || publishStatus.isBlank() ? null : publishStatus.trim().toUpperCase(java.util.Locale.ROOT);
        List<Article> articles = articleMapper.findByBlog(blog.getId(), status, CONTENT_LIST_LIMIT);
        Map<Long, CommunityUser> authors = resolveUsers(articles.stream().map(Article::getAuthorUserId).toList());
        Map<Long, SeriesRef> seriesByArticle = resolveSeriesForArticles(articles);
        return articles.stream().map(article -> articleBrief(article, authors, seriesByArticle.get(article.getId()))).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamSummaryView updateSettings(Long viewerUserId, Long teamId, UpdateTeamSettingsCommand command) {
        Team team = requireActiveTeam(teamId);
        if (!authorityService.hasPermission(viewerUserId, teamId, "MANAGE_TEAM")) {
            throw new BusinessException(403, "Not allowed to update team settings");
        }
        Blog blog = blogMapper.selectById(team.getBlogId());
        if (blog == null || !"TEAM".equals(blog.getBlogType())) throw new BusinessException(404, "Team does not exist");

        String name = command == null || command.name() == null ? null : command.name().trim();
        if (name == null || name.isEmpty()) throw new BusinessException(400, "Team name cannot be empty");
        if (name.length() > 120) throw new BusinessException(400, "Team name is too long");
        String summary = command.summary() == null ? null : command.summary().trim();
        if (summary != null && summary.length() > 500) throw new BusinessException(400, "Team summary is too long");

        String category = trimMax(command.category(), 50, "团队分类");
        String contentDirection = trimMax(command.contentDirection(), 500, "内容方向");
        String theme = trimMax(command.theme(), 50, "主页主题");
        String seoTitle = trimMax(command.seoTitle(), 200, "SEO 标题");
        String seoDescription = trimMax(command.seoDescription(), 500, "SEO 描述");
        String submissionGuideline = trimMax(command.submissionGuideline(), 2000, "投稿说明");
        String contactInfo = trimMax(command.contactInfo(), 500, "联系方式");
        Boolean publicMembers = command.publicMembers() == null || Boolean.TRUE.equals(command.publicMembers());
        Boolean allowSubmissions = command.allowSubmissions() == null || Boolean.TRUE.equals(command.allowSubmissions());

        int blogUpdated = blogMapper.updateProfileWithOptimisticLock(
                blog.getId(), name, summary, command.avatarFileId(), command.backgroundFileId(), blog.getLockVersion());
        if (blogUpdated != 1) throw new BusinessException(409, "Team profile has changed; refresh and retry");
        int teamUpdated = teamMapper.updatePortalSettingsWithOptimisticLock(
                team.getId(), category, contentDirection, theme, seoTitle, seoDescription,
                publicMembers, allowSubmissions, submissionGuideline, contactInfo, team.getLockVersion());
        if (teamUpdated != 1) throw new BusinessException(409, "Team settings have changed; refresh and retry");

        auditEventMapper.insert(auditEvent(team.getId(), viewerUserId, "TEAM_PROFILE_UPDATED", "TEAM", team.getId(),
                "{\"name\":\"" + escape(blog.getName()) + "\"}", "{\"name\":\"" + escape(name) + "\"}"));

        blog.setName(name); blog.setSummary(summary);
        blog.setAvatarFileId(command.avatarFileId()); blog.setBackgroundFileId(command.backgroundFileId());
        team.setCategory(category); team.setContentDirection(contentDirection); team.setTheme(theme);
        team.setSeoTitle(seoTitle); team.setSeoDescription(seoDescription);
        team.setPublicMembers(publicMembers); team.setAllowSubmissions(allowSubmissions);
        team.setSubmissionGuideline(submissionGuideline); team.setContactInfo(contactInfo);
        return summary(team, blog);
    }

    private static String trimMax(String value, int max, String label) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new BusinessException(400, label + " is too long");
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static TeamAuditEvent auditEvent(Long teamId, Long actorUserId, String eventType,
                                             String targetType, Long targetId,
                                             String beforeSnapshot, String afterSnapshot) {
        TeamAuditEvent event = new TeamAuditEvent();
        event.setTeamId(teamId);
        event.setActorUserId(actorUserId);
        event.setEventType(eventType);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setBeforeSnapshot(beforeSnapshot);
        event.setAfterSnapshot(afterSnapshot);
        event.setOccurredAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        return event;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private TeamPortalView portal(Team team, Blog blog) {
        List<TeamMember> members = memberMapper.findActiveMembers(team.getId());
        List<Long> memberUserIds = members.stream().map(TeamMember::getUserId).toList();
        Map<Long, CommunityUser> users = memberUserIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(memberUserIds).stream()
                        .collect(Collectors.toMap(CommunityUser::getId, Function.identity()));
        List<TeamPortalMemberView> views = members.stream().map(member -> {
            CommunityUser user = users.get(member.getUserId());
            return user == null ? null : new TeamPortalMemberView(user.getId(), user.getDisplayName(), user.getUsername(), user.getAvatarFileId(), member.getRoleCode());
        }).filter(Objects::nonNull).toList();
        CommunityUser owner = users.get(team.getOwnerUserId());
        return new TeamPortalView(summary(team, blog), owner == null ? null : owner.getDisplayName(), views,
                settings(team));
    }

    private static TeamPortalSettingsView settings(Team team) {
        return new TeamPortalSettingsView(
                team.getCategory(), team.getContentDirection(), team.getTheme(),
                team.getSeoTitle(), team.getSeoDescription(),
                team.getPublicMembers() == null || Boolean.TRUE.equals(team.getPublicMembers()),
                team.getAllowSubmissions() == null || Boolean.TRUE.equals(team.getAllowSubmissions()),
                team.getSubmissionGuideline(), team.getContactInfo());
    }

    private TeamArticleBriefView articleBrief(Article article, Map<Long, CommunityUser> authors, SeriesRef series) {
        CommunityUser author = authors.get(article.getAuthorUserId());
        return new TeamArticleBriefView(
                article.getId(), article.getTitle(), article.getSlug(),
                article.getPublishStatus(), article.getReviewStatus(), article.getVisibility(),
                article.getAuthorUserId(), author == null ? null : author.getDisplayName(),
                article.getViewCount() == null ? 0 : article.getViewCount().intValue(),
                article.getLikeCount() == null ? 0 : article.getLikeCount().intValue(),
                article.getCommentCount() == null ? 0 : article.getCommentCount().intValue(),
                article.getUpdatedAt(), article.getPublishedAt(),
                series == null ? null : series.seriesId(),
                series == null ? null : series.seriesTitle());
    }

    /** 文章 → 所属系列(一篇文章最多属于一个系列,与章节编排一致)。 */
    private Map<Long, SeriesRef> resolveSeriesForArticles(List<Article> articles) {
        if (articles.isEmpty()) return Map.of();
        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        List<TeamSeriesArticle> memberships = seriesArticleMapper.findByArticles(articleIds);
        if (memberships.isEmpty()) return Map.of();
        List<Long> seriesIds = memberships.stream().map(TeamSeriesArticle::getSeriesId).distinct().toList();
        Map<Long, String> titles = seriesMapper.selectBatchIds(seriesIds).stream()
                .collect(Collectors.toMap(TeamSeries::getId, TeamSeries::getTitle));
        Map<Long, SeriesRef> result = new java.util.HashMap<>();
        for (TeamSeriesArticle membership : memberships) {
            result.putIfAbsent(membership.getArticleId(),
                    new SeriesRef(membership.getSeriesId(), titles.get(membership.getSeriesId())));
        }
        return result;
    }

    private record SeriesRef(Long seriesId, String seriesTitle) { }

    private TeamActivityView activity(TeamAuditEvent event, Map<Long, CommunityUser> actors) {
        CommunityUser actor = event.getActorUserId() == null ? null : actors.get(event.getActorUserId());
        return new TeamActivityView(
                event.getId(), event.getTeamId(), event.getEventType(), event.getTargetType(), event.getTargetId(),
                event.getActorUserId(),
                actor == null ? null : actor.getDisplayName(),
                actor == null ? null : actor.getUsername(),
                actor == null ? null : actor.getAvatarFileId(),
                event.getOccurredAt());
    }

    private Map<Long, CommunityUser> resolveUsers(List<Long> userIds) {
        List<Long> distinct = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(CommunityUser::getId, Function.identity()));
    }

    private int seriesCount(Long teamId) {
        List<TeamCountRow> rows = seriesMapper.countByTeams(List.of(teamId));
        return rows.isEmpty() ? 0 : rows.get(0).getTotal();
    }

    private static Map<Long, Integer> toCountMap(List<TeamCountRow> rows) {
        return rows.stream().collect(Collectors.toMap(TeamCountRow::getTeamId, TeamCountRow::getTotal));
    }

    private Team requireActiveTeam(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null || team.getDeletedAt() != null || !"ACTIVE".equals(team.getStatus())) {
            throw new BusinessException(404, "Team does not exist");
        }
        return team;
    }

    private static TeamSummaryView summary(Team team, Blog blog) {
        if (blog == null) return null;
        return new TeamSummaryView(team.getId(), blog.getId(), blog.getName(), blog.getSlug(), blog.getSummary(),
                blog.getAvatarFileId(), blog.getBackgroundFileId(), blog.getArticleCount() == null ? 0 : blog.getArticleCount(),
                blog.getFollowerCount() == null ? 0 : blog.getFollowerCount(),
                team.getCategory());
    }

    private static String normalizeSlug(String value) {
        if (value == null || !value.matches("[a-z0-9]+(?:[-_][a-z0-9]+)*")) throw new BusinessException(400, "Invalid team slug");
        return value;
    }
}
