/**
 * 星语社区 (pxczxn-community V2.1) - 个人公开主页 (Creator Profile & Knowledge Portfolio)
 */

import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import {
  User,
  Sparkles,
  CheckCircle2,
  FileText,
  BookOpen,
  MessageSquare,
  UserPlus,
  MessageCircle,
  MapPin,
  Globe,
  Share2,
} from 'lucide-react';

export const ProfileView: React.FC = () => {
  const { user, articles, seriesList, moments, routeParams, navigateTo, isCompactViewport } = useApp();

  const [activeTab, setActiveTab] = useState<'HIGHLIGHTS' | 'ARTICLES' | 'SERIES' | 'MOMENTS' | 'TRAJECTORY'>('HIGHLIGHTS');

  return (
    <div className={`max-w-5xl mx-auto px-3 sm:px-4 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Profile Hero Header */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-6 shadow-xs mb-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-center space-x-4">
            <img src={user?.avatar} alt="" className="w-16 h-16 rounded-full object-cover border-2 border-indigo-500 shadow-sm" />
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-base sm:text-lg font-black text-slate-900 dark:text-white">{user?.displayName}</h1>
                <CheckCircle2 className="w-4 h-4 text-indigo-500 fill-indigo-500/20" />
              </div>
              <p className="text-xs text-slate-400">@{user?.username} · {user?.role}</p>
              <p className="text-xs text-slate-600 dark:text-slate-300 mt-1">{user?.bio}</p>
            </div>
          </div>

          <div className="flex items-center space-x-2 w-full sm:w-auto">
            <button className="flex-1 sm:flex-initial px-4 py-1.5 bg-indigo-600 text-white text-xs font-semibold rounded-xl">
              + 关注作者
            </button>
            <button onClick={() => navigateTo('/chat')} className="px-3 py-1.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 text-xs font-medium rounded-xl">
              私信交流
            </button>
          </div>
        </div>

        {/* Stats Strip */}
        <div className="grid grid-cols-4 gap-2 mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 text-center text-xs">
          <div><p className="font-bold">{user?.articlesCount}</p><p className="text-[10px] text-slate-400">文章作品</p></div>
          <div><p className="font-bold">{user?.seriesCount}</p><p className="text-[10px] text-slate-400">专栏系列</p></div>
          <div><p className="font-bold">{user?.followersCount}</p><p className="text-[10px] text-slate-400">关注者</p></div>
          <div><p className="font-bold">{user?.followingCount}</p><p className="text-[10px] text-slate-400">正在关注</p></div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center space-x-2 border-b border-slate-200 dark:border-slate-800 mb-4 pb-1 text-xs font-semibold">
        {[
          { key: 'HIGHLIGHTS', label: '精选主页' },
          { key: 'ARTICLES', label: '文章列表' },
          { key: 'SERIES', label: '专栏系列' },
          { key: 'MOMENTS', label: '动态社交' },
          { key: 'TRAJECTORY', label: '知识轨迹 (Mesh)' },
        ].map((t) => (
          <button
            key={t.key}
            onClick={() => setActiveTab(t.key as any)}
            className={`px-3 py-1.5 rounded-xl transition-colors ${
              activeTab === t.key ? 'bg-indigo-600 text-white font-bold' : 'text-slate-600 dark:text-slate-300'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      {activeTab === 'HIGHLIGHTS' && (
        <div className="space-y-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs">
            <h2 className="text-xs font-bold text-slate-900 dark:text-white mb-2 flex items-center gap-1.5">
              <Sparkles className="w-4 h-4 text-amber-500" />
              <span>置顶代表作品 (Featured Showcase)</span>
            </h2>
            <div className="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-xl border text-xs">
              <h3 className="font-bold text-slate-900 dark:text-white">{articles[0]?.title}</h3>
              <p className="text-[11px] text-slate-500 mt-1">{articles[0]?.summary}</p>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'ARTICLES' && (
        <div className="space-y-2">
          {articles.map((art) => (
            <div key={art.id} className="p-3 bg-white dark:bg-slate-900 border rounded-xl text-xs flex justify-between">
              <span className="font-bold">{art.title}</span>
              <button onClick={() => navigateTo('/articles/:id', { id: art.id })} className="text-indigo-600">阅读</button>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'TRAJECTORY' && (
        <div className="bg-white dark:bg-slate-900 border rounded-2xl p-4 text-xs space-y-2">
          <h2 className="font-bold text-slate-900 dark:text-white">个人知识演进轨迹 (Knowledge Map)</h2>
          <div className="p-3 bg-slate-50 dark:bg-slate-800/50 rounded-xl font-mono text-[11px] text-indigo-600 dark:text-indigo-300">
            Java 21 Virtual Threads → Spring Boot 3.5 Core → React 19 Server Actions → Multi-Agent Architecture
          </div>
        </div>
      )}

    </div>
  );
};
