/**
 * 星语社区 (pxczxn-community V2.2) - 发现页 (Editorial Explore Hub)
 * PC 端优化：正文 14px，模块间距 16~20px，页面宽度 1440px
 */

import React, { useEffect, useState } from 'react';
import { useApp } from '../../context/AppContext';
import { communityApi, type PlatformTag } from '../../../lib/community-api';
import {
  Compass,
  Tag,
  Sparkles,
  BookOpen,
  UserPlus,
  Users,
  MessageSquare,
  ArrowRight,
  Eye,
  Heart,
  MessageCircle,
  TrendingUp,
} from 'lucide-react';

export const DiscoverView: React.FC = () => {
  const { articles, seriesList, teams, moments, navigateTo, isCompactViewport, likeArticle } = useApp();
  const [hotTags, setHotTags] = useState<PlatformTag[]>([]);

  useEffect(() => {
    let cancelled = false;
    void communityApi.getHotTopics(6)
      .then((tags) => {
        if (!cancelled) setHotTags(tags);
      })
      .catch(() => {
        if (!cancelled) setHotTags([]);
      });
    return () => { cancelled = true; };
  }, []);

  const leadArticle = articles[0];
  const secondaryArticles = articles.slice(1, 3);

  return (
    <div className={`max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 transition-all ${isCompactViewport ? 'py-4' : 'py-6'}`}>

      {/* Editorial Header Banner */}
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 dark:border-slate-800 pb-4">
        <div>
          <div className="flex items-center gap-3">
            <Compass className="w-6 h-6 text-indigo-600 dark:text-indigo-400" />
            <h1 className="text-lg sm:text-xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              全站发现
            </h1>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            探索社区精选内容、热门连载系列、前沿创作者与团队项目
          </p>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-300 px-3 py-1.5 rounded-full font-medium">
            编辑策展
          </span>
        </div>
      </div>

      {/* Module A: Hot Topics & Tags */}
      <div className="mb-6 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
        <div className="flex items-center gap-3 mb-4">
          <Tag className="w-5 h-5 text-amber-500" />
          <h2 className="text-base font-bold text-slate-900 dark:text-white">热门探索主题</h2>
        </div>
        <div className="flex flex-wrap gap-3">
          {hotTags.map((tag) => (
            <button
              key={tag.name}
              onClick={() => navigateTo('/articles')}
              className="flex items-center gap-2 px-4 py-2 bg-slate-100 dark:bg-slate-800/80 hover:bg-indigo-50 dark:hover:bg-indigo-950/80 hover:text-indigo-600 dark:hover:text-indigo-400 text-slate-700 dark:text-slate-200 rounded-full text-sm font-medium transition-colors"
            >
              <span>#{tag.name}</span>
              <span className="text-xs text-slate-400 font-mono">({tag.usageCount})</span>
            </button>
          ))}
        </div>
      </div>

      {/* Module B: Editorial Featured Articles (1 Large Lead + 2 Secondary) */}
      <div className="mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-bold text-slate-900 dark:text-white uppercase tracking-wider flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-indigo-500" />
            精选好文 (Editorial Featured)
          </h2>
          <button
            onClick={() => navigateTo('/articles')}
            className="text-sm text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1"
          >
            进入完整文章库
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">

          {/* Large Lead Card (7 cols) */}
          {leadArticle && (
            <div
              onClick={() => navigateTo('/articles/:id', { id: leadArticle.id })}
              className="lg:col-span-7 group bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl overflow-hidden shadow-sm hover:shadow-lg hover:border-indigo-300 dark:hover:border-indigo-700 transition-all cursor-pointer flex flex-col justify-between"
            >
              {leadArticle.coverImage && (
                <div className="relative h-56 overflow-hidden">
                  <img
                    src={leadArticle.coverImage}
                    alt={leadArticle.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                  <div className="absolute top-4 left-4 bg-indigo-600 text-white text-xs font-bold px-3 py-1 rounded-full shadow">
                    主打精选
                  </div>
                </div>
              )}

              <div className="p-5 flex-1 flex flex-col justify-between space-y-3">
                <div>
                  <div className="flex items-center gap-3 text-sm text-slate-400 mb-2">
                    <img src={leadArticle.author.avatar} alt="" className="w-6 h-6 rounded-full" />
                    <span className="font-semibold text-slate-800 dark:text-slate-200">{leadArticle.author.displayName}</span>
                    <span>· {leadArticle.publishedAt}</span>
                  </div>
                  <h3 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors line-clamp-2">
                    {leadArticle.title}
                  </h3>
                  <p className="text-sm text-slate-500 dark:text-slate-400 line-clamp-2 mt-2">
                    {leadArticle.summary}
                  </p>
                </div>

                <div className="flex items-center justify-between text-sm text-slate-400 pt-3 border-t border-slate-100 dark:border-slate-800">
                  <div className="flex items-center gap-4">
                    <span className="flex items-center gap-1.5"><Eye className="w-4 h-4" />{leadArticle.viewsCount}</span>
                    <span className="flex items-center gap-1.5"><Heart className="w-4 h-4 text-rose-500" />{leadArticle.likesCount}</span>
                    <span className="flex items-center gap-1.5"><MessageCircle className="w-4 h-4" />{leadArticle.commentsCount}</span>
                  </div>
                  <span className="text-xs bg-slate-100 dark:bg-slate-800 px-3 py-1 rounded-full">
                    {leadArticle.readingTimeMinutes} min 读完
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* 2 Secondary Cards (5 cols) */}
          <div className="lg:col-span-5 space-y-5">
            {secondaryArticles.map((art) => (
              <div
                key={art.id}
                onClick={() => navigateTo('/articles/:id', { id: art.id })}
                className="group bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-sm hover:shadow-md hover:border-indigo-300 dark:hover:border-indigo-700 transition-all cursor-pointer flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center gap-3 text-sm text-slate-400 mb-2">
                    <img src={art.author.avatar} alt="" className="w-5 h-5 rounded-full" />
                    <span className="font-semibold text-slate-700 dark:text-slate-300">{art.author.displayName}</span>
                  </div>
                  <h4 className="text-base font-bold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors line-clamp-2">
                    {art.title}
                  </h4>
                  <p className="text-sm text-slate-500 dark:text-slate-400 line-clamp-2 mt-2">
                    {art.summary}
                  </p>
                </div>

                <div className="flex items-center justify-between text-sm text-slate-400 pt-3 mt-3 border-t border-slate-100 dark:border-slate-800">
                  <div className="flex items-center gap-3">
                    <span>{art.viewsCount} 阅读</span>
                    <span>·</span>
                    <span>{art.likesCount} 点赞</span>
                  </div>
                  <span className="text-xs text-indigo-600 dark:text-indigo-400 font-medium">查看全文 →</span>
                </div>
              </div>
            ))}
          </div>

        </div>
      </div>

      {/* Module C & D Grid: Series Showcase + Creators */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

        {/* Module C: Series Showcase (连载精选) */}
        <div className="lg:col-span-7 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <BookOpen className="w-5 h-5 text-purple-500" />
              <h2 className="text-base font-bold text-slate-900 dark:text-white">连载精选 (Series Showcase)</h2>
            </div>
            <button onClick={() => navigateTo('/series')} className="text-sm text-purple-600 dark:text-purple-400 hover:underline">
              探索全站系列
            </button>
          </div>

          <div className="space-y-4">
            {seriesList.slice(0, 2).map((ser) => (
              <div
                key={ser.id}
                onClick={() => navigateTo('/series/:id', { id: ser.id })}
                className="p-4 rounded-xl bg-slate-50/80 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 hover:border-purple-300 dark:hover:border-purple-700 transition-all cursor-pointer flex gap-4"
              >
                <img src={ser.coverImage} alt={ser.title} className="w-18 h-22 rounded-lg object-cover shrink-0 shadow-sm" style={{ width: '72px', height: '88px' }} />
                <div className="flex-1 flex flex-col justify-between">
                  <div>
                    <div className="flex items-center gap-3">
                      <span className={`text-xs px-2 py-1 rounded-lg font-semibold ${
                        ser.status === 'ONGOING'
                          ? 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300'
                          : 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300'
                      }`}>
                        {ser.status === 'ONGOING' ? '连载中' : '已完结'}
                      </span>
                      <span className="text-sm text-slate-400">{ser.blogName}</span>
                    </div>
                    <h3 className="text-sm font-bold text-slate-900 dark:text-white mt-2 hover:text-purple-600 transition-colors">
                      {ser.title}
                    </h3>
                    <p className="text-sm text-slate-500 dark:text-slate-400 line-clamp-1 mt-1">
                      {ser.description}
                    </p>
                  </div>

                  <div className="flex items-center gap-4 text-sm text-slate-400">
                    <span>共 {ser.totalChapters} 章节</span>
                    <span>·</span>
                    <span>{ser.followersCount} 人追更</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Module D: Creators & Public Teams */}
        <div className="lg:col-span-5 space-y-5">

          {/* Public Teams Showcase */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <Users className="w-5 h-5 text-indigo-500" />
                <h2 className="text-base font-bold text-slate-900 dark:text-white">公开创新团队</h2>
              </div>
              <button onClick={() => navigateTo('/teams')} className="text-sm text-indigo-600 dark:text-indigo-400">
                全部团队
              </button>
            </div>

            <div className="space-y-3">
              {teams.slice(0, 2).map((tm) => (
                <div
                  key={tm.id}
                  onClick={() => navigateTo('/teams/:slug', { slug: tm.slug })}
                  className="p-4 rounded-xl border border-slate-100 dark:border-slate-800 hover:border-indigo-200 dark:hover:border-indigo-800 transition-colors cursor-pointer flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <img src={tm.avatar} alt="" className="w-10 h-10 rounded-lg object-cover" />
                    <div>
                      <h3 className="text-sm font-bold text-slate-900 dark:text-white line-clamp-1">{tm.name}</h3>
                      <p className="text-xs text-slate-400">{tm.membersCount} 成员 · {tm.articlesCount} 文章</p>
                    </div>
                  </div>
                  <span className="text-xs bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-300 px-3 py-1.5 rounded-lg font-medium">
                    访问主页
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Moments Preview */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <MessageSquare className="w-5 h-5 text-emerald-500" />
                <h2 className="text-base font-bold text-slate-900 dark:text-white">正在发生 (Moments Live)</h2>
              </div>
              <button onClick={() => navigateTo('/moments')} className="text-sm text-emerald-600 dark:text-emerald-400">
                更多动态
              </button>
            </div>

            {moments[0] && (
              <div
                onClick={() => navigateTo('/moments')}
                className="p-4 rounded-xl bg-slate-50/80 dark:bg-slate-800/40 text-sm text-slate-700 dark:text-slate-300 cursor-pointer hover:bg-slate-100 transition-colors"
              >
                <div className="flex items-center gap-2 text-sm text-slate-400 mb-2">
                  <span className="font-semibold text-slate-800 dark:text-slate-200">{moments[0].author.displayName}</span>
                  <span>· {moments[0].createdAt}</span>
                </div>
                <p className="line-clamp-2">{moments[0].textContent}</p>
              </div>
            )}
          </div>

        </div>
      </div>

    </div>
  );
};
