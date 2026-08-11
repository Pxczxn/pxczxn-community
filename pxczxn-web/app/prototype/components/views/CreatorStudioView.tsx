/**
 * 星语社区 (pxczxn-community V2.1) - 创作者中心 (Creator Studio)
 * 包含数据分析 Analytics、内容/系列管理、共创中心、灵感箱 (Idea Box)
 */

import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import {
  LayoutDashboard,
  TrendingUp,
  FileText,
  BookOpen,
  Lightbulb,
  Send,
  Users,
  Eye,
  Heart,
  MessageCircle,
  Plus,
  Sparkles,
} from 'lucide-react';

export const CreatorStudioView: React.FC = () => {
  const { articles, seriesList, ideas, addIdea, navigateTo, isCompactViewport } = useApp();

  const [activeTab, setActiveTab] = useState<'DASHBOARD' | 'ARTICLES' | 'SERIES' | 'IDEAS' | 'ANALYTICS'>('DASHBOARD');
  const [newIdeaTitle, setNewIdeaTitle] = useState('');
  const [newIdeaContent, setNewIdeaContent] = useState('');

  const handleAddIdea = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newIdeaTitle.trim()) return;
    addIdea(newIdeaTitle.trim(), newIdeaContent.trim(), ['灵感', '草稿']);
    setNewIdeaTitle('');
    setNewIdeaContent('');
  };

  return (
    <div className={`max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Header Banner */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs mb-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
            创作者工作台 (Creator Studio)
            <span className="text-[10px] bg-purple-100 dark:bg-purple-950 text-purple-600 dark:text-purple-300 px-2 py-0.5 rounded-full font-mono font-medium">
              Creator OS
            </span>
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            灵感收集箱 · 创作沉淀 · 数据分析 · 共创与投稿中心
          </p>
        </div>

        <button
          onClick={() => navigateTo('/editor/new')}
          className="bg-indigo-600 hover:bg-indigo-700 text-white px-3.5 py-1.5 rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center space-x-1 self-start sm:self-center"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>撰写新文章</span>
        </button>
      </div>

      {/* Tabs */}
      <div className="flex items-center space-x-2 border-b border-slate-200 dark:border-slate-800 mb-4 pb-1 text-xs font-semibold overflow-x-auto w-full">
        {[
          { key: 'DASHBOARD', label: '创作总览', icon: <LayoutDashboard className="w-3.5 h-3.5" /> },
          { key: 'ARTICLES', label: '内容管理', icon: <FileText className="w-3.5 h-3.5" /> },
          { key: 'SERIES', label: '我的系列', icon: <BookOpen className="w-3.5 h-3.5" /> },
          { key: 'IDEAS', label: '灵感箱 Idea Box', icon: <Lightbulb className="w-3.5 h-3.5 text-amber-500" /> },
          { key: 'ANALYTICS', label: '数据分析', icon: <TrendingUp className="w-3.5 h-3.5" /> },
        ].map((t) => (
          <button
            key={t.key}
            onClick={() => setActiveTab(t.key as any)}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl font-semibold transition-all whitespace-nowrap shrink-0 ${
              activeTab === t.key
                ? 'bg-indigo-600 text-white shadow-xs'
                : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            {t.icon}
            <span>{t.label}</span>
          </button>
        ))}
      </div>

      {/* Tab Contents Container with Fixed Width & Min Height */}
      <div className="w-full min-h-[400px]">
        {/* TAB: DASHBOARD */}
        {activeTab === 'DASHBOARD' && (
          <div className="space-y-4 w-full">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="bg-white dark:bg-slate-900 border rounded-2xl p-3.5 text-xs">
                <p className="text-slate-400">近30天阅读总数</p>
                <p className="text-lg font-black mt-0.5">12,840</p>
              </div>
              <div className="bg-white dark:bg-slate-900 border rounded-2xl p-3.5 text-xs">
                <p className="text-slate-400">近30天新增点赞</p>
                <p className="text-lg font-black mt-0.5 text-rose-500">892</p>
              </div>
              <div className="bg-white dark:bg-slate-900 border rounded-2xl p-3.5 text-xs">
                <p className="text-slate-400">粉丝增长</p>
                <p className="text-lg font-black mt-0.5 text-indigo-500">+124</p>
              </div>
              <div className="bg-white dark:bg-slate-900 border rounded-2xl p-3.5 text-xs">
                <p className="text-slate-400">灵感箱积攒</p>
                <p className="text-lg font-black mt-0.5 text-amber-500">{ideas.length}</p>
              </div>
            </div>
          </div>
        )}

        {/* TAB: IDEAS (灵感箱) */}
        {activeTab === 'IDEAS' && (
          <div className="space-y-4 w-full">
            {/* Add Idea Box */}
            <div className="bg-white dark:bg-slate-900 border rounded-2xl p-4 space-y-3">
              <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                <Lightbulb className="w-4 h-4 text-amber-500" />
                <span>记下一条灵感 (Idea Box)</span>
              </h2>

              <form onSubmit={handleAddIdea} className="space-y-2 text-xs">
                <input
                  type="text"
                  placeholder="灵感主题标题..."
                  value={newIdeaTitle}
                  onChange={(e) => setNewIdeaTitle(e.target.value)}
                  className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
                />
                <textarea
                  placeholder="简要记下思路、引用素材或待验证问题..."
                  rows={2}
                  value={newIdeaContent}
                  onChange={(e) => setNewIdeaContent(e.target.value)}
                  className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
                />
                <button type="submit" disabled={!newIdeaTitle.trim()} className="px-4 py-1.5 bg-amber-500 text-white font-bold rounded-xl disabled:opacity-50">
                  存入灵感箱
                </button>
              </form>
            </div>

            {/* Idea Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {ideas.map((idea) => (
                <div key={idea.id} className="bg-white dark:bg-slate-900 border rounded-2xl p-3.5 text-xs space-y-2">
                  <div className="flex justify-between items-center">
                    <h3 className="font-bold text-slate-900 dark:text-white">{idea.title}</h3>
                    <span className="text-[10px] text-slate-400">{idea.createdAt}</span>
                  </div>
                  <p className="text-slate-600 dark:text-slate-300">{idea.content}</p>
                  <div className="pt-2 border-t flex justify-between items-center">
                    <div className="flex gap-1">
                      {idea.tags.map((t) => (
                        <span key={t} className="text-[10px] bg-amber-50 text-amber-700 px-1.5 py-0.2 rounded">#{t}</span>
                      ))}
                    </div>
                    <button onClick={() => navigateTo('/editor/new')} className="text-indigo-600 font-bold">转为文章草稿 →</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* TAB: ANALYTICS */}
        {activeTab === 'ANALYTICS' && (
          <div className="bg-white dark:bg-slate-900 border rounded-2xl p-4 text-xs space-y-3 w-full">
            <h2 className="font-bold text-slate-900 dark:text-white">数据趋势与受众入口</h2>
            <div className="h-64 bg-slate-50 dark:bg-slate-800/50 rounded-xl flex items-center justify-center text-slate-400 font-mono border border-dashed border-slate-200 dark:border-slate-700">
              [ 创作者数据图表: 文章阅读量 & 互动增长趋势 ]
            </div>
          </div>
        )}
      </div>

    </div>
  );
};
