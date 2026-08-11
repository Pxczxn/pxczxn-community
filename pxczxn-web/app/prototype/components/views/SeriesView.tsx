/**
 * 星语社区 (pxczxn-community V2.1) - 系列页 (Series Hub + Shelf)
 * 核心原则：名称必须为“系列”。双层架构（Layer 1: 我的书架读者视角 + Layer 2: 公开系列库）。
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
    <div className={`max-w-7xl mx-auto px-3 sm:px-4 lg:px-6 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Header Banner */}
      <div className="mb-4 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center space-x-3">
          <div className="w-9 h-9 rounded-xl bg-purple-100 dark:bg-purple-950 text-purple-600 dark:text-purple-400 flex items-center justify-center font-bold shadow-xs">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
              系列
            </h1>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              长内容结构化编排 · 读者追更指针 · 章节目录导航
            </p>
          </div>
        </div>

        <button
          onClick={() => navigateTo('/creator')}
          className="bg-purple-600 hover:bg-purple-700 text-white px-3.5 py-1.5 rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center space-x-1 self-start sm:self-center"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>管理/创建我的系列</span>
        </button>
      </div>

      {/* LAYER 1: 我的书架 (Personal Shelf Area) */}
      <div className="mb-6 bg-gradient-to-r from-purple-500/5 via-indigo-500/5 to-slate-500/5 dark:from-purple-950/30 dark:via-indigo-950/30 border border-purple-200/60 dark:border-purple-800/60 rounded-2xl p-4 shadow-xs">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center space-x-2">
            <Bookmark className="w-4 h-4 text-purple-600 dark:text-purple-400" />
            <h2 className="text-xs font-bold text-slate-900 dark:text-white uppercase tracking-wider">
              第一层：我的书架 (My Reading Shelf)
            </h2>
          </div>
          <span className="text-[11px] text-slate-500">已追更/阅读 ({myShelfSeries.length}) 本</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {myShelfSeries.map((ser) => (
            <div
              key={ser.id}
              className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-xl p-3 flex space-x-3 shadow-xs hover:border-purple-300 transition-colors"
            >
              <img src={ser.coverImage} alt={ser.title} className="w-14 h-18 rounded-lg object-cover shrink-0 shadow-xs" />
              <div className="flex-1 flex flex-col justify-between">
                <div>
                  <h3
                    onClick={() => navigateTo('/series/:id', { id: ser.id })}
                    className="text-xs font-bold text-slate-900 dark:text-white line-clamp-1 hover:text-purple-600 cursor-pointer"
                  >
                    {ser.title}
                  </h3>
                  <p className="text-[10px] text-slate-400 mt-0.5">
                    上次读至：{ser.readingProgress?.lastReadChapterTitle || '第1章 开始阅读'}
                  </p>
                </div>

                {ser.readingProgress && (
                  <div className="mt-2 space-y-1">
                    <div className="flex items-center justify-between text-[10px] text-slate-400 font-mono">
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
                  className="mt-2 py-1 bg-purple-50 dark:bg-purple-950 text-purple-600 dark:text-purple-300 rounded-lg text-[11px] font-semibold hover:bg-purple-100 transition-colors flex items-center justify-center space-x-1"
                >
                  <Play className="w-3 h-3 fill-purple-600 dark:fill-purple-300" />
                  <span>继续阅读</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* LAYER 2: 探索系列库 (Public Catalog) */}
      <div>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3">
          <div className="flex items-center space-x-2">
            <Sparkles className="w-4 h-4 text-indigo-500" />
            <h2 className="text-xs font-bold text-slate-900 dark:text-white uppercase tracking-wider">
              第二层：探索公开系列库 (Series Catalog)
            </h2>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-2 pointer-events-none" />
              <input
                type="text"
                placeholder="搜索系列名称..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-8 pr-3 py-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs text-slate-800 dark:text-slate-100 focus:outline-none"
              />
            </div>

            <div className="flex items-center space-x-1 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-1 rounded-xl text-xs">
              {(['ALL', 'ONGOING', 'COMPLETED'] as const).map((st) => (
                <button
                  key={st}
                  onClick={() => setStatusFilter(st)}
                  className={`px-2.5 py-0.5 rounded-lg text-[11px] font-medium transition-colors ${
                    statusFilter === st
                      ? 'bg-purple-600 text-white font-semibold shadow-xs'
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
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
          {filteredCatalog.map((ser) => (
            <div
              key={ser.id}
              className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 shadow-xs hover:border-purple-300 dark:hover:border-purple-700 transition-all flex flex-col justify-between group"
            >
              <div className="flex space-x-3">
                <img src={ser.coverImage} alt={ser.title} className="w-20 h-26 rounded-xl object-cover shrink-0 shadow-xs group-hover:scale-102 transition-transform" />
                <div className="flex-1 space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className={`text-[10px] px-1.5 py-0.2 rounded font-semibold ${
                      ser.status === 'ONGOING'
                        ? 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300'
                        : 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300'
                    }`}>
                      {ser.status === 'ONGOING' ? '连载中' : '已完结'}
                    </span>
                    <span className="text-[10px] text-slate-400">{ser.blogName}</span>
                  </div>

                  <h3
                    onClick={() => navigateTo('/series/:id', { id: ser.id })}
                    className="text-xs font-bold text-slate-900 dark:text-white hover:text-purple-600 transition-colors cursor-pointer line-clamp-1"
                  >
                    {ser.title}
                  </h3>

                  <p className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-2">
                    {ser.description}
                  </p>
                </div>
              </div>

              <div className="flex items-center justify-between pt-3 mt-3 border-t border-slate-100 dark:border-slate-800 text-xs">
                <span className="text-[11px] text-slate-400">{ser.totalChapters} 章节 · {ser.followersCount} 追更</span>

                <button
                  onClick={() => toggleFollowSeries(ser.id)}
                  className={`px-2.5 py-1 rounded-xl text-xs font-semibold transition-colors ${
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
