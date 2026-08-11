/**
 * 星语社区 (pxczxn-community V2.1) - 团队私有工作台 (Team Workspace)
 * 具备 6 大核心板块：概览 Overview / 内容 Content / 系列 Series / 投稿 Submissions / 成员 Members / 设置 Settings
 */

import React, { useEffect, useState } from 'react';
import { useApp } from '../../context/AppContext';
import { communityApi, type TeamSubmission } from '../../../lib/community-api';
import {
  Users,
  LayoutDashboard,
  FileText,
  BookOpen,
  Send,
  Settings,
  ArrowLeft,
  ChevronRight,
  Plus,
  CheckCircle2,
  XCircle,
  AlertCircle,
  TrendingUp,
  UserPlus,
  Shield,
  Eye,
  Heart,
  MessageCircle,
} from 'lucide-react';

export const TeamWorkspaceView: React.FC = () => {
  const { teams, articles, seriesList, routeParams, navigateTo, isCompactViewport } = useApp();

  const slug = routeParams.slug;
  const team = slug ? teams.find((item) => item.slug === slug) : undefined;

  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'CONTENT' | 'SERIES' | 'SUBMISSIONS' | 'MEMBERS' | 'SETTINGS'>('OVERVIEW');
  const [teamSettingsSaved, setTeamSettingsSaved] = useState(false);

  // Local state for submissions
  type WorkspaceSubmission = TeamSubmission & { author: string; submittedAt: string };
  const [submissionsList, setSubmissionsList] = useState<WorkspaceSubmission[]>([]); /*
    {
      id: 'sub-101',
      title: 'React 19 异步组件与 Server Actions 在高并发社区中的最佳实践',
      author: '前端匠人',
      submittedAt: '2026-08-09 16:20',
      status: 'PENDING',
      comment: '请求归入团队技术专栏。',
    },
    {
      id: 'sub-102',
      title: '大模型 Agent 在知识沉淀中的应用',
      author: '智语探索家',
      submittedAt: '2026-08-08 11:00',
      status: 'APPROVED',
      comment: '审核通过：内容符合团队技术方向。',
    },
  ]); */

  useEffect(() => {
    if (!team?.id) return;
    communityApi.teamSubmissions(team.id)
      .then((items) => setSubmissionsList(items.map((item) => ({
        ...item,
        author: item.submittedByUserId,
        submittedAt: item.createdAt,
      }))))
      .catch(() => setSubmissionsList([]));
  }, [team?.id]);

  const handleAuditSubmission = async (submission: TeamSubmission, action: 'approve' | 'revision') => {
    try {
      const updated = await communityApi.decideTeamSubmission(submission.id, action, submission.lockVersion);
      setSubmissionsList((prev) => prev.map((item) => item.id === updated.id ? {
        ...updated,
        author: updated.submittedByUserId,
        submittedAt: updated.createdAt,
      } : item));
    } catch {
      // Keep the persisted value visible when the server rejects a stale or unauthorized action.
    }
  };

  const handleTeamSettingsSave = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!team) return;
    const form = new FormData(event.currentTarget);
    try {
      await communityApi.updateTeamSettings(team.id, {
        name: team.name,
        contentDirection: String(form.get('contentDirection') || ''),
      });
      setTeamSettingsSaved(true);
    } catch {
      setTeamSettingsSaved(false);
    }
  };

  if (!team) {
    return <div className="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 py-6 text-sm text-slate-500">团队加载中或不存在。</div>;
  }

  return (
    <div className={`max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Workspace Header & Team Context Bar */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs mb-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center space-x-3">
            <button
              onClick={() => navigateTo('/teams')}
              className="p-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200"
              title="返回团队大厅"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <img src={team?.avatar} alt="" className="w-10 h-10 rounded-xl object-cover" />
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-sm sm:text-base font-extrabold text-slate-900 dark:text-white">
                  {team?.name} · 私有工作台
                </h1>
                <span className="text-[10px] bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-300 px-2 py-0.2 rounded font-bold">
                  {team?.myRole || 'OWNER'} 权限
                </span>
              </div>
              <p className="text-[11px] text-slate-400 mt-0.5">Team Slug: {team?.slug}</p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => navigateTo('/editor/new')}
              className="bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-xl text-xs font-semibold shadow-xs transition-colors flex items-center space-x-1"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>写团队文章</span>
            </button>
            <button
              onClick={() => navigateTo('/teams/:slug', { slug: team?.slug })}
              className="bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 px-3 py-1.5 rounded-xl text-xs font-medium"
            >
              查看公开主页
            </button>
          </div>
        </div>

        {/* 6 Core Navigation Tabs */}
        <div className="flex items-center space-x-1 sm:space-x-2 pt-3 text-xs font-semibold overflow-x-auto">
          {[
            { key: 'OVERVIEW', label: '概览 Overview', icon: <LayoutDashboard className="w-3.5 h-3.5" /> },
            { key: 'CONTENT', label: '内容 Asset', icon: <FileText className="w-3.5 h-3.5" /> },
            { key: 'SERIES', label: '系列 Series', icon: <BookOpen className="w-3.5 h-3.5" /> },
            { key: 'SUBMISSIONS', label: '投稿 Submissions', icon: <Send className="w-3.5 h-3.5" /> },
            { key: 'MEMBERS', label: '成员 Members', icon: <Users className="w-3.5 h-3.5" /> },
            { key: 'SETTINGS', label: '设置 Settings', icon: <Settings className="w-3.5 h-3.5" /> },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as any)}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl transition-colors whitespace-nowrap ${
                activeTab === tab.key
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              {tab.icon}
              <span>{tab.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* SECTION 1: OVERVIEW */}
      {activeTab === 'OVERVIEW' && (
        <div className="space-y-4">
          {/* Quick Stats Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5">
              <p className="text-[11px] text-slate-400">公开文章总数</p>
              <p className="text-lg font-black text-slate-900 dark:text-white mt-0.5">{team?.articlesCount}</p>
            </div>
            <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5">
              <p className="text-[11px] text-slate-400">团队成员</p>
              <p className="text-lg font-black text-slate-900 dark:text-white mt-0.5">{team?.membersCount}</p>
            </div>
            <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5">
              <p className="text-[11px] text-slate-400">关注人数</p>
              <p className="text-lg font-black text-slate-900 dark:text-white mt-0.5">{team?.followersCount}</p>
            </div>
            <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-3.5">
              <p className="text-[11px] text-slate-400">待处理投稿</p>
              <p className="text-lg font-black text-rose-500 mt-0.5">{submissionsList.filter((s) => s.status === 'TEAM_PENDING').length}</p>
            </div>
          </div>

          {/* Pending Tasks & Recent Activity */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4">
            <h2 className="text-xs font-bold text-slate-900 dark:text-white mb-2">团队协作待办 (Team Todos)</h2>
            <div className="space-y-2 text-xs">
              <div className="p-2.5 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 rounded-xl flex items-center justify-between">
                <span className="text-rose-700 dark:text-rose-300 font-medium">有 1 篇来自“前端匠人”的投稿等待审核</span>
                <button onClick={() => setActiveTab('SUBMISSIONS')} className="text-[11px] font-bold text-rose-600 underline">
                  去审核 →
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* SECTION 2: CONTENT */}
      {activeTab === 'CONTENT' && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold text-slate-900 dark:text-white">团队博客内容资产</h2>
            <button onClick={() => navigateTo('/editor/new')} className="text-xs text-indigo-600 font-semibold">+ 新增内容</button>
          </div>

          <div className="space-y-2">
            {articles.map((art) => (
              <div key={art.id} className="p-3 border border-slate-100 dark:border-slate-800 rounded-xl flex items-center justify-between text-xs">
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white">{art.title}</h3>
                  <p className="text-[10px] text-slate-400">作者：{art.author.displayName} · 发布时间：{art.publishedAt}</p>
                </div>
                <button onClick={() => navigateTo('/articles/:id', { id: art.id })} className="text-indigo-600">查看</button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* SECTION 3: SERIES */}
      {activeTab === 'SERIES' && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 space-y-3">
          <h2 className="text-xs font-bold text-slate-900 dark:text-white">团队系列管理</h2>
          <div className="space-y-2">
            {seriesList.filter((s) => s.isTeam).map((ser) => (
              <div key={ser.id} className="p-3 border border-slate-100 dark:border-slate-800 rounded-xl flex items-center justify-between text-xs">
                <span className="font-bold">{ser.title} ({ser.totalChapters} 章)</span>
                <button onClick={() => navigateTo('/series/:id', { id: ser.id })} className="text-purple-600">编排目录</button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* SECTION 4: SUBMISSIONS */}
      {activeTab === 'SUBMISSIONS' && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 space-y-3">
          <h2 className="text-xs font-bold text-slate-900 dark:text-white">投稿审核队列</h2>
          <div className="space-y-2.5">
            {submissionsList.map((sub) => (
              <div key={sub.id} className="p-3 border border-slate-100 dark:border-slate-800 rounded-xl flex flex-col sm:flex-row justify-between gap-2 text-xs">
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-slate-900 dark:text-white">{sub.sourceArticleTitle}</span>
                    <span className={`text-[10px] px-1.5 py-0.2 rounded font-bold ${
                      sub.status === 'TEAM_PENDING' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
                    }`}>
                      {sub.status}
                    </span>
                  </div>
                  <p className="text-[10px] text-slate-400 mt-0.5">投稿人：{sub.author} · 时间：{sub.submittedAt}</p>
                </div>

                {sub.status === 'TEAM_PENDING' && (
                  <div className="flex items-center space-x-2">
                    <button onClick={() => handleAuditSubmission(sub, 'approve')} className="px-3 py-1 bg-emerald-600 text-white rounded-lg font-bold">
                      通过
                    </button>
                    <button onClick={() => handleAuditSubmission(sub, 'revision')} className="px-3 py-1 bg-rose-600 text-white rounded-lg font-bold">
                      退回修改
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* SECTION 5: MEMBERS */}
      {activeTab === 'MEMBERS' && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold text-slate-900 dark:text-white">成员管理与权限矩阵</h2>
            <button className="text-xs text-indigo-600 font-semibold">+ 邀请新成员</button>
          </div>
          <div className="p-3 border border-slate-100 dark:border-slate-800 rounded-xl flex items-center justify-between text-xs">
            <span>星语客 (OWNER)</span>
            <span className="text-slate-400">拥有全站最高团队权限</span>
          </div>
        </div>
      )}

      {/* SECTION 6: SETTINGS */}
      {activeTab === 'SETTINGS' && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 space-y-3 text-xs">
          <h2 className="text-xs font-bold text-slate-900 dark:text-white">团队 Portal 与 SEO 设置</h2>
          <form onSubmit={handleTeamSettingsSave} className="space-y-2 max-w-md">
            <div>
              <label className="block text-slate-500 mb-1">团队 Content Direction</label>
              <input name="contentDirection" type="text" defaultValue={team?.contentDirection} className="w-full p-2 bg-slate-100 dark:bg-slate-800 border rounded-xl" />
            </div>
            <button type="submit" className="px-4 py-1.5 bg-indigo-600 text-white font-bold rounded-xl">保存设置</button>
            {teamSettingsSaved && <p className="text-emerald-600">设置已保存</p>}
          </form>
        </div>
      )}

    </div>
  );
};
