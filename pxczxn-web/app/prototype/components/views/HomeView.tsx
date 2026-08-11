/**
 * 星语社区 (pxczxn-community V2.2) - 首页 (Personal Dashboard)
 * PC 端优化：minmax(0, 1fr) + 固定侧栏，正文 14px，模块间距 16~20px
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
    <div className={`max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 ${isCompactViewport ? 'py-4' : 'py-6'}`}>

      {/* Main Grid: Left Main Content + Right Fixed Sidebar */}
      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_360px] gap-6">

        {/* Left Column */}
        <div className="space-y-6">

          {/* Module A: Quick Actions Bar */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-sm flex items-center justify-between gap-4">
            <span className="text-sm font-semibold text-slate-700 dark:text-slate-300 hidden sm:block">
              快捷创作行动：
            </span>
            <div className="flex items-center gap-3 w-full sm:w-auto">
              <motion.button
                whileHover={{ scale: 1.03 }}
                whileTap={{ scale: 0.97 }}
                onClick={() => navigateTo('/editor/new')}
                className="flex-1 sm:flex-initial flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-xl text-sm font-medium transition-colors cursor-pointer shadow-sm"
              >
                <PenSquare className="w-4 h-4" />
                <span>撰写文章</span>
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.03 }}
                whileTap={{ scale: 0.97 }}
                onClick={() => navigateTo('/moments')}
                className="flex-1 sm:flex-initial flex items-center justify-center gap-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 px-5 py-2.5 rounded-xl text-sm font-medium transition-colors cursor-pointer"
              >
                <MessageSquare className="w-4 h-4 text-indigo-500" />
                <span>发布动态</span>
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.03 }}
                whileTap={{ scale: 0.97 }}
                onClick={() => navigateTo('/series')}
                className="flex-1 sm:flex-initial flex items-center justify-center gap-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 px-5 py-2.5 rounded-xl text-sm font-medium transition-colors cursor-pointer"
              >
                <BookOpen className="w-4 h-4 text-purple-500" />
                <span>管理系列</span>
              </motion.button>
            </div>
          </div>

          {/* Module B: Continue Reading */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className="w-6 h-6 rounded bg-purple-100 dark:bg-purple-950 text-purple-600 dark:text-purple-400 flex items-center justify-center">
                  <BookOpen className="w-4 h-4" />
                </div>
                <h2 className="text-base font-bold text-slate-900 dark:text-white">继续阅读 (我的书架指针)</h2>
              </div>
              <button
                onClick={() => navigateTo('/series')}
                className="text-sm text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1"
              >
                全部书架 ({seriesList.length})
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>

            {readingSeries.length > 0 ? (
              <div className="space-y-3">
                {readingSeries.slice(0, 2).map((ser) => (
                  <div
                    key={ser.id}
                    className="p-4 rounded-xl bg-slate-50/80 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 hover:border-indigo-200 dark:hover:border-indigo-800 transition-colors"
                  >
                    <div className="flex items-center gap-4">
                      <img
                        src={ser.coverImage}
                        alt={ser.title}
                        className="w-12 h-14 rounded-lg object-cover shrink-0 shadow-sm"
                      />
                      <div>
                        <h3 className="text-sm font-semibold text-slate-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer" onClick={() => navigateTo('/series/:id', { id: ser.id })}>
                          {ser.title}
                        </h3>
                        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
                          上次读至：{ser.readingProgress?.lastReadChapterTitle}
                        </p>
                        {/* Reading Progress Bar */}
                        <div className="flex items-center gap-3 mt-2 w-52">
                          <div className="flex-1 h-1.5 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full"
                              style={{ width: `${ser.readingProgress?.completedPercentage || 0}%` }}
                            />
                          </div>
                          <span className="text-xs font-mono text-slate-500 dark:text-slate-400">
                            {ser.readingProgress?.completedPercentage}%
                          </span>
                        </div>
                      </div>
                    </div>

                    <button
                      onClick={() => navigateTo('/series/:id', { id: ser.id })}
                      className="self-end sm:self-center flex items-center gap-2 bg-indigo-50 dark:bg-indigo-950/80 text-indigo-600 dark:text-indigo-300 px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-100 transition-colors shrink-0"
                    >
                      <Play className="w-4 h-4 fill-indigo-600 dark:fill-indigo-300" />
                      <span>继续阅读</span>
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-slate-400 py-3">暂无进行中的阅读指针</p>
            )}
          </div>

          {/* Module C: Following Updates */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className="w-6 h-6 rounded bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
                  <UserCheck className="w-4 h-4" />
                </div>
                <h2 className="text-base font-bold text-slate-900 dark:text-white">关注作者与团队动态</h2>
              </div>
              <button
                onClick={() => navigateTo('/articles')}
                className="text-sm text-slate-500 hover:text-indigo-600 dark:hover:text-indigo-400 flex items-center gap-1"
              >
                全部文章
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-4">
              {articles.slice(0, 3).map((art) => (
                <div
                  key={art.id}
                  className="p-4 rounded-xl border border-slate-100 dark:border-slate-800 hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition-colors flex flex-col sm:flex-row justify-between gap-4"
                >
                  <div className="flex-1 space-y-2">
                    <div className="flex items-center gap-3 text-sm text-slate-400">
                      <img src={art.author.avatar} alt="" className="w-5 h-5 rounded-full" />
                      <span className="font-medium text-slate-700 dark:text-slate-300">{art.author.displayName}</span>
                      {art.teamName && (
                        <span className="px-2 py-0.5 bg-purple-50 dark:bg-purple-950 text-purple-600 dark:text-purple-300 rounded font-medium">
                          {art.teamName}
                        </span>
                      )}
                      <span>· {art.publishedAt}</span>
                    </div>

                    <h3
                      onClick={() => navigateTo('/articles/:id', { id: art.id })}
                      className="text-base font-bold text-slate-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors cursor-pointer line-clamp-1"
                    >
                      {art.title}
                    </h3>
                    <p className="text-sm text-slate-500 dark:text-slate-400 line-clamp-1">{art.summary}</p>

                    <div className="flex items-center gap-3 pt-2 flex-wrap">
                      <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium bg-slate-100 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 border border-slate-200/60 dark:border-slate-700/60">
                        <Eye className="w-3.5 h-3.5 text-slate-400" />
                        <span>{art.viewsCount} 次阅读</span>
                      </span>

                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          likeArticle(art.id);
                        }}
                        className={`inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold transition-all ${
                          art.isLiked
                            ? 'bg-rose-100 dark:bg-rose-950 text-rose-600 dark:text-rose-300 border border-rose-300 dark:border-rose-800'
                            : 'bg-rose-50/80 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200/60 dark:border-rose-900/50 hover:bg-rose-100 dark:hover:bg-rose-900/60'
                        }`}
                      >
                        <Heart className={`w-3.5 h-3.5 ${art.isLiked ? 'fill-rose-500 text-rose-500' : 'text-rose-500'}`} />
                        <span>{art.likesCount} 点赞</span>
                      </button>

                      <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium bg-indigo-50/80 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 border border-indigo-200/60 dark:border-indigo-900/50">
                        <MessageSquare className="w-3.5 h-3.5 text-indigo-500" />
                        <span>{art.commentsCount} 评论</span>
                      </span>
                    </div>
                  </div>

                  {art.coverImage && (
                    <img
                      src={art.coverImage}
                      alt={art.title}
                      className="w-full sm:w-28 h-20 rounded-lg object-cover shrink-0"
                    />
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Column: Personal Side Rail */}
        <div className="space-y-6 lg:sticky lg:top-24 self-start">

          {/* Personal Profile Card */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
            <div className="flex items-center gap-4">
              <img
                src={user?.avatar}
                alt={user?.displayName}
                className="w-14 h-14 rounded-full object-cover border-2 border-indigo-500/20"
              />
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  {user?.displayName}
                  <CheckCircle2 className="w-4 h-4 text-indigo-500 fill-indigo-500/20" />
                </h3>
                <p className="text-sm text-slate-400">@{user?.username}</p>
                <span className="inline-block mt-1.5 text-xs bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-300 px-2 py-0.5 rounded font-medium">
                  {user?.role}
                </span>
              </div>
            </div>

            <p className="text-sm text-slate-600 dark:text-slate-300 mt-4 line-clamp-2">{user?.bio}</p>

            {/* Quick Stats */}
            <div className="grid grid-cols-3 gap-3 mt-4 pt-4 border-t border-slate-100 dark:border-slate-800 text-center">
              <div className="p-3 rounded-2xl bg-indigo-50/60 dark:bg-indigo-950/40 border border-indigo-100 dark:border-indigo-900/50 flex flex-col items-center justify-center hover:scale-105 transition-transform">
                <span className="text-base font-extrabold text-indigo-600 dark:text-indigo-400 font-mono">{user?.articlesCount}</span>
                <span className="text-xs font-semibold text-slate-600 dark:text-slate-300 mt-1">文章</span>
              </div>
              <div className="p-3 rounded-2xl bg-purple-50/60 dark:bg-purple-950/40 border border-purple-100 dark:border-purple-900/50 flex flex-col items-center justify-center hover:scale-105 transition-transform">
                <span className="text-base font-extrabold text-purple-600 dark:text-purple-400 font-mono">{user?.seriesCount}</span>
                <span className="text-xs font-semibold text-slate-600 dark:text-slate-300 mt-1">系列</span>
              </div>
              <div className="p-3 rounded-2xl bg-emerald-50/60 dark:bg-emerald-950/40 border border-emerald-100 dark:border-emerald-900/50 flex flex-col items-center justify-center hover:scale-105 transition-transform">
                <span className="text-base font-extrabold text-emerald-600 dark:text-emerald-400 font-mono">{user?.followersCount}</span>
                <span className="text-xs font-semibold text-slate-600 dark:text-slate-300 mt-1">粉丝</span>
              </div>
            </div>

            <button
              onClick={() => navigateTo('/me')}
              className="w-full mt-4 py-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-xl text-sm font-medium transition-colors flex items-center justify-center gap-2"
            >
              <span>进入我的资产空间</span>
              <ArrowUpRight className="w-4 h-4" />
            </button>
          </div>

          {/* My Teams Rail */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <Users className="w-5 h-5 text-indigo-500" />
                <h3 className="text-base font-bold text-slate-900 dark:text-white">我的团队</h3>
              </div>
              <button onClick={() => navigateTo('/teams')} className="text-sm text-indigo-600 dark:text-indigo-400">
                管理
              </button>
            </div>

            <div className="space-y-3">
              {teams.slice(0, 2).map((tm) => (
                <div
                  key={tm.id}
                  onClick={() => navigateTo('/teams/:slug/workspace', { slug: tm.slug })}
                  className="p-3 rounded-xl bg-slate-50/70 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 hover:border-indigo-300 dark:hover:border-indigo-700 transition-colors cursor-pointer flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <img src={tm.avatar} alt={tm.name} className="w-8 h-8 rounded-lg object-cover" />
                    <div>
                      <h4 className="text-sm font-semibold text-slate-900 dark:text-white line-clamp-1">{tm.name}</h4>
                      <p className="text-xs text-slate-400">{tm.myRole || '成员'} · {tm.membersCount} 成员</p>
                    </div>
                  </div>
                  <ChevronRight className="w-4 h-4 text-slate-400" />
                </div>
              ))}
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};
