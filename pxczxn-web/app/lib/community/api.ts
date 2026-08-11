import { communityRequest } from "./client";
import type { CommunitySession } from "./session";
import type { CommunityFile } from "./files";
import type {
  CurrentCommunityUser,
  BlogSettings,
  PersonalBlog,
  PublicBlog,
  PublicArticlePage,
  PublicArticleDetail,
  PublicDiscoveryPage,
  DiscoverySort,
  UnifiedSearchType,
  UnifiedSearchPage,
  EditorialCollection,
  BlogCategory,
  PlatformTag,
  ArticleEditor,
  ArticleReviewStatus,
  CommunityContentType,
  LikeRelationship,
  FavoriteRelationship,
  FavoriteFolder,
  FavoriteContentPage,
  LikedContentPage,
  SocialCounts,
  SocialProfilePage,
  FollowingFeedPage,
  CreatorAnalytics,
  MomentFeedFilter,
  MomentPage,
  Moment,
  CommentPage,
  CommunityComment,
  NotificationCategory,
  NotificationPage,
  UnreadNotificationCount,
  CommunityReport,
  CommunityBlock,
  AppealContext,
  CommunityAppeal,
  CommunitySanction,
  SubmitTeamApplicationInput,
  TeamApplication,
  TeamInvitation,
  TeamSummary,
  Series,
  SeriesChapter,
  SeriesReaderState,
  ArticleSeriesContext,
  TeamSubmission,
  SubmittableArticle,
  ArticleCollaboration,
  TeamPortal,
  TeamWorkspace,
  MyTeam,
  TeamDashboard,
  TeamActivity,
  TeamArticleBrief,
  TeamMemberView,
  CommunityChatMessage,
  CommunityChatConversation,
  CommunityCreatorIdea,
  BlogFollowRelationship,
  AccountEnforcementCase,
  AccountEnforcementAppeal,
  ArticleVersionSummary,
  ArticleVersionPage,
  ArticleVersionDetail,
  RestoreArticleVersionInput,
  CreateCategoryInput,
  CreateBlogCategoryInput,
  UpdateCategoryInput,
  UpdateBlogCategoryInput,
  CreateFavoriteFolderInput,
  UpdateFavoriteFolderInput,
  UpdateFolderInput,
  MomentDeletionResponse,
  CommentModerationResponse,
} from "./types";

export const communityApi = {
  checkUsername(username: string) {
    return communityRequest<{ available: boolean }>(
      `/api/v1/auth/check-username?username=${encodeURIComponent(username)}`,
      {},
      false,
    );
  },
  checkEmail(email: string) {
    return communityRequest<{ available: boolean }>(
      `/api/v1/auth/check-email?email=${encodeURIComponent(email)}`,
      {},
      false,
    );
  },
  register(input: {
    username: string;
    email: string;
    password: string;
    displayName?: string;
  }) {
    return communityRequest<{
      userId: string;
      blogId: string;
      username: string;
      blogSlug: string;
    }>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(input),
    }, false);
  },
  login(input: { email: string; password: string; rememberMe?: boolean }) {
    return communityRequest<CommunitySession>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(input),
    }, false);
  },
  sendPasswordResetCode(email: string) {
    return communityRequest<void>("/api/v1/auth/forgot-password/send-code", {
      method: "POST",
      body: JSON.stringify({ email }),
    }, false);
  },
  resetPassword(email: string, code: string, newPassword: string) {
    return communityRequest<void>("/api/v1/auth/forgot-password/reset", {
      method: "POST",
      body: JSON.stringify({ email, code, newPassword }),
    }, false);
  },
  logout() {
    return communityRequest<void>("/api/v1/auth/logout", { method: "POST" });
  },
  me() {
    return communityRequest<CurrentCommunityUser>("/api/v1/account/me");
  },
  updateProfile(input: { displayName: string; bio: string | null }) {
    return communityRequest<CurrentCommunityUser>("/api/v1/account/me", {
      method: "PATCH",
      body: JSON.stringify(input),
    });
  },
  changePassword(currentPassword: string, newPassword: string) {
    return communityRequest<void>("/api/v1/account/password", {
      method: "POST",
      body: JSON.stringify({ currentPassword, newPassword }),
    });
  },
  myAccountEnforcements() {
    return communityRequest<AccountEnforcementCase[]>("/api/v1/account-enforcements/me");
  },
  myAccountEnforcementAppeals() {
    return communityRequest<AccountEnforcementAppeal[]>("/api/v1/account-enforcements/appeals/me");
  },
  uploadFile(file: File) {
    const formData = new FormData();
    formData.append("file", file);
    return communityRequest<CommunityFile>("/api/v1/files", { method: "POST", body: formData });
  },
  appealAccountEnforcement(caseId: string, input: { statement: string; evidenceFileIds?: string[] }) {
    return communityRequest<AccountEnforcementAppeal>(`/api/v1/account-enforcements/${encodeURIComponent(caseId)}/appeals`, { method: "POST", body: JSON.stringify(input) });
  },
  myBlog() {
    return communityRequest<PersonalBlog>("/api/v1/blogs/me");
  },
  updateMyBlog(input: Partial<{
    name: string;
    summary: string;
    avatarFileId: string;
    clearAvatar: boolean;
    backgroundFileId: string;
    clearBackground: boolean;
  }>) {
    return communityRequest<PersonalBlog>("/api/v1/blogs/me", {
      method: "PATCH",
      body: JSON.stringify(input),
    });
  },
  updateBlogSettings(input: Partial<BlogSettings>) {
    return communityRequest<BlogSettings>("/api/v1/blogs/me/settings", {
      method: "PATCH",
      body: JSON.stringify(input),
    });
  },
  publicBlog(slug: string) {
    return communityRequest<PublicBlog>(
      `/api/v1/public/blogs/${encodeURIComponent(slug)}`,
      {},
      false,
    );
  },
  publicArticles(slug: string, pageNum = 1, pageSize = 20, categorySlug?: string) {
    const query = new URLSearchParams({
      pageNum: String(pageNum),
      pageSize: String(pageSize),
    });
    if (categorySlug) query.set("categorySlug", categorySlug);
    return communityRequest<PublicArticlePage>(
      `/api/v1/public/blogs/${encodeURIComponent(slug)}/articles?${query}`,
      {},
      false,
    );
  },
  publicArticle(articleId: string) {
    return communityRequest<PublicArticleDetail>(
      `/api/v1/public/articles/${encodeURIComponent(articleId)}`,
      {},
      false,
    );
  },
  discoverArticles(pageNum = 1, pageSize = 20) {
    return communityRequest<PublicArticlePage>(
      `/api/v1/public/articles?pageNum=${pageNum}&pageSize=${pageSize}`,
      {},
      false,
    );
  },
  discoverRankedArticles(sort: DiscoverySort = "LATEST", pageNum = 1, pageSize = 20, tagSlug?: string, keyword?: string) {
    const query = new URLSearchParams({ sort, pageNum: String(pageNum), pageSize: String(pageSize) });
    if (tagSlug) query.set("tagSlug", tagSlug);
    if (keyword) query.set("keyword", keyword);
    return communityRequest<PublicDiscoveryPage>(`/api/v1/public/discover/articles?${query}`, {}, false);
  },
  search(keyword: string, type: UnifiedSearchType = "ALL", pageNum = 1, pageSize = 20) {
    const query = new URLSearchParams({ keyword, type, pageNum: String(pageNum), pageSize: String(pageSize) });
    return communityRequest<UnifiedSearchPage>(`/api/v1/public/search?${query}`, {}, false);
  },
  editorialCollections() { return communityRequest<EditorialCollection[]>("/api/v1/public/editorial", {}, false); },
  categories() {
    return communityRequest<BlogCategory[]>("/api/v1/blogs/me/categories");
  },
  tags(keyword?: string) {
    const suffix = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
    return communityRequest<PlatformTag[]>(`/api/v1/tags${suffix}`, {}, false);
  },
  getHotTopics(limit?: number) {
    const suffix = limit ? `?limit=${limit}` : "";
    return communityRequest<PlatformTag[]>(`/api/v1/tags${suffix}`, {}, false);
  },
  createArticle(input: Record<string, unknown>) {
    return communityRequest<ArticleEditor>("/api/v1/articles", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },
  editor(articleId: string) {
    return communityRequest<ArticleEditor>(
      `/api/v1/articles/${encodeURIComponent(articleId)}/editor`,
    );
  },
  saveArticle(articleId: string, input: Record<string, unknown>, autosave = false) {
    return communityRequest<ArticleEditor>(
      `/api/v1/articles/${encodeURIComponent(articleId)}${autosave ? "/autosave" : ""}`,
      {
        method: "PUT",
        body: JSON.stringify(input),
      },
    );
  },
  submitReview(articleId: string, expectedLockVersion: number) {
    const idempotencyKey =
      `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
    return communityRequest<ArticleReviewStatus>(
      `/api/v1/articles/${encodeURIComponent(articleId)}/submit-review`,
      {
        method: "POST",
        body: JSON.stringify({ idempotencyKey, expectedLockVersion }),
      },
    );
  },
  reviewStatus(articleId: string) {
    return communityRequest<ArticleReviewStatus>(
      `/api/v1/articles/${encodeURIComponent(articleId)}/review-status`,
    );
  },
  withdrawReview(articleId: string, expectedLockVersion: number, reason?: string) {
    return communityRequest<ArticleReviewStatus>(
      `/api/v1/articles/${encodeURIComponent(articleId)}/withdraw-review`,
      {
        method: "POST",
        body: JSON.stringify({ expectedLockVersion, reason }),
      },
    );
  },
  publish(articleId: string, expectedLockVersion: number) {
    return communityRequest<{
      articleId: string;
      publishStatus: string;
      reviewStatus: string;
      canonicalPath: string | null;
      lockVersion: number;
    }>(`/api/v1/articles/${encodeURIComponent(articleId)}/publish`, {
      method: "POST",
      body: JSON.stringify({ expectedLockVersion }),
    });
  },
  moments(pageNum = 1, pageSize = 20, filters?: MomentFeedFilter) {
    const params: string[] = [`pageNum=${pageNum}`, `pageSize=${pageSize}`];
    if (filters?.momentTypes?.length) {
      params.push(`momentTypes=${filters.momentTypes.join(",")}`);
    }
    return communityRequest<MomentPage>(
      `/api/v1/moments?${params.join("&")}`,
      {},
      "optional",
    );
  },
  moment(momentId: string) {
    return communityRequest<Moment>(
      `/api/v1/moments/${encodeURIComponent(momentId)}`,
      {},
      "optional",
    );
  },
  myMoments(pageNum = 1, pageSize = 20) {
    return communityRequest<MomentPage>(
      `/api/v1/social/me/moments?pageNum=${pageNum}&pageSize=${pageSize}`,
    );
  },
  publishMoment(input: {
    blogId?: string;
    momentType: string;
    textContent?: string;
    linkUrl?: string;
    articleId?: string;
    repostMomentId?: string;
    visibility: string;
  }) {
    return communityRequest<{
      moment: Moment;
      moderationWarning: boolean;
      moderationResult: string | null;
    }>("/api/v1/moments", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },
  comments(targetType: CommunityContentType, targetId: string, pageNum = 1, pageSize = 20) {
    return communityRequest<CommentPage>(
      `/api/v1/interactions/${targetType}/${encodeURIComponent(targetId)}/comments?pageNum=${pageNum}&pageSize=${pageSize}`,
      {},
      "optional",
    );
  },
  createComment(targetType: CommunityContentType, targetId: string, content: string) {
    return communityRequest<CommunityComment>(
      `/api/v1/interactions/${targetType}/${encodeURIComponent(targetId)}/comments`,
      { method: "POST", body: JSON.stringify({ content }) },
    );
  },
  likeRelationship(targetType: CommunityContentType, targetId: string) {
    return communityRequest<LikeRelationship>(
      `/api/v1/interactions/${targetType}/${encodeURIComponent(targetId)}/like`,
    );
  },
  setLike(targetType: CommunityContentType, targetId: string, liked: boolean) {
    return communityRequest<LikeRelationship>(
      `/api/v1/interactions/${targetType}/${encodeURIComponent(targetId)}/like`,
      { method: liked ? "POST" : "DELETE" },
    );
  },
  favoriteRelationship(targetType: CommunityContentType, targetId: string) {
    return communityRequest<FavoriteRelationship>(
      `/api/v1/interactions/${targetType}/${encodeURIComponent(targetId)}/favorite`,
    );
  },
  setFavorite(
    targetType: CommunityContentType,
    targetId: string,
    favorited: boolean,
    folderIds: string[] = [],
  ) {
    return communityRequest<FavoriteRelationship>(
      `/api/v1/interactions/${targetType}/${encodeURIComponent(targetId)}/favorite`,
      favorited
        ? { method: "POST", body: JSON.stringify({ folderIds }) }
        : { method: "DELETE" },
    );
  },
  favoriteFolders() {
    return communityRequest<FavoriteFolder[]>("/api/v1/social/me/favorite-folders");
  },
  createFavoriteFolder(input: {
    name: string;
    description?: string;
    visibility?: string;
    sortOrder?: number;
  }) {
    return communityRequest<FavoriteFolder>("/api/v1/social/me/favorite-folders", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },
  favoriteItems(folderId: string, pageNum = 1, pageSize = 20) {
    return communityRequest<FavoriteContentPage>(
      `/api/v1/favorite-folders/${encodeURIComponent(folderId)}/items?pageNum=${pageNum}&pageSize=${pageSize}`,
    );
  },
  myLikes(pageNum = 1, pageSize = 20) {
    return communityRequest<LikedContentPage>(
      `/api/v1/social/me/likes?pageNum=${pageNum}&pageSize=${pageSize}`,
    );
  },
  likeListPrivacy() {
    return communityRequest<{ ownerUserId: string; visibility: string }>(
      "/api/v1/social/me/likes/privacy",
    );
  },
  updateLikeListPrivacy(visibility: string) {
    return communityRequest<{ ownerUserId: string; visibility: string }>(
      "/api/v1/social/me/likes/privacy",
      { method: "PATCH", body: JSON.stringify({ visibility }) },
    );
  },
  socialCounts() {
    return communityRequest<SocialCounts>("/api/v1/social/me/counts");
  },
  myFollowing(pageNum = 1, pageSize = 20) {
    return communityRequest<SocialProfilePage>(
      `/api/v1/social/me/following?pageNum=${pageNum}&pageSize=${pageSize}`,
    );
  },
  followingFeed(pageNum = 1, pageSize = 20) {
    return communityRequest<FollowingFeedPage>(`/api/v1/social/me/following-feed?pageNum=${pageNum}&pageSize=${pageSize}`);
  },
  creatorAnalytics(days = 30) { return communityRequest<CreatorAnalytics>(`/api/v1/analytics/me?days=${days}`); },
  trackArticleAnalytics(articleId:string,input:{sourceType?:string;searchTerm?:string}) { return communityRequest<void>(`/api/v1/public/analytics/articles/${encodeURIComponent(articleId)}/events`,{method:"POST",body:JSON.stringify(input)},false); },
  myFollowers(pageNum = 1, pageSize = 20) {
    return communityRequest<SocialProfilePage>(
      `/api/v1/social/me/followers?pageNum=${pageNum}&pageSize=${pageSize}`,
    );
  },
  blogFollowRelationship(blogId: string) {
    return communityRequest<BlogFollowRelationship>(
      `/api/v1/blogs/${encodeURIComponent(blogId)}/follow`,
    );
  },
  setBlogFollow(blogId: string, following: boolean) {
    return communityRequest<BlogFollowRelationship>(
      `/api/v1/blogs/${encodeURIComponent(blogId)}/follow`,
      following
        ? {
          method: "POST",
          body: JSON.stringify({ notificationLevel: "ALL", specialFollow: false }),
        }
        : { method: "DELETE" },
    );
  },
  notifications(
    category?: NotificationCategory,
    status?: "UNREAD" | "READ",
    pageNum = 1,
    pageSize = 20,
  ) {
    const query = new URLSearchParams({
      pageNum: String(pageNum),
      pageSize: String(pageSize),
    });
    if (category) query.set("category", category);
    if (status) query.set("status", status);
    return communityRequest<NotificationPage>(`/api/v1/notifications?${query}`);
  },
  unreadNotifications() {
    return communityRequest<UnreadNotificationCount>("/api/v1/notifications/unread-count");
  },
  readNotification(notificationId: string) {
    return communityRequest<{
      notificationId: string;
      status: "READ";
      readAt: string;
      idempotentReplay: boolean;
      unreadCount: number;
    }>(`/api/v1/notifications/${encodeURIComponent(notificationId)}/read`, {
      method: "PATCH",
    });
  },
  readAllNotifications(category?: NotificationCategory) {
    const suffix = category ? `?category=${encodeURIComponent(category)}` : "";
    return communityRequest<{
      category: NotificationCategory | null;
      affectedNotifications: number;
      unreadCount: number;
    }>(`/api/v1/notifications/read-all${suffix}`, { method: "PATCH" });
  },

  createReport(input: { targetType: CommunityReport["targetType"]; targetId: string; reasonCode: string; description?: string; evidenceJson?: string }) {
    return communityRequest<CommunityReport>("/api/v1/reports", { method: "POST", body: JSON.stringify(input) });
  },
  myReports() { return communityRequest<CommunityReport[]>("/api/v1/reports/me"); },

  createBlock(input: { targetType: CommunityBlock["targetType"]; targetId: string }) {
    return communityRequest<CommunityBlock>("/api/v1/blocks", { method: "POST", body: JSON.stringify(input) });
  },
  myBlocks() { return communityRequest<CommunityBlock[]>("/api/v1/blocks"); },
  removeBlock(targetType: CommunityBlock["targetType"], targetId: string) {
    return communityRequest<void>(`/api/v1/blocks/${encodeURIComponent(targetType)}/${encodeURIComponent(targetId)}`, { method: "DELETE" });
  },
  appealContext(reportId: string) { return communityRequest<AppealContext>(`/api/v1/reports/${encodeURIComponent(reportId)}/appeal-context`); },
  submitAppeal(reportId: string, input: { appealReason: string; evidenceJson?: string }) { return communityRequest<CommunityAppeal>(`/api/v1/reports/${encodeURIComponent(reportId)}/appeals`, { method: "POST", body: JSON.stringify(input) }); },
  myAppeals() { return communityRequest<CommunityAppeal[]>("/api/v1/appeals/me"); },
  mySanctions() { return communityRequest<CommunitySanction[]>("/api/v1/sanctions/me"); },

  // Team applications
  submitTeamApplication(input: SubmitTeamApplicationInput) {
    return communityRequest<TeamApplication>("/api/v1/team-applications", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },
  myTeamApplication() {
    return communityRequest<TeamApplication | null>("/api/v1/team-applications/me");
  },
  getTeamApplication(applicationId: string) {
    return communityRequest<TeamApplication>(
      `/api/v1/team-applications/${applicationId}`,
    );
  },
  cancelTeamApplication(applicationId: string) {
    return communityRequest<void>(
      `/api/v1/team-applications/${applicationId}`,
      { method: "DELETE" },
    );
  },

  myTeamInvitations() {
    return communityRequest<TeamInvitation[]>("/api/v1/teams/invitations/me");
  },
  acceptTeamInvitation(invitationId: string) {
    return communityRequest<void>(`/api/v1/teams/invitations/${encodeURIComponent(invitationId)}/accept`, {
      method: "POST",
    });
  },
  rejectTeamInvitation(invitationId: string) {
    return communityRequest<void>(`/api/v1/teams/invitations/${encodeURIComponent(invitationId)}/reject`, {
      method: "POST",
    });
  },
  teams() { return communityRequest<TeamSummary[]>("/api/v1/teams", {}, false); },
  // --- 系列：公开读（游客可访问，登录后附带 viewer* 个人状态） ---
  series() { return communityRequest<Series[]>("/api/v1/series", {}, false); },
  seriesDetail(seriesId: string) { return communityRequest<Series>(`/api/v1/series/${encodeURIComponent(seriesId)}`, {}, false); },
  /** 某个博客（个人或团队）已公开的系列，用于博客门户的"系列"页签。 */
  blogPublicSeries(blogId: string) { return communityRequest<Series[]>(`/api/v1/public/blogs/${encodeURIComponent(blogId)}/series`, {}, false); },

  // --- 系列：作者 / 管理侧，一律以 blogId 为维度，个人博客与团队博客共用 ---
  blogSeries(blogId: string) { return communityRequest<Series[]>(`/api/v1/blogs/${encodeURIComponent(blogId)}/series/manage`); },
  blogSeriesArticles(blogId: string) { return communityRequest<SeriesChapter[]>(`/api/v1/blogs/${encodeURIComponent(blogId)}/series/articles`); },
  createBlogSeries(blogId: string, input: { title: string; slug?: string; summary?: string; coverFileId?: string | null; serializationStatus?: Series["serializationStatus"] }) { return communityRequest<Series>(`/api/v1/blogs/${encodeURIComponent(blogId)}/series`, { method: "POST", body: JSON.stringify(input) }); },
  updateSeries(seriesId: string, input: { title: string; slug?: string; summary?: string; coverFileId?: string | null; serializationStatus?: Series["serializationStatus"]; expectedLockVersion: number }) { return communityRequest<Series>(`/api/v1/series/${encodeURIComponent(seriesId)}`, { method: "PATCH", body: JSON.stringify(input) }); },
  saveSeriesChapters(seriesId: string, articleIds: string[], expectedLockVersion: number) { return communityRequest<Series>(`/api/v1/series/${encodeURIComponent(seriesId)}/chapters`, { method: "POST", body: JSON.stringify({ articleIds, expectedLockVersion }) }); },
  submitSeriesReview(seriesId: string, expectedLockVersion: number) { return communityRequest<Series>(`/api/v1/series/${encodeURIComponent(seriesId)}/submit-review`, { method: "POST", body: JSON.stringify({ expectedLockVersion }) }); },

  // --- 系列：读者侧追更与阅读进度 ---
  myFollowedSeries() { return communityRequest<Series[]>("/api/v1/me/series/following"); },
  /** "继续阅读"书架，按最近阅读时间倒序。 */
  myReadingSeries(limit = 10) { return communityRequest<Series[]>(`/api/v1/me/series/reading?limit=${limit}`); },
  seriesReaderState(seriesId: string) { return communityRequest<SeriesReaderState>(`/api/v1/series/${encodeURIComponent(seriesId)}/reader-state`); },
  followSeries(seriesId: string) { return communityRequest<SeriesReaderState>(`/api/v1/series/${encodeURIComponent(seriesId)}/follow`, { method: "POST" }); },
  unfollowSeries(seriesId: string) { return communityRequest<SeriesReaderState>(`/api/v1/series/${encodeURIComponent(seriesId)}/follow`, { method: "DELETE" }); },
  /** 读者打开某一章时上报，维持"继续阅读"指针；最远进度只增不减。 */
  recordSeriesProgress(seriesId: string, articleId: string) { return communityRequest<SeriesReaderState>(`/api/v1/series/${encodeURIComponent(seriesId)}/progress`, { method: "POST", body: JSON.stringify({ articleId }) }); },
  /** 文章页导航：取该文章所属公开系列的章节顺序与读者态；不在任何公开系列时返回 null。 */
  articleSeriesContext(articleId: string) {
    return communityRequest<ArticleSeriesContext | null>(`/api/v1/public/articles/${encodeURIComponent(articleId)}/series`, {}, false);
  },
  myTeamSubmissions() { return communityRequest<TeamSubmission[]>("/api/v1/team-submissions/me"); },
  /** 可投稿的个人文章：后端已按"作者本人 + 个人博客 + 有当前版本"过滤，前端直接列出即可。 */
  submittableArticles() { return communityRequest<SubmittableArticle[]>("/api/v1/team-submissions/candidates"); },
  createTeamSubmission(input: { sourceArticleId: string; targetTeamId: string; supersedesSubmissionId?: string | null; idempotencyKey?: string }) { const idempotencyKey = input.idempotencyKey || `team-submission-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`; return communityRequest<TeamSubmission>("/api/v1/team-submissions", { method: "POST", body: JSON.stringify({ ...input, idempotencyKey }) }); },
  teamSubmissions(teamId: string) { return communityRequest<TeamSubmission[]>(`/api/v1/team-submissions/teams/${encodeURIComponent(teamId)}`); },
  decideTeamSubmission(submissionId: string, action: "approve" | "revision" | "reject", expectedLockVersion: number, comment?: string) { return communityRequest<TeamSubmission>(`/api/v1/team-submissions/${encodeURIComponent(submissionId)}/team/${action}`, { method: "POST", body: JSON.stringify({ expectedLockVersion, comment }) }); },
  collaborators(articleId: string) { return communityRequest<ArticleCollaboration[]>(`/api/v1/articles/${encodeURIComponent(articleId)}/collaborators`); },
  inviteCollaborator(articleId: string, input: { inviteeUserId: string; contributionType: string; canEdit: boolean; attributionOrder: number }) { const idempotencyKey = `article-collaboration-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`; return communityRequest<ArticleCollaboration>(`/api/v1/articles/${encodeURIComponent(articleId)}/collaborators/invitations`, { method: "POST", body: JSON.stringify({ ...input, idempotencyKey }) }); },
  myCollaborationInvitations() { return communityRequest<ArticleCollaboration[]>("/api/v1/articles/collaboration-invitations/me"); },
  respondToCollaboration(invitationId: string, action: "accept" | "reject", expectedLockVersion: number) { return communityRequest<ArticleCollaboration | void>(`/api/v1/articles/collaboration-invitations/${encodeURIComponent(invitationId)}/${action}`, { method: "POST", body: JSON.stringify({ expectedLockVersion }) }); },
  revokeCollaborator(articleId: string, collaboratorId: string, expectedLockVersion: number) { return communityRequest<void>(`/api/v1/articles/${encodeURIComponent(articleId)}/collaborators/${encodeURIComponent(collaboratorId)}?expectedLockVersion=${expectedLockVersion}`, { method: "DELETE" }); },
  team(slug: string) { return communityRequest<TeamPortal>(`/api/v1/teams/slug/${encodeURIComponent(slug)}`, {}, false); },
  teamWorkspace(teamId: string) { return communityRequest<TeamWorkspace>(`/api/v1/teams/${encodeURIComponent(teamId)}/workspace`); },
  /** All active memberships of the current user; drives the smart default tab and the team switcher. */
  myTeams() { return communityRequest<MyTeam[]>("/api/v1/teams/me"); },
  /** Single round trip powering the workspace overview page. Members only. */
  teamDashboard(teamId: string) { return communityRequest<TeamDashboard>(`/api/v1/teams/${encodeURIComponent(teamId)}/dashboard`); },
  teamActivities(teamId: string, limit = 20) { return communityRequest<TeamActivity[]>(`/api/v1/teams/${encodeURIComponent(teamId)}/activities?limit=${limit}`); },
  /** All team articles regardless of status, optionally filtered by publish_status. Members only. */
  teamArticles(teamId: string, publishStatus?: string) {
    const query = publishStatus ? `?publishStatus=${encodeURIComponent(publishStatus)}` : "";
    return communityRequest<TeamArticleBrief[]>(`/api/v1/teams/${encodeURIComponent(teamId)}/articles${query}`);
  },
  /** Approved series of one team for its public portal; no login required. */
  teamPublicSeries(teamId: string) { return communityRequest<Series[]>(`/api/v1/public/teams/${encodeURIComponent(teamId)}/series`, {}, false); },
  teamMembers(teamId: string) { return communityRequest<TeamMemberView[]>(`/api/v1/teams/${encodeURIComponent(teamId)}/members`); },
  updateTeamSettings(teamId: string, input: { name: string; summary?: string | null; avatarFileId?: string | null; backgroundFileId?: string | null; category?: string | null; contentDirection?: string | null; theme?: string | null; seoTitle?: string | null; seoDescription?: string | null; publicMembers?: boolean; allowSubmissions?: boolean; submissionGuideline?: string | null; contactInfo?: string | null }) {
    return communityRequest<TeamSummary>(`/api/v1/teams/${encodeURIComponent(teamId)}`, { method: "PATCH", body: JSON.stringify(input) });
  },
  inviteTeamMember(teamId: string, input: { userId: string; roleCode: string; idempotencyKey?: string }) {
    const idempotencyKey = input.idempotencyKey || `team-invite-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
    return communityRequest<TeamInvitation>(`/api/v1/teams/${encodeURIComponent(teamId)}/invitations`, { method: "POST", body: JSON.stringify({ userId: input.userId, roleCode: input.roleCode, idempotencyKey }) });
  },
  /** 团队已发出的邀请（含历史状态），需要 MANAGE_MEMBERS 权限。 */
  teamInvitations(teamId: string) {
    return communityRequest<TeamInvitation[]>(`/api/v1/teams/${encodeURIComponent(teamId)}/invitations`);
  },
  /** 撤销尚未处理的邀请；撤销后可以重新邀请同一人。 */
  revokeTeamInvitation(teamId: string, invitationId: string) {
    return communityRequest<void>(
      `/api/v1/teams/${encodeURIComponent(teamId)}/invitations/${encodeURIComponent(invitationId)}`,
      { method: "DELETE" },
    );
  },
  changeTeamMemberRole(teamId: string, userId: string, roleCode: string) {
    return communityRequest<void>(`/api/v1/teams/${encodeURIComponent(teamId)}/members/${encodeURIComponent(userId)}/role`, { method: "POST", body: JSON.stringify({ roleCode }) });
  },
  removeTeamMember(teamId: string, userId: string) {
    return communityRequest<void>(`/api/v1/teams/${encodeURIComponent(teamId)}/members/${encodeURIComponent(userId)}`, { method: "DELETE" });
  },
  leaveTeam(teamId: string) { return communityRequest<void>(`/api/v1/teams/${encodeURIComponent(teamId)}/leave`, { method: "POST" }); },
  transferTeamOwnership(teamId: string, userId: string) {
    return communityRequest<void>(`/api/v1/teams/${encodeURIComponent(teamId)}/owner`, { method: "POST", body: JSON.stringify({ userId }) });
  },
  disbandTeam(teamId: string) { return communityRequest<void>(`/api/v1/teams/${encodeURIComponent(teamId)}/disband`, { method: "POST" }); },
  chatHistory(peerId: string) { return communityRequest<CommunityChatMessage[]>(`/api/v1/chat/messages/${encodeURIComponent(peerId)}`); },
  chatConversations(limit = 20) { return communityRequest<CommunityChatConversation[]>(`/api/v1/chat/conversations?limit=${limit}`); },
  sendChatMessage(recipientUserId: string, contentText: string) { return communityRequest<CommunityChatMessage>("/api/v1/chat/messages", { method: "POST", body: JSON.stringify({ recipientUserId, contentText }) }); },
  markChatRead(peerId: string) { return communityRequest<void>(`/api/v1/chat/messages/${encodeURIComponent(peerId)}/read`, { method: "POST" }); },
  chatTicket() { return communityRequest<{ ticket: string; expiresInSeconds: number }>("/api/v1/chat/websocket-ticket", { method: "POST" }); },
  creatorIdeas() { return communityRequest<CommunityCreatorIdea[]>("/api/v1/creator/ideas"); },
  createCreatorIdea(input: { title: string; content: string; tags: string[]; sourceType?: "MANUAL" | "ARTICLE" | "MOMENT" }) { return communityRequest<CommunityCreatorIdea>("/api/v1/creator/ideas", { method: "POST", body: JSON.stringify(input) }); },

  // --- Article Deletion, Drafts & Versions ---
  deleteArticle(articleId: string, expectedLockVersion: number) {
    return communityRequest<void>(
      `/api/v1/articles/${encodeURIComponent(articleId)}?expectedLockVersion=${expectedLockVersion}`,
      { method: "DELETE" },
    );
  },

  articleVersions(articleId: string, pageNum = 1, pageSize = 20) {
    return communityRequest<ArticleVersionPage>(
      `/api/v1/articles/${encodeURIComponent(articleId)}/versions?pageNum=${pageNum}&pageSize=${pageSize}`,
    );
  },

  articleVersionDetail(articleId: string, versionId: string) {
    return communityRequest<ArticleVersionDetail>(
      `/api/v1/articles/${encodeURIComponent(articleId)}/versions/${encodeURIComponent(versionId)}`,
    );
  },

  restoreArticleVersion(articleId: string, versionId: string, expectedLockVersion: number) {
    return communityRequest<ArticleEditor>(
      `/api/v1/articles/${encodeURIComponent(articleId)}/versions/${encodeURIComponent(versionId)}/restore`,
      {
        method: "POST",
        body: JSON.stringify({ expectedLockVersion }),
      },
    );
  },

  // --- Blog Categories ---
  publicBlogCategories(blogSlug: string) {
    return communityRequest<BlogCategory[]>(
      `/api/v1/public/blogs/${encodeURIComponent(blogSlug)}/categories`,
      {},
      false,
    );
  },

  createCategory(input: CreateCategoryInput) {
    return communityRequest<BlogCategory>("/api/v1/blogs/me/categories", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  updateCategory(categoryId: string, input: UpdateCategoryInput) {
    return communityRequest<BlogCategory>(
      `/api/v1/blogs/me/categories/${encodeURIComponent(categoryId)}`,
      {
        method: "PATCH",
        body: JSON.stringify(input),
      },
    );
  },

  deleteCategory(categoryId: string) {
    return communityRequest<void>(
      `/api/v1/blogs/me/categories/${encodeURIComponent(categoryId)}`,
      { method: "DELETE" },
    );
  },

  // --- Favorite Folders Management ---
  updateFavoriteFolder(folderId: string, input: UpdateFavoriteFolderInput) {
    return communityRequest<FavoriteFolder>(
      `/api/v1/social/me/favorite-folders/${encodeURIComponent(folderId)}`,
      {
        method: "PATCH",
        body: JSON.stringify(input),
      },
    );
  },

  deleteFavoriteFolder(folderId: string) {
    return communityRequest<void>(
      `/api/v1/social/me/favorite-folders/${encodeURIComponent(folderId)}`,
      { method: "DELETE" },
    );
  },

  // --- Interactive Entity Deletions & Removal ---
  deleteMoment(momentId: string, expectedLockVersion = 0) {
    return communityRequest<MomentDeletionResponse>(
      `/api/v1/moments/${encodeURIComponent(momentId)}?expectedLockVersion=${expectedLockVersion}`,
      { method: "DELETE" },
    );
  },

  deleteComment(commentId: string) {
    return communityRequest<CommentModerationResponse>(
      `/api/v1/comments/${encodeURIComponent(commentId)}`,
      { method: "DELETE" },
    );
  },


  // --- Invitation Cancellation ---
  cancelCollaborationInvitation(invitationId: string, lockVersion = 0) {
    return communityRequest<void>(
      `/api/v1/articles/collaboration-invitations/${encodeURIComponent(invitationId)}/cancel`,
      {
        method: "POST",
        body: JSON.stringify({ expectedLockVersion: lockVersion }),
      },
    );
  },
};
