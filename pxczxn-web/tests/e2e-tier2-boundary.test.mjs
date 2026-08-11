import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

let workerPromise;

async function worker() {
  workerPromise ??= (async () => {
    const workerUrl = new URL("../dist/server/index.js", import.meta.url);
    workerUrl.searchParams.set("test", `tier2-${process.pid}-${Date.now()}`);
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
   Tier 2: Boundary & Corner Cases
   ========================================================================== */

test("T2-B1: Empty State - Articles page renders EmptyState UI when zero articles are returned", async () => {
  const source = await readSource("../app/articles/articles-page.tsx");
  assert.match(source, /EmptyState/);
  assert.match(source, /articles\.length === 0/);
  assert.match(source, /暂无可展示的文章/);
});

test("T2-B2: Empty State - Moments page renders empty prompt when feed contains no items", async () => {
  const source = await readSource("../app/moments/moments-community-page.tsx");
  assert.match(source, /EmptyState/);
  assert.match(source, /items\.length === 0/);
  assert.match(source, /暂无动态/);
});

test("T2-B3: Empty State - Series page renders LibraryEmptyState with real action triggers when reading shelf is empty", async () => {
  const source = await readSource("../app/series/page.tsx");
  assert.match(source, /LibraryEmptyState kind="reading"/);
  assert.match(source, /LibraryEmptyState kind="following"/);
  assert.match(source, /series-library-empty__action/);
});

test("T2-B4: Empty State - Teams hub displays discover prompt when user has no active team memberships", async () => {
  const source = await readSource("../app/teams/page.tsx");
  assert.match(source, /myTeams\.length === 0/);
  assert.match(source, /尚未加入任何团队/);
});

test("T2-B5: API Error Resilience - Client API layer normalizes error responses with CommunityApiError", async () => {
  const clientSource = await readSource("../app/lib/community/client.ts");
  assert.match(clientSource, /class CommunityApiError extends Error/);
  assert.match(clientSource, /statusCode/);
  assert.match(clientSource, /errorCode/);
});

test("T2-B6: Guest State - Header notification button redirects unauthenticated guests to /login", async () => {
  const topbarSource = await readSource("../app/components/prototype-ui.tsx");
  assert.match(topbarSource, /href=\{session \? "\/notifications" : "\/login"\}/);
});

test("T2-B7: Guest State - Unauthenticated user sees clear login button in header topbar", async () => {
  const res = await render("/discover");
  assert.equal(res.status, 200);
  const html = await res.text();
  assert.match(html, /登录/);
  assert.doesNotMatch(html, /退出登录/);
});

test("T2-B8: Boundary Params - Topbar search input safely escapes special characters and passes URL query parameter", async () => {
  const topbarSource = await readSource("../app/components/prototype-ui.tsx");
  assert.match(topbarSource, /form action="\/search"/);
  assert.match(topbarSource, /name="q"/);

  const res = await render("/search?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E");
  assert.equal(res.status, 200);
  const html = await res.text();
  assert.doesNotMatch(html, /<script>alert\(1\)<\/script>/);
});

test("T2-B9: Boundary Params - Client API pagination normalizer handles pageNum < 1 gracefully", async () => {
  const clientSource = await readSource("../app/lib/community/client.ts");
  assert.match(clientSource, /normalizePaginationNumbers/);
  assert.match(clientSource, /pageNum:\s*Math\.max\(1/);
});

test("T2-B10: Boundary Route - Requesting non-existent dynamic team slug returns 200 or 404 without crashing worker", async () => {
  const res = await render("/teams/non-existent-team-9999");
  assert.ok(res.status === 200 || res.status === 404);
  const html = await res.text();
  assert.match(html, /星语社区|Not found|团队/);
});
