/**
 * 星语社区 (pxczxn-community V2.1) - 动态页 (Social Timeline)
 * 核心原则：三栏社交 Timeline 骨架 (Left Nav + Center Feed & Publisher + Right Active Detail)
 */

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useApp } from '../../context/AppContext';
import { Moment } from '../../types';
import {
  MessageSquare,
  Sparkles,
  UserCheck,
  Clock,
  TrendingUp,
  Send,
  Heart,
  Bookmark,
  Share2,
  Globe,
  Lock,
  Link2,
  FileText,
  MessageCircle,
  Hash,
} from 'lucide-react';

export const MomentsView: React.FC = () => {
  const { moments, user, addMoment, likeMoment, favoriteMoment, isCompactViewport, navigateTo } = useApp();

  const [activeTab, setActiveTab] = useState<'RECOMMENDED' | 'FOLLOWING' | 'LATEST'>('RECOMMENDED');
  const [newMomentText, setNewMomentText] = useState('');
  const [momentType, setMomentType] = useState<Moment['momentType']>('TEXT');
  const [linkInput, setLinkInput] = useState('');
  const [activeMomentId, setActiveMomentId] = useState<string>(moments[0]?.id || '');
  const [commentInput, setCommentInput] = useState('');

  const activeMoment = moments.find((m) => m.id === activeMomentId) || moments[0];

  const handlePublish = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMomentText.trim()) return;
    addMoment(newMomentText.trim(), momentType, linkInput || undefined);
    setNewMomentText('');
    setLinkInput('');
  };

  return (
    <div className={`max-w-7xl mx-auto px-3 sm:px-4 lg:px-6 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Three Column Grid Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-3 sm:gap-4">

        {/* Left Nav Column (25% width) */}
        <div className="lg:col-span-3 sticky top-16 self-start space-y-2">

          {/* Feed Filter Tabs */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-2 shadow-xs">
            <h2 className="text-xs font-bold text-slate-900 dark:text-white mb-1 pb-1 border-b border-slate-100 dark:border-slate-800 flex items-center gap-1.5">
              <MessageSquare className="w-3.5 h-3.5 text-emerald-500" />
              <span>动态 Feed 导航</span>
            </h2>

            <div className="space-y-0.5 text-xs font-medium">
              <button
                onClick={() => setActiveTab('RECOMMENDED')}
                className={`w-full text-left px-2 py-1 rounded-lg flex items-center space-x-2 transition-colors ${
                  activeTab === 'RECOMMENDED'
                    ? 'bg-emerald-50 text-emerald-600 dark:bg-emerald-950 dark:text-emerald-300 font-bold'
                    : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                <Sparkles className="w-3.5 h-3.5 shrink-0" />
                <span>精选推荐</span>
              </button>

              <button
                onClick={() => setActiveTab('FOLLOWING')}
                className={`w-full text-left px-2 py-1 rounded-lg flex items-center space-x-2 transition-colors ${
                  activeTab === 'FOLLOWING'
                    ? 'bg-emerald-50 text-emerald-600 dark:bg-emerald-950 dark:text-emerald-300 font-bold'
                    : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                <UserCheck className="w-3.5 h-3.5 shrink-0" />
                <span>关注更新</span>
              </button>

              <button
                onClick={() => setActiveTab('LATEST')}
                className={`w-full text-left px-2 py-1 rounded-lg flex items-center space-x-2 transition-colors ${
                  activeTab === 'LATEST'
                    ? 'bg-emerald-50 text-emerald-600 dark:bg-emerald-950 dark:text-emerald-300 font-bold'
                    : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                <Clock className="w-3.5 h-3.5 shrink-0" />
                <span>最新发布</span>
              </button>
            </div>
          </div>

          {/* Hot Topics Wrapped */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3 shadow-xs space-y-2">
            <h3 className="text-xs font-bold text-slate-900 dark:text-white pb-2 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <TrendingUp className="w-3.5 h-3.5 text-amber-500" />
                <span>社区讨论热榜</span>
              </span>
              <span className="text-[10px] px-1.5 py-0.5 bg-amber-50 dark:bg-amber-950 text-amber-600 dark:text-amber-400 rounded-full font-mono font-bold">
                TOP
              </span>
            </h3>
            <div className="space-y-1.5 text-xs">
              {[
                { tag: '#星语V2.1发布讨论', heat: '421 热度', rank: '1', color: 'bg-rose-50 text-rose-600 dark:bg-rose-950 dark:text-rose-300' },
                { tag: '#React19服务端Actions', heat: '289 热度', rank: '2', color: 'bg-amber-50 text-amber-600 dark:bg-amber-950 dark:text-amber-300' },
                { tag: '#AI社区知识网', heat: '195 热度', rank: '3', color: 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-300' },
              ].map((item) => (
                <div
                  key={item.tag}
                  className="p-2 rounded-xl bg-slate-50 dark:bg-slate-800/50 hover:bg-slate-100 dark:hover:bg-slate-800 border border-slate-100 dark:border-slate-800/80 cursor-pointer flex items-center justify-between transition-all"
                >
                  <div className="flex items-center space-x-1.5 min-w-0 pr-1">
                    <span className={`w-4 h-4 rounded-md flex items-center justify-center text-[10px] font-bold ${item.color}`}>
                      {item.rank}
                    </span>
                    <span className="truncate font-semibold text-slate-800 dark:text-slate-200">{item.tag}</span>
                  </div>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-amber-50 dark:bg-amber-950/80 text-amber-600 dark:text-amber-400 border border-amber-200 dark:border-amber-800 shrink-0">
                    🔥 {item.heat}
                  </span>
                </div>
              ))}
            </div>
          </div>

        </div>

        {/* Center Column: Feed Publisher + Timeline Stream (5 cols) */}
        <div className="lg:col-span-5 space-y-3">

          {/* Moment Publisher Box */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 shadow-xs">
            <div className="flex items-center space-x-2.5 mb-2.5">
              <img src={user?.avatar} alt="" className="w-7 h-7 rounded-full object-cover" />
              <span className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                发布轻量想法或关联文章
              </span>
            </div>

            <form onSubmit={handlePublish} className="space-y-2">
              <textarea
                value={newMomentText}
                onChange={(e) => setNewMomentText(e.target.value)}
                placeholder="此刻在思考什么？写下短文本或分享外部链接..."
                rows={2}
                className="w-full p-2.5 text-xs bg-slate-50 dark:bg-slate-800/60 border border-slate-200/80 dark:border-slate-700 rounded-xl text-slate-800 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:border-emerald-500 transition-all resize-none"
              />

              {momentType === 'LINK' && (
                <input
                  type="text"
                  placeholder="https:// 外部链接 URL..."
                  value={linkInput}
                  onChange={(e) => setLinkInput(e.target.value)}
                  className="w-full px-3 py-1.5 text-xs bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl"
                />
              )}

              <div className="flex items-center justify-between pt-1 border-t border-slate-100 dark:border-slate-800">
                <div className="flex items-center space-x-1 text-slate-400">
                  <button
                    type="button"
                    onClick={() => setMomentType(momentType === 'LINK' ? 'TEXT' : 'LINK')}
                    className={`p-1.5 rounded-lg text-xs hover:bg-slate-100 dark:hover:bg-slate-800 ${
                      momentType === 'LINK' ? 'text-emerald-500 font-bold' : ''
                    }`}
                    title="添加链接"
                  >
                    <Link2 className="w-3.5 h-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => navigateTo('/articles')}
                    className="p-1.5 rounded-lg text-xs hover:bg-slate-100 dark:hover:bg-slate-800"
                    title="关联文章"
                  >
                    <FileText className="w-3.5 h-3.5" />
                  </button>
                </div>

                <button
                  type="submit"
                  disabled={!newMomentText.trim()}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-1.5 rounded-xl text-xs font-semibold disabled:opacity-50 transition-colors flex items-center space-x-1"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>发布动态</span>
                </button>
              </div>
            </form>
          </div>

          {/* Timeline Stream */}
          <div className="space-y-3">
            <AnimatePresence mode="popLayout">
              {moments.map((mom) => {
                const isSelected = activeMomentId === mom.id;
                return (
                  <motion.div
                    key={mom.id}
                    layout
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                    transition={{ duration: 0.2 }}
                    onClick={() => setActiveMomentId(mom.id)}
                    className={`bg-white dark:bg-slate-900 border rounded-2xl p-3.5 shadow-xs transition-all cursor-pointer ${
                      isSelected
                        ? 'border-emerald-500 ring-2 ring-emerald-500/10'
                        : 'border-slate-200/80 dark:border-slate-800 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center space-x-2">
                        <img src={mom.author.avatar} alt="" className="w-6 h-6 rounded-full" />
                        <div>
                          <span className="text-xs font-bold text-slate-900 dark:text-white">{mom.author.displayName}</span>
                          <span className="text-[10px] text-slate-400 ml-2">{mom.createdAt}</span>
                        </div>
                      </div>
                    </div>

                    <p className="text-xs text-slate-800 dark:text-slate-200 leading-relaxed whitespace-pre-wrap">
                      {mom.textContent}
                    </p>

                    {/* Share Article Preview */}
                    {mom.articleTitle && (
                      <div
                        onClick={(e) => {
                          e.stopPropagation();
                          if (mom.articleId) navigateTo('/articles/:id', { id: mom.articleId });
                        }}
                        className="mt-2.5 p-2.5 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200/80 dark:border-slate-700 flex items-center space-x-2 text-xs text-indigo-600 dark:text-indigo-400 hover:underline cursor-pointer"
                      >
                        <FileText className="w-4 h-4 shrink-0" />
                        <span className="font-semibold line-clamp-1">{mom.articleTitle}</span>
                      </div>
                    )}

                    {/* Interactions Footer Wrapped */}
                    <div className="flex items-center justify-between p-1 bg-slate-50 dark:bg-slate-800/60 rounded-xl border border-slate-200/60 dark:border-slate-700/60 mt-3 text-xs">
                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.9 }}
                        onClick={(e) => {
                          e.stopPropagation();
                          likeMoment(mom.id);
                        }}
                        className={`flex items-center space-x-1 px-2.5 py-1 rounded-lg transition-all cursor-pointer ${
                          mom.isLiked
                            ? 'bg-rose-50 dark:bg-rose-950/80 text-rose-500 font-bold border border-rose-200 dark:border-rose-800'
                            : 'hover:bg-white dark:hover:bg-slate-700 text-slate-500 dark:text-slate-400'
                        }`}
                      >
                        <Heart className={`w-3.5 h-3.5 ${mom.isLiked ? 'fill-rose-500' : ''}`} />
                        <span>{mom.likesCount}</span>
                      </motion.button>

                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.9 }}
                        onClick={(e) => {
                          e.stopPropagation();
                          favoriteMoment(mom.id);
                        }}
                        className={`flex items-center space-x-1 px-2.5 py-1 rounded-lg transition-all cursor-pointer ${
                          mom.isFavorited
                            ? 'bg-amber-50 dark:bg-amber-950/80 text-amber-600 dark:text-amber-400 font-bold border border-amber-200 dark:border-amber-800'
                            : 'hover:bg-white dark:hover:bg-slate-700 text-slate-500 dark:text-slate-400'
                        }`}
                      >
                        <Bookmark className={`w-3.5 h-3.5 ${mom.isFavorited ? 'fill-amber-500' : ''}`} />
                        <span>{mom.favoritesCount}</span>
                      </motion.button>

                      <button className="flex items-center space-x-1 px-2.5 py-1 rounded-lg hover:bg-white dark:hover:bg-slate-700 text-slate-500 dark:text-slate-400 transition-all cursor-pointer">
                        <MessageCircle className="w-3.5 h-3.5" />
                        <span>{mom.commentsCount}</span>
                      </button>

                      <button className="p-1 rounded-lg hover:bg-white dark:hover:bg-slate-700 text-slate-400 transition-all cursor-pointer">
                        <Share2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </motion.div>
                );
              })}
            </AnimatePresence>
          </div>

        </div>

        {/* Right Column: Active Moment Detail & Comments (4 cols) */}
        <div className="lg:col-span-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5 shadow-xs sticky top-16 space-y-3">
            <h3 className="text-xs font-bold text-slate-900 dark:text-white pb-2 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <span>动态详情与讨论</span>
              <span className="text-[10px] text-emerald-600 font-mono">ID: {activeMoment?.id}</span>
            </h3>

            {activeMoment && (
              <div className="space-y-3">
                <div className="flex items-center space-x-2">
                  <img src={activeMoment.author.avatar} alt="" className="w-7 h-7 rounded-full" />
                  <div>
                    <h4 className="text-xs font-bold text-slate-900 dark:text-white">{activeMoment.author.displayName}</h4>
                    <p className="text-[10px] text-slate-400">@{activeMoment.author.username}</p>
                  </div>
                </div>

                <p className="text-xs text-slate-700 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/40 p-2.5 rounded-xl">
                  {activeMoment.textContent}
                </p>

                {/* Comment Input */}
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    if (commentInput.trim()) {
                      alert('动态评论发送成功！');
                      setCommentInput('');
                    }
                  }}
                  className="space-y-2 pt-2"
                >
                  <input
                    type="text"
                    placeholder="发表你的看法..."
                    value={commentInput}
                    onChange={(e) => setCommentInput(e.target.value)}
                    className="w-full px-3 py-1.5 text-xs bg-slate-100 dark:bg-slate-800 border border-transparent focus:border-emerald-500 rounded-xl"
                  />
                  <button
                    type="submit"
                    disabled={!commentInput.trim()}
                    className="w-full py-1.5 bg-emerald-600 text-white rounded-xl text-xs font-semibold disabled:opacity-50"
                  >
                    发送评论
                  </button>
                </form>
              </div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
};
