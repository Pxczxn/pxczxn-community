/**
 * 星语社区 (pxczxn-community V2.1) - 团队公开主页 (Team Public Homepage)
 */

import React, { useEffect, useState } from 'react';
import { useApp } from '../../context/AppContext';
import { communityApi, type SubmittableArticle } from '../../../lib/community-api';
import {
  Users,
  BookOpen,
  FileText,
  Send,
  ArrowLeft,
  UserPlus,
  CheckCircle2,
  Globe,
  Share2,
  Shield,
  Sparkles,
} from 'lucide-react';

export const TeamDetailView: React.FC = () => {
  const { teams, articles, seriesList, routeParams, navigateTo, toggleFollowTeam, isCompactViewport } = useApp();

  const slug = routeParams.slug || 'starry-core-dev';
  const team = teams.find((t) => t.slug === slug) || teams[0];

  const [activeTab, setActiveTab] = useState<'ARTICLES' | 'SERIES' | 'MEMBERS' | 'SUBMIT'>('ARTICLES');
  const [submittableArticles, setSubmittableArticles] = useState<SubmittableArticle[]>([]);
  const [selectedArticleId, setSelectedArticleId] = useState('');
  const [submissionComment, setSubmissionComment] = useState('');
  const [submittedSuccess, setSubmittedSuccess] = useState(false);

  useEffect(() => {
    communityApi.submittableArticles()
      .then((items) => {
        setSubmittableArticles(items);
        setSelectedArticleId((current) => current || items[0]?.articleId || '');
      })
      .catch(() => setSubmittableArticles([]));
  }, []);

  const handleSubmitToTeam = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedArticleId || !team?.id) return;
    try {
      await communityApi.createTeamSubmission({
        sourceArticleId: selectedArticleId,
        targetTeamId: team.id,
      });
      setSubmittedSuccess(true);
      setSubmissionComment('');
    } catch {
      setSubmittedSuccess(false);
    }
  };

  return (
    <div className={`max-w-6xl mx-auto px-3 sm:px-4 lg:px-6 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Top Bar */}
      <button
        onClick={() => navigateTo('/teams')}
        className="flex items-center space-x-1 text-xs text-slate-600 dark:text-slate-300 hover:text-indigo-600 mb-4"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>返回团队大厅</span>
      </button>

      {/* Hero Header */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-6 shadow-xs mb-4">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-center space-x-4">
            <img src={team?.avatar} alt="" className="w-14 h-14 sm:w-16 sm:h-16 rounded-2xl object-cover border-2 border-indigo-500 shadow-xs" />
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-base sm:text-lg font-black text-slate-900 dark:text-white">
                  {team?.name}
                </h1>
                <CheckCircle2 className="w-4 h-4 text-indigo-500 fill-indigo-500/20" />
              </div>
              <p className="text-xs text-slate-400">Team Slug: @{team?.slug} · 方向: {team?.contentDirection}</p>
              <p className="text-xs text-slate-600 dark:text-slate-300 mt-1 line-clamp-2">{team?.description}</p>
            </div>
          </div>

          <div className="flex items-center space-x-2 w-full sm:w-auto">
            <button
              onClick={() => toggleFollowTeam(team.id)}
              className={`flex-1 sm:flex-initial px-4 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                team.isFollowing ? 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300' : 'bg-indigo-600 text-white shadow-xs'
              }`}
            >
              {team.isFollowing ? '已关注团队' : '+ 关注团队'}
            </button>

            {team.isMyTeam && (
              <button
                onClick={() => navigateTo('/teams/:slug/workspace', { slug: team.slug })}
                className="px-3.5 py-1.5 bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 text-xs font-bold rounded-xl"
              >
                进入管理工作台
              </button>
            )}
          </div>
        </div>

        {/* Stats Strip */}
        <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 text-center text-xs">
          <div><p className="font-bold text-slate-900 dark:text-white">{team?.articlesCount}</p><p className="text-[10px] text-slate-400">公开文章</p></div>
          <div><p className="font-bold text-slate-900 dark:text-white">{team?.membersCount}</p><p className="text-[10px] text-slate-400">核心成员</p></div>
          <div><p className="font-bold text-slate-900 dark:text-white">{team?.followersCount}</p><p className="text-[10px] text-slate-400">关注者</p></div>
          <div className="hidden sm:block"><p className="font-bold text-emerald-600">已开放</p><p className="text-[10px] text-slate-400">对外投稿</p></div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center space-x-2 border-b border-slate-200 dark:border-slate-800 mb-4 pb-1 text-xs font-semibold">
        {[
          { key: 'ARTICLES', label: '团队内容', icon: <FileText className="w-3.5 h-3.5" /> },
          { key: 'SERIES', label: '专栏系列', icon: <BookOpen className="w-3.5 h-3.5" /> },
          { key: 'MEMBERS', label: '团队成员', icon: <Users className="w-3.5 h-3.5" /> },
          { key: 'SUBMIT', label: '向团队投稿', icon: <Send className="w-3.5 h-3.5 text-indigo-500" /> },
        ].map((t) => (
          <button
            key={t.key}
            onClick={() => setActiveTab(t.key as any)}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl transition-colors ${
              activeTab === t.key ? 'bg-indigo-600 text-white font-bold' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            {t.icon}
            <span>{t.label}</span>
          </button>
        ))}
      </div>

      {/* Tab Panels */}
      {activeTab === 'ARTICLES' && (
        <div className="space-y-3">
          {articles.map((art) => (
            <div
              key={art.id}
              onClick={() => navigateTo('/articles/:id', { id: art.id })}
              className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs hover:border-indigo-300 transition-colors cursor-pointer flex justify-between items-center text-xs"
            >
              <div>
                <h3 className="font-bold text-slate-900 dark:text-white text-sm">{art.title}</h3>
                <p className="text-slate-500 dark:text-slate-400 mt-1 line-clamp-1">{art.summary}</p>
                <p className="text-[10px] text-slate-400 mt-1">作者：{art.author.displayName} · 发布于 {art.publishedAt}</p>
              </div>
              <button className="text-indigo-600 font-semibold shrink-0 ml-4">阅读</button>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'SERIES' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {seriesList.filter((s) => s.isTeam).map((ser) => (
            <div
              key={ser.id}
              onClick={() => navigateTo('/series/:id', { id: ser.id })}
              className="bg-white dark:bg-slate-900 border rounded-2xl p-4 text-xs space-y-2 cursor-pointer hover:border-purple-300 transition-colors"
            >
              <h3 className="font-bold text-slate-900 dark:text-white">{ser.title}</h3>
              <p className="text-slate-500">{ser.description}</p>
              <div className="pt-2 border-t flex justify-between text-[11px] text-slate-400">
                <span>{ser.totalChapters} 章节</span>
                <span>{ser.followersCount} 人追更</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'MEMBERS' && (
        <div className="bg-white dark:bg-slate-900 border rounded-2xl p-4 text-xs space-y-3">
          <h2 className="font-bold text-slate-900 dark:text-white">团队核心成员列表 ({team?.membersCount})</h2>
          <div className="space-y-2">
            <div className="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-xl flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <img src={team?.avatar} alt="" className="w-8 h-8 rounded-full" />
                <div>
                  <p className="font-bold">星语客</p>
                  <p className="text-[10px] text-slate-400">团队 Owner · 社区主理人</p>
                </div>
              </div>
              <span className="text-[10px] bg-indigo-100 text-indigo-800 font-bold px-2 py-0.5 rounded">Owner</span>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'SUBMIT' && (
        <div className="bg-white dark:bg-slate-900 border rounded-2xl p-4 text-xs space-y-3">
          <h2 className="font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
            <Send className="w-4 h-4 text-indigo-500" />
            <span>向 {team?.name} 提交投稿请求</span>
          </h2>

          {submittedSuccess ? (
            <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl font-bold">
              🎉 投稿申请已成功推送到团队审核队列！管理员审核通过后将自动收录至团队专栏。
            </div>
          ) : (
            <form onSubmit={handleSubmitToTeam} className="space-y-3 max-w-lg">
              <div>
                <label className="block text-slate-500 mb-1">选择已发布文章标题或手动填写</label>
                <select
                  value={selectedArticleId}
                  onChange={(e) => setSelectedArticleId(e.target.value)}
                  className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
                >
                  {submittableArticles.length === 0 ? (
                    <option value="">暂无可投稿的文章</option>
                  ) : submittableArticles.map((article) => (
                    <option key={article.articleId} value={article.articleId}>{article.title}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-slate-500 mb-1">附言 / 审核备注</label>
                <textarea
                  rows={2}
                  placeholder="说明文章与团队技术方向的关联度..."
                  value={submissionComment}
                  onChange={(e) => setSubmissionComment(e.target.value)}
                  className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl"
                />
              </div>
              <button type="submit" disabled={!selectedArticleId} className="px-4 py-1.5 bg-indigo-600 text-white font-bold rounded-xl disabled:opacity-50">
                提交审核
              </button>
            </form>
          )}
        </div>
      )}

    </div>
  );
};
