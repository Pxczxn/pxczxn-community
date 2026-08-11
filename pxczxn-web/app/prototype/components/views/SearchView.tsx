/**
 * 星语社区 (pxczxn-community V2.1) - 全局跨实体搜索 (Global Cross-Entity Search)
 */

import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import {
  Search,
  FileText,
  BookOpen,
  MessageSquare,
  Users,
  ArrowRight,
  Filter,
} from 'lucide-react';

export const SearchView: React.FC = () => {
  const { globalSearchQuery, setGlobalSearchQuery, articles, seriesList, moments, teams, navigateTo, isCompactViewport } = useApp();

  const [activeTab, setActiveTab] = useState<'ALL' | 'ARTICLES' | 'SERIES' | 'MOMENTS' | 'TEAMS'>('ALL');

  const formatTabCount = (count: number) => {
    if (count > 999) return '999+';
    return count.toString();
  };

  const q = globalSearchQuery.toLowerCase().trim();

  const matchedArticles = articles.filter(
    (a) => a.title.toLowerCase().includes(q) || a.summary.toLowerCase().includes(q) || a.tags.some((t) => t.toLowerCase().includes(q))
  );

  const matchedSeries = seriesList.filter(
    (s) => s.title.toLowerCase().includes(q) || s.description.toLowerCase().includes(q)
  );

  const matchedMoments = moments.filter((m) => m.textContent.toLowerCase().includes(q));

  const matchedTeams = teams.filter(
    (t) => t.name.toLowerCase().includes(q) || t.description.toLowerCase().includes(q)
  );

  const totalSearchCount = matchedArticles.length + matchedSeries.length + matchedMoments.length + matchedTeams.length;

  return (
    <div className={`max-w-5xl mx-auto px-3 sm:px-4 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Search Input Bar */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs mb-4">
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="全站全局搜索：文章、系列、思考动态、团队与用户..."
            value={globalSearchQuery}
            onChange={(e) => setGlobalSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 text-xs sm:text-sm bg-slate-50 dark:bg-slate-800 border rounded-xl focus:outline-hidden focus:border-indigo-500 font-medium"
          />
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center space-x-2 border-b border-slate-200 dark:border-slate-800 mb-4 pb-1 text-xs font-semibold overflow-x-auto">
        {[
          { key: 'ALL', label: `全部结果 (${formatTabCount(totalSearchCount)})` },
          { key: 'ARTICLES', label: `文章 (${formatTabCount(matchedArticles.length)})` },
          { key: 'SERIES', label: `系列 (${formatTabCount(matchedSeries.length)})` },
          { key: 'MOMENTS', label: `思考动态 (${formatTabCount(matchedMoments.length)})` },
          { key: 'TEAMS', label: `团队 (${formatTabCount(matchedTeams.length)})` },
        ].map((t) => (
          <button
            key={t.key}
            onClick={() => setActiveTab(t.key as any)}
            className={`px-3 py-1.5 rounded-xl transition-colors whitespace-nowrap font-semibold ${
              activeTab === t.key ? 'bg-indigo-600 text-white shadow-2xs' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Search Results Display */}
      <div className="space-y-4">

        {/* ARTICLES */}
        {(activeTab === 'ALL' || activeTab === 'ARTICLES') && matchedArticles.length > 0 && (
          <div className="bg-white dark:bg-slate-900 border rounded-2xl p-4 space-y-2.5 text-xs">
            <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
              <FileText className="w-4 h-4 text-indigo-500" />
              <span>匹配文章</span>
            </h2>
            {matchedArticles.map((art) => (
              <div
                key={art.id}
                onClick={() => navigateTo('/articles/:id', { id: art.id })}
                className="p-2.5 bg-slate-50 dark:bg-slate-800/40 rounded-xl hover:border-indigo-300 border border-transparent transition-colors cursor-pointer flex justify-between items-center"
              >
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white">{art.title}</h3>
                  <p className="text-[11px] text-slate-500 line-clamp-1 mt-0.5">{art.summary}</p>
                </div>
                <button className="text-indigo-600 font-semibold shrink-0 ml-3">查看</button>
              </div>
            ))}
          </div>
        )}

        {/* SERIES */}
        {(activeTab === 'ALL' || activeTab === 'SERIES') && matchedSeries.length > 0 && (
          <div className="bg-white dark:bg-slate-900 border rounded-2xl p-4 space-y-2.5 text-xs">
            <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
              <BookOpen className="w-4 h-4 text-purple-500" />
              <span>匹配系列专栏</span>
            </h2>
            {matchedSeries.map((s) => (
              <div
                key={s.id}
                onClick={() => navigateTo('/series/:id', { id: s.id })}
                className="p-2.5 bg-slate-50 dark:bg-slate-800/40 rounded-xl cursor-pointer flex justify-between items-center"
              >
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white">{s.title}</h3>
                  <p className="text-[11px] text-slate-500">{s.description}</p>
                </div>
                <button className="text-purple-600 font-semibold shrink-0 ml-3">访问系列</button>
              </div>
            ))}
          </div>
        )}

        {/* TEAMS */}
        {(activeTab === 'ALL' || activeTab === 'TEAMS') && matchedTeams.length > 0 && (
          <div className="bg-white dark:bg-slate-900 border rounded-2xl p-4 space-y-2.5 text-xs">
            <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
              <Users className="w-4 h-4 text-indigo-500" />
              <span>匹配团队</span>
            </h2>
            {matchedTeams.map((t) => (
              <div
                key={t.id}
                onClick={() => navigateTo('/teams/:slug', { slug: t.slug })}
                className="p-2.5 bg-slate-50 dark:bg-slate-800/40 rounded-xl cursor-pointer flex justify-between items-center"
              >
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white">{t.name}</h3>
                  <p className="text-[11px] text-slate-500">{t.description}</p>
                </div>
                <button className="text-indigo-600 font-semibold shrink-0 ml-3">进入主页</button>
              </div>
            ))}
          </div>
        )}

      </div>

    </div>
  );
};
