import { request } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface CommunityDailyMetric {
  date: string
  userCount: number
  articleCount: number
}

export interface CommunityDashboard {
  userCount: number
  activeUserCount: number
  blogCount: number
  articleCount: number
  publishedArticleCount: number
  scheduledArticleCount: number
  publishFailedCount: number
  pendingReviewCount: number
  dailyMetrics: CommunityDailyMetric[]
}

export interface CommunityUser {
  id: string
  username: string
  displayName: string
  email: string
  status: string
  verificationStatus: string
  personalBlogId?: string
  personalBlogName?: string
  lastLoginAt?: string
  createdAt: string
}

export interface CommunityBlog {
  id: string
  blogType: string
  ownerUserId?: string
  ownerUsername?: string
  name: string
  slug: string
  summary?: string
  status: string
  articleCount: number
  followerCount: number
  createdAt: string
  updatedAt: string
}

export interface CommunityArticle {
  id: string
  blogId: string
  blogName: string
  blogSlug: string
  authorUserId: string
  authorUsername: string
  categoryId?: string
  categoryName?: string
  title: string
  slug: string
  summary?: string
  contentMode: string
  visibility: string
  publishMethod: string
  publishStatus: string
  reviewStatus: string
  currentVersionId?: string
  publishedVersionId?: string
  reviewVersionId?: string
  currentVersionNo?: number
  renderedHtml?: string
  tocJson?: string
  wordCount?: number
  readingTimeMinutes?: number
  tags: string[]
  scheduledPublishAt?: string
  publishedAt?: string
  canonicalPath?: string
  viewCount: number
  likeCount: number
  favoriteCount: number
  commentCount: number
  lockVersion: number
  createdAt: string
  updatedAt: string
}

export interface ArticleReviewListItem {
  taskId: string
  articleId: string
  fixedVersionId: string
  articleTitle: string
  authorUserId: string
  authorDisplayName: string
  blogId: string
  blogName: string
  status: string
  riskLevel: string
  reviewStage: string
  reviewType: string
  assigneeAdminId?: string
  resultCode?: string
  taskLockVersion: number
  submittedAt: string
  claimedAt?: string
  completedAt?: string
}

export interface ArticleReviewContent {
  versionId: string
  versionNo: number
  contentMode: string
  richTextJson?: string
  markdownContent?: string
  renderedHtml: string
  plainText: string
  tocJson?: string
  contentHash: string
  wordCount: number
  readingTimeMinutes: number
}

export interface ArticleReviewDetail extends ArticleReviewListItem {
  taskStatus: string
  articleSummary?: string
  visibility: string
  publishStatus: string
  reviewStatus: string
  authorUsername: string
  blogSlug: string
  submittedByUserId: string
  resultReason?: string
  articleLockVersion: number
  content: ArticleReviewContent
}

export interface PlatformTag {
  tagId: string
  name: string
  slug: string
  description?: string
  status: string
  usageCount: number
}

export interface GovernanceEvent {
  id: string
  subjectType: 'COMMENT' | 'MOMENT'
  subjectId: string
  action: string
  actorType: string
  actorUserId?: string
  actorAdminId?: string
  previousStatus?: string
  newStatus: string
  reason?: string
  metadataJson?: string
  createdAt: string
}

export interface CommunityComment {
  id: string
  authorUserId: string
  authorUsername?: string
  authorDisplayName?: string
  targetType: 'ARTICLE' | 'MOMENT'
  targetId: string
  targetTitle?: string
  rootCommentId?: string
  parentCommentId?: string
  replyToUserId?: string
  contentText: string
  renderedHtml: string
  status: string
  likeCount: number
  lockVersion: number
  eventCount: number
  createdAt: string
  updatedAt?: string
  deletedAt?: string
}

export interface CommunityCommentDetail {
  comment: CommunityComment
  events: GovernanceEvent[]
}

export interface CommunityMoment {
  id: string
  actorUserId: string
  actorUsername?: string
  actorDisplayName?: string
  blogId: string
  blogName?: string
  blogSlug?: string
  momentType: string
  textContent?: string
  renderedHtml?: string
  linkUrl?: string
  articleId?: string
  repostMomentId?: string
  visibility: string
  status: string
  likeCount: number
  favoriteCount: number
  commentCount: number
  repostCount: number
  lockVersion: number
  eventCount: number
  createdAt: string
  updatedAt?: string
  deletedAt?: string
}

export interface CommunityMomentDetail {
  moment: CommunityMoment
  events: GovernanceEvent[]
}

export interface CommunityInteraction {
  id: string
  interactionType: 'LIKE' | 'FAVORITE' | 'FOLLOW'
  actorUserId: string
  actorUsername?: string
  actorDisplayName?: string
  targetType: string
  targetId: string
  targetTitle?: string
  notificationLevel?: string
  specialFollow: boolean
  createdAt: string
  updatedAt?: string
}

export type GovernanceAction = 'approve' | 'reject' | 'take-down' | 'restore'

export interface GovernanceTarget {
  id: string
  expectedLockVersion: number
}

export interface GovernanceResult {
  id: string
  status: string
  lockVersion: number
  replay: boolean
  affectedCount: number
  affectedIds: string[]
}

const adminConfig = { baseURL: '' }

export const communityApi = {
  dashboard: () => request<CommunityDashboard>({
    ...adminConfig,
    url: '/admin-api/community/dashboard',
    method: 'get'
  }),
  users: (params: Record<string, unknown>) => request<PageResult<CommunityUser>>({
    ...adminConfig,
    url: '/admin-api/community/users',
    method: 'get',
    params
  }),
  blogs: (params: Record<string, unknown>) => request<PageResult<CommunityBlog>>({
    ...adminConfig,
    url: '/admin-api/community/blogs',
    method: 'get',
    params
  }),
  articles: (params: Record<string, unknown>) => request<PageResult<CommunityArticle>>({
    ...adminConfig,
    url: '/admin-api/community/articles',
    method: 'get',
    params
  }),
  article: (articleId: string) => request<CommunityArticle>({
    ...adminConfig,
    url: `/admin-api/community/articles/${articleId}`,
    method: 'get'
  }),
  comments: (params: Record<string, unknown>) => request<PageResult<CommunityComment>>({
    ...adminConfig,
    url: '/admin-api/community/comments',
    method: 'get',
    params
  }),
  comment: (commentId: string) => request<CommunityCommentDetail>({
    ...adminConfig,
    url: `/admin-api/community/comments/${commentId}`,
    method: 'get'
  }),
  governComment: (
    commentId: string,
    action: GovernanceAction,
    expectedLockVersion: number,
    reason?: string
  ) => request<GovernanceResult>({
    ...adminConfig,
    url: `/admin-api/community/comments/${commentId}/${action}`,
    method: 'post',
    data: { expectedLockVersion, reason }
  }),
  batchGovernComments: (
    action: GovernanceAction,
    targets: GovernanceTarget[],
    reason?: string
  ) => request<GovernanceResult[]>({
    ...adminConfig,
    url: `/admin-api/community/comments/batch/${action}/execute`,
    method: 'post',
    data: { targets, reason }
  }),
  moments: (params: Record<string, unknown>) => request<PageResult<CommunityMoment>>({
    ...adminConfig,
    url: '/admin-api/community/moments',
    method: 'get',
    params
  }),
  moment: (momentId: string) => request<CommunityMomentDetail>({
    ...adminConfig,
    url: `/admin-api/community/moments/${momentId}`,
    method: 'get'
  }),
  governMoment: (
    momentId: string,
    action: GovernanceAction,
    expectedLockVersion: number,
    reason?: string
  ) => request<GovernanceResult>({
    ...adminConfig,
    url: `/admin-api/community/moments/${momentId}/${action}`,
    method: 'post',
    data: { expectedLockVersion, reason }
  }),
  batchGovernMoments: (
    action: GovernanceAction,
    targets: GovernanceTarget[],
    reason?: string
  ) => request<GovernanceResult[]>({
    ...adminConfig,
    url: `/admin-api/community/moments/batch/${action}/execute`,
    method: 'post',
    data: { targets, reason }
  }),
  interactions: (params: Record<string, unknown>) =>
    request<PageResult<CommunityInteraction>>({
      ...adminConfig,
      url: '/admin-api/community/interactions',
      method: 'get',
      params
    }),
  reviews: (params: Record<string, unknown>) => request<PageResult<ArticleReviewListItem>>({
    ...adminConfig,
    url: '/admin-api/community/reviews',
    method: 'get',
    params
  }),
  review: (taskId: string) => request<ArticleReviewDetail>({
    ...adminConfig,
    url: `/admin-api/community/reviews/${taskId}`,
    method: 'get'
  }),
  claimReview: (taskId: string, expectedTaskLockVersion: number) => request<ArticleReviewDetail>({
    ...adminConfig,
    url: `/admin-api/community/reviews/${taskId}/claim`,
    method: 'post',
    data: { expectedTaskLockVersion }
  }),
  decideReview: (
    taskId: string,
    action: 'approve' | 'revision' | 'reject',
    expectedTaskLockVersion: number,
    reason?: string
  ) => request<ArticleReviewDetail>({
    ...adminConfig,
    url: `/admin-api/community/reviews/${taskId}/${action}`,
    method: 'post',
    data: { expectedTaskLockVersion, reason }
  }),
  tags: (params: Record<string, unknown>) => request<PlatformTag[]>({
    ...adminConfig,
    url: '/admin-api/community/tags',
    method: 'get',
    params
  }),
  createTag: (data: Pick<PlatformTag, 'name' | 'slug' | 'description'>) =>
    request<PlatformTag>({
      ...adminConfig,
      url: '/admin-api/community/tags',
      method: 'post',
      data
    }),
  updateTag: (
    tagId: string,
    data: Pick<PlatformTag, 'name' | 'slug' | 'description' | 'status'>
  ) => request<PlatformTag>({
    ...adminConfig,
    url: `/admin-api/community/tags/${tagId}`,
    method: 'patch',
    data
  }),
  deleteTag: (tagId: string) => request<void>({
    ...adminConfig,
    url: `/admin-api/community/tags/${tagId}`,
    method: 'delete'
  })
}
