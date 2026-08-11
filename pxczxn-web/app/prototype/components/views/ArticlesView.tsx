/**
 * 星语社区 (pxczxn-community V2.2) - 文章索引页 (Content Index)
 * PC 端优化：正文 14px，模块间距 16~20px，标签侧栏 padding 增大
 */

import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import {
  FileText,
  Search,
  Filter,
  SlidersHorizontal,
  Tag,
  Eye,
  Heart,
  MessageCircle,
  Clock,
  BookOpen,
  ChevronLeft,
  ChevronRight,
  List,
  Grid,
} from 'lucide-react';

export const ArticlesView: React.FC = () => {
  const { articles, navigateTo, isCompactViewport, likeArticle, favoriteArticle } = useApp();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTag, setSelectedTag] = useState<string | null>(null);
  const [sortOption, setSortOption] = useState<'QUALITY' | 'LATEST' | 'LIKES' | 'VIEWS'>('QUALITY');
  const [viewMode, setViewMode] = useState<'DENSE' | 'CARD'>('DENSE');

  const allTags = Array.from(new Set(articles.flatMap((a) => a.tags)));

  // Filter & Sort
  const filteredArticles = articles
    .filter((a) => {
      const matchSearch =
        a.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        a.summary.toLowerCase().includes(searchQuery.toLowerCase());
      const matchTag = selectedTag ? a.tags.includes(selectedTag) : true;
      return matchSearch && matchTag;
    })
    .sort((a, b) => {
      if (sortOption === 'LATEST') return new Date(b.publishedAt).getTime() - new Date(a.publishedAt).getTime();
      if (sortOption === 'LIKES') return b.likesCount - a.likesCount;
      if (sortOption === 'VIEWS') return b.viewsCount - a.viewsCount;
      return (b.likesCount + b.viewsCount / 10) - (a.likesCount + a.viewsCount / 10);
    });

  return (
    <div className={`max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 transition-all ${isCompactViewport ? 'py-4' : 'py-6'}`}>

      {/* Header Toolbar */}
      <div className="mb-6 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">

          {/* Title & Icon */}
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-xl bg-indigo-100 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-white">
                文章内容索引 (Content Index)
              </h1>
              <p className="text-sm text-slate-400">高效检索与知识查阅 · 工具型单类型浏览</p>
            </div>
          </div>

          {/* Search Input & Sort Selector */}
          <div className="flex flex-wrap items-center gap-3">
            {/* Search Input */}
            <div className="relative flex-1 sm:flex-initial">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
              <input
                type="text"
                placeholder="搜索标题或摘要关键词..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full sm:w-64 pl-10 pr-4 py-2.5 bg-slate-100 dark:bg-slate-800 border border-transparent focus:border-indigo-500 rounded-xl text-sm text-slate-800 dark:text-slate-100 placeholder-slate-400 focus:outline-none"
              />
            </div>

            {/* Sort Dropdown */}
            <div className="flex items-center gap-2 bg-slate-100 dark:bg-slate-800 p-1.5 rounded-xl text-sm">
              <SlidersHorizontal className="w-4 h-4 text-slate-400" />
              {(['QUALITY', 'LATEST', 'LIKES', 'VIEWS'] as const).map((opt) => (
                <button
                  key={opt}
                  onClick={() => setSortOption(opt)}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                    sortOption === opt
                      ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm font-semibold'
                      : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
                  }`}
                >
                  {opt === 'QUALITY' && '综合推荐'}
                  {opt === 'LATEST' && '最新发布'}
                  {opt === 'LIKES' && '最多点赞'}
                  {opt === 'VIEWS' && '最多阅读'}
                </button>
              ))}
            </div>

            {/* Density View Toggle */}
            <div className="hidden sm:flex items-center gap-1 bg-slate-100 dark:bg-slate-800 p-1.5 rounded-xl text-sm">
              <button
                onClick={() => setViewMode('DENSE')}
                className={`p-2 rounded-lg ${viewMode === 'DENSE' ? 'bg-white dark:bg-slate-900 text-indigo-600 shadow-sm' : 'text-slate-400'}`}
                title="高密度列表"
              >
                <List className="w-4 h-4" />
              </button>
              <button
                onClick={() => setViewMode('CARD')}
                className={`p-2 rounded-lg ${viewMode === 'CARD' ? 'bg-white dark:bg-slate-900 text-indigo-600 shadow-sm' : 'text-slate-400'}`}
                title="大卡布局"
              >
                <Grid className="w-4 h-4" />
              </button>
            </div>
          </div>

        </div>
      </div>

      {/* Main Layout: Tag Sidebar + Article Index List */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

        {/* Left Tag Filtering Sidebar */}
        <div className="lg:col-span-3 sticky top-20 self-start">
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-sm">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white">
                <Tag className="w-4 h-4 text-indigo-500" />
                <span>标签分类筛选</span>
              </div>
              {selectedTag && (
                <button
                  onClick={() => setSelectedTag(null)}
                  className="text-xs text-rose-500 hover:underline font-medium"
                >
                  清除重置
                </button>
              )}
            </div>

            <div className="flex flex-wrap lg:flex-col gap-2 text-sm">
              <button
                onClick={() => setSelectedTag(null)}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm font-medium transition-colors flex items-center justify-between ${
                  selectedTag === null
                    ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/70 dark:text-indigo-400 font-bold'
                    : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                <span>全部文章</span>
                <span className="text-xs font-mono text-slate-400">({articles.length})</span>
              </button>

              {allTags.map((tag) => {
                const count = articles.filter((a) => a.tags.includes(tag)).length;
                const isSelected = selectedTag === tag;
                return (
                  <button
                    key={tag}
                    onClick={() => setSelectedTag(isSelected ? null : tag)}
                    className={`text-left px-3 py-2 rounded-lg text-sm font-medium transition-colors flex items-center justify-between ${
                      isSelected
                        ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/70 dark:text-indigo-400 font-bold'
                        : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                    }`}
                  >
                    <span>#{tag}</span>
                    <span className="text-xs font-mono text-slate-400">({count})</span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Right Article List */}
        <div className="lg:col-span-9 space-y-5">

          {/* Active Filter Pill */}
          {selectedTag && (
            <div className="bg-indigo-50 dark:bg-indigo-950/60 border border-indigo-200 dark:border-indigo-800 rounded-xl px-4 py-2.5 flex items-center justify-between text-sm text-indigo-700 dark:text-indigo-300">
              <span>当前已筛选标签：<strong className="font-bold">#{selectedTag}</strong></span>
              <button onClick={() => setSelectedTag(null)} className="text-sm font-medium underline">
                取消筛选
              </button>
            </div>
          )}

          {filteredArticles.length > 0 ? (
            <div className={viewMode === 'CARD' ? 'grid grid-cols-1 md:grid-cols-2 gap-5' : 'space-y-4'}>
              {filteredArticles.map((art) => (
                <div
                  key={art.id}
                  className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-sm hover:border-indigo-300 dark:hover:border-indigo-700 transition-all flex flex-col sm:flex-row justify-between gap-4 group"
                >
                  <div className="flex-1 space-y-2">
                    {/* Meta Header */}
                    <div className="flex items-center gap-3 text-sm text-slate-400">
                      <img src={art.author.avatar} alt="" className="w-5 h-5 rounded-full" />
                      <span className="font-medium text-slate-700 dark:text-slate-300">{art.author.displayName}</span>
                      {art.seriesTitle && (
                        <span className="bg-purple-50 dark:bg-purple-950 text-purple-600 dark:text-purple-300 px-2 py-0.5 rounded-lg font-medium flex items-center gap-1">
                          <BookOpen className="w-3.5 h-3.5" />
                          {art.seriesTitle}
                        </span>
                      )}
                      <span>· {art.publishedAt}</span>
                    </div>

                    {/* Title */}
                    <h2
                      onClick={() => navigateTo('/articles/:id', { id: art.id })}
                      className="text-base font-bold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors cursor-pointer line-clamp-1"
                    >
                      {art.title}
                    </h2>

                    {/* Summary */}
                    <p className="text-sm text-slate-500 dark:text-slate-400 line-clamp-2">
                      {art.summary}
                    </p>

                    {/* Tags & Footer Stats */}
                    <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
                      <div className="flex flex-wrap items-center gap-2">
                        {art.tags.map((t) => (
                          <span
                            key={t}
                            onClick={() => setSelectedTag(t)}
                            className="text-xs bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 px-2.5 py-1 rounded hover:bg-indigo-50 hover:text-indigo-600 cursor-pointer"
                          >
                            #{t}
                          </span>
                        ))}
                      </div>

                      <div className="flex items-center gap-2 text-sm flex-wrap">
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full font-medium bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200/60 dark:border-slate-700/60">
                          <Eye className="w-4 h-4 text-slate-400" />
                          <span>{art.viewsCount}</span>
                        </span>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            likeArticle(art.id);
                          }}
                          className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full font-semibold transition-all ${
                            art.isLiked
                              ? 'bg-rose-100 dark:bg-rose-950 text-rose-600 dark:text-rose-300 border border-rose-300 dark:border-rose-800'
                              : 'bg-rose-50/80 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200/60 dark:border-rose-900/50 hover:bg-rose-100'
                          }`}
                        >
                          <Heart className={`w-4 h-4 ${art.isLiked ? 'fill-rose-500 text-rose-500' : 'text-rose-500'}`} />
                          <span>{art.likesCount}</span>
                        </button>
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full font-medium bg-indigo-50/80 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 border border-indigo-200/60 dark:border-indigo-900/50">
                          <MessageCircle className="w-4 h-4 text-indigo-500" />
                          <span>{art.commentsCount}</span>
                        </span>
                      </div>
                    </div>
                  </div>

                  {art.coverImage && (
                    <img
                      src={art.coverImage}
                      alt=""
                      className="w-full sm:w-36 h-24 rounded-xl object-cover shrink-0"
                    />
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-10 text-center text-sm text-slate-400">
              未搜索到匹配的文章内容，建议清除筛选条件重试。
            </div>
          )}

          {/* Pagination */}
          <div className="flex items-center justify-between bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-xl p-4 text-sm text-slate-500">
            <span>显示第 1 - {filteredArticles.length} 条，共 {filteredArticles.length} 条结果</span>
            <div className="flex items-center gap-2">
              <button disabled className="px-3 py-1.5 rounded bg-slate-100 dark:bg-slate-800 opacity-50 cursor-not-allowed">
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button className="px-4 py-1.5 rounded bg-indigo-600 text-white font-bold">1</button>
              <button disabled className="px-3 py-1.5 rounded bg-slate-100 dark:bg-slate-800 opacity-50 cursor-not-allowed">
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>

        </div>

      </div>
    </div>
  );
};
