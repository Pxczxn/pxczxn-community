/**
 * 星语社区 (pxczxn-community V2.1) - 全局 App Context
 */

"use client";

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import {
  communityApi,
  publicFileUrl,
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
import {
  currentUser,
  mockArticles,
  mockMoments,
  mockSeries,
  mockTeams,
  mockNotifications,
  mockConversations,
  mockChatMessages,
  mockIdeas,
} from '../data/mockData';

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
  chatMessages: ChatMessage[];
  ideas: IdeaItem[];

  // Global Actions
  likeArticle: (articleId: string) => void;
  favoriteArticle: (articleId: string) => void;
  likeMoment: (momentId: string) => void;
  favoriteMoment: (momentId: string) => void;
  toggleFollowSeries: (seriesId: string) => void;
  toggleFollowTeam: (teamId: string) => void;
  addMoment: (content: string, type?: Moment['momentType'], link?: string) => void;
  markNotificationRead: (id: string) => void;
  markAllNotificationsRead: () => void;
  markAllConversationsRead: () => void;
  sendChatMessage: (peerId: string, text: string) => void;
  addIdea: (title: string, content: string, tags: string[]) => void;
  addArticle: (newArticle: Partial<Article>) => Article;

  // Quick Search
  globalSearchQuery: string;
  setGlobalSearchQuery: (query: string) => void;
  triggerSearch: (query: string) => void;
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
    owner: currentUser,
    contentDirection: '',
    allowSubmissions: false,
  };
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
  const [isCompactViewport, setIsCompactViewport] = useState<boolean>(true); // Default compact mode for 600-700px CSS height!
  const [theme, setThemeState] = useState<ThemeMode>('light');
  const [mobileMenuOpen, setMobileMenuOpen] = useState<boolean>(false);

  // User Auth
  const [user, setUser] = useState<User | null>(currentUser);
  const isLoggedIn = user !== null;

  // Data States
  const [articles, setArticles] = useState<Article[]>(mockArticles);
  const [moments, setMoments] = useState<Moment[]>(mockMoments);
  const [seriesList, setSeriesList] = useState<Series[]>(mockSeries);
  const [teams, setTeams] = useState<Team[]>(mockTeams);
  const [notifications, setNotifications] = useState<NotificationItem[]>(mockNotifications);
  const [conversations, setConversations] = useState<ChatConversation[]>(mockConversations);
  const [activeConversationId, setActiveConversationId] = useState<string>('conv-1');
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>(mockChatMessages);
  const [ideas, setIdeas] = useState<IdeaItem[]>(mockIdeas);
  const [globalSearchQuery, setGlobalSearchQuery] = useState<string>('');

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

  // Apply theme class to <html>
  useEffect(() => {
    const root = document.documentElement;
    root.classList.remove('light', 'dark', 'starlight');
    root.classList.add(theme);
    if (theme === 'dark' || theme === 'starlight') {
      root.classList.add('dark');
    }
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
    setUser((prev) => (prev ? null : currentUser));
  };

  // Article Actions
  const likeArticle = (articleId: string) => {
    setArticles((prev) =>
      prev.map((art) => {
        if (art.id === articleId) {
          const isLiked = !art.isLiked;
          return {
            ...art,
            isLiked,
            likesCount: isLiked ? art.likesCount + 1 : art.likesCount - 1,
          };
        }
        return art;
      })
    );
    const target = articles.find((article) => article.id === articleId);
    if (target) void communityApi.setLike('ARTICLE', articleId, !target.isLiked).catch(() => undefined);
  };

  const favoriteArticle = (articleId: string) => {
    setArticles((prev) =>
      prev.map((art) => {
        if (art.id === articleId) {
          const isFavorited = !art.isFavorited;
          return {
            ...art,
            isFavorited,
            favoritesCount: isFavorited ? art.favoritesCount + 1 : art.favoritesCount - 1,
          };
        }
        return art;
      })
    );
    const target = articles.find((article) => article.id === articleId);
    if (target) void communityApi.setFavorite('ARTICLE', articleId, !target.isFavorited).catch(() => undefined);
  };

  // Moment Actions
  const likeMoment = (momentId: string) => {
    setMoments((prev) =>
      prev.map((m) => {
        if (m.id === momentId) {
          const isLiked = !m.isLiked;
          return {
            ...m,
            isLiked,
            likesCount: isLiked ? m.likesCount + 1 : m.likesCount - 1,
          };
        }
        return m;
      })
    );
    const target = moments.find((moment) => moment.id === momentId);
    if (target) void communityApi.setLike('MOMENT', momentId, !target.isLiked).catch(() => undefined);
  };

  const favoriteMoment = (momentId: string) => {
    setMoments((prev) =>
      prev.map((m) => {
        if (m.id === momentId) {
          const isFavorited = !m.isFavorited;
          return {
            ...m,
            isFavorited,
            favoritesCount: isFavorited ? m.favoritesCount + 1 : m.favoritesCount - 1,
          };
        }
        return m;
      })
    );
    const target = moments.find((moment) => moment.id === momentId);
    if (target) void communityApi.setFavorite('MOMENT', momentId, !target.isFavorited).catch(() => undefined);
  };

  const addMoment = (content: string, type: Moment['momentType'] = 'TEXT', linkUrl?: string) => {
    if (!user) return;
    const newMom: Moment = {
      id: `mom-${Date.now()}`,
      author: user,
      momentType: type,
      textContent: content,
      linkUrl,
      createdAt: '刚刚',
      likesCount: 0,
      favoritesCount: 0,
      commentsCount: 0,
      visibility: 'PUBLIC',
    };
    setMoments([newMom, ...moments]);
  };

  // Series Actions
  const toggleFollowSeries = (seriesId: string) => {
    setSeriesList((prev) =>
      prev.map((s) => {
        if (s.id === seriesId) {
          const isFollowing = !s.isFollowing;
          return {
            ...s,
            isFollowing,
            followersCount: isFollowing ? s.followersCount + 1 : s.followersCount - 1,
          };
        }
        return s;
      })
    );
    const target = seriesList.find((series) => series.id === seriesId);
    if (target) {
      void (target.isFollowing
        ? communityApi.unfollowSeries(seriesId)
        : communityApi.followSeries(seriesId)
      ).catch(() => undefined);
    }
  };

  // Team Actions
  const toggleFollowTeam = (teamId: string) => {
    setTeams((prev) =>
      prev.map((t) => {
        if (t.id === teamId) {
          const isFollowing = !t.isFollowing;
          return {
            ...t,
            isFollowing,
            followersCount: isFollowing ? t.followersCount + 1 : t.followersCount - 1,
          };
        }
        return t;
      })
    );
  };

  // Notifications
  const markNotificationRead = (id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
    );
  };

  const markAllNotificationsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
  };

  const markAllConversationsRead = () => {
    setConversations((prev) => prev.map((c) => ({ ...c, unreadCount: 0 })));
  };

  // Chat Actions
  const sendChatMessage = (peerId: string, text: string) => {
    if (!text.trim() || !user) return;
    const newMsg: ChatMessage = {
      id: `msg-${Date.now()}`,
      senderId: user.id,
      receiverId: peerId,
      text: text.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isSelf: true,
    };
    setChatMessages((prev) => [...prev, newMsg]);

    // Update conversation last message
    setConversations((prev) =>
      prev.map((c) =>
        c.peerUser.id === peerId
          ? {
              ...c,
              lastMessage: text.trim(),
              lastTime: '刚刚',
            }
          : c
      )
    );

    // Simulated reply after 1.5s
    setTimeout(() => {
      const replyMsg: ChatMessage = {
        id: `msg-reply-${Date.now()}`,
        senderId: peerId,
        receiverId: user.id,
        text: '收到！星语社区 V2.1 协同消息测试非常稳定~ 🚀',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        isSelf: false,
      };
      setChatMessages((prev) => [...prev, replyMsg]);
      setConversations((prev) =>
        prev.map((c) =>
          c.peerUser.id === peerId
            ? {
                ...c,
                lastMessage: replyMsg.text,
                lastTime: '刚刚',
              }
            : c
        )
      );
    }, 1500);
  };

  // Idea Box
  const addIdea = (title: string, content: string, tags: string[]) => {
    const newIdea: IdeaItem = {
      id: `idea-${Date.now()}`,
      title,
      content,
      tags,
      createdAt: new Date().toISOString().split('T')[0],
      sourceType: 'MANUAL',
    };
    setIdeas([newIdea, ...ideas]);
  };

  // Article Addition
  const addArticle = (newArticle: Partial<Article>): Article => {
    const created: Article = {
      id: `art-${Date.now()}`,
      title: newArticle.title || '无标题文章',
      slug: (newArticle.title || 'untitled').toLowerCase().replace(/\s+/g, '-'),
      summary: newArticle.summary || '暂无摘要',
      content: newArticle.content || '',
      coverImage: newArticle.coverImage || 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&auto=format&fit=crop&q=80',
      author: user || currentUser,
      blogId: 'pxczxn-blog',
      publishedAt: '刚刚',
      updatedAt: '刚刚',
      viewsCount: 1,
      likesCount: 0,
      favoritesCount: 0,
      commentsCount: 0,
      tags: newArticle.tags || ['系统设计'],
      readingTimeMinutes: 5,
      wordCount: (newArticle.content || '').length,
      version: 'v1.0.0',
    };
    setArticles([created, ...articles]);
    return created;
  };

  // Search
  const triggerSearch = (query: string) => {
    setGlobalSearchQuery(query);
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
