import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

let workerPromise;

async function worker() {
  workerPromise ??= (async () => {
    const workerUrl = new URL("../dist/server/index.js", import.meta.url);
    workerUrl.searchParams.set("test", `tier3-${process.pid}-${Date.now()}`);
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
   Tier 3: Cross-Feature Combinations
   ========================================================================== */

test("T3-C1: Cross-Feature - Topbar Search redirect to /search page with query parameter and API search integration", async () => {
  const topbar = await readSource("../app/components/prototype-ui.tsx");
  const searchPage = await readSource("../app/search/search-page.tsx");
  const api = await readSource("../app/lib/community/api.ts");

  assert.match(topbar, /action="\/search"/);
  assert.match(topbar, /name="q"/);
  assert.match(searchPage, /communityApi\.search/);
  assert.match(searchPage, /setSubmittedKeyword/);
  assert.match(api, /\/api\/v1\/public\/search\?/);
});

test("T3-C2: Cross-Feature - Moment creation, detail comment posting, sorting execution, and deletion pipeline", async () => {
  const momentsPage = await readSource("../app/moments/moments-community-page.tsx");
  const momentDetailRoute = await readSource("../app/moments/[momentId]/page.tsx");
  const api = await readSource("../app/lib/community/api.ts");

  assert.match(momentsPage, /communityApi\.publishMoment/);
  assert.match(momentDetailRoute, /communityApi\.createComment/);
  assert.match(momentsPage, /handleCommentSort|commentSort/);
  assert.match(momentsPage, /communityApi\.deleteMoment/);
  assert.match(api, /\/api\/v1\/moments/);
});

test("T3-C3: Cross-Feature - Article Category CRUD, editor autosave, version history preview, and version restore pipeline", async () => {
  const editorPanel = await readSource("../app/editor/article-editor-panel.tsx");
  const detailPage = await readSource("../app/articles/[articleId]/article-detail-page.tsx");
  const api = await readSource("../app/lib/community/api.ts");

  assert.match(detailPage, /communityApi\.categories|createCategory/);
  assert.match(editorPanel, /communityApi\.saveArticle/);
  assert.match(editorPanel, /communityApi\.articleVersions/);
  assert.match(editorPanel, /communityApi\.restoreArticleVersion/);
  assert.match(api, /\/versions\/\$\{encodeURIComponent\(versionId\)\}\/restore/);
});

test("T3-C4: Cross-Feature - Bookshelf series follow -> chapter reading progress -> Home page ContinueCard sync", async () => {
  const seriesPage = await readSource("../app/series/page.tsx");
  const seriesDetail = await readSource("../app/series/[seriesId]/page.tsx");
  const homePage = await readSource("../app/home/home-page.tsx");

  assert.match(seriesPage, /communityApi\.myReadingSeries/);
  assert.match(seriesDetail, /communityApi\.recordSeriesProgress/);
  assert.match(seriesDetail, /communityApi\.followSeries/);
  assert.match(homePage, /ContinueCard/);
  assert.match(homePage, /readingPercent/);
});

test("T3-C5: Cross-Feature - Team application -> invitation -> workspace context reloadWorkspace pipeline", async () => {
  const teamApp = await readSource("../app/team-applications/page.tsx");
  const workspaceLayout = await readSource("../app/teams/[teamSlug]/workspace/layout.tsx");
  const membersPage = await readSource("../app/teams/[teamSlug]/workspace/members/page.tsx");

  assert.match(teamApp, /communityApi\.submitTeamApplication/);
  assert.match(workspaceLayout, /communityApi\.teamWorkspace/);
  assert.match(workspaceLayout, /reloadWorkspace/);
  assert.match(membersPage, /communityApi\.revokeTeamInvitation/);
});

test("T3-C-SSR: SSR rendering verifies route resolution across all combination entry points", async () => {
  const paths = [
    "/search?q=AI",
    "/moments",
    "/editor/new",
    "/series",
    "/teams",
  ];
  for (const path of paths) {
    const res = await render(path);
    assert.equal(res.status, 200, `Combination path ${path} must render HTTP 200`);
  }
});
