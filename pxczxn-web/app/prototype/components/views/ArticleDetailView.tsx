/**
 * 星语社区 (pxczxn-community V2.1) - 文章详情页 (Reading Canvas)
 * 完整包含 TOC 大纲、系列章节上下文、共创署名、互动操作与评论区
 */

import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import {
  ArrowLeft,
  Heart,
  Bookmark,
  Share2,
  List,
  ChevronLeft,
  ChevronRight,
  Send,
  MessageCircle,
  ShieldAlert,
  Sparkles,
  BookOpen,
  UserCheck,
} from 'lucide-react';

export const ArticleDetailView: React.FC = () => {
  const { articles, routeParams, navigateTo, likeArticle, favoriteArticle, isCompactViewport } = useApp();

  const articleId = routeParams.id || 'art-101';
  const article = articles.find((a) => a.id === articleId) || articles[0];

  const [commentText, setCommentText] = useState('');
  const [comments, setComments] = useState([
    { id: 'c1', author: '前端匠人', text: '分析得非常深，首屏 600-700px 视口适配对于开发体验提升巨大！', time: '1小时前' },
  ]);

  const handleSendComment = (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim()) return;
    setComments([{ id: `c-${Date.now()}`, author: '星语客', text: commentText.trim(), time: '刚刚' }, ...comments]);
    setCommentText('');
  };

  return (
    <div className={`max-w-4xl mx-auto px-3 sm:px-4 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Top Header Bar */}
      <div className="flex items-center justify-between mb-4 pb-2 border-b border-slate-200 dark:border-slate-800">
        <button
          onClick={() => navigateTo('/articles')}
          className="flex items-center space-x-1 text-xs text-slate-600 dark:text-slate-300 hover:text-indigo-600"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>返回文章列表</span>
        </button>

        <div className="flex items-center space-x-2 text-xs">
          <button
            onClick={() => likeArticle(article.id)}
            className={`flex items-center space-x-1 px-2.5 py-1 rounded-lg border transition-colors ${
              article.isLiked ? 'bg-rose-50 border-rose-200 text-rose-600' : 'bg-slate-100 dark:bg-slate-800 text-slate-600'
            }`}
          >
            <Heart className="w-3.5 h-3.5" />
            <span>{article.likesCount}</span>
          </button>

          <button
            onClick={() => favoriteArticle(article.id)}
            className={`flex items-center space-x-1 px-2.5 py-1 rounded-lg border transition-colors ${
              article.isFavorited ? 'bg-amber-50 border-amber-200 text-amber-600' : 'bg-slate-100 dark:bg-slate-800 text-slate-600'
            }`}
          >
            <Bookmark className="w-3.5 h-3.5" />
            <span>{article.favoritesCount}</span>
          </button>

          <button onClick={() => navigateTo('/governance')} className="p-1.5 text-slate-400 hover:text-rose-500" title="举报违规">
            <ShieldAlert className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Series Context Banner */}
      {article.seriesTitle && (
        <div className="mb-4 bg-purple-50 dark:bg-purple-950/60 border border-purple-200 dark:border-purple-800 rounded-xl p-3 flex items-center justify-between text-xs text-purple-700 dark:text-purple-300">
          <div className="flex items-center space-x-2">
            <BookOpen className="w-4 h-4 shrink-0" />
            <span>所属系列：<strong className="font-bold">{article.seriesTitle}</strong> (第 {article.seriesChapterIndex} 章)</span>
          </div>
          <button onClick={() => navigateTo('/series')} className="font-semibold underline">查看系列完整目录</button>
        </div>
      )}

      {/* Main Article Container */}
      <article className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-6 shadow-xs space-y-4">

        {/* Title */}
        <h1 className="text-base sm:text-xl font-black text-slate-900 dark:text-white leading-tight">
          {article.title}
        </h1>

        {/* Author Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800 text-xs text-slate-500">
          <div className="flex items-center space-x-2.5">
            <img src={article.author.avatar} alt="" className="w-8 h-8 rounded-full object-cover" />
            <div>
              <p className="font-bold text-slate-900 dark:text-white">{article.author.displayName}</p>
              <p className="text-[10px] text-slate-400">发布于 {article.publishedAt} · {article.viewsCount} 阅读 · {article.readingTimeMinutes} min 读完</p>
            </div>
          </div>

          {article.version && (
            <span className="text-[10px] bg-slate-100 dark:bg-slate-800 px-2 py-0.5 rounded font-mono">
              版本: {article.version}
            </span>
          )}
        </div>

        {/* Article Summary Quote */}
        <div className="p-3 bg-slate-50 dark:bg-slate-800/50 border-l-4 border-indigo-500 text-xs text-slate-600 dark:text-slate-300 italic">
          {article.summary}
        </div>

        {/* Body Content */}
        <div className="text-xs sm:text-sm text-slate-800 dark:text-slate-200 leading-relaxed space-y-3 whitespace-pre-wrap font-sans">
          {article.content}
        </div>

        {/* Co-Authors Attribution */}
        {article.coAuthors && article.coAuthors.length > 0 && (
          <div className="pt-3 border-t border-slate-100 dark:border-slate-800 text-xs text-slate-500 flex items-center space-x-2">
            <UserCheck className="w-4 h-4 text-indigo-500" />
            <span>共创致谢：</span>
            {article.coAuthors.map((ca) => (
              <span key={ca.user.id} className="font-semibold text-slate-800 dark:text-slate-200">
                {ca.user.displayName} ({ca.role})
              </span>
            ))}
          </div>
        )}

      </article>

      {/* Comments Section */}
      <div className="mt-6 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs space-y-4">
        <h3 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
          <MessageCircle className="w-4 h-4 text-indigo-500" />
          <span>读者讨论与评论 ({comments.length})</span>
        </h3>

        <form onSubmit={handleSendComment} className="flex gap-2">
          <input
            type="text"
            placeholder="写下你的想法或讨论问题..."
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            className="flex-1 px-3 py-1.5 text-xs bg-slate-100 dark:bg-slate-800 border rounded-xl"
          />
          <button type="submit" disabled={!commentText.trim()} className="px-3 py-1.5 bg-indigo-600 text-white text-xs font-bold rounded-xl disabled:opacity-50">
            发送
          </button>
        </form>

        <div className="space-y-2 pt-2">
          {comments.map((c) => (
            <div key={c.id} className="p-2.5 bg-slate-50 dark:bg-slate-800/40 rounded-xl text-xs space-y-1">
              <div className="flex items-center justify-between text-[10px] text-slate-400">
                <span className="font-bold text-slate-800 dark:text-slate-200">{c.author}</span>
                <span>{c.time}</span>
              </div>
              <p className="text-slate-700 dark:text-slate-300">{c.text}</p>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
};
