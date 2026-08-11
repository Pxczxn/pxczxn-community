import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const pages = [
  ["../app/home/home-page.tsx", "communityApi.followingFeed"],
  ["../app/articles/articles-page.tsx", "communityApi.discoverRankedArticles"],
  ["../app/discover/discover-page.tsx", "communityApi.discover"],
  ["../app/moments/moments-community-page.tsx", "communityApi.moments"],
  ["../app/series/page.tsx", "communityApi.series"],
  ["../app/teams/page.tsx", "communityApi.teams"],
];

test("core community pages render API data instead of fixed sample content", async () => {
  for (const [path, apiCall] of pages) {
    const source = await readFile(new URL(path, import.meta.url), "utf8");
    assert.match(source, new RegExp(apiCall.replaceAll(".", "\\.")), path);
    assert.doesNotMatch(source, /images\.unsplash\.com/, path);
  }
});

test("home defers browser session resolution until after the first render", async () => {
  const source = await readFile(
    new URL("../app/home/home-page.tsx", import.meta.url),
    "utf8",
  );

  assert.doesNotMatch(source, /const session = readSession\(\)/);
  assert.match(source, /setSession\(readSession\(\)\)/);
  assert.match(source, /session === undefined/);
});

test("submission detail does not invent a record when articleId is absent", async () => {
  const source = await readFile(
    new URL("../app/submissions/ai-agent/submission-detail-panel.tsx", import.meta.url),
    "utf8",
  );

  assert.doesNotMatch(source, /2024-04-12T14:32:00/);
  assert.doesNotMatch(source, /基于向量数据库的企业知识库构建实践/);
});

test("detail routes accept backend canonical URLs and dynamic params", async () => {
  const series = await readFile(
    new URL("../app/series/[seriesId]/page.tsx", import.meta.url),
    "utf8",
  );
  assert.match(series, /Promise\.resolve\(params\)/);

  const canonicalArticleRoute = await readFile(
    new URL("../app/[slug]/[articleId]/[articleSlug]/page.tsx", import.meta.url),
    "utf8",
  );
  assert.match(canonicalArticleRoute, /<ArticleDetailPage articleId=\{articleId\} \/>/);
});

test("search defers URL query hydration until after the first render", async () => {
  const source = await readFile(
    new URL("../app/search/search-page.tsx", import.meta.url),
    "utf8",
  );
  assert.doesNotMatch(source, /useState\(\(\) => initialKeyword\(\)\)/);
  assert.match(source, /setSubmittedKeyword\(nextKeyword\)/);
});

test("team application avoids unsupported styled-jsx DOM attributes", async () => {
  const source = await readFile(
    new URL("../app/team-applications/page.tsx", import.meta.url),
    "utf8",
  );
  assert.doesNotMatch(source, /<style jsx>/);
});
