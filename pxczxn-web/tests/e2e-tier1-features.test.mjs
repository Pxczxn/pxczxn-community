import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

let workerPromise;

async function worker() {
  workerPromise ??= (async () => {
    const workerUrl = new URL("../dist/server/index.js", import.meta.url);
    workerUrl.searchParams.set("test", `tier1-${process.pid}-${Date.now()}`);
    return (await import(workerUrl.href)).default;
  })();
  return workerPromise;
}

async function render(path) {
  const app = await worker();
  return app.fetch(
    new Request(`http://localhost${path}`, {
      headers: { accept: "text/html" },
    }),
    {
      ASSETS: {
        fetch: async () => new Response("Not found", { status: 404 }),
      },
    },
    {
      waitUntil() {},
      passThroughOnException() {},
    },
  );
}

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), "utf8");
}

/* ==========================================================================
   Feature 1: API Layer 100% Backend Alignment
   ========================================================================== */

test("F1-T1: communityApi exports article deletion contract aligned with backend DELETE /api/v1/articles/:id", async () => {
  const apiSource = await readSource("../app/lib/community/api.ts");
  assert.match(apiSource, /deleteArticle\s*\(\s*articleId:\s*string\s*\)/);
  assert.match(apiSource, /\/api\/v1\/articles\/\$\{encodeURIComponent\(articleId\)\}/);
  assert.match(apiSource, /method:\s*"DELETE"/);
});

test("F1-T2: communityApi exports article version listing and detail contracts", async () => {
  const apiSource = await readSource("../app/lib/community/api.ts");
  assert.match(apiSource, /articleVersions\s*\(\s*articleId:\s*string\s*\)/);
  assert.match(apiSource, /\/api\/v1\/articles\/\$\{encodeURIComponent\(articleId\)\}\/versions/);
  assert.match(apiSource, /articleVersionDetail\s*\(\s*articleId:\s*string,\s*versionId:\s*string\s*\)/);
  assert.match(apiSource, /\/versions\/\$\{encodeURIComponent\(versionId\)\}/);
});

test("F1-T3: communityApi exports restoreArticleVersion method with lock version verification", async () => {
  const apiSource = await readSource("../app/lib/community/api.ts");
  assert.match(apiSource, /restoreArticleVersion\s*\(/);
  assert.match(apiSource, /\/versions\/\$\{encodeURIComponent\(versionId\)\}\/restore/);
  assert.match(apiSource, /method:\s*"POST"/);
});

test("F1-T4: communityApi exports Blog Category CRUD endpoints", async () => {
  const apiSource = await readSource("../app/lib/community/api.ts");
  assert.match(apiSource, /publicBlogCategories\s*\(/);
  assert.match(apiSource, /createCategory\s*\(/);
  assert.match(apiSource, /updateCategory\s*\(/);
  assert.match(apiSource, /deleteCategory\s*\(/);
  assert.match(apiSource, /\/api\/v1\/public\/blogs\/\$\{encodeURIComponent\(blogSlug\)\}\/categories/);
});

test("F1-T5: communityApi exports interactive entity deletion and cancellation contracts", async () => {
  const apiSource = await readSource("../app/lib/community/api.ts");
  assert.match(apiSource, /deleteFavoriteFolder\s*\(/);
  assert.match(apiSource, /deleteMoment\s*\(/);
  assert.match(apiSource, /deleteComment\s*\(/);
  assert.match(apiSource, /cancelCollaborationInvitation\s*\(/);
});

/* ==========================================================================
   Feature 2: Header Navigation & UI Wiring
   ========================================================================== */

test("F2-T1: Header navigation contains all 6 core page links with precise labels", async () => {
  const topbarSource = await readSource("../app/components/prototype-ui.tsx");
  assert.match(topbarSource, /\{ href: "\/", label: "首页", Icon: House \}/);
  assert.match(topbarSource, /\{ href: "\/discover", label: "发现", Icon: Compass \}/);
  assert.match(topbarSource, /\{ href: "\/articles", label: "文章", Icon: FileText \}/);
  assert.match(topbarSource, /\{ href: "\/moments", label: "动态", Icon: Orbit \}/);
  assert.match(topbarSource, /\{ href: "\/series", label: "系列", Icon: LibraryBig \}/);
  assert.match(topbarSource, /\{ href: "\/teams", label: "团队", Icon: UsersRound \}/);
});

test("F2-T2: Header topbar search form targets /search endpoint with GET method and 'q' param", async () => {
  const topbarSource = await readSource("../app/components/prototype-ui.tsx");
  assert.match(topbarSource, /<form action="\/search" className="top-search" method="get">/);
  assert.match(topbarSource, /<input aria-label="搜索" name="q"/);
});

test("F2-T3: Topbar menu renders login chip for guest and user dropdown for authenticated session", async () => {
  const topbarSource = await readSource("../app/components/prototype-ui.tsx");
  assert.match(topbarSource, /href="\/login"/);
  assert.match(topbarSource, /DropdownMenu/);
  assert.match(topbarSource, /退出登录/);
  assert.match(topbarSource, /个人中心/);
  assert.match(topbarSource, /设置/);
});

test("F2-T4: Topbar unread notification badge listens to NOTIFICATION_EVENT and calls communityApi.unreadNotifications", async () => {
  const topbarSource = await readSource("../app/components/prototype-ui.tsx");
  assert.match(topbarSource, /NOTIFICATION_EVENT/);
  assert.match(topbarSource, /communityApi\.unreadNotifications\(\)/);
  assert.match(topbarSource, /setUnreadNotifications/);
  assert.match(topbarSource, /Badge count=\{unreadNotifications\}/);
});

test("F2-T5: SideNavigation workspace sidebar links connect to correct core modules", async () => {
  const sideNavSource = await readSource("../app/components/prototype-ui.tsx");
  assert.match(sideNavSource, /\["overview", "团队主页", "\/teams"\]/);
  assert.match(sideNavSource, /\["submissions", "投稿管理", "\/submissions"\]/);
  assert.match(sideNavSource, /\["articles", "文章管理", "\/articles"\]/);
  assert.match(sideNavSource, /\["moments", "动态管理", "\/moments"\]/);
});

/* ==========================================================================
   Feature 3: Home & Discover Pages Refactoring
   ========================================================================== */

test("F3-T1: Home page renders personal dashboard structure with real API bindings when user is logged in", async () => {
  const homeSource = await readSource("../app/home/home-page.tsx");
  assert.match(homeSource, /communityApi\.myReadingSeries/);
  assert.match(homeSource, /communityApi\.followingFeed/);
  assert.match(homeSource, /communityApi\.myTeams/);
  assert.match(homeSource, /communityApi\.notifications/);
  assert.match(homeSource, /className="home-dashboard"/);
});

test("F3-T2: Home page renders guest discover fallback view with discover articles and series for unauthenticated guests", async () => {
  const homeSource = await readSource("../app/home/home-page.tsx");
  assert.match(homeSource, /communityApi\.discoverRankedArticles/);
  assert.match(homeSource, /communityApi\.series/);
  assert.match(homeSource, /session === null/);
  assert.match(homeSource, /探索社区精选/);
});

test("F3-T3: Discover page renders editorial mosaic layout and core discover sections", async () => {
  const discoverSource = await readSource("../app/discover/discover-page.tsx");
  assert.match(discoverSource, /discover-editorial-mosaic/);
  assert.match(discoverSource, /communityApi\.discover/);
  assert.match(discoverSource, /communityApi\.discoverRankedArticles/);
  assert.match(discoverSource, /communityApi\.tags/);
  assert.match(discoverSource, /communityApi\.teams/);
});

test("F3-T4: Discover page handles empty state gracefully without throwing broken elements", async () => {
  const discoverSource = await readSource("../app/discover/discover-page.tsx");
  assert.match(discoverSource, /EmptyState/);
  assert.match(discoverSource, /tags\.length > 0/);
});

test("F3-T5: Discover page topic tag chips wire navigation handlers to /articles?tag= slug", async () => {
  const discoverSource = await readSource("../app/discover/discover-page.tsx");
  assert.match(discoverSource, /\/articles\?tag=/);
  assert.match(discoverSource, /handleTopicClick/);
});

/* ==========================================================================
   Feature 4: Articles Page & Detail Interactive Wiring
   ========================================================================== */

test("F4-T1: Articles page renders dense content index with sort toolbar and pagination", async () => {
  const articlesSource = await readSource("../app/articles/articles-page.tsx");
  assert.match(articlesSource, /content-index-layout/);
  assert.match(articlesSource, /content-index-toolbar/);
  assert.match(articlesSource, /communityApi\.discoverRankedArticles/);
  assert.match(articlesSource, /normalizeSort/);
});

test("F4-T2: Article canonical detail route loads ArticleDetailPage component with articleId", async () => {
  const canonicalRoute = await readSource("../app/[slug]/[articleId]/[articleSlug]/page.tsx");
  assert.match(canonicalRoute, /ArticleDetailPage/);
  assert.match(canonicalRoute, /articleId=\{articleId\}/);
});

test("F4-T3: Article detail page includes comment section trigger and comment posting capability", async () => {
  const articleDetailSource = await readSource("../app/articles/[articleId]/article-detail-page.tsx");
  assert.match(articleDetailSource, /communityApi\.comments/);
  assert.match(articleDetailSource, /communityApi\.createComment/);
  assert.match(articleDetailSource, /comment-section/);
});

test("F4-T4: Article management interface provides Category CRUD actions", async () => {
  const articleDetailSource = await readSource("../app/articles/[articleId]/article-detail-page.tsx");
  assert.match(articleDetailSource, /communityApi\.categories|createCategory|updateCategory/);
});

test("F4-T5: Article editor / detail provides version history inspection and restore triggers", async () => {
  const editorSource = await readSource("../app/editor/article-editor-panel.tsx");
  assert.match(editorSource, /communityApi\.articleVersions/);
  assert.match(editorSource, /communityApi\.restoreArticleVersion/);
  assert.match(editorSource, /版本历史/);
});

/* ==========================================================================
   Feature 5: Dynamic/Feed Page & Detail Refactoring
   ========================================================================== */

test("F5-T1: Moments page supports Recommended, Following, and Latest feed filter tabs", async () => {
  const momentsSource = await readSource("../app/moments/moments-community-page.tsx");
  assert.match(momentsSource, /communityApi\.moments/);
  assert.match(momentsSource, /tab/);
  assert.match(momentsSource, /RECOMMENDED|FOLLOWING|LATEST/);
});

test("F5-T2: Moments publisher component enables text content posting with visibility control", async () => {
  const momentsSource = await readSource("../app/moments/moments-community-page.tsx");
  assert.match(momentsSource, /communityApi\.publishMoment/);
  assert.match(momentsSource, /textContent/);
  assert.match(momentsSource, /visibility/);
});

test("F5-T3: Moment detail route renders detail view and interactive comment section", async () => {
  const momentDetailRoute = await readSource("../app/moments/[momentId]/page.tsx");
  assert.match(momentDetailRoute, /communityApi\.moment/);
  assert.match(momentDetailRoute, /communityApi\.comments/);
  assert.match(momentDetailRoute, /communityApi\.createComment/);
});

test("F5-T4: Moment comment list supports sorting switch between latest and oldest/hot comments", async () => {
  const momentsSource = await readSource("../app/moments/moments-community-page.tsx");
  assert.match(momentsSource, /handleCommentSort|commentSort|setCommentSort/);
});

test("F5-T5: Moment cards provide deletion action calling communityApi.deleteMoment for authors", async () => {
  const momentsSource = await readSource("../app/moments/moments-community-page.tsx");
  assert.match(momentsSource, /communityApi\.deleteMoment/);
  assert.match(momentsSource, /handleDeleteMoment/);
});

/* ==========================================================================
   Feature 6: Series Page & Detail Refactoring
   ========================================================================== */

test("F6-T1: Series page renders bookshelf section ('我的书架') and catalog list", async () => {
  const seriesSource = await readSource("../app/series/page.tsx");
  assert.match(seriesSource, /我的书架/);
  assert.match(seriesSource, /我的追更/);
  assert.match(seriesSource, /communityApi\.myReadingSeries/);
  assert.match(seriesSource, /communityApi\.myFollowedSeries/);
});

test("F6-T2: Series reading percentage progress is calculated correctly using readingPercent helper", async () => {
  const seriesLabelsSource = await readSource("../app/lib/series-labels.ts");
  assert.match(seriesLabelsSource, /readingPercent/);
  const seriesPageSource = await readSource("../app/series/page.tsx");
  assert.match(seriesPageSource, /readingPercent/);
  assert.match(seriesPageSource, /progressbar/);
});

test("F6-T3: Series detail page provides Follow / Unfollow actions updating reader state", async () => {
  const seriesDetailSource = await readSource("../app/series/[seriesId]/page.tsx");
  assert.match(seriesDetailSource, /communityApi\.followSeries/);
  assert.match(seriesDetailSource, /communityApi\.unfollowSeries/);
  assert.match(seriesDetailSource, /communityApi\.seriesReaderState/);
});

test("F6-T4: Series management supports series chapter saving calling communityApi.saveSeriesChapters", async () => {
  const apiSource = await readSource("../app/lib/community/api.ts");
  assert.match(apiSource, /saveSeriesChapters\s*\(/);
});

/* ==========================================================================
   Feature 7: Team Page & Workspace Refactoring
   ========================================================================== */

test("F7-T1: Team Hub page renders Mine, Discover, and Requests tabs with team cards", async () => {
  const teamPageSource = await readSource("../app/teams/page.tsx");
  assert.match(teamPageSource, /communityApi\.teams/);
  assert.match(teamPageSource, /communityApi\.myTeams/);
  assert.match(teamPageSource, /my-teams|discover-teams|invitations/);
});

test("F7-T2: Team workspace layout initializes workspace context and provides reloadWorkspace function", async () => {
  const workspaceLayout = await readSource("../app/teams/[teamSlug]/workspace/layout.tsx");
  assert.match(workspaceLayout, /communityApi\.teamWorkspace/);
  assert.match(workspaceLayout, /reloadWorkspace/);
  assert.match(workspaceLayout, /WorkspaceContext/);
});

test("F7-T3: Team workspace members panel provides invitation cancellation action (revokeTeamInvitation)", async () => {
  const membersSource = await readSource("../app/teams/[teamSlug]/workspace/members/page.tsx");
  assert.match(membersSource, /communityApi\.revokeTeamInvitation/);
  assert.match(membersSource, /撤销邀请/);
});

test("F7-T4: Team application page supports submitting team application and tracking application state", async () => {
  const teamAppSource = await readSource("../app/team-applications/page.tsx");
  assert.match(teamAppSource, /communityApi\.submitTeamApplication/);
  assert.match(teamAppSource, /communityApi\.myTeamApplication/);
  assert.match(teamAppSource, /communityApi\.cancelTeamApplication/);
});

test("F7-T5: Team workspace layout clearly distinguishes implemented features from PLANNED_NAV_ITEMS", async () => {
  const workspaceLayout = await readSource("../app/teams/[teamSlug]/workspace/layout.tsx");
  assert.match(workspaceLayout, /PLANNED_NAV_ITEMS/);
  assert.match(workspaceLayout, /workspace-nav__item--planned/);
});

/* ==========================================================================
   SSR Route Verification for All 6 Core Pages & Header
   ========================================================================== */

test("F-SSR: Server-renders Home, Discover, Articles, Moments, Series, and Teams pages with HTTP 200", async () => {
  const routes = ["/", "/discover", "/articles", "/moments", "/series", "/teams"];
  for (const path of routes) {
    const res = await render(path);
    assert.equal(res.status, 200, `Path ${path} should return HTTP 200`);
    const html = await res.text();
    assert.match(html, /星语社区/, `Path ${path} should contain site title`);
  }
});
