/**
 * 星语社区 (pxczxn-community V2.2) - 系列页 (Series Hub + Shelf)
 * PC 端优化：正文 14px，模块间距 16~20px，卡片 padding p-4/p-5
 */

import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import {
  BookOpen,
  Bookmark,
  Play,
  CheckCircle2,
  Filter,
  Search,
  Sparkles,
  ChevronRight,
  UserCheck,
  Plus,
} from 'lucide-react';

export const SeriesView: React.FC = () => {
  const { seriesList, navigateTo, isCompactViewport, toggleFollowSeries } = useApp();

  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ONGOING' | 'COMPLETED'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Layer 1: My Shelf (Books with reading progress or followed)
  const myShelfSeries = seriesList.filter((s) => s.readingProgress || s.isFollowing);

  // Layer 2: Public Catalog
  const filteredCatalog = seriesList.filter((s) => {
    const matchStatus = statusFilter === 'ALL' || s.status === statusFilter;
    const matchQuery =
      s.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchStatus && matchQuery;
  });

  return (
    <div className={`max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 transition-all ${isCompactViewport ? 'py-4' : 'py-6'}`}>

      {/* Header Banner */}
      <div className="mb-6 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-purple-100 dark:bg-purple-950 text-purple-600 dark:text-purple-400 flex items-center justify-center font-bold shadow-sm">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-lg sm:text-xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
              系列
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
              长内容结构化编排 · 读者追更指针 · 章节目录导航
            </p>
          </div>
        </div>

        <button
          onClick={() => navigateTo('/creator')}
          className="bg-purple-600 hover:bg-purple-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold shadow-sm transition-colors flex items-center gap-2 self-start sm:self-center"
        >
          <Plus className="w-4 h-4" />
          <span>管理/创建我的系列</span>
        </button>
      </div>

      {/* LAYER 1: 我的书架 (Personal Shelf Area) */}
      <div className="mb-6 bg-gradient-to-r from-purple-500/5 via-indigo-500/5 to-slate-500/5 dark:from-purple-950/30 dark:via-indigo-950/30 border border-purple-200/60 dark:border-purple-800/60 rounded-2xl p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <Bookmark className="w-5 h-5 text-purple-600 dark:text-purple-400" />
            <h2 className="text-base font-bold text-slate-900 dark:text-white uppercase tracking-wider">
              第一层：我的书架 (My Reading Shelf)
            </h2>
          </div>
          <span className="text-sm text-slate-500">已追更/阅读 ({myShelfSeries.length}) 本</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
          {myShelfSeries.map((ser) => (
            <div
              key={ser.id}
              className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-xl p-4 flex gap-4 shadow-sm hover:border-purple-300 transition-colors"
            >
              <img src={ser.coverImage} alt={ser.title} className="w-16 h-20 rounded-lg object-cover shrink-0 shadow-sm" />
              <div className="flex-1 flex flex-col justify-between">
                <div>
                  <h3
                    onClick={() => navigateTo('/series/:id', { id: ser.id })}
                    className="text-sm font-bold text-slate-900 dark:text-white line-clamp-1 hover:text-purple-600 cursor-pointer"
                  >
                    {ser.title}
                  </h3>
                  <p className="text-sm text-slate-400 mt-1">
                    上次读至：{ser.readingProgress?.lastReadChapterTitle || '第1章 开始阅读'}
                  </p>
                </div>

                {ser.readingProgress && (
                  <div className="mt-3 space-y-2">
                    <div className="flex items-center justify-between text-xs text-slate-400 font-mono">
                      <span>进度: {ser.readingProgress.completedPercentage}%</span>
                      <span>已读 {ser.readingProgress.lastReadChapterIndex}/{ser.totalChapters} 章</span>
                    </div>
                    <div className="w-full h-1.5 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-purple-500 to-indigo-500"
                        style={{ width: `${ser.readingProgress.completedPercentage}%` }}
                      />
                    </div>
                  </div>
                )}

                <button
                  onClick={() => navigateTo('/series/:id', { id: ser.id })}
                  className="mt-3 py-2 bg-purple-50 dark:bg-purple-950 text-purple-600 dark:text-purple-300 rounded-lg text-sm font-semibold hover:bg-purple-100 transition-colors flex items-center justify-center gap-2"
                >
                  <Play className="w-4 h-4 fill-purple-600 dark:fill-purple-300" />
                  <span>继续阅读</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* LAYER 2: 探索系列库 (Public Catalog) */}
      <div>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-5">
          <div className="flex items-center gap-3">
            <Sparkles className="w-5 h-5 text-indigo-500" />
            <h2 className="text-base font-bold text-slate-900 dark:text-white uppercase tracking-wider">
              第二层：探索公开系列库 (Series Catalog)
            </h2>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
              <input
                type="text"
                placeholder="搜索系列名称..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10 pr-4 py-2.5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-slate-800 dark:text-slate-100 focus:outline-none"
              />
            </div>

            <div className="flex items-center gap-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-1.5 rounded-xl text-sm">
              {(['ALL', 'ONGOING', 'COMPLETED'] as const).map((st) => (
                <button
                  key={st}
                  onClick={() => setStatusFilter(st)}
                  className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                    statusFilter === st
                      ? 'bg-purple-600 text-white font-semibold shadow-sm'
                      : 'text-slate-600 dark:text-slate-300 hover:text-slate-900'
                  }`}
                >
                  {st === 'ALL' && '全部系列'}
                  {st === 'ONGOING' && '连载中'}
                  {st === 'COMPLETED' && '已完结'}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Series Catalog Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
          {filteredCatalog.map((ser) => (
            <div
              key={ser.id}
              className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm hover:border-purple-300 dark:hover:border-purple-700 transition-all flex flex-col justify-between group"
            >
              <div className="flex gap-4">
                <img src={ser.coverImage} alt={ser.title} className="w-20 h-26 rounded-xl object-cover shrink-0 shadow-sm group-hover:scale-[1.02] transition-transform" />
                <div className="flex-1 space-y-2">
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

                  <h3
                    onClick={() => navigateTo('/series/:id', { id: ser.id })}
                    className="text-sm font-bold text-slate-900 dark:text-white hover:text-purple-600 transition-colors cursor-pointer line-clamp-1"
                  >
                    {ser.title}
                  </h3>

                  <p className="text-sm text-slate-500 dark:text-slate-400 line-clamp-2">
                    {ser.description}
                  </p>
                </div>
              </div>

              <div className="flex items-center justify-between pt-4 mt-4 border-t border-slate-100 dark:border-slate-800 text-sm">
                <span className="text-sm text-slate-400">{ser.totalChapters} 章节 · {ser.followersCount} 追更</span>

                <button
                  onClick={() => toggleFollowSeries(ser.id)}
                  className={`px-4 py-2 rounded-xl text-sm font-semibold transition-colors ${
                    ser.isFollowing
                      ? 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'
                      : 'bg-purple-50 dark:bg-purple-950 text-purple-600 dark:text-purple-300 hover:bg-purple-100'
                  }`}
                >
                  {ser.isFollowing ? '已追更' : '+ 追更系列'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
};
