import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

let workerPromise;

async function worker() {
  workerPromise ??= (async () => {
    const workerUrl = new URL("../dist/server/index.js", import.meta.url);
    workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
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

test("server-renders the community login entry instead of the starter skeleton", async () => {
  const response = await render("/login");
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /登录 \/ 注册/);
  assert.match(html, /星语社区/);
  assert.match(html, /发现有价值的内容/);
  assert.match(html, /请输入邮箱地址/);
  assert.match(html, /暂不登录，先浏览社区内容/);
  assert.doesNotMatch(html, /Your site is taking shape|Codex is working/);
});

test("server-renders formal community routes without fixed prototype content", async () => {
  const routes = [
    ["/discover", "发现值得阅读的内容"],
    ["/articles", "按公开时间浏览社区文章"],
    ["/teams", "团队博客"],
    ["/teams/ai-explorers", "团队"],
    ["/workspace/team", "团队工作台"],
    ["/submissions/ai-agent", "当前不会展示虚构的投稿审核记录"],
    ["/collaboration/articles/agent-patterns", "文章共创"],
    ["/moments/agent-architecture", "星语社区"],
    ["/me/favorites", "我的文件夹"],
    ["/notifications", "通知中心"],
    ["/settings", "主题设置"],
    ["/editor/new", "创作中心"],
  ];
  routes[5][1] = "外部投稿";

  for (const [path, marker] of routes) {
    const response = await render(path);
    assert.equal(response.status, 200, path);
    assert.match(await response.text(), new RegExp(marker), path);
  }
});

test("keeps M2.5 discovery, login and dynamic-route semantics in source", async () => {
  const [home, auth, discover, topbar, workspace] = await Promise.all([
    readFile(new URL("../app/page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/login/auth-panel.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/discover/discover-page.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/components/prototype-ui.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/workspace/team/page.tsx", import.meta.url), "utf8"),
  ]);

  assert.match(home, /redirect\("\/discover"\)/);
  assert.match(auth, /new URLSearchParams\(window\.location\.search\)\.get\("returnTo"\)/);
  assert.match(auth, /: "\/discover"/);
  assert.match(discover, /communityApi\.discoverArticles/);
  assert.match(discover, /communityApi\.moments/);
  assert.match(discover, /communityApi\.tags/);
  assert.match(discover, /communityApi\.myFollowing/);
  assert.match(discover, /不使用智能推荐算法/);
  for (const path of ["/discover", "/articles", "/moments", "/series", "/teams", "/tags"]) {
    assert.match(topbar, new RegExp(path.replaceAll("/", "\\/")));
  }
  assert.doesNotMatch(topbar, /ai-explorers|agent-architecture|agent-patterns/);
  assert.match(workspace, /M3 团队协作创作/);
  assert.match(workspace, /communityApi\.teamWorkspace/);
  assert.doesNotMatch(workspace, /待处理投稿|AI探索者团队/);
});

test("keeps M1 user flows connected to the real community API", async () => {
  const [api, auth, editor, submission, settings, css] = await Promise.all([
    readFile(new URL("../app/lib/community-api.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/login/auth-panel.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/editor/article-editor-panel.tsx", import.meta.url), "utf8"),
    readFile(
      new URL("../app/submissions/ai-agent/submission-detail-panel.tsx", import.meta.url),
      "utf8",
    ),
    readFile(new URL("../app/settings/settings-panel.tsx", import.meta.url), "utf8"),
    readFile(new URL("../app/globals.css", import.meta.url), "utf8"),
  ]);

  assert.match(api, /pxczxn-community-session/);
  assert.match(api, /\/api\/v1\/auth\/register/);
  assert.match(api, /\/api\/v1\/auth\/login/);
  assert.match(api, /\/api\/v1\/public\/articles/);
  assert.match(api, /\/submit-review/);
  assert.match(api, /\/publish/);
  assert.match(auth, /communityApi\.register/);
  assert.match(auth, /communityApi\.login/);
  assert.match(editor, /communityApi\.saveArticle/);
  assert.match(editor, /communityApi\.submitReview/);
  assert.match(submission, /communityApi\.reviewStatus/);
  assert.match(settings, /communityApi\.updateMyBlog/);
  assert.match(css, /:root\[data-theme="light"\]/);
  assert.match(css, /:root\[data-theme="dark"\]/);
  assert.match(css, /:root\[data-theme="starry"\]/);
  assert.match(css, /@media \(max-width: 767px\)/);
  assert.match(css, /prefers-reduced-motion/);
});

test("keeps M2 moments, social relationships and notifications on real APIs", async () => {
  const [api, moments, social, notifications, topbar] = await Promise.all([
    readFile(new URL("../app/lib/community-api.ts", import.meta.url), "utf8"),
    readFile(new URL("../app/moments/moments-community-page.tsx", import.meta.url), "utf8"),
    readFile(
      new URL("../app/me/favorites/social-center-page.tsx", import.meta.url),
      "utf8",
    ),
    readFile(
      new URL("../app/notifications/notifications-page.tsx", import.meta.url),
      "utf8",
    ),
    readFile(new URL("../app/components/prototype-ui.tsx", import.meta.url), "utf8"),
  ]);

  assert.match(api, /\/api\/v1\/moments/);
  assert.match(api, /\/api\/v1\/social\/me\/favorite-folders/);
  assert.match(api, /\/api\/v1\/social\/me\/following/);
  assert.match(api, /\/api\/v1\/social\/me\/followers/);
  assert.match(api, /\/api\/v1\/notifications/);
  assert.match(moments, /communityApi\.publishMoment/);
  assert.match(moments, /communityApi\.createComment/);
  assert.match(moments, /communityApi\.setLike/);
  assert.match(moments, /communityApi\.setFavorite/);
  assert.match(social, /communityApi\.favoriteItems/);
  assert.match(social, /communityApi\.myLikes/);
  assert.match(social, /communityApi\.myFollowing/);
  assert.match(social, /communityApi\.myFollowers/);
  assert.match(notifications, /communityApi\.readNotification/);
  assert.match(notifications, /communityApi\.readAllNotifications/);
  assert.match(topbar, /unreadNotifications/);
  assert.match(topbar, /href=\{session \? "\/notifications" : "\/login"\}/);
});
