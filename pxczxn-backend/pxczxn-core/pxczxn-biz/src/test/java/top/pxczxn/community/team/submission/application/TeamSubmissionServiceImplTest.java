package top.pxczxn.community.team.submission.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.team.application.TeamAuthorityService;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.submission.model.TeamSubmission;
import top.pxczxn.community.team.submission.persistence.TeamSubmissionMapper;
import top.pxczxn.community.sanction.application.CommunitySanctionService;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TeamSubmissionServiceImplTest {
    private TeamSubmissionMapper submissionMapper;
    private ArticleMapper articleMapper;
    private ArticleVersionMapper versionMapper;
    private TeamMapper teamMapper;
    private TeamMemberMapper teamMemberMapper;
    private BlogMapper blogMapper;
    private TeamAuthorityService authorityService;
    private TeamSubmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        submissionMapper = mock(TeamSubmissionMapper.class); articleMapper = mock(ArticleMapper.class); versionMapper = mock(ArticleVersionMapper.class);
        teamMapper = mock(TeamMapper.class); teamMemberMapper = mock(TeamMemberMapper.class); blogMapper = mock(BlogMapper.class); authorityService = mock(TeamAuthorityService.class);
        service = new TeamSubmissionServiceImpl(submissionMapper, articleMapper, versionMapper, teamMapper, teamMemberMapper, blogMapper, authorityService, mock(TeamAuditEventMapper.class), mock(ApplicationEventPublisher.class), mock(CommunitySanctionService.class));
    }

    @Test
    void submitFixesCurrentSourceVersionAndDoesNotMutateSourceArticle() {
        Article source = sourceArticle(10L, 3L, 101L); ArticleVersion fixed = version(101L, 10L); Team team = activeTeam(20L, 200L);
        when(submissionMapper.findByIdempotencyKey("submit-001")).thenReturn(null);
        when(articleMapper.selectById(10L)).thenReturn(source); when(versionMapper.selectById(101L)).thenReturn(fixed);
        when(teamMapper.selectById(20L)).thenReturn(team); when(blogMapper.selectById(3L)).thenReturn(personalBlog(3L)); when(submissionMapper.insert(any())).thenReturn(1);

        TeamSubmissionView result = service.submit(3L, new CreateTeamSubmissionCommand(10L, 20L, null, "submit-001"));

        assertThat(result.fixedSourceVersionId()).isEqualTo(101L); assertThat(result.status()).isEqualTo("TEAM_PENDING");
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void submitRejectsAttemptToPostAnotherAuthorsArticle() {
        when(submissionMapper.findByIdempotencyKey("submit-002")).thenReturn(null);
        when(articleMapper.selectById(10L)).thenReturn(sourceArticle(10L, 4L, 101L));

        assertThatThrownBy(() -> service.submit(3L, new CreateTeamSubmissionCommand(10L, 20L, null, "submit-002"))).isInstanceOf(BusinessException.class);
    }

    @Test
    void platformApprovalCreatesIndependentTeamArticleFromFixedSnapshot() {
        TeamSubmission submission = submission(70L, 10L, 101L, 20L, 3L, 1); Article source = sourceArticle(10L, 3L, 102L); ArticleVersion fixed = version(101L, 10L); Team team = activeTeam(20L, 200L); Blog teamBlog = teamBlog(200L);
        when(submissionMapper.selectById(70L)).thenReturn(submission); when(submissionMapper.beginPlatformPublication(eq(70L), eq(1), eq(99L), any(), any())).thenReturn(1); when(submissionMapper.completePublication(eq(70L), eq(2), any(), any())).thenReturn(1);
        when(articleMapper.selectById(10L)).thenReturn(source); when(versionMapper.selectById(101L)).thenReturn(fixed); when(teamMapper.selectById(20L)).thenReturn(team); when(blogMapper.selectById(200L)).thenReturn(teamBlog); when(articleMapper.insert(any())).thenReturn(1); when(versionMapper.insert(any())).thenReturn(1);

        TeamSubmissionView result = service.platformApprove(99L, 70L, new DecideTeamSubmissionCommand(1, "approved"));

        ArgumentCaptor<Article> article = ArgumentCaptor.forClass(Article.class); verify(articleMapper).insert(article.capture());
        assertThat(article.getValue().getBlogId()).isEqualTo(200L); assertThat(article.getValue().getAuthorUserId()).isEqualTo(3L); assertThat(article.getValue().getId()).isNotEqualTo(10L); assertThat(article.getValue().getCurrentVersionId()).isNotEqualTo(101L); assertThat(result.status()).isEqualTo("PUBLISHED"); assertThat(result.publishedTeamArticleId()).isEqualTo(article.getValue().getId());
    }

    @Test
    void submitRejectsOutsiderWhenTeamClosedSubmissions() {
        when(submissionMapper.findByIdempotencyKey("submit-003")).thenReturn(null);
        when(articleMapper.selectById(10L)).thenReturn(sourceArticle(10L, 3L, 101L));
        when(versionMapper.selectById(101L)).thenReturn(version(101L, 10L));
        Team closed = activeTeam(20L, 200L);
        closed.setAllowSubmissions(false);
        when(teamMapper.selectById(20L)).thenReturn(closed);
        when(teamMemberMapper.findActiveMember(20L, 3L)).thenReturn(null);

        assertThatThrownBy(() -> service.submit(3L, new CreateTeamSubmissionCommand(10L, 20L, null, "submit-003")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void submitAllowsMemberWhenTeamClosedSubmissions() {
        when(submissionMapper.findByIdempotencyKey("submit-004")).thenReturn(null);
        when(articleMapper.selectById(10L)).thenReturn(sourceArticle(10L, 3L, 101L));
        when(versionMapper.selectById(101L)).thenReturn(version(101L, 10L));
        when(blogMapper.selectById(3L)).thenReturn(personalBlog(3L));
        Team closed = activeTeam(20L, 200L);
        closed.setAllowSubmissions(false);
        when(teamMapper.selectById(20L)).thenReturn(closed);
        TeamMember member = new TeamMember(); member.setTeamId(20L); member.setUserId(3L); member.setRoleCode("AUTHOR");
        when(teamMemberMapper.findActiveMember(20L, 3L)).thenReturn(member);
        when(submissionMapper.insert(any())).thenReturn(1);

        TeamSubmissionView result = service.submit(3L, new CreateTeamSubmissionCommand(10L, 20L, null, "submit-004"));

        assertThat(result.status()).isEqualTo("TEAM_PENDING");
    }

    private static Article sourceArticle(Long id, Long author, Long currentVersion) { Article a = new Article(); a.setId(id); a.setBlogId(3L); a.setAuthorUserId(author); a.setCurrentVersionId(currentVersion); a.setTitle("Source article"); a.setSlug("source-article"); a.setSummary("Summary"); return a; }
    private static ArticleVersion version(Long id, Long articleId) { ArticleVersion v = new ArticleVersion(); v.setId(id); v.setArticleId(articleId); v.setContentMode("MARKDOWN"); v.setMarkdownContent("# fixed"); v.setRenderedHtml("<h1>fixed</h1>"); v.setPlainText("fixed"); v.setContentHash("a".repeat(64)); v.setWordCount(1); v.setReadingTimeMinutes(1); return v; }
    private static Team activeTeam(Long id, Long blogId) { Team t = new Team(); t.setId(id); t.setBlogId(blogId); t.setOwnerUserId(4L); t.setStatus("ACTIVE"); t.setAllowSubmissions(true); t.setPublicMembers(true); return t; }
    private static Blog personalBlog(Long id) { Blog b = new Blog(); b.setId(id); b.setBlogType("PERSONAL"); return b; }
    private static Blog teamBlog(Long id) { Blog b = new Blog(); b.setId(id); b.setBlogType("TEAM"); b.setSlug("team"); return b; }
    private static TeamSubmission submission(Long id, Long source, Long fixed, Long team, Long author, Integer lock) { TeamSubmission s = new TeamSubmission(); s.setId(id); s.setSourceArticleId(source); s.setFixedSourceVersionId(fixed); s.setTargetTeamId(team); s.setSubmittedByUserId(author); s.setStatus("PLATFORM_PENDING"); s.setLockVersion(lock); return s; }
}
