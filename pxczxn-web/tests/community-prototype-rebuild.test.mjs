import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

async function source(path) {
  return readFile(new URL(`../${path}`, import.meta.url), "utf8");
}

test("renders the imported community prototype through the public entry routes", async () => {
  const [home, discover, articles, moments, series, teams, shell] = await Promise.all([
    source("app/page.tsx"),
    source("app/discover/page.tsx"),
    source("app/articles/page.tsx"),
    source("app/moments/page.tsx"),
    source("app/series/page.tsx"),
    source("app/teams/page.tsx"),
    source("app/prototype/app-shell.tsx"),
  ]);

  for (const route of [home, discover, articles, moments, series, teams]) {
    assert.match(route, /<PrototypeRoute \/>/);
  }
  for (const view of ["HomeView", "DiscoverView", "ArticlesView", "MomentsView", "SeriesView", "TeamsView"]) {
    assert.match(shell, new RegExp(view));
  }
});

test("keeps prototype navigation on real URLs", async () => {
  const context = await source("app/prototype/context/AppContext.tsx");

  assert.match(context, /usePathname/);
  assert.match(context, /useRouter/);
  assert.match(context, /router\.push\(resolveRoute/);
  assert.match(context, /\/articles\/:id/);
  assert.match(context, /\/teams\/:slug\/workspace/);
});

test("maps available content and interactions to the community API", async () => {
  const context = await source("app/prototype/context/AppContext.tsx");

  for (const apiCall of [
    "communityApi.discoverArticles",
    "communityApi.moments",
    "communityApi.series",
    "communityApi.teams",
    "communityApi.me",
    "communityApi.setLike",
    "communityApi.setFavorite",
    "communityApi.followSeries",
    "communityApi.unfollowSeries",
  ]) {
    assert.match(context, new RegExp(apiCall.replaceAll(".", "\\.")));
  }
});

test("does not keep supported community actions as prototype-only state", async () => {
  const [context, api] = await Promise.all([
    source("app/prototype/context/AppContext.tsx"),
    source("app/lib/community/api.ts"),
  ]);

  for (const apiCall of [
    "communityApi.publishMoment",
    "communityApi.notifications",
    "communityApi.readNotification",
    "communityApi.readAllNotifications",
    "communityApi.search",
    "communityApi.myBlog",
    "communityApi.creatorAnalytics",
    "communityApi.chatConversations",
    "communityApi.chatHistory",
    "communityApi.sendChatMessage",
    "communityApi.markChatRead",
    "communityApi.setBlogFollow",
    "communityApi.creatorIdeas",
    "communityApi.createCreatorIdea",
    "communityApi.createArticle",
    "communityApi.saveArticle",
    "communityApi.submitReview",
  ]) {
    assert.match(context, new RegExp(apiCall.replaceAll(".", "\\.")));
  }

  assert.match(api, /chatConversations\(limit = 20\)/);
  assert.doesNotMatch(context, /Simulated reply/);
});

test("connects chat selection and team submission actions to their persisted APIs", async () => {
  const [chat, teamDetail, workspace, settings, search, api] = await Promise.all([
    source("app/prototype/components/views/ChatView.tsx"),
    source("app/prototype/components/views/TeamDetailView.tsx"),
    source("app/prototype/components/views/TeamWorkspaceView.tsx"),
    source("app/prototype/components/views/SettingsView.tsx"),
    source("app/prototype/components/views/SearchView.tsx"),
    source("app/lib/community/api.ts"),
  ]);

  assert.match(chat, /selectedPeerId \|\| activeConversationId/);
  assert.match(teamDetail, /communityApi\.submittableArticles/);
  assert.match(teamDetail, /communityApi\.createTeamSubmission/);
  assert.match(workspace, /communityApi\.teamSubmissions/);
  assert.match(workspace, /communityApi\.decideTeamSubmission/);
  assert.match(workspace, /communityApi\.updateTeamSettings/);
  assert.match(settings, /communityApi\.updateProfile/);
  assert.match(settings, /communityApi\.updateMyBlog/);
  assert.match(search, /communityApi\.search/);
  assert.match(api, /updateProfile\(input: \{ displayName: string; bio: string \| null \}\)/);
});

test("formats IPv6 localhost as a valid backend URL", async () => {
  const client = await source("app/lib/community/client.ts");

  assert.match(client, /hostname\.replace\(\/\^\\\[\|\\\]\$\/g, ""\)/);
  assert.match(client, /browserHostname\.includes\(":"\)/);
  assert.match(client, /\$\{browserHostForUrl\}:8849/);
});
