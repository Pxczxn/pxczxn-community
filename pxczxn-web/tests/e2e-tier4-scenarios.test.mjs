import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

let workerPromise;

async function worker() {
  workerPromise ??= (async () => {
    const workerUrl = new URL("../dist/server/index.js", import.meta.url);
    workerUrl.searchParams.set("test", `tier4-${process.pid}-${Date.now()}`);
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
   Tier 4: Real-World Application Scenarios
   ========================================================================== */

test("T4-S1: Real-World Scenario 1 - Unauthenticated Guest Reader Discovery & Deep Reading Journey", async () => {
  // Step 1: Render Discover Page
  const discoverRes = await render("/discover");
  assert.equal(discoverRes.status, 200);
  const discoverHtml = await discoverRes.text();
  assert.match(discoverHtml, /由编辑视角组织主题/);

  // Step 2: Render Articles Index Page
  const articlesRes = await render("/articles");
  assert.equal(articlesRes.status, 200);
  const articlesHtml = await articlesRes.text();
  assert.match(articlesHtml, /按公开时间浏览社区文章|CONTENT INDEX/);

  // Step 3: Verify source contracts for guest discovery to article detail navigation
  const discoverSource = await readSource("../app/discover/discover-page.tsx");
  const articlesSource = await readSource("../app/articles/articles-page.tsx");
  const articleDetailSource = await readSource("../app/articles/[articleId]/article-detail-page.tsx");

  assert.match(discoverSource, /communityApi\.discoverRankedArticles/);
  assert.match(articlesSource, /communityApi\.discoverRankedArticles/);
  assert.match(articleDetailSource, /communityApi\.publicArticle/);
  assert.match(articleDetailSource, /communityApi\.articleSeriesContext/);
});

test("T4-S2: Real-World Scenario 2 - Content Creator & Team Workspace Collaboration Journey", async () => {
  // Step 1: Render Home Page Dashboard
  const homeRes = await render("/");
  assert.equal(homeRes.status, 200);

  // Step 2: Render Editor Page
  const editorRes = await render("/editor/new");
  assert.equal(editorRes.status, 200);
  const editorHtml = await editorRes.text();
  assert.match(editorHtml, /创作中心/);

  // Step 3: Render Team Hub Page
  const teamsRes = await render("/teams");
  assert.equal(teamsRes.status, 200);
  const teamsHtml = await teamsRes.text();
  assert.match(teamsHtml, /团队/);

  // Step 4: Verify creator & workspace source contracts
  const editorSource = await readSource("../app/editor/article-editor-panel.tsx");
  const workspaceSource = await readSource("../app/teams/[teamSlug]/workspace/layout.tsx");
  const membersSource = await readSource("../app/teams/[teamSlug]/workspace/members/page.tsx");

  assert.match(editorSource, /communityApi\.saveArticle/);
  assert.match(editorSource, /communityApi\.submitReview/);
  assert.match(workspaceSource, /communityApi\.teamWorkspace/);
  assert.match(workspaceSource, /reloadWorkspace/);
  assert.match(membersSource, /communityApi\.revokeTeamInvitation/);
});

test("T4-S3: Real-World Scenario 3 - Interactive Community Member Social & Bookshelf Reader Journey", async () => {
  // Step 1: Render Moments Page
  const momentsRes = await render("/moments");
  assert.equal(momentsRes.status, 200);

  // Step 2: Render Series Bookshelf Page
  const seriesRes = await render("/series");
  assert.equal(seriesRes.status, 200);
  const seriesHtml = await seriesRes.text();
  assert.match(seriesHtml, /我的书架/);

  // Step 3: Verify Theme switcher and social bookshelf contracts
  const themeSource = await readSource("../app/components/theme-bootstrap.tsx");
  const topbarSource = await readSource("../app/components/prototype-ui.tsx");
  const momentsSource = await readSource("../app/moments/moments-community-page.tsx");
  const seriesSource = await readSource("../app/series/page.tsx");

  assert.match(themeSource, /setTheme/);
  assert.match(topbarSource, /toggleTheme/);
  assert.match(momentsSource, /communityApi\.publishMoment/);
  assert.match(momentsSource, /communityApi\.createComment/);
  assert.match(seriesSource, /communityApi\.myReadingSeries/);
  assert.match(seriesSource, /ContinueCard/);
});
