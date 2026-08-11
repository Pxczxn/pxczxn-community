/**
 * 星语社区 (pxczxn-community V2.1) - 全局类型定义
 */

export type ThemeMode = 'light' | 'dark' | 'starlight';

export type UserRole = 'USER' | 'CREATOR' | 'TEAM_ADMIN' | 'ADMIN';

export interface User {
  id: string;
  username: string;
  displayName: string;
  avatar: string;
  bio: string;
  blogSlug: string;
  followersCount: number;
  followingCount: number;
  articlesCount: number;
  seriesCount: number;
  verified?: boolean;
  role: UserRole;
  location?: string;
  website?: string;
  tags?: string[];
  blogName?: string;
}

export type ContentVisibility = 'PUBLIC' | 'PRIVATE' | 'TEAM_ONLY' | 'UNLISTED';

export interface Article {
  id: string;
  title: string;
  slug: string;
  summary: string;
  content: string; // Markdown or HTML
  coverImage?: string;
  author: User;
  blogId: string;
  teamId?: string;
  teamName?: string;
  publishedAt: string;
  updatedAt: string;
  viewsCount: number;
  likesCount: number;
  favoritesCount: number;
  commentsCount: number;
  tags: string[];
  readingTimeMinutes: number;
  wordCount: number;
  isLiked?: boolean;
  isFavorited?: boolean;
  seriesId?: string;
  seriesTitle?: string;
  seriesChapterIndex?: number;
  coAuthors?: { user: User; role: string }[];
  version?: string;
  precedingArticleId?: string;
  followingArticleId?: string;
  isDraft?: boolean;
}

export type MomentType = 'TEXT' | 'ARTICLE_SHARE' | 'LINK' | 'REPOST';

export interface Moment {
  id: string;
  author: User;
  momentType: MomentType;
  textContent: string;
  linkUrl?: string;
  articleId?: string;
  articleTitle?: string;
  repostMomentId?: string;
  createdAt: string;
  likesCount: number;
  favoritesCount: number;
  commentsCount: number;
  isLiked?: boolean;
  isFavorited?: boolean;
  visibility: ContentVisibility;
}

export type SeriesStatus = 'ONGOING' | 'COMPLETED';

export interface SeriesChapter {
  id: string;
  index: number;
  articleId: string;
  title: string;
  readTime: string;
  isRead?: boolean;
}

export interface Series {
  id: string;
  title: string;
  slug: string;
  description: string;
  coverImage: string;
  author: User;
  blogName: string;
  isTeam: boolean;
  status: SeriesStatus;
  totalChapters: number;
  followersCount: number;
  updatedAt: string;
  chapters: SeriesChapter[];
  readingProgress?: {
    lastReadChapterIndex: number;
    lastReadChapterTitle: string;
    completedPercentage: number;
    progressPercentage?: number;
  };
  isFollowing?: boolean;
}

export type TeamRole = 'OWNER' | 'ADMIN' | 'EDITOR' | 'AUTHOR';

export interface TeamMember {
  user: User;
  role: TeamRole;
  joinedAt: string;
  articlesCount: number;
}

export interface Team {
  id: string;
  name: string;
  slug: string;
  description: string;
  avatar: string;
  bgBanner: string;
  blogId: string;
  category: string;
  membersCount: number;
  articlesCount: number;
  seriesCount: number;
  followersCount: number;
  pendingSubmissionsCount: number;
  myRole?: TeamRole;
  owner: User;
  isFollowing?: boolean;
  contentDirection: string;
  allowSubmissions: boolean;
  submissionGuidelines?: string;
  isMyTeam?: boolean;
}

export type SubmissionStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'NEEDS_REVISION';

export interface Submission {
  id: string;
  articleId: string;
  articleTitle: string;
  author: User;
  teamId: string;
  teamName: string;
  submittedAt: string;
  status: SubmissionStatus;
  reviewComment?: string;
}

export type NotificationCategory =
  | 'INTERACTION'
  | 'FOLLOW'
  | 'COMMENT'
  | 'COAUTHOR'
  | 'SUBMISSION'
  | 'TEAM'
  | 'REVIEW'
  | 'SYSTEM';

export interface NotificationItem {
  id: string;
  category: NotificationCategory;
  title: string;
  content: string;
  createdAt: string;
  isRead: boolean;
  sender?: User;
  targetUrl?: string;
  type?: string;
  timestamp?: string;
}

export interface ChatMessage {
  id: string;
  senderId: string;
  receiverId: string;
  text: string;
  timestamp: string;
  isSelf: boolean;
}

export interface ChatConversation {
  id: string;
  peerUser: User;
  lastMessage: string;
  lastTime: string;
  unreadCount: number;
}

export interface CommentItem {
  id: string;
  author: User;
  content: string;
  createdAt: string;
  likesCount: number;
  isLiked?: boolean;
  replies?: CommentItem[];
}

export interface IdeaItem {
  id: string;
  title: string;
  content: string;
  tags: string[];
  createdAt: string;
  sourceType: 'ARTICLE' | 'MOMENT' | 'MANUAL';
}

export interface GovernanceCase {
  id: string;
  targetType: 'ARTICLE' | 'MOMENT' | 'COMMENT' | 'USER' | 'TEAM';
  targetTitle: string;
  reason: string;
  status: 'PENDING' | 'RESOLVED' | 'APPEALED';
  createdAt: string;
  result?: string;
}

export type RoutePath =
  | '/'
  | '/discover'
  | '/articles'
  | '/blogs'
  | '/articles/:id'
  | '/moments'
  | '/moments/:id'
  | '/series'
  | '/series/:id'
  | '/teams'
  | '/teams/:slug/workspace'
  | '/teams/:slug'
  | '/profile/:slug'
  | '/profile/:username'
  | '/users/:username'
  | '/me'
  | '/space'
  | '/creator'
  | '/settings'
  | '/search'
  | '/notifications'
  | '/chat'
  | '/editor/new'
  | '/editor/:id'
  | '/governance'
  | '/admin';
