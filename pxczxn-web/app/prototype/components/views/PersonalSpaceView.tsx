/**
 * 星语社区 (pxczxn-community V2.1) - 我的空间 / 资产中心 (Personal Space & Asset Center)
 * 高度集成的个人知识资产、创作产出、阅读进度指针、收藏夹管理与社交关系中心
 */

import React, { useEffect, useState } from 'react';
import { useApp } from '../../context/AppContext';
import { communityApi, type FavoriteContent, type LikedContent, type SocialCounts, type SocialProfile, type TeamSubmission } from '../../../lib/community-api';
import {
  User,
  FileText,
  Bookmark,
  Heart,
  Clock,
  Users,
  Send,
  Sparkles,
  BookOpen,
  FolderPlus,
  TrendingUp,
  Eye,
  MessageSquare,
  Edit3,
  ExternalLink,
  Plus,
  CheckCircle2,
  AlertCircle,
  Search,
  Filter,
  Share2,
  Trash2,
  Folder,
  ChevronRight,
  ShieldCheck,
  Zap,
} from 'lucide-react';

export const PersonalSpaceView: React.FC = () => {
  const { user, articles, seriesList, moments, teams, navigateTo, isCompactViewport } = useApp();

  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'CONTENT' | 'FAVORITES' | 'LIKES' | 'READING' | 'SOCIAL' | 'SUBMISSIONS'>('OVERVIEW');

  // Helper to format tab navigation counts: if > 999 display '999+'
  const formatTabCount = (count: number) => {
    if (count > 999) return '999+';
    return count.toString();
  };

  // Favorites folder state
  const [selectedFolder, setSelectedFolder] = useState<string>('');
  const [folders, setFolders] = useState<Array<{ id: string; name: string; count: number; isPrivate: boolean }>>([]);
  const [favoriteItems, setFavoriteItems] = useState<FavoriteContent[]>([]);
  const [likedItems, setLikedItems] = useState<LikedContent[]>([]);
  const [socialCounts, setSocialCounts] = useState<SocialCounts>({ following: 0, followers: 0, mutual: 0 });
  const [socialProfiles, setSocialProfiles] = useState<SocialProfile[]>([]);
  const [readingSeries, setReadingSeries] = useState<Array<{
    id: string;
    title: string;
    readingProgress?: { lastReadChapterTitle: string; progressPercentage: number };
  }>>([]);
  const [teamSubmissions, setTeamSubmissions] = useState<TeamSubmission[]>([]);
  const [newFolderName, setNewFolderName] = useState('');
  const [showAddFolder, setShowAddFolder] = useState(false);

  // Content Filter
  const [contentFilter, setContentFilter] = useState<'ALL' | 'PUBLISHED' | 'DRAFT' | 'TEAM'>('ALL');
  const [contentSearch, setContentSearch] = useState('');

  // Social Filter
  const [socialTab, setSocialTab] = useState<'FOLLOWING' | 'FOLLOWERS' | 'TEAMS'>('FOLLOWING');
  const [socialSearch, setSocialSearch] = useState('');

  const activeFolderId = selectedFolder || folders.find((folder) => !folder.isPrivate)?.id || folders[0]?.id || '';

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      communityApi.favoriteFolders(),
      communityApi.myLikes(),
      communityApi.socialCounts(),
      communityApi.myFollowing(),
      communityApi.myFollowers(),
      communityApi.myReadingSeries(),
      communityApi.myTeamSubmissions(),
    ]).then(([favoriteFolders, likes, counts, following, followers, reading, submissions]) => {
      if (cancelled) return;
      setFolders(favoriteFolders.map((folder) => ({
        id: folder.folderId,
        name: folder.name,
        count: folder.itemCount,
        isPrivate: folder.visibility === 'PRIVATE',
      })));
      setLikedItems(likes.records);
      setSocialCounts(counts);
      setSocialProfiles([...following.records, ...followers.records.filter((profile) => !following.records.some((item) => item.userId === profile.userId))]);
      setReadingSeries(reading.map((series) => {
        const lastRead = series.chapters.find((chapter) => chapter.articleId === series.viewerLastReadArticleId);
        return {
          id: series.id,
          title: series.title,
          readingProgress: {
            lastReadChapterTitle: lastRead?.title || '从头开始阅读',
            progressPercentage: series.chapterCount ? Math.round((series.viewerReadChapterCount / series.chapterCount) * 100) : 0,
          },
        };
      }));
      setTeamSubmissions(submissions);
    }).catch(() => {
      if (!cancelled) {
        setFolders([]);
        setLikedItems([]);
        setSocialProfiles([]);
        setReadingSeries([]);
        setTeamSubmissions([]);
      }
    });
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!activeFolderId) return;
    let cancelled = false;
    communityApi.favoriteItems(activeFolderId)
      .then((page) => {
        if (!cancelled) setFavoriteItems(page.records);
      })
      .catch(() => {
        if (!cancelled) setFavoriteItems([]);
      });
    return () => { cancelled = true; };
  }, [activeFolderId]);

  const socialUsersData = socialProfiles.map((profile) => ({
    id: profile.userId,
    name: profile.displayName || profile.username,
    handle: `@${profile.username}`,
    avatar: user?.avatar || '',
    bio: profile.bio || '这位创作者暂未填写简介。',
    role: profile.blogName || '社区创作者',
    articlesCount: 0,
    followersCount: 0,
    isMutual: profile.mutual,
    online: false,
    category: profile.following ? 'FOLLOWING' : 'FOLLOWERS',
  }));

  const filteredSocialUsers = socialUsersData.filter((u) => {
    const matchesTab =
      socialTab === 'FOLLOWING'
        ? u.category === 'FOLLOWING' || u.isMutual
        : socialTab === 'FOLLOWERS'
        ? u.category === 'FOLLOWERS' || u.isMutual
        : u.isMutual;
    const matchesSearch =
      u.name.toLowerCase().includes(socialSearch.toLowerCase()) ||
      u.handle.toLowerCase().includes(socialSearch.toLowerCase()) ||
      u.bio.toLowerCase().includes(socialSearch.toLowerCase());
    return matchesTab && matchesSearch;
  });

  // Handle Add Folder
  const handleAddFolder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFolderName.trim()) return;
    try {
      const folder = await communityApi.createFavoriteFolder({ name: newFolderName.trim() });
      setFolders((current) => [...current, { id: folder.folderId, name: folder.name, count: folder.itemCount, isPrivate: folder.visibility === 'PRIVATE' }]);
      setSelectedFolder(folder.folderId);
      setNewFolderName('');
      setShowAddFolder(false);
    } catch { /* Keep the input visible when the server rejects the folder. */ }
  };

  const myArticles = articles.filter((a) => a.author.username === user?.username || true);

  const filteredArticles = myArticles.filter((art) => {
    const matchesSearch = art.title.toLowerCase().includes(contentSearch.toLowerCase()) || art.summary.toLowerCase().includes(contentSearch.toLowerCase());
    if (contentFilter === 'PUBLISHED') return matchesSearch;
    if (contentFilter === 'DRAFT') return matchesSearch && art.isDraft;
    if (contentFilter === 'TEAM') return matchesSearch && art.teamName;
    return matchesSearch;
  });

  return (
    <div className={`max-w-6xl mx-auto px-3 sm:px-4 transition-all ${isCompactViewport ? 'py-3 sm:py-4' : 'py-6'}`}>

      {/* 1. Header Hero Banner */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs mb-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-start sm:items-center space-x-3.5">
            <div className="relative shrink-0">
              <img
                src={user?.avatar}
                alt={user?.displayName}
                className="w-14 h-14 sm:w-16 sm:h-16 rounded-2xl object-cover border-2 border-indigo-500/80 shadow-xs"
              />
              <span className="absolute -bottom-1 -right-1 p-1 bg-indigo-600 text-white rounded-lg text-[10px] shadow-2xs">
                <ShieldCheck className="w-3 h-3" />
              </span>
            </div>

            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-white">
                  {user?.displayName}
                </h1>
                <span className="text-xs px-2 py-0.5 rounded-full bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-400 font-mono font-semibold">
                  @{user?.username}
                </span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-50 dark:bg-amber-950 text-amber-600 dark:text-amber-400 font-bold border border-amber-200/50">
                  星语签约技术作者
                </span>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-1 max-w-xl">
                {user?.bio || '专注前端架构、分布式计算与 AI 工具链沉淀。欢迎交流与联合撰写专栏！'}
              </p>
              <div className="flex items-center space-x-3 text-[11px] text-slate-400 pt-0.5">
                <span>📍 北京 / 远程</span>
                <span>🔗 <a href={user?.website} target="_blank" rel="noreferrer" className="text-indigo-600 dark:text-indigo-400 hover:underline">{user?.website}</a></span>
                <span>⚡ 团队: 星语核心研发组</span>
              </div>
            </div>
          </div>

          {/* Header Social Stats Block */}
          <div className="flex items-center gap-2 sm:gap-4 px-3.5 py-2 bg-slate-50/80 dark:bg-slate-800/60 rounded-xl border border-slate-100 dark:border-slate-800 self-start lg:self-center shrink-0">
            <button
              onClick={() => {
                setActiveTab('SOCIAL');
                setSocialTab('FOLLOWING');
              }}
              className="text-center px-2 py-0.5 rounded-lg hover:bg-slate-200/60 dark:hover:bg-slate-700/60 transition-colors group cursor-pointer"
            >
              <div className="text-xs font-extrabold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                {socialCounts.following.toLocaleString()}
              </div>
              <div className="text-[10px] text-slate-400 font-medium">关注</div>
            </button>

            <div className="h-6 w-px bg-slate-200/80 dark:bg-slate-700/80 shrink-0" />

            <button
              onClick={() => {
                setActiveTab('SOCIAL');
                setSocialTab('FOLLOWERS');
              }}
              className="text-center px-2 py-0.5 rounded-lg hover:bg-slate-200/60 dark:hover:bg-slate-700/60 transition-colors group cursor-pointer"
            >
              <div className="text-xs font-extrabold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                {socialCounts.followers.toLocaleString()}
              </div>
              <div className="text-[10px] text-slate-400 font-medium">粉丝</div>
            </button>

            <div className="h-6 w-px bg-slate-200/80 dark:bg-slate-700/80 shrink-0" />

            <button
              onClick={() => {
                setActiveTab('SOCIAL');
                setSocialTab('TEAMS');
              }}
              className="text-center px-2 py-0.5 rounded-lg hover:bg-slate-200/60 dark:hover:bg-slate-700/60 transition-colors group cursor-pointer"
            >
              <div className="text-xs font-extrabold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                {socialCounts.mutual}
              </div>
              <div className="text-[10px] text-slate-400 font-medium">互关</div>
            </button>

            <div className="h-6 w-px bg-slate-200/80 dark:bg-slate-700/80 shrink-0" />

            <button
              onClick={() => setActiveTab('CONTENT')}
              className="text-center px-2 py-0.5 rounded-lg hover:bg-slate-200/60 dark:hover:bg-slate-700/60 transition-colors group cursor-pointer"
            >
              <div className="text-xs font-extrabold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                {myArticles.length || 48}
              </div>
              <div className="text-[10px] text-slate-400 font-medium">文章</div>
            </button>
          </div>

          <div className="flex items-center space-x-2 shrink-0">
            <button
              onClick={() => navigateTo('/editor/new')}
              className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl shadow-xs transition-all flex items-center gap-1.5"
            >
              <Edit3 className="w-3.5 h-3.5" />
              <span>写新文章</span>
            </button>
            <button
              onClick={() => navigateTo('/creator')}
              className="px-3 py-1.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-200 dark:hover:bg-slate-700 text-xs font-semibold rounded-xl transition-all flex items-center gap-1.5"
            >
              <Sparkles className="w-3.5 h-3.5 text-amber-500" />
              <span>创作者中心</span>
            </button>
          </div>
        </div>
      </div>

      {/* 2. Top Navigation Tabs */}
      <div className="flex items-center space-x-1 sm:space-x-2 border-b border-slate-200 dark:border-slate-800 mb-4 pb-1 text-xs font-semibold overflow-x-auto">
        {[
          { key: 'OVERVIEW', label: '资产概览', icon: <User className="w-3.5 h-3.5" /> },
          { key: 'CONTENT', label: `我的作品 (${formatTabCount(myArticles.length)})`, icon: <FileText className="w-3.5 h-3.5" /> },
          { key: 'FAVORITES', label: '知识收藏夹', icon: <Bookmark className="w-3.5 h-3.5" /> },
          { key: 'LIKES', label: '赞赏与喜欢', icon: <Heart className="w-3.5 h-3.5" /> },
          { key: 'READING', label: '阅读指针', icon: <Clock className="w-3.5 h-3.5" /> },
          { key: 'SOCIAL', label: `社交关系 (${formatTabCount(socialCounts.followers + socialCounts.following)})`, icon: <Users className="w-3.5 h-3.5" /> },
          { key: 'SUBMISSIONS', label: '团队投稿', icon: <Send className="w-3.5 h-3.5" /> },
        ].map((t) => (
          <button
            key={t.key}
            onClick={() => setActiveTab(t.key as any)}
            className={`flex items-center space-x-1.5 px-3 py-2 rounded-xl transition-colors font-semibold whitespace-nowrap ${
              activeTab === t.key
                ? 'bg-indigo-600 text-white shadow-2xs'
                : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            {t.icon}
            <span>{t.label}</span>
          </button>
        ))}
      </div>

      {/* 3. TAB CONTENT */}

      {/* TAB 1: OVERVIEW */}
      {activeTab === 'OVERVIEW' && (
        <div className="space-y-4 text-xs">

          {/* Key Metric Analytics Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 space-y-1">
              <div className="flex items-center justify-between text-slate-400">
                <span>累计创作</span>
                <FileText className="w-4 h-4 text-indigo-500" />
              </div>
              <div className="text-lg font-extrabold text-slate-900 dark:text-white">48 篇</div>
              <div className="text-[11px] text-emerald-600 dark:text-emerald-400 flex items-center gap-1 font-medium">
                <TrendingUp className="w-3 h-3" />
                <span>本月新增 +5 篇</span>
              </div>
            </div>

            <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 space-y-1">
              <div className="flex items-center justify-between text-slate-400">
                <span>知识积累</span>
                <Bookmark className="w-4 h-4 text-purple-500" />
              </div>
              <div className="text-lg font-extrabold text-slate-900 dark:text-white">5 专栏 / 12 收藏夹</div>
              <div className="text-[11px] text-purple-600 dark:text-purple-400 font-medium">
                收录优质技术文档 128 篇
              </div>
            </div>

            <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 space-y-1">
              <div className="flex items-center justify-between text-slate-400">
                <span>社区影响力</span>
                <Users className="w-4 h-4 text-amber-500" />
              </div>
              <div className="text-lg font-extrabold text-slate-900 dark:text-white">1,280 粉丝</div>
              <div className="text-[11px] text-amber-600 dark:text-amber-400 font-medium">
                全站综合排名 Top 2%
              </div>
            </div>

            <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 space-y-1">
              <div className="flex items-center justify-between text-slate-400">
                <span>连续打卡</span>
                <Zap className="w-4 h-4 text-rose-500" />
              </div>
              <div className="text-lg font-extrabold text-slate-900 dark:text-white">42 天</div>
              <div className="text-[11px] text-slate-400">
                本周阅读时长 14.5 小时
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">

            {/* Left 2 Cols: Active Reading Progress & Recent Works */}
            <div className="lg:col-span-2 space-y-4">

              {/* Active Reading Pointer */}
              <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 space-y-3">
                <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
                  <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <BookOpen className="w-4 h-4 text-indigo-500" />
                    <span>追更与阅读指针 (Reading Pointers)</span>
                  </h3>
                  <button onClick={() => setActiveTab('READING')} className="text-indigo-600 dark:text-indigo-400 hover:underline font-semibold">
                    管理全部阅读记录 →
                  </button>
                </div>

                <div className="space-y-2.5">
                  {readingSeries.map((ser) => (
                    <div
                      key={ser.id}
                      className="p-3 bg-slate-50/70 dark:bg-slate-800/40 border border-slate-200/60 dark:border-slate-700/60 rounded-xl flex items-center justify-between gap-3"
                    >
                      <div className="space-y-1 flex-1 min-w-0">
                        <div className="flex items-center space-x-2">
                          <span className="font-bold text-slate-900 dark:text-white truncate">{ser.title}</span>
                          <span className="px-2 py-0.5 rounded-full text-[10px] bg-purple-50 dark:bg-purple-950 text-purple-600 dark:text-purple-300 font-mono shrink-0">
                            已读 {ser.readingProgress?.progressPercentage || 40}%
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate">
                          上次读至：{ser.readingProgress?.lastReadChapterTitle || '第 1 章 基础概念剖析'}
                        </p>
                        <div className="w-full bg-slate-200 dark:bg-slate-700 h-1.5 rounded-full overflow-hidden">
                          <div
                            className="bg-indigo-600 h-full rounded-full transition-all"
                            style={{ width: `${ser.readingProgress?.progressPercentage || 40}%` }}
                          />
                        </div>
                      </div>

                      <button
                        onClick={() => navigateTo('/series/:id', { id: ser.id })}
                        className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl shrink-0 transition-all active:scale-95"
                      >
                        继续阅读
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Recent Works Showcase */}
              <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 space-y-3">
                <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
                  <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <FileText className="w-4 h-4 text-emerald-500" />
                    <span>最新创作沉淀</span>
                  </h3>
                  <button onClick={() => setActiveTab('CONTENT')} className="text-indigo-600 dark:text-indigo-400 hover:underline font-semibold">
                    查看全部 48 篇 →
                  </button>
                </div>

                <div className="space-y-2">
                  {articles.slice(0, 3).map((art) => (
                    <div
                      key={art.id}
                      className="p-3 bg-slate-50/70 dark:bg-slate-800/40 border border-slate-200/60 dark:border-slate-700/60 rounded-xl flex items-center justify-between gap-3 group hover:border-indigo-300 dark:hover:border-indigo-700 transition-colors"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center space-x-2">
                          <span className="font-bold text-slate-900 dark:text-white group-hover:text-indigo-600 transition-colors">{art.title}</span>
                          <span className="text-[10px] px-1.5 py-0.5 rounded-md bg-emerald-50 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-300 font-bold">
                            已发布
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-1">{art.summary}</p>
                        <div className="flex items-center space-x-3 text-[11px] text-slate-400">
                          <span className="flex items-center gap-1"><Eye className="w-3 h-3" /> {art.viewsCount}</span>
                          <span className="flex items-center gap-1"><Heart className="w-3 h-3 text-rose-500" /> {art.likesCount}</span>
                          <span className="flex items-center gap-1"><MessageSquare className="w-3 h-3 text-indigo-500" /> {art.commentsCount}</span>
                        </div>
                      </div>

                      <div className="flex items-center space-x-1 shrink-0">
                        <button
                          onClick={() => navigateTo('/editor/:id', { id: art.id })}
                          className="p-1.5 bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-lg hover:bg-slate-300"
                          title="编辑文章"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => navigateTo('/articles/:id', { id: art.id })}
                          className="p-1.5 bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-300 rounded-lg hover:bg-indigo-100"
                          title="预览文章"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

            </div>

            {/* Right 1 Col: Starred Folders & Quick Actions */}
            <div className="space-y-4">

              {/* Knowledge Folders Quick Access */}
              <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 space-y-3">
                <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
                  <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <Folder className="w-4 h-4 text-amber-500" />
                    <span>知识收藏夹</span>
                  </h3>
                  <button onClick={() => setActiveTab('FAVORITES')} className="text-amber-600 dark:text-amber-400 hover:underline font-semibold">
                    全部分类 →
                  </button>
                </div>

                <div className="space-y-1.5">
                  {folders.map((f) => (
                    <div
                      key={f.id}
                      onClick={() => setActiveTab('FAVORITES')}
                      className="p-2.5 bg-slate-50 dark:bg-slate-800/40 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer flex items-center justify-between transition-colors"
                    >
                      <div className="flex items-center space-x-2">
                        <Folder className="w-4 h-4 text-indigo-500" />
                        <span className="font-semibold text-slate-800 dark:text-slate-200">{f.name}</span>
                      </div>
                      <span className="text-[11px] px-2 py-0.5 rounded-full bg-slate-200 dark:bg-slate-700 font-mono">
                        {f.count}
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Creator Matrix Status */}
              <div className="bg-gradient-to-br from-indigo-600 to-purple-700 text-white rounded-2xl p-4 space-y-3 shadow-md">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-sm">星语创作声誉积分</span>
                  <Sparkles className="w-4 h-4 text-amber-300" />
                </div>
                <div className="text-2xl font-black tracking-tight">2,480 PTS</div>
                <p className="text-[11px] text-indigo-100 leading-relaxed">
                  尊享社区专栏精选推荐位、团队协同高阶极速通道与 API 创作助理额度。
                </p>
                <button
                  onClick={() => navigateTo('/creator')}
                  className="w-full py-1.5 bg-white text-indigo-700 font-bold rounded-xl text-xs hover:bg-indigo-50 transition-all text-center"
                >
                  去创作者中心兑换权益
                </button>
              </div>

            </div>

          </div>

        </div>
      )}

      {/* TAB 2: MY CONTENT (我的作品) */}
      {activeTab === 'CONTENT' && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs space-y-4 text-xs">

          {/* Header & Search/Filter Controls */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100 dark:border-slate-800">
            <div className="flex items-center space-x-2">
              {[
                { key: 'ALL', label: '全部文章' },
                { key: 'PUBLISHED', label: '已发布' },
                { key: 'DRAFT', label: '草稿箱' },
                { key: 'TEAM', label: '团队专栏' },
              ].map((filter) => (
                <button
                  key={filter.key}
                  onClick={() => setContentFilter(filter.key as any)}
                  className={`px-3 py-1 rounded-xl font-semibold transition-colors ${
                    contentFilter === filter.key
                      ? 'bg-indigo-600 text-white'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'
                  }`}
                >
                  {filter.label}
                </button>
              ))}
            </div>

            <div className="relative w-full sm:w-64">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="搜索已创作的文章..."
                value={contentSearch}
                onChange={(e) => setContentSearch(e.target.value)}
                className="w-full pl-8 pr-3 py-1.5 bg-slate-50 dark:bg-slate-800 border rounded-xl"
              />
            </div>
          </div>

          {/* Article List Grid */}
          <div className="space-y-3">
            {filteredArticles.map((art) => (
              <div
                key={art.id}
                className="p-3.5 bg-slate-50/60 dark:bg-slate-800/40 border border-slate-200/60 dark:border-slate-700/60 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:border-indigo-300 dark:hover:border-indigo-700 transition-all"
              >
                <div className="space-y-1.5 flex-1">
                  <div className="flex items-center space-x-2">
                    <h3 className="font-bold text-sm text-slate-900 dark:text-white hover:text-indigo-600 cursor-pointer" onClick={() => navigateTo('/articles/:id', { id: art.id })}>
                      {art.title}
                    </h3>
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300">
                      已发布
                    </span>
                    {art.teamName && (
                      <span className="px-2 py-0.5 rounded-full text-[10px] bg-indigo-50 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300">
                        {art.teamName}
                      </span>
                    )}
                  </div>
                  <p className="text-slate-500 dark:text-slate-400 line-clamp-1">{art.summary}</p>
                  <div className="flex items-center space-x-4 text-slate-400 text-[11px]">
                    <span>发布于 2026-08-09</span>
                    <span>阅读 {art.viewsCount}</span>
                    <span>点赞 {art.likesCount}</span>
                    <span>评论 {art.commentsCount}</span>
                  </div>
                </div>

                <div className="flex items-center space-x-2 shrink-0">
                  <button
                    onClick={() => navigateTo('/editor/:id', { id: art.id })}
                    className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl flex items-center gap-1"
                  >
                    <Edit3 className="w-3.5 h-3.5" />
                    <span>编辑</span>
                  </button>
                  <button
                    onClick={() => navigateTo('/articles/:id', { id: art.id })}
                    className="px-3 py-1.5 bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-200 font-semibold rounded-xl"
                  >
                    预览
                  </button>
                </div>
              </div>
            ))}
          </div>

        </div>
      )}

      {/* TAB 3: FAVORITES (知识收藏夹) */}
      {activeTab === 'FAVORITES' && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-xs">

          {/* Folders List Sidebar */}
          <div className="md:col-span-1 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
              <span className="font-bold text-slate-900 dark:text-white">我的收藏夹</span>
              <button
                onClick={() => setShowAddFolder(true)}
                className="p-1 bg-indigo-50 text-indigo-600 rounded-lg hover:bg-indigo-100 font-bold"
                title="新建收藏夹"
              >
                <Plus className="w-3.5 h-3.5" />
              </button>
            </div>

            {showAddFolder && (
              <form onSubmit={handleAddFolder} className="space-y-2 p-2 bg-slate-50 dark:bg-slate-800 rounded-xl">
                <input
                  type="text"
                  placeholder="收藏夹名称..."
                  value={newFolderName}
                  onChange={(e) => setNewFolderName(e.target.value)}
                  className="w-full px-2 py-1 bg-white dark:bg-slate-900 border rounded-lg text-xs"
                />
                <div className="flex justify-end space-x-1">
                  <button type="button" onClick={() => setShowAddFolder(false)} className="px-2 py-0.5 text-slate-400">取消</button>
                  <button type="submit" className="px-2 py-0.5 bg-indigo-600 text-white rounded-lg font-bold">创建</button>
                </div>
              </form>
            )}

            <div className="space-y-1">
              {folders.map((f) => (
                <button
                  key={f.id}
                  onClick={() => setSelectedFolder(f.id)}
                  className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-left font-semibold transition-all ${
                    activeFolderId === f.id
                      ? 'bg-indigo-600 text-white shadow-2xs'
                      : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                  }`}
                >
                  <div className="flex items-center space-x-2 truncate">
                    <Folder className="w-3.5 h-3.5 shrink-0" />
                    <span className="truncate">{f.name}</span>
                  </div>
                  <span className="text-[10px] opacity-80 font-mono ml-1">{f.count}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Articles inside selected folder */}
          <div className="md:col-span-3 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
              <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                <Bookmark className="w-4 h-4 text-indigo-500" />
                <span>{folders.find((f) => f.id === activeFolderId)?.name || '默认收藏夹'} 中的文档</span>
              </h2>
              <span className="text-slate-400 text-[11px]">共 {favoriteItems.length} 篇沉淀</span>
            </div>

            <div className="space-y-2.5">
              {favoriteItems.map((art) => (
                <div
                  key={art.favoriteItemId}
                  className="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-xl flex items-center justify-between gap-3 hover:border-indigo-300 border border-transparent transition-colors"
                >
                  <div className="space-y-1">
                    <h3
                      onClick={() => navigateTo('/articles/:id', { id: art.targetId })}
                      className="font-bold text-slate-900 dark:text-white hover:text-indigo-600 cursor-pointer"
                    >
                      {art.title}
                    </h3>
                    <p className="text-[11px] text-slate-500 line-clamp-1">{art.excerpt || '暂无摘要'}</p>
                    <div className="flex items-center space-x-2 text-[10px] text-slate-400">
                      <span>收藏于 {art.favoritedAt}</span>
                      <span>•</span>
                      <span>{art.targetType}</span>
                    </div>
                  </div>

                  <div className="flex items-center space-x-2 shrink-0">
                    <button
                      onClick={() => navigateTo('/articles/:id', { id: art.targetId })}
                      className="px-3 py-1 bg-indigo-600 text-white font-bold rounded-lg"
                    >
                      阅读
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

        </div>
      )}

      {/* TAB 4: LIKES */}
      {activeTab === 'LIKES' && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs space-y-3 text-xs">
          <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5 pb-2 border-b">
            <Heart className="w-4 h-4 text-rose-500" />
            <span>我认可并点赞的内容</span>
          </h2>

          <div className="space-y-2.5">
              {likedItems.map((art) => (
              <div key={art.likeId} className="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-xl flex justify-between items-center">
                <div className="space-y-1">
                  <h3 className="font-bold text-slate-900 dark:text-white cursor-pointer hover:text-indigo-600" onClick={() => navigateTo('/articles/:id', { id: art.targetId })}>
                    {art.title}
                  </h3>
                  <p className="text-slate-500 text-[11px]">{art.excerpt || '暂无摘要'}</p>
                </div>
                <button onClick={() => navigateTo('/articles/:id', { id: art.targetId })} className="text-indigo-600 font-bold shrink-0">
                  访问原文
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* TAB 5: READING (阅读中心) */}
      {activeTab === 'READING' && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs space-y-4 text-xs">
          <div className="flex items-center justify-between pb-2 border-b">
            <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-purple-500" />
              <span>阅读时间线与系列进度指针</span>
            </h2>
            <span className="text-slate-400">本周累计精读 14.5 小时</span>
          </div>

          <div className="space-y-3">
            {readingSeries.map((ser) => (
              <div key={ser.id} className="p-3.5 border rounded-2xl bg-slate-50 dark:bg-slate-800/40 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                <div className="space-y-1 flex-1">
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-slate-900 dark:text-white text-sm">{ser.title}</span>
                    <span className="px-2 py-0.5 bg-purple-100 text-purple-700 text-[10px] font-bold rounded-full">专栏追更中</span>
                  </div>
                  <p className="text-slate-500 text-[11px]">上次阅读章节：{ser.readingProgress?.lastReadChapterTitle || '架构演进篇'}</p>
                </div>

                <button
                  onClick={() => navigateTo('/series/:id', { id: ser.id })}
                  className="px-4 py-1.5 bg-purple-600 hover:bg-purple-700 text-white font-bold rounded-xl shrink-0"
                >
                  跳至该位置继续阅读
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* TAB 6: SOCIAL (社交关系) */}
      {activeTab === 'SOCIAL' && (
        <div className="space-y-4 text-xs animate-in fade-in">

          {/* Top Metric Cards - Elegant placement of follower/following/mutual counts */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div
              onClick={() => setSocialTab('FOLLOWING')}
              className={`cursor-pointer p-3.5 rounded-2xl border transition-all ${
                socialTab === 'FOLLOWING'
                  ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                  : 'bg-white dark:bg-slate-900 border-slate-200/80 dark:border-slate-800 text-slate-800 dark:text-slate-200 hover:border-indigo-300'
              }`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className={`text-[11px] font-semibold ${socialTab === 'FOLLOWING' ? 'text-indigo-100' : 'text-slate-500 dark:text-slate-400'}`}>
                  我关注的创作者
                </span>
                <Users className={`w-4 h-4 ${socialTab === 'FOLLOWING' ? 'text-indigo-200' : 'text-indigo-500'}`} />
              </div>
              <div className="text-xl font-extrabold tracking-tight">{socialCounts.following.toLocaleString()} <span className="text-xs font-normal opacity-80">人</span></div>
              <p className={`text-[10px] mt-1 line-clamp-1 ${socialTab === 'FOLLOWING' ? 'text-indigo-100/90' : 'text-slate-400'}`}>
                订阅的技术作者与专栏团队
              </p>
            </div>

            <div
              onClick={() => setSocialTab('FOLLOWERS')}
              className={`cursor-pointer p-3.5 rounded-2xl border transition-all ${
                socialTab === 'FOLLOWERS'
                  ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                  : 'bg-white dark:bg-slate-900 border-slate-200/80 dark:border-slate-800 text-slate-800 dark:text-slate-200 hover:border-indigo-300'
              }`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className={`text-[11px] font-semibold ${socialTab === 'FOLLOWERS' ? 'text-indigo-100' : 'text-slate-500 dark:text-slate-400'}`}>
                  关注我的粉丝
                </span>
                <Users className={`w-4 h-4 ${socialTab === 'FOLLOWERS' ? 'text-indigo-200' : 'text-purple-500'}`} />
              </div>
              <div className="text-xl font-extrabold tracking-tight">{socialCounts.followers.toLocaleString()} <span className="text-xs font-normal opacity-80">人</span></div>
              <p className={`text-[10px] mt-1 line-clamp-1 ${socialTab === 'FOLLOWERS' ? 'text-indigo-100/90' : 'text-slate-400'}`}>
                支持并追更作品的社区同行
              </p>
            </div>

            <div
              onClick={() => setSocialTab('TEAMS')}
              className={`cursor-pointer p-3.5 rounded-2xl border transition-all ${
                socialTab === 'TEAMS'
                  ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                  : 'bg-white dark:bg-slate-900 border-slate-200/80 dark:border-slate-800 text-slate-800 dark:text-slate-200 hover:border-indigo-300'
              }`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className={`text-[11px] font-semibold ${socialTab === 'TEAMS' ? 'text-indigo-100' : 'text-slate-500 dark:text-slate-400'}`}>
                  互相关注 / 总社交网
                </span>
                <Sparkles className={`w-4 h-4 ${socialTab === 'TEAMS' ? 'text-amber-200' : 'text-amber-500'}`} />
              </div>
              <div className="text-xl font-extrabold tracking-tight">{socialCounts.mutual} <span className="text-xs font-normal opacity-80">互关</span></div>
              <p className={`text-[10px] mt-1 line-clamp-1 ${socialTab === 'TEAMS' ? 'text-indigo-100/90' : 'text-slate-400'}`}>
                深度双向互动与联合专栏伙伴
              </p>
            </div>
          </div>

          {/* Wrapped Data Container Box - "后面的数据页包裹一下" */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs space-y-4">

            {/* Header & Controls Toolbar */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center space-x-2">
                <Users className="w-4 h-4 text-indigo-500" />
                <h2 className="font-bold text-slate-900 dark:text-white text-xs sm:text-sm">
                  {socialTab === 'FOLLOWING' && '我关注的创作者列表'}
                  {socialTab === 'FOLLOWERS' && '关注我的粉丝成员'}
                  {socialTab === 'TEAMS' && '互相关注与协同伙伴'}
                </h2>
                <span className="px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-[10px] text-slate-500 font-mono">
                  {socialTab === 'FOLLOWING' && `${socialCounts.following} 位`}
                  {socialTab === 'FOLLOWERS' && `${socialCounts.followers} 位`}
                  {socialTab === 'TEAMS' && `${socialCounts.mutual} 位伙伴`}
                </span>
              </div>

              {/* Search & Filter Input */}
              <div className="flex items-center space-x-2">
                <div className="relative flex-1 sm:w-48">
                  <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    placeholder="搜索名称、ID 或领域..."
                    value={socialSearch}
                    onChange={(e) => setSocialSearch(e.target.value)}
                    className="w-full pl-8 pr-2.5 py-1.5 bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl text-xs focus:bg-white dark:focus:bg-slate-900 transition-all"
                  />
                </div>
              </div>
            </div>

            {/* User Data Cards Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {filteredSocialUsers.map((u) => (
                <div
                  key={u.id}
                  className="p-3.5 bg-slate-50/70 dark:bg-slate-800/40 border border-slate-200/60 dark:border-slate-800/80 rounded-2xl space-y-2.5 hover:border-indigo-300 dark:hover:border-indigo-800 transition-all group"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-center space-x-2.5">
                      <div className="relative">
                        <img
                          src={u.avatar}
                          alt={u.name}
                          className="w-10 h-10 rounded-xl object-cover border border-slate-200 dark:border-slate-700 shrink-0"
                        />
                        {u.online && (
                          <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-emerald-500 border-2 border-white dark:border-slate-900 rounded-full" />
                        )}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center space-x-1">
                          <h3 className="font-bold text-slate-900 dark:text-white truncate group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                            {u.name}
                          </h3>
                        </div>
                        <p className="text-[10px] text-slate-400 font-mono truncate">{u.handle}</p>
                      </div>
                    </div>

                    <span className={`px-2 py-0.5 rounded-md text-[9px] font-bold shrink-0 ${
                      u.isMutual
                        ? 'bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-400 border border-indigo-200/60 dark:border-indigo-800/60'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-500'
                    }`}>
                      {u.isMutual ? '互相关注' : '已关注'}
                    </span>
                  </div>

                  <p className="text-[11px] text-slate-600 dark:text-slate-300 line-clamp-2 leading-snug">
                    {u.bio}
                  </p>

                  <div className="flex items-center justify-between text-[10px] text-slate-400 pt-1 border-t border-slate-100 dark:border-slate-800/80">
                    <span className="font-semibold text-indigo-600 dark:text-indigo-400 bg-indigo-50/50 dark:bg-indigo-950/30 px-1.5 py-0.5 rounded">
                      {u.role}
                    </span>
                    <span>{u.articlesCount} 篇作品 • {u.followersCount} 粉丝</span>
                  </div>

                  <div className="flex items-center gap-1.5 pt-1">
                    <button
                      onClick={() => navigateTo('/chat')}
                      className="flex-1 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-[11px] transition-colors flex items-center justify-center gap-1"
                    >
                      <span>发私信</span>
                    </button>
                    <button
                      onClick={() => navigateTo('/me')}
                      className="px-2.5 py-1.5 bg-slate-200/70 dark:bg-slate-700/60 text-slate-700 dark:text-slate-200 font-medium rounded-xl text-[11px] hover:bg-slate-300 transition-colors"
                    >
                      主页
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {/* Pagination Footer */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-2 pt-3 border-t border-slate-100 dark:border-slate-800 text-[11px] text-slate-400">
              <span>显示 1 - {filteredSocialUsers.length} 项 / 共 {socialTab === 'FOLLOWING' ? socialCounts.following : socialTab === 'FOLLOWERS' ? socialCounts.followers : socialCounts.mutual} 位关系成员</span>
              <div className="flex items-center space-x-1">
                <button disabled className="px-2.5 py-1 bg-slate-100 dark:bg-slate-800 rounded-lg text-slate-300 dark:text-slate-600 cursor-not-allowed">
                  上一页
                </button>
                <button className="px-2.5 py-1 bg-indigo-600 text-white font-bold rounded-lg">1</button>
                <button className="px-2.5 py-1 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 rounded-lg hover:bg-slate-200">2</button>
                <button className="px-2.5 py-1 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 rounded-lg hover:bg-slate-200">3</button>
                <span>...</span>
                <button className="px-2.5 py-1 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 rounded-lg hover:bg-slate-200">
                  下一页
                </button>
              </div>
            </div>

          </div>

        </div>
      )}

      {/* TAB 7: SUBMISSIONS (我的投稿) */}
      {activeTab === 'SUBMISSIONS' && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs space-y-3 text-xs">
          <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5 pb-2 border-b">
            <Send className="w-4 h-4 text-indigo-500" />
            <span>向社区团队专栏的投稿记录</span>
          </h2>

          <div className="space-y-2.5">
            {teamSubmissions.map((sub) => (
              <div key={sub.id} className="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-xl flex items-center justify-between">
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-slate-900 dark:text-white">{sub.sourceArticleTitle}</span>
                    <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 font-bold rounded-md text-[10px]">{sub.status}</span>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-0.5">提交时间：{sub.createdAt}</p>
                </div>
                <button onClick={() => navigateTo('/teams')} className="text-indigo-600 font-bold">查看专栏</button>
              </div>
            ))}
            {teamSubmissions.length === 0 && <p className="py-4 text-center text-slate-400">暂无团队投稿记录</p>}
            {false && (
            [
              { title: 'React 19 & Next.js 16 全栈实战指南', team: '星语核心研发组', status: 'APPROVED', time: '2026-08-09' },
              { title: '星语社区 V2.1 架构演进：从单体博客到多端协作知识矩阵', team: '星语核心研发组', status: 'APPROVED', time: '2026-08-08' },
            ].map((sub, idx) => (
              <div key={idx} className="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-xl flex items-center justify-between">
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-slate-900 dark:text-white">{sub.title}</span>
                    <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 font-bold rounded-md text-[10px]">
                      已通过审阅并归档入册
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-0.5">投稿至：{sub.team} • 提交时间：{sub.time}</p>
                </div>
                <button onClick={() => navigateTo('/teams')} className="text-indigo-600 font-bold">查看专栏</button>
              </div>
            ))
            )}
          </div>
        </div>
      )}

    </div>
  );
};
