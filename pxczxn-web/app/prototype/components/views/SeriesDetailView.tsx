/**
 * 星语社区 (pxczxn-community V2.1) - 系列详情页 (Series Detail)
 */

import React from 'react';
import { useApp } from '../../context/AppContext';
import { ArrowLeft, BookOpen, Bookmark, Play, CheckCircle2, ChevronRight } from 'lucide-react';

export const SeriesDetailView: React.FC = () => {
  const { seriesList, routeParams, navigateTo, toggleFollowSeries, isCompactViewport } = useApp();

  const seriesId = routeParams.id || 'ser-1';
  const series = seriesList.find((s) => s.id === seriesId) || seriesList[0];

  return (
    <div className={`max-w-4xl mx-auto px-3 sm:px-4 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>
      <button
        onClick={() => navigateTo('/series')}
        className="flex items-center space-x-1 text-xs text-slate-600 dark:text-slate-300 hover:text-purple-600 mb-4"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>返回系列库</span>
      </button>

      {/* Series Banner Card */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs mb-4 flex flex-col sm:flex-row space-y-3 sm:space-y-0 sm:space-x-4">
        <img src={series.coverImage} alt={series.title} className="w-24 h-32 rounded-xl object-cover shrink-0 shadow-md" />
        <div className="flex-1 flex flex-col justify-between space-y-2">
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-[10px] bg-purple-100 text-purple-800 font-bold px-2 py-0.5 rounded">
                {series.status === 'ONGOING' ? '连载中' : '已完结'}
              </span>
              <span className="text-xs text-slate-400">{series.blogName}</span>
            </div>
            <h1 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-white mt-1">
              {series.title}
            </h1>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">{series.description}</p>
          </div>

          <div className="flex items-center space-x-3 text-xs">
            <button
              onClick={() => toggleFollowSeries(series.id)}
              className={`px-3 py-1.5 rounded-xl font-bold transition-colors ${
                series.isFollowing ? 'bg-slate-100 text-slate-600' : 'bg-purple-600 text-white'
              }`}
            >
              {series.isFollowing ? '已追更本系列' : '+ 追更系列'}
            </button>
            <span className="text-slate-400">{series.followersCount} 人追更 · 共 {series.totalChapters} 章节</span>
          </div>
        </div>
      </div>

      {/* Chapters Table of Contents */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs space-y-3">
        <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <BookOpen className="w-4 h-4 text-purple-600" />
          <span>章节目录 (Table of Contents)</span>
        </h2>

        <div className="space-y-2">
          {series.chapters.map((chap) => (
            <div
              key={chap.id}
              onClick={() => navigateTo('/articles/:id', { id: chap.articleId })}
              className="p-3 bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800 rounded-xl flex items-center justify-between text-xs hover:border-purple-300 transition-colors cursor-pointer"
            >
              <div className="flex items-center space-x-2">
                <span className="font-mono text-purple-600 font-bold">第 {chap.index} 章</span>
                <span className="font-semibold text-slate-800 dark:text-slate-200">{chap.title}</span>
                {chap.isRead && <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />}
              </div>
              <span className="text-[10px] text-slate-400">{chap.readTime}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
