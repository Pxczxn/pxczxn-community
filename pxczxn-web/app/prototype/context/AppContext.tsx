/**
 * 星语社区 (pxczxn-community V2.1) - 全局 App Context
 */

"use client";

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import {
  communityApi,
  publicFileUrl,
  type CommunityChatConversation as ApiChatConversation,
  type CommunityChatMessage as ApiChatMessage,
  type CommunityCreatorIdea as ApiCreatorIdea,
  type CommunityNotification as ApiNotification,
  type CurrentCommunityUser,
  type Moment as ApiMoment,
  type PublicArticleSummary,
  type Series as ApiSeries,
  type TeamSummary,
} from '../../lib/community-api';
import {
  ThemeMode,
  User,
  Article,
  Moment,
  Series,
  Team,
  NotificationItem,
  ChatMessage,
  ChatConversation,
  IdeaItem,
  RoutePath,
} from '../types';

interface AppContextType {
  // Navigation & Viewport
  currentRoute: string;
  routeParams: Record<string, string>;
  navigateTo: (route: string, params?: Record<string, string>) => void;
  isCompactViewport: boolean;
  toggleCompactViewport: () => void;
  theme: ThemeMode;
  setTheme: (theme: ThemeMode) => void;
  mobileMenuOpen: boolean;
  setMobileMenuOpen: (open: boolean) => void;

  // User Auth
  user: User | null;
  setUser: (user: User | null) => void;
  isLoggedIn: boolean;
  toggleLogin: () => void;

  // Content Data & Interactivity State
  articles: Article[];
  moments: Moment[];
  seriesList: Series[];
  teams: Team[];
  notifications: NotificationItem[];
  conversations: ChatConversation[];
  activeConversationId: string;
  selectConversation: (peerId: string) => void;
  chatMessages: ChatMessage[];
  ideas: IdeaItem[];

  // Global Actions
  likeArticle: (articleId: string) => void;
  favoriteArticle: (articleId: string) => void;
  likeMoment: (momentId: string) => void;
  favoriteMoment: (momentId: string) => void;
  toggleFollowSeries: (seriesId: string) => void;
  toggleFollowTeam: (teamId: string) => void;
  addMoment: (content: string, type?: Moment['momentType'], link?: string) => Promise<boolean>;
  markNotificationRead: (id: string) => void;
  markAllNotificationsRead: () => void;
  markAllConversationsRead: () => void;
  sendChatMessage: (peerId: string, text: string) => void;
  addIdea: (title: string, content: string, tags: string[]) => void;
  addArticle: (newArticle: Partial<Article>) => Promise<string | null>;
  saveArticleDraft: (newArticle: Partial<Article>, existing?: ArticleDraftState) => Promise<ArticleDraftState | null>;

  // Quick Search
  globalSearchQuery: string;
  setGlobalSearchQuery: (query: string) => void;
  triggerSearch: (query: string) => void;
}

interface ArticleDraftState {
  articleId: string;
  slug: string;
  lockVersion: number;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

function toPrototypeUser(user: CurrentCommunityUser): User {
  return {
    id: user.userId,
    username: user.username,
    displayName: user.displayName || user.username,
    avatar: publicFileUrl(user.avatarFileId) || '',
    bio: user.bio || '',
    blogSlug: user.blogSlug || '',
    blogName: user.blogName || undefined,
    followersCount: 0,
    followingCount: 0,
    articlesCount: 0,
    seriesCount: 0,
    verified: user.verificationStatus === 'VERIFIED',
    role: 'USER',
  };
}

function toPrototypeArticle(article: PublicArticleSummary): Article {
  return {
    id: article.articleId,
    title: article.title,
    slug: article.slug,
    summary: article.summary || '',
    content: '',
    coverImage: publicFileUrl(article.coverFileId) || undefined,
    author: {
      id: article.author.userId,
      username: article.author.username,
      displayName: article.author.displayName || article.author.username,
      avatar: publicFileUrl(article.author.avatarFileId) || '',
      bio: article.author.bio || '',
      blogSlug: '',
      followersCount: 0,
      followingCount: 0,
      articlesCount: 0,
      seriesCount: 0,
      role: 'USER',
    },
    blogId: '',
    publishedAt: article.publishedAt,
    updatedAt: article.updatedAt,
    viewsCount: article.viewCount,
    likesCount: article.likeCount,
    favoritesCount: article.favoriteCount,
    commentsCount: article.commentCount,
    tags: article.tags.map((tag) => tag.name),
    readingTimeMinutes: article.readingTimeMinutes,
    wordCount: article.wordCount,
  };
}

function toPrototypeMoment(moment: ApiMoment): Moment {
  return {
    id: moment.momentId,
    author: {
      id: moment.author.userId,
      username: moment.author.username,
      displayName: moment.author.displayName || moment.author.username,
      avatar: publicFileUrl(moment.author.avatarFileId) || '',
      bio: '',
      blogSlug: moment.blog.slug,
      followersCount: 0,
      followingCount: 0,
      articlesCount: 0,
      seriesCount: 0,
      role: 'USER',
    },
    momentType: moment.momentType as Moment['momentType'],
    textContent: moment.textContent || '',
    linkUrl: moment.linkUrl || undefined,
    articleId: moment.article?.articleId || undefined,
    articleTitle: moment.article?.title || undefined,
    createdAt: moment.createdAt,
    likesCount: moment.likeCount,
    favoritesCount: moment.favoriteCount,
    commentsCount: moment.commentCount,
    isLiked: moment.liked,
    isFavorited: moment.favorited,
    visibility: moment.visibility as Moment['visibility'],
  };
}

function toPrototypeSeries(series: ApiSeries): Series {
  return {
    id: series.id,
    title: series.title,
    slug: series.slug,
    description: series.summary || '',
    coverImage: publicFileUrl(series.coverFileId) || '',
    author: {
      id: series.createdByUserId || '',
      username: series.creatorUsername || '',
      displayName: series.creatorDisplayName || series.creatorUsername || '星语作者',
      avatar: publicFileUrl(series.creatorAvatarFileId) || '',
      bio: '',
      blogSlug: series.blogSlug || '',
      followersCount: 0,
      followingCount: 0,
      articlesCount: 0,
      seriesCount: 0,
      role: 'USER',
    },
    blogName: series.blogName || '',
    isTeam: series.blogType === 'TEAM',
    status: series.serializationStatus === 'COMPLETED' ? 'COMPLETED' : 'ONGOING',
    totalChapters: series.chapterCount,
    followersCount: series.followerCount,
    updatedAt: series.updatedAt,
    isFollowing: series.viewerFollowing,
    chapters: series.chapters.map((chapter) => ({
      id: chapter.articleId,
      index: chapter.chapterOrder,
      articleId: chapter.articleId,
      title: chapter.title,
      readTime: '',
      isRead: chapter.articleId === series.viewerLastReadArticleId,
    })),
  };
}

function toPrototypeTeam(team: TeamSummary): Team {
  return {
    id: team.teamId,
    name: team.name,
    slug: team.slug,
    description: team.summary || '',
    avatar: publicFileUrl(team.avatarFileId) || '',
    bgBanner: publicFileUrl(team.backgroundFileId) || '',
    blogId: team.blogId,
    category: team.category || '社区团队',
    membersCount: 0,
    articlesCount: Number(team.articleCount) || 0,
    seriesCount: 0,
    followersCount: Number(team.followerCount) || 0,
    pendingSubmissionsCount: 0,
    owner: {
      id: '',
      username: '',
      displayName: '',
      avatar: '',
      bio: '',
      blogSlug: '',
      followersCount: 0,
      followingCount: 0,
      articlesCount: 0,
      seriesCount: 0,
      role: 'USER',
    },
    contentDirection: '',
    allowSubmissions: false,
  };
}

function toPrototypeNotification(notification: ApiNotification): NotificationItem {
  return {
    id: notification.notificationId,
    category: notification.category as NotificationItem['category'],
    title: notification.title || notification.notificationType,
    content: notification.content || '',
    createdAt: notification.createdAt,
    isRead: notification.status === 'READ',
    sender: notification.sender ? {
      id: notification.sender.userId,
      username: notification.sender.username,
      displayName: notification.sender.displayName || notification.sender.username,
      avatar: publicFileUrl(notification.sender.avatarFileId) || '',
      bio: '', blogSlug: '', followersCount: 0, followingCount: 0, articlesCount: 0, seriesCount: 0, role: 'USER',
    } : undefined,
    targetUrl: notification.canonicalPath || undefined,
    type: notification.notificationType,
  };
}

function toPrototypeConversation(conversation: ApiChatConversation): ChatConversation {
  return {
    id: conversation.peerUserId,
    peerUser: {
      id: conversation.peerUserId,
      username: conversation.peerUsername,
      displayName: conversation.peerDisplayName || conversation.peerUsername,
      avatar: publicFileUrl(conversation.peerAvatarFileId) || '',
      bio: '', blogSlug: '', followersCount: 0, followingCount: 0, articlesCount: 0, seriesCount: 0, role: 'USER',
    },
    lastMessage: conversation.lastMessage,
    lastTime: conversation.lastMessageAt,
    unreadCount: conversation.unreadCount,
  };
}

function toPrototypeChatMessage(message: ApiChatMessage, currentUserId: string): ChatMessage {
  return { id: message.id, senderId: message.senderUserId, receiverId: message.recipientUserId, text: message.contentText, timestamp: message.createdAt, isSelf: message.senderUserId === currentUserId };
}

function toPrototypeIdea(idea: ApiCreatorIdea): IdeaItem {
  return { id: idea.id, title: idea.title, content: idea.content, tags: idea.tags, createdAt: idea.createdAt, sourceType: idea.sourceType };
}

function routeFromPathname(pathname: string): string {
  if (/^\/articles\/[^/]+/.test(pathname)) return '/articles/:id';
  if (/^\/moments\/[^/]+/.test(pathname)) return '/moments/:id';
  if (/^\/series\/[^/]+/.test(pathname)) return '/series/:id';
  if (/^\/teams\/[^/]+\/workspace/.test(pathname)) return '/teams/:slug/workspace';
  if (/^\/teams\/[^/]+/.test(pathname)) return '/teams/:slug';
  if (/^\/(?:profile|users)\/[^/]+/.test(pathname)) return '/profile/:slug';
  return pathname as RoutePath;
}

function paramsFromPathname(pathname: string): Record<string, string> {
  const segments = pathname.split('/').filter(Boolean);
  if (segments[0] === 'articles' || segments[0] === 'moments' || segments[0] === 'series') {
    return segments[1] ? { id: segments[1] } : {};
  }
  if (segments[0] === 'teams' || segments[0] === 'profile' || segments[0] === 'users') {
    return segments[1] ? { slug: segments[1] } : {};
  }
  return {};
}

function resolveRoute(route: string, params: Record<string, string>): string {
  return Object.entries(params).reduce<string>(
    (resolved, [key, value]) => resolved.replace(`:${key}`, encodeURIComponent(value)),
    route,
  );
}

export function AppProvider({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  // Navigation State
  const currentRoute = routeFromPathname(pathname);
  const routeParams = useMemo(() => paramsFromPathname(pathname), [pathname]);
  const [isCompactViewport, setIsCompactViewport] = useState<boolean>(false);
  const [theme, setThemeState] = useState<ThemeMode>('light');
  const [mobileMenuOpen, setMobileMenuOpen] = useState<boolean>(false);

  // User Auth
  const [user, setUser] = useState<User | null>(null);
  const userId = user?.id;
  const isLoggedIn = user !== null;

  // Data States
  const [articles, setArticles] = useState<Article[]>([]);
  const [moments, setMoments] = useState<Moment[]>([]);
  const [seriesList, setSeriesList] = useState<Series[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [conversations, setConversations] = useState<ChatConversation[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<string>('');
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [ideas, setIdeas] = useState<IdeaItem[]>([]);
  const [globalSearchQuery, setGlobalSearchQuery] = useState<string>('');

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const savedTheme = window.localStorage.getItem('pxczxn-theme');
      if (savedTheme === 'dark' || savedTheme === 'starlight') setThemeState(savedTheme);
      if (savedTheme === 'starry') setThemeState('starlight');
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    let active = true;

    void communityApi.discoverArticles().then((page) => {
      if (active && page.records.length > 0) setArticles(page.records.map(toPrototypeArticle));
    }).catch(() => undefined);

    void communityApi.moments().then((page) => {
      if (active && page.records.length > 0) setMoments(page.records.map(toPrototypeMoment));
    }).catch(() => undefined);

    void communityApi.series().then((items) => {
      if (active && items.length > 0) setSeriesList(items.map(toPrototypeSeries));
    }).catch(() => undefined);

    void communityApi.teams().then((items) => {
      if (active && items.length > 0) setTeams(items.map(toPrototypeTeam));
    }).catch(() => undefined);

    void communityApi.me().then((account) => {
      if (active) setUser(toPrototypeUser(account));
    }).catch(() => undefined);

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!userId) return;
    let active = true;
    void Promise.all([
      communityApi.notifications(),
      communityApi.chatConversations(),
      communityApi.myBlog(),
      communityApi.creatorAnalytics(),
      communityApi.creatorIdeas(),
    ]).then(([notificationPage, chatConversations, , , creatorIdeas]) => {
      if (!active) return;
      setNotifications(notificationPage.records.map(toPrototypeNotification));
      const nextConversations = chatConversations.map(toPrototypeConversation);
      setConversations(nextConversations);
      setActiveConversationId(nextConversations[0]?.id || '');
      setIdeas(creatorIdeas.map(toPrototypeIdea));
    }).catch(() => undefined);
    return () => { active = false; };
  }, [userId]);

  useEffect(() => {
    if (!userId || !activeConversationId) return;
    let active = true;
    void communityApi.chatHistory(activeConversationId).then((history) => {
      if (active) setChatMessages(history.map((message) => toPrototypeChatMessage(message, userId)));
    }).catch(() => undefined);
    void communityApi.markChatRead(activeConversationId).then(() => {
      if (active) setConversations((prev) => prev.map((conversation) => conversation.peerUser.id === activeConversationId
        ? { ...conversation, unreadCount: 0 }
        : conversation));
    }).catch(() => undefined);
    return () => { active = false; };
  }, [activeConversationId, userId]);

  // Apply theme class to <html>
  useEffect(() => {
    const root = document.documentElement;
    root.classList.remove('light', 'dark', 'starlight');
    root.classList.add(theme);
    if (theme === 'dark' || theme === 'starlight') {
      root.classList.add('dark');
    }
    root.dataset.theme = theme === 'starlight' ? 'starry' : theme;
    window.localStorage.setItem('pxczxn-theme', theme === 'starlight' ? 'starry' : theme);
  }, [theme]);

  const setTheme = (mode: ThemeMode) => {
    setThemeState(mode);
  };

  const navigateTo = (route: string, params: Record<string, string> = {}) => {
    setMobileMenuOpen(false);
    router.push(resolveRoute(route, params));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const toggleCompactViewport = () => {
    setIsCompactViewport((prev) => !prev);
  };

  const toggleLogin = () => {
    if (user) {
      void communityApi.logout().catch(() => undefined);
      setUser(null);
      return;
    }
    router.push('/login');
  };

  // Article Actions
  const likeArticle = (articleId: string) => {
    const target = articles.find((article) => article.id === articleId);
    if (!target) return;
    void communityApi.setLike('ARTICLE', articleId, !target.isLiked).then((relationship) => {
      setArticles((prev) => prev.map((article) => article.id === articleId
        ? { ...article, isLiked: relationship.liked, likesCount: relationship.likeCount }
        : article));
    }).catch(() => undefined);
  };

  const favoriteArticle = (articleId: string) => {
    const target = articles.find((article) => article.id === articleId);
    if (!target) return;
    void communityApi.setFavorite('ARTICLE', articleId, !target.isFavorited).then((relationship) => {
      setArticles((prev) => prev.map((article) => article.id === articleId
        ? { ...article, isFavorited: relationship.favorited, favoritesCount: relationship.favoriteCount }
        : article));
    }).catch(() => undefined);
  };

  // Moment Actions
  const likeMoment = (momentId: string) => {
    const target = moments.find((moment) => moment.id === momentId);
    if (!target) return;
    void communityApi.setLike('MOMENT', momentId, !target.isLiked).then((relationship) => {
      setMoments((prev) => prev.map((moment) => moment.id === momentId
        ? { ...moment, isLiked: relationship.liked, likesCount: relationship.likeCount }
        : moment));
    }).catch(() => undefined);
  };

  const favoriteMoment = (momentId: string) => {
    const target = moments.find((moment) => moment.id === momentId);
    if (!target) return;
    void communityApi.setFavorite('MOMENT', momentId, !target.isFavorited).then((relationship) => {
      setMoments((prev) => prev.map((moment) => moment.id === momentId
        ? { ...moment, isFavorited: relationship.favorited, favoritesCount: relationship.favoriteCount }
        : moment));
    }).catch(() => undefined);
  };

  const addMoment = async (content: string, type: Moment['momentType'] = 'TEXT', linkUrl?: string) => {
    if (!user) return false;
    try {
      const result = await communityApi.publishMoment({ momentType: type, textContent: content, linkUrl, visibility: 'PUBLIC' });
      setMoments((prev) => [toPrototypeMoment(result.moment), ...prev]);
      return true;
    } catch {
      return false;
    }
  };

  // Series Actions
  const toggleFollowSeries = (seriesId: string) => {
    const target = seriesList.find((series) => series.id === seriesId);
    if (target) {
      void (target.isFollowing
        ? communityApi.unfollowSeries(seriesId)
        : communityApi.followSeries(seriesId)
      ).then((readerState) => {
        setSeriesList((prev) => prev.map((series) => series.id === seriesId
          ? { ...series, isFollowing: readerState.following, followersCount: readerState.followerCount }
          : series));
      }).catch(() => undefined);
    }
  };

  // Team Actions
  const toggleFollowTeam = (teamId: string) => {
    const target = teams.find((team) => team.id === teamId);
    if (!target?.blogId) return;
    void communityApi.setBlogFollow(target.blogId, !target.isFollowing).then((relationship) => {
      setTeams((prev) => prev.map((team) => team.id === teamId
        ? { ...team, isFollowing: relationship.following, followersCount: relationship.followerCount }
        : team));
    }).catch(() => undefined);
  };

  // Notifications
  const markNotificationRead = (id: string) => {
    void communityApi.readNotification(id)
      .then(() => setNotifications((prev) => prev.map((notification) => notification.id === id ? { ...notification, isRead: true } : notification)))
      .catch(() => undefined);
  };

  const markAllNotificationsRead = () => {
    void communityApi.readAllNotifications()
      .then(() => setNotifications((prev) => prev.map((notification) => ({ ...notification, isRead: true }))))
      .catch(() => undefined);
  };

  const markAllConversationsRead = () => {
    void Promise.all(conversations.filter((conversation) => conversation.unreadCount > 0)
      .map((conversation) => communityApi.markChatRead(conversation.peerUser.id)))
      .then(() => setConversations((prev) => prev.map((conversation) => ({ ...conversation, unreadCount: 0 }))))
      .catch(() => undefined);
  };

  const selectConversation = (peerId: string) => {
    setActiveConversationId(peerId);
  };

  // Chat Actions
  const sendChatMessage = (peerId: string, text: string) => {
    if (!text.trim() || !user) return;
    void communityApi.sendChatMessage(peerId, text.trim()).then((message) => {
      const nextMessage = toPrototypeChatMessage(message, user.id);
      setChatMessages((prev) => [...prev, nextMessage]);
      setConversations((prev) => prev.map((conversation) => conversation.peerUser.id === peerId
        ? { ...conversation, lastMessage: nextMessage.text, lastTime: nextMessage.timestamp }
        : conversation));
    }).catch(() => undefined);
  };

  // Idea Box
  const addIdea = (title: string, content: string, tags: string[]) => {
    void communityApi.createCreatorIdea({ title, content, tags, sourceType: 'MANUAL' })
      .then((idea) => setIdeas((prev) => [toPrototypeIdea(idea), ...prev]))
      .catch(() => undefined);
  };

  // Article Addition
  const addArticle = async (newArticle: Partial<Article>): Promise<string | null> => {
    if (!user) return null;
    const title = newArticle.title?.trim();
    const markdownContent = newArticle.content?.trim();
    if (!title || !markdownContent) return null;
    try {
      const created = await communityApi.createArticle({
        title,
        summary: newArticle.summary?.trim() || markdownContent.slice(0, 160),
        contentMode: 'MARKDOWN',
        markdownContent,
        visibility: 'PUBLIC',
        publishMethod: 'PLATFORM_REVIEW',
        tagIds: [],
        contentFileIds: [],
      });
      const saved = await communityApi.saveArticle(created.articleId, {
        title,
        slug: created.slug,
        summary: newArticle.summary?.trim() || markdownContent.slice(0, 160),
        contentMode: 'MARKDOWN',
        markdownContent,
        visibility: 'PUBLIC',
        publishMethod: 'PLATFORM_REVIEW',
        tagIds: [],
        contentFileIds: [],
        expectedLockVersion: created.lockVersion,
      });
      await communityApi.submitReview(saved.articleId, saved.lockVersion);
      return saved.articleId;
    } catch {
      return null;
    }
  };

  const saveArticleDraft = async (newArticle: Partial<Article>, existing?: ArticleDraftState): Promise<ArticleDraftState | null> => {
    if (!user) return null;
    const title = newArticle.title?.trim();
    const markdownContent = newArticle.content?.trim();
    if (!title || !markdownContent) return null;
    const draftPayload = {
      title,
      summary: newArticle.summary?.trim() || markdownContent.slice(0, 160),
      contentMode: 'MARKDOWN',
      markdownContent,
      visibility: 'PRIVATE',
      publishMethod: 'PLATFORM_REVIEW',
      tagIds: [],
      contentFileIds: [],
    };
    try {
      if (existing) {
        const saved = await communityApi.saveArticle(existing.articleId, {
          ...draftPayload,
          slug: existing.slug,
          expectedLockVersion: existing.lockVersion,
        }, true);
        return { articleId: saved.articleId, slug: saved.slug, lockVersion: saved.lockVersion };
      }
      const created = await communityApi.createArticle(draftPayload);
      const saved = await communityApi.saveArticle(created.articleId, {
        ...draftPayload,
        slug: created.slug,
        expectedLockVersion: created.lockVersion,
      }, true);
      return { articleId: saved.articleId, slug: saved.slug, lockVersion: saved.lockVersion };
    } catch {
      return null;
    }
  };

  // Search
  const triggerSearch = (query: string) => {
    setGlobalSearchQuery(query);
    void communityApi.search(query).catch(() => undefined);
    navigateTo('/search');
  };

  return (
    <AppContext.Provider
      value={{
        currentRoute,
        routeParams,
        navigateTo,
        isCompactViewport,
        toggleCompactViewport,
        theme,
        setTheme,
        mobileMenuOpen,
        setMobileMenuOpen,
        user,
        setUser,
        isLoggedIn,
        toggleLogin,
        articles,
        moments,
        seriesList,
        teams,
        notifications,
        conversations,
        activeConversationId,
        selectConversation,
        chatMessages,
        ideas,
        likeArticle,
        favoriteArticle,
        likeMoment,
        favoriteMoment,
        toggleFollowSeries,
        toggleFollowTeam,
        addMoment,
        markNotificationRead,
        markAllNotificationsRead,
        markAllConversationsRead,
        sendChatMessage,
        addIdea,
        addArticle,
        saveArticleDraft,
        globalSearchQuery,
        setGlobalSearchQuery,
        triggerSearch,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
