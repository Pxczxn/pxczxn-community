import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

test("home keeps a personal dashboard skeleton and uses only related real data", async () => {
  const [home, api, css] = await Promise.all([
    readFile(new URL("../app/home/home-page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/lib/community/api.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/globals.css", import.meta.url), "utf8"),
  ]);

  assert.match(home, /className="home-dashboard"/);
  assert.match(home, /className="home-dashboard__main"/);
  assert.match(home, /className="home-personal-rail"/);
  assert.match(home, /aria-label="与你相关"/);
  assert.match(home, /管理我的系列/);
  assert.match(home, /communityApi\.notifications\(/);
  assert.match(home, /communityApi\.followingFeed\(/);
  assert.match(home, /communityApi\.myReadingSeries\(/);
  assert.match(home, /communityApi\.myTeams\(/);
  assert.match(home, /communityApi\.unreadNotifications\(/);
  assert.doesNotMatch(home, /communityApi\.moments\(/);
  assert.doesNotMatch(home, /return "#"/);

  assert.match(api, /\/api\/v1\/notifications\?/);
  assert.match(api, /\/api\/v1\/social\/me\/following-feed/);
  assert.match(api, /\/api\/v1\/me\/series\/reading/);
  assert.match(api, /\/api\/v1\/teams\/me/);

  assert.match(css, /\.home-dashboard\s*\{/);
  assert.match(css, /\.home-personal-rail\s*\{/);
  assert.match(css, /var\(--bg-surface\)/);
  assert.match(css, /var\(--border-default\)/);
});

test("home resolves the browser session after hydration instead of reading localStorage during render", async () => {
  const home = await readFile(new URL("../app/home/home-page.tsx", import.meta.url), "utf8");

  assert.match(home, /setSession\(readSession\(\)\)/);
  assert.doesNotMatch(home, /const session = readSession\(\)/);
  assert.match(home, /session === undefined/);
});
