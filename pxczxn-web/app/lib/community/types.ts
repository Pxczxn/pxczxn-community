export interface CurrentCommunityUser {
  userId: string;
  username: string;
  displayName: string | null;
  bio: string | null;
  avatarFileId: string | null;
  email: string;
  status: string;
  verificationStatus: string;
  forcePasswordChange: boolean;
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

export interface MomentFeedFilter {
  momentTypes?: string[];
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

/**
 * 团队邀请。团队名与邀请人信息由后端内联返回：被邀请人还不是成员，
 * 无权读取团队详情，缺了这些字段卡片只能显示"团队 #123"。
 */
export interface TeamInvitation {
  id: string;
  teamId: string;
  teamName: string | null;
  teamSlug: string | null;
  teamAvatarFileId: string | null;
  inviteeUserId: string;
  inviteeDisplayName: string | null;
  inviteeUsername: string | null;
  inviteeAvatarFileId: string | null;
  roleCode: "OWNER" | "ADMIN" | "EDITOR" | "AUTHOR";
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "EXPIRED" | "REVOKED";
  inviterUserId: string | null;
  inviterDisplayName: string | null;
  inviterUsername: string | null;
  expiresAt: string;
  createdAt: string;
}

/** 投稿页"选择我的文章"下拉的一条候选（GET /api/v1/team-submissions/candidates）。 */
export interface SubmittableArticle {
  articleId: string;
  title: string;
  publishStatus: string;
  visibility: string;
  updatedAt: string | null;
}
export interface TeamSummary { teamId: string; blogId: string; name: string; slug: string; summary: string | null; avatarFileId: string | null; backgroundFileId: string | null; articleCount: string; followerCount: string; category: string | null; }
export interface TeamSubmission { id: string; sourceArticleId: string; sourceArticleTitle: string; fixedSourceVersionId: string; targetTeamId: string; submittedByUserId: string; supersedesSubmissionId: string | null; status: string; teamReviewerUserId: string | null; teamReviewComment: string | null; teamReviewedAt: string | null; platformReviewerAdminId: string | null; platformReviewComment: string | null; platformReviewedAt: string | null; publishedTeamArticleId: string | null; lockVersion: number; createdAt: string; updatedAt: string; }
export interface TeamPortalSettings {
  category: string | null;
  contentDirection: string | null;
  theme: string | null;
  seoTitle: string | null;
  seoDescription: string | null;
  publicMembers: boolean;
  allowSubmissions: boolean;
  submissionGuideline: string | null;
  contactInfo: string | null;
}
export interface TeamMemberProfile { userId: string; displayName: string | null; username: string; avatarFileId: string | null; roleCode: string; }
export interface TeamPortal { team: TeamSummary; ownerDisplayName: string | null; members: TeamMemberProfile[]; settings: TeamPortalSettings; }
export interface TeamWorkspace { team: TeamPortal; viewerRole: string; capabilities: string[]; permissions: string[]; }
/** One membership card on the "我的团队" tab and one entry of the workspace team switcher. */
export interface MyTeam {
  teamId: string;
  blogId: string;
  name: string;
  slug: string;
  summary: string | null;
  avatarFileId: string | null;
  backgroundFileId: string | null;
  viewerRole: string;
  capabilities: string[];
  permissions: string[];
  memberCount: number;
  articleCount: number;
  seriesCount: number;
  followerCount: number;
  pendingSubmissionCount: number;
  revisionRequiredCount: number;
  joinedAt: string;
  updatedAt: string;
}
export interface TeamStats {
  publishedArticleCount: number;
  draftArticleCount: number;
  reviewingArticleCount: number;
  seriesCount: number;
  memberCount: number;
  followerCount: number;
  totalViewCount: number;
  totalInteractionCount: number;
}
export interface TeamTodo {
  pendingSubmissionCount: number;
  revisionRequiredCount: number;
  pendingInvitationCount: number;
  pendingSeriesReviewCount: number;
  contentRiskCount: number;
}
export interface TeamActivity {
  id: string;
  teamId: string;
  eventType: string;
  targetType: string;
  targetId: string | null;
  actorUserId: string | null;
  actorDisplayName: string | null;
  actorUsername: string | null;
  actorAvatarFileId: string | null;
  occurredAt: string;
}
export interface TeamArticleBrief {
  articleId: string;
  title: string;
  slug: string;
  publishStatus: string;
  reviewStatus: string;
  visibility: string;
  authorUserId: string;
  authorDisplayName: string | null;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  updatedAt: string;
  publishedAt: string | null;
  seriesId: string | null;
  seriesTitle: string | null;
}
export interface TeamDashboard {
  team: TeamSummary;
  viewerRole: string;
  capabilities: string[];
  permissions: string[];
  stats: TeamStats;
  recentArticles: TeamArticleBrief[];
  todos: TeamTodo;
  recentActivities: TeamActivity[];
}
/**
 * 团队成员行。资料字段内联返回，成员页无需再拉一次门户接口按 userId 手动合并。
 * contributionCount 是该成员在团队博客下的存活文章数，无文章时为 0（不是 null）。
 */
export interface TeamMemberView {
  userId: string;
  displayName: string | null;
  username: string | null;
  avatarFileId: string | null;
  roleCode: string;
  joinedAt: string;
  contributionCount: number;
  lastActiveAt: string | null;
}
export interface SeriesChapter { articleId: string; title: string; slug: string; publishStatus: string; chapterOrder: number; }
/**
 * 一个系列永远归属于某个博客（个人或团队），不再直接挂在团队上。
 * blog* / creator* 字段由后端一次性带出，前端渲染"谁在维护"无需二次请求。
 * viewer* 字段只对已登录读者有值，游客拿到 false / null / 0。
 */
export interface Series {
  id: string;
  blogId: string;
  blogName: string | null;
  blogSlug: string | null;
  blogType: "PERSONAL" | "TEAM" | string;
  createdByUserId: string | null;
  creatorDisplayName: string | null;
  creatorUsername: string | null;
  creatorAvatarFileId: string | null;
  title: string;
  slug: string;
  summary: string | null;
  coverFileId: string | null;
  serializationStatus: "ONGOING" | "COMPLETED" | "PAUSED";
  reviewStatus: "DRAFT" | "PENDING_REVIEW" | "APPROVED" | "REJECTED";
  reviewComment: string | null;
  chapterCount: number;
  followerCount: number;
  viewerFollowing: boolean;
  viewerLastReadArticleId: string | null;
  viewerReadChapterCount: number;
  lockVersion: number;
  publishedAt: string | null;
  updatedAt: string;
  chapters: SeriesChapter[];
}
/** 关注 / 取消关注 / 记录进度后的读者态，写操作直接返回，前端无需重新拉整个系列。 */
export interface SeriesReaderState {
  seriesId: string;
  following: boolean;
  followerCount: number;
  lastReadArticleId: string | null;
  readChapterCount: number;
  chapterCount: number;
}
export interface CommunityChatMessage { id: string; senderUserId: string; recipientUserId: string; contentText: string; status: string; readAt: string | null; createdAt: string; }
/** 单篇文章所属系列的上下文：用于文章页"上一篇 / 查看目录 / 下一篇"导航。文章不在任何公开系列时为 null。 */
export interface ArticleSeriesContext {
  seriesId: string;
  seriesSlug: string;
  seriesTitle: string;
  /** 本文在该系列中的章节序号；用于定位上一篇 / 下一篇。 */
  chapterOrder: number | null;
  followerCount: number;
  following: boolean;
  lastReadArticleId: string | null;
  readChapterCount: number;
  /** 该系列已发布的章节（已按章节序号排序）。 */
  chapters: SeriesChapter[];
}
export interface ArticleCollaboration { id: string; articleId: string; userId: string; username: string | null; displayName: string | null; contributionType: string; canEdit: boolean; attributionOrder: number; status: string; lockVersion: number; createdAt: string; expiresAt: string | null; }

export interface SubmitTeamApplicationInput {
  teamName: string;
  teamSlug: string;
  description?: string;
  idempotencyKey?: string;
}

export interface AccountEnforcementCase {
  id: string;
  measureType: string;
  status: string;
  reasonCode: string | null;
  userVisibleReason: string;
  startsAt: string | null;
  expiresAt: string | null;
  appealDeadlineAt: string | null;
  executeAfter: string | null;
}
export interface AccountEnforcementAppeal { id: string; caseId: string; status: string; statement: string; createdAt: string; }