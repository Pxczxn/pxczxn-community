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
  const [context, shell] = await Promise.all([
    source("app/prototype/context/AppContext.tsx"),
    source("app/prototype/app-shell.tsx"),
  ]);

  assert.match(context, /usePathname/);
  assert.match(context, /useRouter/);
  assert.match(context, /router\.push\(resolveRoute/);
  assert.match(context, /\/articles\/:id/);
  assert.match(context, /\/teams\/:slug\/workspace/);
  assert.match(shell, /case '\/moments\/:id'/);
  assert.match(shell, /case '\/profile\/:slug'/);
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

test("does not display unconfirmed interaction updates as persisted", async () => {
  const [context, moments] = await Promise.all([
    source("app/prototype/context/AppContext.tsx"),
    source("app/prototype/components/views/MomentsView.tsx"),
  ]);

  assert.match(context, /communityApi\.setLike\([\s\S]*?\.then\(\(relationship\) =>/);
  assert.match(context, /communityApi\.setFavorite\([\s\S]*?\.then\(\(relationship\) =>/);
  assert.match(context, /communityApi\.followSeries\(seriesId\)[\s\S]*?\.then\(\(readerState\) =>/);
  assert.match(context, /communityApi\.unfollowSeries\(seriesId\)[\s\S]*?\.then\(\(readerState\) =>/);
  assert.match(context, /addMoment: \(content: string, type\?: Moment\['momentType'\], link\?: string\) => Promise<boolean>/);
  assert.match(moments, /const published = await addMoment/);
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
  const [chat, navbar, teamDetail, workspace, settings, search, articleDetail, moments, personalSpace, governance, editor, api] = await Promise.all([
    source("app/prototype/components/views/ChatView.tsx"),
    source("app/prototype/components/layout/Navbar.tsx"),
    source("app/prototype/components/views/TeamDetailView.tsx"),
    source("app/prototype/components/views/TeamWorkspaceView.tsx"),
    source("app/prototype/components/views/SettingsView.tsx"),
    source("app/prototype/components/views/SearchView.tsx"),
    source("app/prototype/components/views/ArticleDetailView.tsx"),
    source("app/prototype/components/views/MomentsView.tsx"),
    source("app/prototype/components/views/PersonalSpaceView.tsx"),
    source("app/prototype/components/views/GovernanceView.tsx"),
    source("app/prototype/components/views/EditorView.tsx"),
    source("app/lib/community/api.ts"),
  ]);

  assert.match(chat, /selectedPeerId \|\| activeConversationId/);
  assert.match(navbar, /sendChatMessage\(selectedChatConv\.peerUser\.id, chatInputText\.trim\(\)\)/);
  assert.match(navbar, /selectConversation\(conv\.peerUser\.id\)/);
  assert.doesNotMatch(navbar, /chatMessages\[selectedChatConv\.id\]/);
  assert.match(teamDetail, /communityApi\.submittableArticles/);
  assert.match(teamDetail, /communityApi\.createTeamSubmission/);
  assert.match(workspace, /communityApi\.teamSubmissions/);
  assert.match(workspace, /communityApi\.decideTeamSubmission/);
  assert.match(workspace, /communityApi\.updateTeamSettings/);
  assert.match(settings, /communityApi\.updateProfile/);
  assert.match(settings, /communityApi\.updateMyBlog/);
  assert.match(settings, /communityApi\.updateBlogSettings/);
  assert.match(settings, /communityApi\.likeListPrivacy/);
  assert.match(settings, /communityApi\.updateLikeListPrivacy/);
  assert.match(settings, /communityApi\.uploadFile/);
  assert.match(settings, /communityApi\.preferences/);
  assert.match(settings, /communityApi\.updatePreferences/);
  assert.match(api, /preferences\(\)/);
  assert.match(api, /updatePreferences\(settings: Record<string, unknown>\)/);
  assert.match(search, /communityApi\.search/);
  assert.match(articleDetail, /communityApi\.comments/);
  assert.match(articleDetail, /communityApi\.createComment/);
  assert.match(moments, /communityApi\.comments\('MOMENT', activeMoment\.id\)/);
  assert.match(moments, /setMomentComments/);
  for (const apiCall of [
    "communityApi.favoriteFolders",
    "communityApi.favoriteItems",
    "communityApi.myLikes",
    "communityApi.socialCounts",
    "communityApi.myFollowing",
    "communityApi.myFollowers",
    "communityApi.myReadingSeries",
    "communityApi.myTeamSubmissions",
  ]) {
    assert.match(personalSpace, new RegExp(apiCall.replaceAll(".", "\\.")));
  }
  assert.match(governance, /communityApi\.createReport/);
  assert.match(editor, /saveArticleDraft/);
  assert.match(settings, /communityApi\.myBlocks/);
  assert.match(settings, /communityApi\.removeBlock/);
  assert.match(settings, /communityApi\.changePassword/);
  assert.match(api, /updateProfile\(input: \{ displayName: string; bio: string \| null \}\)/);
});

test("formats IPv6 localhost as a valid backend URL", async () => {
  const client = await source("app/lib/community/client.ts");

  assert.match(client, /hostname\.replace\(\/\^\\\[\|\\\]\$\/g, ""\)/);
  assert.match(client, /browserHostname\.includes\(":"\)/);
  assert.match(client, /\$\{browserHostForUrl\}:8849/);
});

test("registers creator persistence mappers required by connected creator actions", async () => {
  const application = await readFile(
    new URL("../../pxczxn-backend/pxczxn-starter/src/main/java/top/pxczxn/community/PxczxnCommunityApplication.java", import.meta.url),
    "utf8",
  );

  assert.match(application, /"top\.pxczxn\.community\.creator\.persistence"/);
});

test("does not seed community runtime state from prototype mock data", async () => {
  const [context, settings, articleDetail, teamDetail, teamWorkspace, notifications] = await Promise.all([
    source("app/prototype/context/AppContext.tsx"),
    source("app/prototype/components/views/SettingsView.tsx"),
    source("app/prototype/components/views/ArticleDetailView.tsx"),
    source("app/prototype/components/views/TeamDetailView.tsx"),
    source("app/prototype/components/views/TeamWorkspaceView.tsx"),
    source("app/prototype/components/views/NotificationsView.tsx"),
  ]);

  assert.doesNotMatch(context, /from ['"]\.\.\/data\/mockData['"]/);
  assert.doesNotMatch(context, /useState<Article\[\]>\(mockArticles\)/);
  assert.doesNotMatch(settings, /Active Sessions Mock Data/);
  assert.doesNotMatch(settings, /useState\([^\n]*images\.unsplash\.com/);
  assert.doesNotMatch(settings, /useState\([^\n]*pxczxn\.community/);
  assert.doesNotMatch(articleDetail, /routeParams\.id \|\|/);
  assert.doesNotMatch(teamDetail, /routeParams\.slug \|\|/);
  assert.doesNotMatch(teamWorkspace, /routeParams\.slug \|\|/);
  assert.doesNotMatch(notifications, /starry-core-dev|art-101/);
});

test("derives the blog portal from live community content instead of a static blog list", async () => {
  const blogs = await source("app/prototype/components/views/BlogsView.tsx");

  assert.doesNotMatch(blogs, /const blogsList = \[/);
  assert.match(blogs, /useMemo/);
  assert.match(blogs, /teams/);
});

test("loads discovery tags from the community API instead of prototype seed values", async () => {
  const discover = await source("app/prototype/components/views/DiscoverView.tsx");

  assert.doesNotMatch(discover, /const hotTags = \[/);
  assert.match(discover, /communityApi\.getHotTopics/);
});

test("does not present unsupported security features as active services", async () => {
  const settings = await source("app/prototype/components/views/SettingsView.tsx");

  assert.match(settings, /disabled=\{sessions\.length === 0\}/);
  assert.match(settings, /NOT AVAILABLE/);
  assert.doesNotMatch(settings, /sk-••/);
});

test("does not simulate account export as a completed backend operation", async () => {
  const settings = await source("app/prototype/components/views/SettingsView.tsx");

  assert.doesNotMatch(settings, /setTimeout\(\(\) => setExportingData/);
  assert.match(settings, /NOT AVAILABLE/);
});
