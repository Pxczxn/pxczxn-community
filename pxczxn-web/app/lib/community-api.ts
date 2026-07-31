"use client";

const configuredApiBaseUrl =
  process.env.NEXT_PUBLIC_COMMUNITY_API_BASE_URL?.trim();
const browserHostname =
  typeof window !== "undefined" ? window.location.hostname : "";
const isLocalBrowser =
  typeof window !== "undefined" &&
  (
    ["localhost", "127.0.0.1", "::1"].includes(browserHostname)
    || /^10\./.test(browserHostname)
    || /^192\.168\./.test(browserHostname)
    || /^172\.(1[6-9]|2\d|3[01])\./.test(browserHostname)
    || /^198\.(18|19)\./.test(browserHostname)
  );
const inferredLocalApiBaseUrl =
  isLocalBrowser
    ? `${window.location.protocol}//${browserHostname}:8849`
    : "";
const inferredSameOriginApiBaseUrl =
  typeof window !== "undefined" && !isLocalBrowser
    ? window.location.origin
    : "";

export const COMMUNITY_API_BASE_URL =
  (configuredApiBaseUrl || inferredLocalApiBaseUrl || inferredSameOriginApiBaseUrl)
    .replace(/\/+$/, "");

const SESSION_KEY = "pxczxn-community-session";
export const SESSION_EVENT = "pxczxn-session-change";
export const NOTIFICATION_EVENT = "pxczxn-notification-change";

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface CommunitySession {
  tokenName: string;
  tokenValue: string;
  expiresIn: number;
  userId: string;
  username: string;
  displayName?: string | null;
  blogSlug?: string | null;
}

export interface CurrentCommunityUser {
  userId: string;
  username: string;
  displayName: string | null;
  bio: string | null;
  avatarFileId: string | null;
  email: string;
  status: string;
  verificationStatus: string;
  personalBlogId: string | null;
  blogName: string | null;
  blogSlug: string | null;
}

export interface BlogSettings {
  commentScope: string;
  defaultVisibility: string;
  allowRepost: string;
  themeKey: string;
  seoTitle: string | null;
  seoDescription: string | null;
}

export interface PersonalBlog {
  blogId: string;
  name: string;
  slug: string;
  summary: string | null;
  avatarFileId: string | null;
  backgroundFileId: string | null;
  status: string;
  articleCount: number;
  followerCount: number;
  settings: BlogSettings;
}

export interface PublicBlog {
  blogId: string;
  blogType: string;
  name: string;
  slug: string;
  summary: string | null;
  avatarFileId: string | null;
  backgroundFileId: string | null;
  articleCount: number;
  followerCount: number;
  ownerUsername: string;
  ownerDisplayName: string | null;
  ownerBio: string | null;
  ownerAvatarFileId: string | null;
  themeKey: string;
  themeConfigJson: string | null;
  seoTitle: string | null;
  seoDescription: string | null;
}

export interface ArticleAuthor {
  userId: string;
  username: string;
  displayName: string | null;
  bio: string | null;
  avatarFileId: string | null;
}

export interface ArticleCategory {
  categoryId: string;
  name: string;
  slug: string;
  description: string | null;
  defaultCategory: boolean;
}

export interface ArticleTag {
  tagId: string;
  name: string;
  slug: string;
}

export interface PublicArticleSummary {
  articleId: string;
  title: string;
  slug: string;
  summary: string | null;
  coverFileId: string | null;
  contentMode: string;
  author: ArticleAuthor;
  category: ArticleCategory | null;
  tags: ArticleTag[];
  publishedAt: string;
  updatedAt: string;
  wordCount: number;
  readingTimeMinutes: number;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  canonicalPath: string;
}

export interface PublicArticlePage {
  records: PublicArticleSummary[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export type DiscoverySort = "LATEST" | "VIEWS" | "LIKES" | "FAVORITES" | "COMMENTS" | "QUALITY" | "RISK";
export interface PublicDiscoveryPage extends PublicArticlePage { sort: DiscoverySort; sortExplanation: string; }

export type UnifiedSearchType = "ALL" | "ARTICLE" | "MOMENT" | "BLOG" | "SERIES" | "TAG" | "USER";

export interface UnifiedSearchResult {
  type: Exclude<UnifiedSearchType, "ALL">;
  targetId: string;
  title: string;
  titleHighlightHtml: string;
  excerptHighlightHtml: string;
  canonicalPath: string;
  authorUserId: string | null;
  authorName: string | null;
  blogName: string | null;
  occurredAt: string;
}

export interface UnifiedSearchPage {
  records: UnifiedSearchResult[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface EditorialCollection { id: string; kind: "TOPIC" | "EVENT" | "ANNOUNCEMENT" | "FEATURED" | "COLLECTION"; title: string; slug: string; summary: string | null; coverFileId: string | null; status: string; startsAt: string | null; endsAt: string | null; displayOrder: number; publishedAt: string | null; items: Array<{ targetType: "ARTICLE" | "SERIES"; targetId: string; displayOrder: number }>; }

export interface PublicArticleDetail extends PublicArticleSummary {
  visibility: string;
  renderedHtml: string;
  tocJson: string;
  blog: {
    blogId: string;
    blogType: string;
    name: string;
    slug: string;
    summary: string | null;
    avatarFileId: string | null;
    backgroundFileId: string | null;
    themeKey: string;
    themeConfigJson: string | null;
  };
  seo: {
    title: string;
    description: string | null;
    canonicalPath: string;
  };
}

export interface BlogCategory {
  categoryId: string;
  name: string;
  slug: string;
  description: string | null;
  sortOrder: number;
  defaultCategory: boolean;
  articleCount: number;
}

export interface PlatformTag {
  tagId: string;
  name: string;
  slug: string;
  description: string | null;
  usageCount: number;
}

export interface ArticleEditor {
  articleId: string;
  blogId: string;
  authorUserId: string;
  categoryId: string | null;
  title: string;
  slug: string;
  summary: string | null;
  coverFileId: string | null;
  contentMode: "RICH_TEXT" | "MARKDOWN";
  visibility: string;
  publishMethod: string;
  publishStatus: string;
  reviewStatus: string;
  currentVersionId: string | null;
  publishedVersionId: string | null;
  reviewVersionId: string | null;
  lockVersion: number;
  tagIds: string[];
  contentFileIds: string[];
  richTextJson: string | null;
  markdownContent: string | null;
  renderedHtml: string;
  plainText: string;
  tocJson: string;
  contentHash: string;
  wordCount: number;
  readingTimeMinutes: number;
  createdAt: string;
  updatedAt: string;
  versionCreatedAt: string;
}

export interface ArticleReviewTask {
  taskId: string;
  fixedVersionId: string;
  reviewStage: string;
  reviewType: string;
  status: string;
  riskLevel: string | null;
  resultCode: string | null;
  resultReason: string | null;
  lockVersion: number;
  submittedAt: string;
  claimedAt: string | null;
  completedAt: string | null;
}

export interface ArticleReviewStatus {
  articleId: string;
  publishStatus: string;
  reviewStatus: string;
  currentVersionId: string | null;
  reviewVersionId: string | null;
  lockVersion: number;
  latestTask: ArticleReviewTask | null;
}

export type CommunityContentType = "ARTICLE" | "MOMENT";

export interface ContentRelationship {
  targetType: CommunityContentType;
  targetId: string;
}

export interface LikeRelationship extends ContentRelationship {
  liked: boolean;
  likeCount: number;
}

export interface FavoriteRelationship extends ContentRelationship {
  favorited: boolean;
  favoriteCount: number;
  folderIds: string[];
}

export interface FavoriteFolder {
  folderId: string;
  ownerUserId: string;
  name: string;
  description: string | null;
  visibility: string;
  defaultFolder: boolean;
  itemCount: number;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CommunityContentListItem {
  targetType: CommunityContentType;
  targetId: string;
  authorUserId: string;
  blogId: string;
  title: string;
  excerpt: string | null;
  coverFileId: string | null;
  canonicalPath: string;
}

export interface FavoriteContent extends CommunityContentListItem {
  favoriteItemId: string;
  favoriteCount: number;
  favoritedAt: string;
}

export interface FavoriteContentPage {
  folderId: string;
  records: FavoriteContent[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface LikedContent extends CommunityContentListItem {
  likeId: string;
  likeCount: number;
  likedAt: string;
}

export interface LikedContentPage {
  records: LikedContent[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface SocialCounts {
  following: number;
  followers: number;
  mutual: number;
}

export interface BlogFollowRelationship {
  blogId: string;
  following: boolean;
  followedBy: boolean;
  mutual: boolean;
  specialFollow: boolean;
  notificationLevel: "ALL" | "IMPORTANT" | "MUTED";
  followerCount: number;
}

export interface SocialProfile {
  userId: string;
  username: string;
  displayName: string | null;
  bio: string | null;
  avatarFileId: string | null;
  blogId: string;
  blogName: string;
  blogSlug: string;
  following: boolean;
  followedBy: boolean;
  mutual: boolean;
  specialFollow: boolean;
  notificationLevel: "ALL" | "IMPORTANT" | "MUTED";
  followedAt: string;
}

export interface SocialProfilePage {
  records: SocialProfile[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface FollowingFeedItem { itemType: "ARTICLE" | "MOMENT" | "SERIES" | "TAG_ARTICLE"; targetId: string; title: string; excerpt: string | null; canonicalPath: string; authorName: string | null; blogName: string | null; tagName: string | null; occurredAt: string; }
export interface FollowingFeedPage { records: FollowingFeedItem[]; total: number; pageNum: number; pageSize: number; }
export interface CreatorAnalytics { articleCount:number; viewCount:number; likeCount:number; favoriteCount:number; commentCount:number; followerCount:number; daily:Array<{day:string;count:number}>; topArticles:Array<{articleId:string;title:string;viewCount:number;likeCount:number;favoriteCount:number;commentCount:number}>; trafficSources:Array<{label:string;count:number}>; searchTerms:Array<{label:string;count:number}>; }

export interface MomentAuthor {
  userId: string;
  username: string;
  displayName: string | null;
  avatarFileId: string | null;
}

export interface MomentBlog {
  blogId: string;
  blogType: string;
  name: string;
  slug: string;
  avatarFileId: string | null;
}

export interface MomentArticle {
  articleId: string | null;
  available: boolean;
  title: string | null;
  summary: string | null;
  coverFileId: string | null;
  canonicalPath: string | null;
}

export interface MomentSource {
  momentId: string | null;
  available: boolean;
  momentType: string | null;
  author: MomentAuthor | null;
  blog: MomentBlog | null;
  textContent: string | null;
  renderedHtml: string | null;
  linkUrl: string | null;
  article: MomentArticle | null;
  repostMomentId: string | null;
  createdAt: string | null;
}

export interface Moment {
  momentId: string;
  momentType: string;
  author: MomentAuthor;
  blog: MomentBlog;
  textContent: string | null;
  renderedHtml: string;
  linkUrl: string | null;
  article: MomentArticle | null;
  repostSource: MomentSource | null;
  visibility: string;
  status: string;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  repostCount: number;
  liked: boolean;
  favorited: boolean;
  canonicalPath: string;
  lockVersion: number;
  createdAt: string;
  updatedAt: string;
}

export interface MomentPage {
  records: Moment[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface CommentAuthor {
  userId: string;
  username: string;
  displayName: string | null;
  avatarFileId: string | null;
}

export interface CommunityComment {
  commentId: string;
  targetType: CommunityContentType;
  targetId: string;
  rootCommentId: string | null;
  parentCommentId: string | null;
  replyToUserId: string | null;
  author: CommentAuthor | null;
  contentText: string | null;
  renderedHtml: string;
  status: string;
  deleted: boolean;
  likeCount: number;
  liked: boolean;
  moderationWarning: boolean;
  moderationResult: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CommentThread {
  root: CommunityComment;
  replyPreview: CommunityComment[];
  replyCount: number;
}

export interface CommentPage {
  targetType: CommunityContentType;
  targetId: string;
  records: CommentThread[];
  total: number;
  commentCount: number;
  pageNum: number;
  pageSize: number;
}

export type NotificationCategory =
  | "INTERACTION"
  | "FOLLOW"
  | "COMMENT"
  | "COAUTHOR"
  | "SUBMISSION"
  | "TEAM"
  | "REVIEW"
  | "SYSTEM";

export interface CommunityNotification {
  notificationId: string;
  notificationType: string;
  category: NotificationCategory;
  importance: "NORMAL" | "HIGH";
  sender: MomentAuthor | null;
  title: string | null;
  content: string | null;
  targetType: string | null;
  targetId: string | null;
  targetAvailable: boolean;
  canonicalPath: string | null;
  aggregateCount: number;
  status: "UNREAD" | "READ";
  readAt: string | null;
  createdAt: string;
  lastActivityAt: string;
}

export interface NotificationPage {
  records: CommunityNotification[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface UnreadNotificationCount {
  total: number;
  categories: Partial<Record<NotificationCategory, number>>;
}

export interface TeamApplication {
  id: string;
  applicantUserId: string;
  teamName: string;
  teamSlug: string;
  description?: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  reviewerUserId?: string;
  reviewComment?: string;
  reviewedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CommunityReport {
  id: string;
  reporterUserId: string;
  targetType: "ARTICLE" | "MOMENT" | "COMMENT" | "BLOG" | "USER" | "TEAM" | "CHAT";
  targetId: string;
  reasonCode: string;
  description: string | null;
  evidenceJson: string | null;
  status: "PENDING" | "ASSIGNED" | "RESOLVED" | "DISMISSED";
  resolutionCode: string | null;
  resolutionNote: string | null;
  lockVersion: number;
}

export interface CommunityBlock {
  id: string;
  targetType: "USER" | "BLOG" | "TAG" | "CHAT";
  targetId: string;
  createdAt: string;
}

export interface CommunityAppeal { id: string; reportId: string; status: "PENDING" | "UPHELD" | "REVOKED"; appealReason: string; reviewNote: string | null; lockVersion: number; }
export interface CommunitySanction { id: string; type: string; reasonCode: string; reasonNote: string | null; status: string; expiresAt: string | null; }
export interface AppealContext { reportId: string; targetType: string; targetId: string; resolutionCode: string | null; resolutionNote: string | null; }

export interface TeamInvitation {
  id: string;
  teamId: string;
  inviteeUserId: string;
  roleCode: "OWNER" | "ADMIN" | "EDITOR" | "AUTHOR";
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "EXPIRED";
  expiresAt: string;
  createdAt: string;
}

export interface TeamSummary { teamId: string; blogId: string; name: string; slug: string; summary: string | null; avatarFileId: string | null; backgroundFileId: string | null; articleCount: string; followerCount: string; }
export interface TeamSubmission { id: string; sourceArticleId: string; sourceArticleTitle: string; fixedSourceVersionId: string; targetTeamId: string; submittedByUserId: string; supersedesSubmissionId: string | null; status: string; teamReviewerUserId: string | null; teamReviewComment: string | null; teamReviewedAt: string | null; platformReviewerAdminId: string | null; platformReviewComment: string | null; platformReviewedAt: string | null; publishedTeamArticleId: string | null; lockVersion: number; createdAt: string; updatedAt: string; }
export interface TeamMemberProfile { userId: string; displayName: string | null; username: string; avatarFileId: string | null; roleCode: string; }
export interface TeamPortal { team: TeamSummary; ownerDisplayName: string | null; members: TeamMemberProfile[]; }
export interface TeamWorkspace { team: TeamPortal; viewerRole: string; capabilities: string[]; }
export interface TeamSeriesChapter { articleId: string; title: string; slug: string; publishStatus: string; chapterOrder: number; }
export interface TeamSeries { id: string; teamId: string; title: string; slug: string; summary: string | null; coverFileId: string | null; serializationStatus: "ONGOING" | "COMPLETED" | "PAUSED"; reviewStatus: "DRAFT" | "PENDING_REVIEW" | "APPROVED" | "REJECTED"; reviewComment: string | null; lockVersion: number; publishedAt: string | null; updatedAt: string; chapters: TeamSeriesChapter[]; }
export interface CommunityChatMessage { id: string; senderUserId: string; recipientUserId: string; contentText: string; status: string; readAt: string | null; createdAt: string; }
export interface ArticleCollaboration { id: string; articleId: string; userId: string; username: string | null; displayName: string | null; contributionType: string; canEdit: boolean; attributionOrder: number; status: string; lockVersion: number; createdAt: string; expiresAt: string | null; }

export interface SubmitTeamApplicationInput {
  teamName: string;
  teamSlug: string;
  description?: string;
  idempotencyKey?: string;
}

export class CommunityApiError extends Error {
  constructor(
    message: string,
    public readonly code: number,
    public readonly status: number,
  ) {
    super(message);
    this.name = "CommunityApiError";
  }
}

export function readSession(): CommunitySession | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as CommunitySession;
  } catch {
    window.localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function saveSession(session: CommunitySession | null) {
  if (typeof window === "undefined") return;
  if (session) {
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } else {
    window.localStorage.removeItem(SESSION_KEY);
  }
  window.dispatchEvent(new CustomEvent(SESSION_EVENT));
}

export function publicFileUrl(fileId?: string | null) {
  return fileId && COMMUNITY_API_BASE_URL
    ? `${COMMUNITY_API_BASE_URL}/api/v1/public/files/${encodeURIComponent(fileId)}/content`
    : null;
}

export async function communityRequest<T>(
  path: string,
  init: RequestInit = {},
  authenticated: boolean | "optional" = true,
): Promise<T> {
  if (!COMMUNITY_API_BASE_URL) {
    throw new CommunityApiError(
      "线上预览未配置社区 API；可先浏览原型页面，本地完整业务使用 8849 后端。",
      503,
      503,
    );
  }

  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (authenticated) {
    const session = readSession();
    if (!session && authenticated !== "optional") {
      throw new CommunityApiError("请先登录后继续操作", 401, 401);
    }
    if (session) {
      headers.set(session.tokenName || "pxczxn-community-token", session.tokenValue);
    }
  }

  let response: Response;
  try {
    response = await fetch(`${COMMUNITY_API_BASE_URL}${path}`, {
      ...init,
      headers,
      credentials: "include",
    });
  } catch {
    throw new CommunityApiError(
      `无法连接社区服务（${COMMUNITY_API_BASE_URL}）`,
      503,
      503,
    );
  }

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json() as ApiResult<T>
    : null;

  if (!response.ok || !payload || payload.code !== 200) {
    const code = payload?.code ?? response.status;
    if (code === 401 || response.status === 401) {
      saveSession(null);
    }
    throw new CommunityApiError(
      payload?.message || `请求失败（HTTP ${response.status}）`,
      code,
      response.status,
    );
  }
  return payload.data;
}

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
  login(input: { email: string; password: string }) {
    return communityRequest<CommunitySession>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(input),
    }, false);
  },
  logout() {
    return communityRequest<void>("/api/v1/auth/logout", { method: "POST" });
  },
  me() {
    return communityRequest<CurrentCommunityUser>("/api/v1/account/me");
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
  discoverRankedArticles(sort: DiscoverySort = "LATEST", pageNum = 1, pageSize = 20) {
    return communityRequest<PublicDiscoveryPage>(`/api/v1/public/discover/articles?sort=${sort}&pageNum=${pageNum}&pageSize=${pageSize}`, {}, false);
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
  moments(pageNum = 1, pageSize = 20) {
    return communityRequest<MomentPage>(
      `/api/v1/moments?pageNum=${pageNum}&pageSize=${pageSize}`,
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
  series() { return communityRequest<TeamSeries[]>("/api/v1/series", {}, false); },
  seriesDetail(seriesId: string) { return communityRequest<TeamSeries>(`/api/v1/series/${encodeURIComponent(seriesId)}`, {}, false); },
  teamSeries(teamId: string) { return communityRequest<TeamSeries[]>(`/api/v1/teams/${encodeURIComponent(teamId)}/series`); },
  teamSeriesArticles(teamId: string) { return communityRequest<TeamSeriesChapter[]>(`/api/v1/teams/${encodeURIComponent(teamId)}/series/articles`); },
  createTeamSeries(teamId: string, input: { title: string; slug?: string; summary?: string; coverFileId?: string | null; serializationStatus?: TeamSeries["serializationStatus"] }) { return communityRequest<TeamSeries>(`/api/v1/teams/${encodeURIComponent(teamId)}/series`, { method: "POST", body: JSON.stringify(input) }); },
  updateTeamSeries(seriesId: string, input: { title: string; slug?: string; summary?: string; coverFileId?: string | null; serializationStatus?: TeamSeries["serializationStatus"]; expectedLockVersion: number }) { return communityRequest<TeamSeries>(`/api/v1/series/${encodeURIComponent(seriesId)}`, { method: "PATCH", body: JSON.stringify(input) }); },
  saveSeriesChapters(seriesId: string, articleIds: string[], expectedLockVersion: number) { return communityRequest<TeamSeries>(`/api/v1/series/${encodeURIComponent(seriesId)}/chapters`, { method: "POST", body: JSON.stringify({ articleIds, expectedLockVersion }) }); },
  submitSeriesReview(seriesId: string, expectedLockVersion: number) { return communityRequest<TeamSeries>(`/api/v1/series/${encodeURIComponent(seriesId)}/submit-review`, { method: "POST", body: JSON.stringify({ expectedLockVersion }) }); },
  myTeamSubmissions() { return communityRequest<TeamSubmission[]>("/api/v1/team-submissions/me"); },
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
  chatHistory(peerId: string) { return communityRequest<CommunityChatMessage[]>(`/api/v1/chat/messages/${encodeURIComponent(peerId)}`); },
  sendChatMessage(recipientUserId: string, contentText: string) { return communityRequest<CommunityChatMessage>("/api/v1/chat/messages", { method: "POST", body: JSON.stringify({ recipientUserId, contentText }) }); },
  markChatRead(peerId: string) { return communityRequest<void>(`/api/v1/chat/messages/${encodeURIComponent(peerId)}/read`, { method: "POST" }); },
  chatTicket() { return communityRequest<{ ticket: string; expiresInSeconds: number }>("/api/v1/chat/websocket-ticket", { method: "POST" }); },
};
