import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

async function source(path) {
  return readFile(new URL(path, import.meta.url), "utf8");
}

test("discover is the only asymmetric editorial mosaic", async () => {
  const discover = await source("../app/discover/discover-page.tsx");

  assert.match(discover, /discover-editorial-mosaic/);
  assert.match(discover, /discover-feature-grid/);
  assert.match(discover, /discover-serial-rail/);
  assert.match(discover, /discover-community-split/);
  assert.match(discover, /discover-live-rail/);
  assert.match(discover, /精选文章/);
  assert.match(discover, /连载精选/);
  assert.match(discover, /正在发生/);
  assert.doesNotMatch(discover, /searchInput|handleSearch|placeholder="搜索文章/);
  assert.doesNotMatch(discover, /篇公开文章/);
  assert.doesNotMatch(discover, /--color-|var\(--border,/);
});

test("articles keeps a dense index with a right rail and explicit request lifecycle", async () => {
  const articles = await source("../app/articles/articles-page.tsx");

  assert.match(articles, /content-index-layout content-index-layout--right-rail/);
  assert.match(articles, /content-index-toolbar/);
  assert.match(articles, /content-index-pagination/);
  assert.match(articles, /normalizeSort/);
  assert.match(articles, /setLoading\(true\)/);
  assert.match(articles, /setError\(null\)/);
});

test("moments keeps feed and detail together without placeholder controls", async () => {
  const moments = await source("../app/moments/moments-community-page.tsx");

  assert.match(moments, /moments-page prototype-moments/);
  assert.match(moments, /moments-sidebar/);
  assert.match(moments, /moments-feed/);
  assert.match(moments, /moments-right-panel/);
  assert.match(moments, /topicHref/);
  assert.doesNotMatch(moments, /href="#"/);
  assert.doesNotMatch(moments, />换一换</);
  assert.doesNotMatch(moments, /稍后阅读/);
});

test("series is a reading shelf followed by a separated catalog", async () => {
  const series = await source("../app/series/page.tsx");

  assert.match(series, /series-reading-space/);
  assert.match(series, /series-reading-shelf/);
  assert.match(series, /series-following-list/);
  assert.match(series, /series-catalog-toolbar/);
  assert.match(series, /series-catalog-grid/);
  assert.match(series, /UserTopbar title="系列"/);
  assert.match(series, /我的书架/);
  assert.match(series, /我的追更/);
  assert.doesNotMatch(series, /series-layout__sidebar/);
  assert.doesNotMatch(series, /我的收藏|>推荐<|>热门</);
});

test("six page shells keep distinct visual accents and the library empty state has a real next action", async () => {
  const [series, css] = await Promise.all([
    source("../app/series/page.tsx"),
    source("../app/globals.css"),
  ]);

  assert.match(series, /LibraryEmptyState kind="reading"/);
  assert.match(series, /LibraryEmptyState kind="following"/);
  assert.match(series, /series-library-empty__action/);
  assert.match(series, /id="series-catalog"/);

  for (const selector of [
    ".home-dashboard-section::before",
    ".discover-feature-card--lead::after",
    ".content-index-toolbar::before",
    ".moments-feed::before",
    ".series-library-empty--reading",
    ".teams-hub__header::before",
  ]) {
    assert.match(css, new RegExp(selector.replace(/[.*+?^${}()|[\\]\\]/g, "\\$&")));
  }
});

test("strong visual redesign removes empty chrome and gives each core page a content-first treatment", async () => {
  const [discover, articles, series, css] = await Promise.all([
    source("../app/discover/discover-page.tsx"),
    source("../app/articles/articles-page.tsx"),
    source("../app/series/page.tsx"),
    source("../app/globals.css"),
  ]);

  assert.match(discover, /tags\.length > 0 &&/);
  assert.match(discover, /discover-editorial-cover/);
  assert.match(articles, /content-index-row__number/);
  assert.match(articles, /CONTENT INDEX/);
  assert.match(series, /series-library-stage/);

  for (const selector of [
    ".discover-editorial-cover",
    ".content-index-row__number",
    ".series-library-stage",
    ".home-dashboard-actions::before",
    ".moments-sidebar::after",
    ".teams-hub__tabs::after",
  ]) {
    assert.match(css, new RegExp(selector.replace(/[.*+?^${}()|[\\]\\]/g, "\\$&")));
  }
});

test("team workspace owns navigation and labels planned capabilities honestly", async () => {
  const workspace = await source("../app/teams/[teamSlug]/workspace/layout.tsx");

  assert.match(workspace, /label: "系列"/);
  assert.match(workspace, /PLANNED_NAV_ITEMS/);
  assert.match(workspace, /任务/);
  assert.match(workspace, /文档/);
  assert.match(workspace, /workspace-nav__item--planned/);
  assert.doesNotMatch(workspace, /href: "\/workspace\/(tasks|docs)"/);
});

test("shared navigation calls the product series and submits only supported search", async () => {
  const topbar = await source("../app/components/prototype-ui.tsx");

  assert.match(topbar, /\{ href: "\/series", label: "系列", Icon: LibraryBig \}/);
  assert.match(topbar, /action="\/search"/);
  assert.match(topbar, /name="q"/);
  assert.match(topbar, /placeholder="搜索文章和动态"/);
  assert.doesNotMatch(topbar, /搜索文章、动态和团队/);
});
