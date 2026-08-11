/**
 * 星语社区 (pxczxn-community V2.1) - 首页 (Personal Dashboard)
 * 核心原则：只负责“我”。在 600-700px CSS 可视高度下完整展示核心模块。
 */

import React from 'react';
import { motion } from 'motion/react';
import { useApp } from '../../context/AppContext';
import {
  PenSquare,
  MessageSquare,
  BookOpen,
  Play,
  Bell,
  Users,
  ChevronRight,
  Sparkles,
  TrendingUp,
  Bookmark,
  UserCheck,
  CheckCircle2,
  Clock,
  ArrowUpRight,
  Eye,
  Heart,
} from 'lucide-react';

export const HomeView: React.FC = () => {
  const {
    user,
    seriesList,
    articles,
    moments,
    teams,
    notifications,
    navigateTo,
    isCompactViewport,
    likeArticle,
  } = useApp();

  // Find series with reading progress
  const readingSeries = seriesList.filter((s) => s.readingProgress);
  const unreadNotifs = notifications.filter((n) => !n.isRead);

  return (
    <div className={`max-w-7xl mx-auto px-3 sm:px-4 lg:px-6 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Main Grid: Left Main Content Column + Right Personal Rail */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-3 sm:gap-4">

        {/* Left Column (8 cols) */}
        <div className="lg:col-span-8 space-y-3 sm:space-y-4">

          {/* Module A: Quick Actions Bar (快捷操作) */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3 shadow-xs flex items-center justify-between gap-2">
            <span className="text-xs font-semibold text-slate-700 dark:text-slate-300 hidden sm:inline">
              快捷创作行动：
            </span>
            <div className="flex items-center space-x-2 w-full sm:w-auto">
              <motion.button
                whileHover={{ scale: 1.03 }}
                whileTap={{ scale: 0.96 }}
                onClick={() => navigateTo('/editor/new')}
                className="flex-1 sm:flex-initial flex items-center justify-center space-x-1.5 bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-xl text-xs font-medium transition-colors cursor-pointer shadow-xs"
              >
                <PenSquare className="w-3.5 h-3.5" />
                <span>撰写文章</span>
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.03 }}
                whileTap={{ scale: 0.96 }}
                onClick={() => navigateTo('/moments')}
                className="flex-1 sm:flex-initial flex items-center justify-center space-x-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 px-3 py-1.5 rounded-xl text-xs font-medium transition-colors cursor-pointer"
              >
                <MessageSquare className="w-3.5 h-3.5 text-indigo-500" />
                <span>发布动态</span>
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.03 }}
                whileTap={{ scale: 0.96 }}
                onClick={() => navigateTo('/series')}
                className="flex-1 sm:flex-initial flex items-center justify-center space-x-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 px-3 py-1.5 rounded-xl text-xs font-medium transition-colors cursor-pointer"
              >
                <BookOpen className="w-3.5 h-3.5 text-purple-500" />
                <span>管理系列</span>
              </motion.button>
            </div>
          </div>

          {/* Module B: Continue Reading (继续阅读) */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 shadow-xs">
            <div className="flex items-center justify-between mb-2.5">
              <div className="flex items-center space-x-2">
                <div className="w-5 h-5 rounded bg-purple-100 dark:bg-purple-950 text-purple-600 dark:text-purple-400 flex items-center justify-center">
                  <BookOpen className="w-3 h-3" />
                </div>
                <h2 className="text-xs font-bold text-slate-900 dark:text-white">继续阅读 (我的书架指针)</h2>
              </div>
              <button
                onClick={() => navigateTo('/series')}
                className="text-[11px] text-indigo-600 dark:text-indigo-400 hover:underline flex items-center"
              >
                全部书架 ({seriesList.length})
                <ChevronRight className="w-3 h-3 ml-0.5" />
              </button>
            </div>

            {readingSeries.length > 0 ? (
              <div className="space-y-2">
                {readingSeries.slice(0, 2).map((ser) => (
                  <div
                    key={ser.id}
                    className="p-2.5 rounded-xl bg-slate-50/80 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 hover:border-indigo-200 dark:hover:border-indigo-800 transition-colors"
                  >
                    <div className="flex items-center space-x-3">
                      <img
                        src={ser.coverImage}
                        alt={ser.title}
                        className="w-10 h-12 rounded-lg object-cover shrink-0 shadow-xs"
                      />
                      <div>
                        <h3 className="text-xs font-semibold text-slate-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer" onClick={() => navigateTo('/series/:id', { id: ser.id })}>
                          {ser.title}
                        </h3>
                        <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                          上次读至：{ser.readingProgress?.lastReadChapterTitle}
                        </p>
                        {/* Reading Progress Bar */}
                        <div className="flex items-center space-x-2 mt-1.5 w-44">
                          <div className="flex-1 h-1.5 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full"
                              style={{ width: `${ser.readingProgress?.completedPercentage || 0}%` }}
                            />
                          </div>
                          <span className="text-[10px] font-mono text-slate-500 dark:text-slate-400">
                            {ser.readingProgress?.completedPercentage}%
                          </span>
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={() => navigateTo('/series/:id', { id: ser.id })}
                      className="self-end sm:self-center flex items-center space-x-1 bg-indigo-50 dark:bg-indigo-950/80 text-indigo-600 dark:text-indigo-300 px-2.5 py-1 rounded-lg text-xs font-medium hover:bg-indigo-100 transition-colors shrink-0"
                    >
                      <Play className="w-3 h-3 fill-indigo-600 dark:fill-indigo-300" />
                      <span>继续阅读</span>
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-400 py-2">暂无进行中的阅读指针</p>
            )}
          </div>

          {/* Module C: Following Updates (关注更新 Feed) */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 shadow-xs">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <div className="w-5 h-5 rounded bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
                  <UserCheck className="w-3 h-3" />
                </div>
                <h2 className="text-xs font-bold text-slate-900 dark:text-white">关注作者与团队动态</h2>
              </div>
              <button
                onClick={() => navigateTo('/articles')}
                className="text-[11px] text-slate-500 hover:text-indigo-600 dark:hover:text-indigo-400 flex items-center"
              >
                全部文章
                <ChevronRight className="w-3 h-3 ml-0.5" />
              </button>
            </div>

            <div className="space-y-2.5">
              {articles.slice(0, 3).map((art) => (
                <div
                  key={art.id}
                  className="p-3 rounded-xl border border-slate-100 dark:border-slate-800 hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition-colors flex flex-col sm:flex-row justify-between gap-2"
                >
                  <div className="flex-1 space-y-1">
                    <div className="flex items-center space-x-2 text-[11px] text-slate-400">
                      <img src={art.author.avatar} alt="" className="w-4 h-4 rounded-full" />
                      <span className="font-medium text-slate-700 dark:text-slate-300">{art.author.displayName}</span>
                      {art.teamName && (
                        <span className="px-1.5 py-0.2 bg-purple-50 dark:bg-purple-950 text-purple-600 dark:text-purple-300 rounded font-medium">
                          {art.teamName}
                        </span>
                      )}
                      <span>· {art.publishedAt}</span>
                    </div>

                    <h3
                      onClick={() => navigateTo('/articles/:id', { id: art.id })}
                      className="text-xs font-bold text-slate-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer line-clamp-1"
                    >
                      {art.title}
                    </h3>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-1">{art.summary}</p>

                    <div className="flex items-center gap-2 pt-1.5 flex-wrap">
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-medium bg-slate-100 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 border border-slate-200/60 dark:border-slate-700/60">
                        <Eye className="w-3 h-3 text-slate-400" />
                        <span>{art.viewsCount} 次阅读</span>
                      </span>

                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          likeArticle(art.id);
                        }}
                        className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-semibold transition-all ${
                          art.isLiked
                            ? 'bg-rose-100 dark:bg-rose-950 text-rose-600 dark:text-rose-300 border border-rose-300 dark:border-rose-800'
                            : 'bg-rose-50/80 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200/60 dark:border-rose-900/50 hover:bg-rose-100 dark:hover:bg-rose-900/60'
                        }`}
                      >
                        <Heart className={`w-3 h-3 ${art.isLiked ? 'fill-rose-500 text-rose-500' : 'text-rose-500'}`} />
                        <span>{art.likesCount} 点赞</span>
                      </button>

                      <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-medium bg-indigo-50/80 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 border border-indigo-200/60 dark:border-indigo-900/50">
                        <MessageSquare className="w-3 h-3 text-indigo-500" />
                        <span>{art.commentsCount} 评论</span>
                      </span>
                    </div>
                  </div>

                  {art.coverImage && (
                    <img
                      src={art.coverImage}
                      alt={art.title}
                      className="w-full sm:w-24 h-16 rounded-lg object-cover shrink-0"
                    />
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Column: Personal Side Rail (4 cols) */}
        <div className="lg:col-span-4 space-y-3 sm:space-y-4 lg:sticky lg:top-20 self-start">

          {/* Personal Profile Card */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 shadow-xs">
            <div className="flex items-center space-x-3">
              <img
                src={user?.avatar}
                alt={user?.displayName}
                className="w-12 h-12 rounded-full object-cover border-2 border-indigo-500/20"
              />
              <div>
                <h3 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1">
                  {user?.displayName}
                  <CheckCircle2 className="w-3.5 h-3.5 text-indigo-500 fill-indigo-500/20" />
                </h3>
                <p className="text-[11px] text-slate-400">@{user?.username}</p>
                <span className="inline-block mt-1 text-[10px] bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-300 px-1.5 py-0.2 rounded font-medium">
                  {user?.role}
                </span>
              </div>
            </div>

            <p className="text-[11px] text-slate-600 dark:text-slate-300 mt-2.5 line-clamp-2">{user?.bio}</p>

            {/* Quick Stats Wrapped */}
            <div className="grid grid-cols-3 gap-2 mt-3 pt-3 border-t border-slate-100 dark:border-slate-800 text-center">
              <div className="p-2 rounded-2xl bg-indigo-50/60 dark:bg-indigo-950/40 border border-indigo-100 dark:border-indigo-900/50 flex flex-col items-center justify-center hover:scale-105 transition-transform">
                <span className="text-xs font-extrabold text-indigo-600 dark:text-indigo-400 font-mono">{user?.articlesCount}</span>
                <span className="text-[10px] font-semibold text-slate-600 dark:text-slate-300 mt-0.5">文章</span>
              </div>
              <div className="p-2 rounded-2xl bg-purple-50/60 dark:bg-purple-950/40 border border-purple-100 dark:border-purple-900/50 flex flex-col items-center justify-center hover:scale-105 transition-transform">
                <span className="text-xs font-extrabold text-purple-600 dark:text-purple-400 font-mono">{user?.seriesCount}</span>
                <span className="text-[10px] font-semibold text-slate-600 dark:text-slate-300 mt-0.5">系列</span>
              </div>
              <div className="p-2 rounded-2xl bg-emerald-50/60 dark:bg-emerald-950/40 border border-emerald-100 dark:border-emerald-900/50 flex flex-col items-center justify-center hover:scale-105 transition-transform">
                <span className="text-xs font-extrabold text-emerald-600 dark:text-emerald-400 font-mono">{user?.followersCount}</span>
                <span className="text-[10px] font-semibold text-slate-600 dark:text-slate-300 mt-0.5">粉丝</span>
              </div>
            </div>

            <button
              onClick={() => navigateTo('/me')}
              className="w-full mt-3 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-medium transition-colors flex items-center justify-center space-x-1"
            >
              <span>进入我的资产空间</span>
              <ArrowUpRight className="w-3 h-3" />
            </button>
          </div>

          {/* My Teams Rail */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 shadow-xs">
            <div className="flex items-center justify-between mb-2.5">
              <div className="flex items-center space-x-2">
                <Users className="w-4 h-4 text-indigo-500" />
                <h3 className="text-xs font-bold text-slate-900 dark:text-white">我的团队</h3>
              </div>
              <button onClick={() => navigateTo('/teams')} className="text-[11px] text-indigo-600 dark:text-indigo-400">
                管理
              </button>
            </div>

            <div className="space-y-2">
              {teams.slice(0, 2).map((tm) => (
                <div
                  key={tm.id}
                  onClick={() => navigateTo('/teams/:slug/workspace', { slug: tm.slug })}
                  className="p-2 rounded-xl bg-slate-50/70 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 hover:border-indigo-300 dark:hover:border-indigo-700 transition-colors cursor-pointer flex items-center justify-between"
                >
                  <div className="flex items-center space-x-2">
                    <img src={tm.avatar} alt={tm.name} className="w-7 h-7 rounded-lg object-cover" />
                    <div>
                      <h4 className="text-xs font-semibold text-slate-900 dark:text-white line-clamp-1">{tm.name}</h4>
                      <p className="text-[10px] text-slate-400">{tm.myRole || '成员'} · {tm.membersCount} 成员</p>
                    </div>
                  </div>
                  <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
                </div>
              ))}
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};
