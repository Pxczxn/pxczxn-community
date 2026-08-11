/**
 * 星语社区 (pxczxn-community V2.2) - 团队大厅 (Collaboration Hub)
 * PC 端优化：正文 14px，模块间距 16~20px，卡片 padding p-4/p-5
 */

import React, { useEffect, useState } from 'react';
import { useApp } from '../../context/AppContext';
import { communityApi, type TeamApplication, type TeamInvitation } from '../../../lib/community-api';
import { Team } from '../../types';
import {
  Users,
  Plus,
  Compass,
  UserCheck,
  Building2,
  ChevronRight,
  ShieldCheck,
  FileText,
  BookOpen,
  Send,
  Sparkles,
  Check,
  X,
} from 'lucide-react';

export const TeamsView: React.FC = () => {
  const { teams, navigateTo, isCompactViewport, toggleFollowTeam } = useApp();

  const [activeTab, setActiveTab] = useState<'MY_TEAMS' | 'DISCOVER_TEAMS' | 'APPLICATIONS'>('MY_TEAMS');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newTeamName, setNewTeamName] = useState('');
  const [newTeamDesc, setNewTeamDesc] = useState('');
  const [newTeamCategory, setNewTeamCategory] = useState('技术研发');
  const [application, setApplication] = useState<TeamApplication | null>(null);
  const [invitations, setInvitations] = useState<TeamInvitation[]>([]);

  useEffect(() => {
    Promise.all([communityApi.myTeamApplication(), communityApi.myTeamInvitations()])
      .then(([currentApplication, currentInvitations]) => {
        setApplication(currentApplication);
        setInvitations(currentInvitations.filter((item) => item.status === 'PENDING'));
      })
      .catch(() => {
        setApplication(null);
        setInvitations([]);
      });
  }, []);

  const myTeams = teams.filter((t) => t.myRole !== undefined);

  const handleCreateTeamSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTeamName.trim()) return;
    const slug = `team-${Date.now().toString(36)}`;
    try {
      const created = await communityApi.submitTeamApplication({ teamName: newTeamName.trim(), teamSlug: slug, description: newTeamDesc.trim() || undefined });
      setApplication(created);
      setNewTeamName(''); setNewTeamDesc(''); setShowCreateModal(false);
    } catch { /* Preserve form input when the API rejects the request. */ }
  };

  const handleInvitation = async (invitationId: string, action: 'accept' | 'reject') => {
    try {
      if (action === 'accept') await communityApi.acceptTeamInvitation(invitationId);
      else await communityApi.rejectTeamInvitation(invitationId);
      setInvitations((current) => current.filter((item) => item.id !== invitationId));
    } catch { /* Keep the invitation visible if the server rejects the action. */ }
  };

  return (
    <div className={`max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 transition-all ${isCompactViewport ? 'py-4' : 'py-6'}`}>

      {/* Header Banner */}
      <div className="mb-6 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-indigo-100 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold shadow-sm">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-lg sm:text-xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
              团队大厅
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
              团队博客归属 · 协同投稿审核 · 团队工作台 (Workspace)
            </p>
          </div>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold shadow-sm transition-all flex items-center gap-2 self-start sm:self-center"
        >
          <Plus className="w-4 h-4" />
          <span>申请创建新团队</span>
        </button>
      </div>

      {/* Main Tab Navigation */}
      <div className="flex items-center gap-3 border-b border-slate-200 dark:border-slate-800 mb-6 pb-2 text-sm font-semibold">
        <button
          onClick={() => setActiveTab('MY_TEAMS')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl transition-colors ${
            activeTab === 'MY_TEAMS'
              ? 'bg-indigo-600 text-white shadow-sm'
              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
          }`}
        >
          <Building2 className="w-4 h-4" />
          <span>我的团队 ({myTeams.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('DISCOVER_TEAMS')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl transition-colors ${
            activeTab === 'DISCOVER_TEAMS'
              ? 'bg-indigo-600 text-white shadow-sm'
              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
          }`}
        >
          <Compass className="w-4 h-4" />
          <span>发现公开团队 ({teams.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('APPLICATIONS')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl transition-colors ${
            activeTab === 'APPLICATIONS'
              ? 'bg-indigo-600 text-white shadow-sm'
              : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
          }`}
        >
          <UserCheck className="w-4 h-4" />
          <span>邀请与建队申请</span>
        </button>
      </div>

      {/* TAB 1: 我的团队 */}
      {activeTab === 'MY_TEAMS' && (
        <div className="space-y-5">
          {myTeams.map((tm) => (
            <div
              key={tm.id}
              className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm flex flex-col sm:flex-row justify-between gap-5"
            >
              <div className="flex items-start gap-4">
                <img src={tm.avatar} alt={tm.name} className="w-14 h-14 rounded-xl object-cover shrink-0 border border-slate-100 shadow-sm" />
                <div className="space-y-2">
                  <div className="flex items-center gap-3">
                    <h2 className="text-base font-extrabold text-slate-900 dark:text-white">{tm.name}</h2>
                    <span className="text-xs bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-300 px-2.5 py-1 rounded-lg font-bold">
                      你的角色：{tm.myRole}
                    </span>
                  </div>
                  <p className="text-sm text-slate-500 dark:text-slate-400">{tm.description}</p>

                  <div className="flex flex-wrap items-center gap-3 text-sm pt-1">
                    <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full font-medium bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200/60 dark:border-slate-700/60">
                      <Users className="w-4 h-4 text-slate-400" />
                      <span>{tm.membersCount} 成员</span>
                    </span>

                    <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full font-medium bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200/60 dark:border-slate-700/60">
                      <FileText className="w-4 h-4 text-slate-400" />
                      <span>{tm.articlesCount} 文章</span>
                    </span>

                    <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full font-medium bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200/60 dark:border-slate-700/60">
                      <BookOpen className="w-4 h-4 text-slate-400" />
                      <span>{tm.seriesCount} 系列</span>
                    </span>

                    {tm.pendingSubmissionsCount > 0 && (
                      <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full font-semibold bg-rose-50 dark:bg-rose-950/60 text-rose-600 dark:text-rose-300 border border-rose-200 dark:border-rose-900/50">
                        <Send className="w-4 h-4 text-rose-500" />
                        <span>{tm.pendingSubmissionsCount} 待处理投稿</span>
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex flex-row sm:flex-col items-end justify-between sm:justify-center gap-3 border-t sm:border-t-0 pt-4 sm:pt-0 border-slate-100 dark:border-slate-800">
                <button
                  onClick={() => navigateTo('/teams/:slug/workspace', { slug: tm.slug })}
                  className="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold shadow-sm transition-colors flex items-center gap-2"
                >
                  <span>进入私有工作台 (Workspace)</span>
                  <ChevronRight className="w-4 h-4" />
                </button>

                <button
                  onClick={() => navigateTo('/teams/:slug', { slug: tm.slug })}
                  className="text-sm text-slate-500 hover:text-indigo-600 dark:hover:text-indigo-400"
                >
                  查看团队公开主页
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* TAB 2: 发现公开团队 */}
      {activeTab === 'DISCOVER_TEAMS' && (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
          {teams.map((tm) => (
            <div
              key={tm.id}
              className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center gap-4 mb-3">
                  <img src={tm.avatar} alt="" className="w-12 h-12 rounded-xl object-cover" />
                  <div>
                    <h3 className="text-sm font-bold text-slate-900 dark:text-white line-clamp-1">{tm.name}</h3>
                    <span className="text-xs text-slate-400">{tm.category} · {tm.membersCount} 成员</span>
                  </div>
                </div>
                <p className="text-sm text-slate-500 dark:text-slate-400 line-clamp-2">{tm.description}</p>
              </div>

              <div className="flex items-center justify-between pt-4 mt-4 border-t border-slate-100 dark:border-slate-800">
                <button
                  onClick={() => navigateTo('/teams/:slug', { slug: tm.slug })}
                  className="text-sm text-indigo-600 dark:text-indigo-400 font-semibold"
                >
                  公开主页 →
                </button>

                <button
                  onClick={() => toggleFollowTeam(tm.id)}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    tm.isFollowing
                      ? 'bg-slate-100 dark:bg-slate-800 text-slate-600'
                      : 'bg-indigo-50 dark:bg-indigo-950 text-indigo-600 hover:bg-indigo-100'
                  }`}
                >
                  {tm.isFollowing ? '已关注' : '+ 关注团队'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* TAB 3: 邀请与申请 */}
      {activeTab === 'APPLICATIONS' && (
        <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-5 shadow-sm space-y-5">
          <h2 className="text-base font-bold text-slate-900 dark:text-white">待处理的团队邀请</h2>
          {application && (
            <div className="p-4 bg-indigo-50 dark:bg-indigo-950/30 rounded-xl text-sm text-indigo-800 dark:text-indigo-200">
              建队申请：{application.teamName} · {application.status}
            </div>
          )}
          {invitations.map((invitation) => (
            <div key={invitation.id} className="p-4 bg-slate-50 dark:bg-slate-800/40 rounded-xl flex items-center justify-between text-sm">
              <span>{invitation.teamName || `团队 #${invitation.teamId}`} · {invitation.roleCode}</span>
              <div className="flex items-center gap-3">
                <button onClick={() => handleInvitation(invitation.id, 'accept')} className="px-4 py-2 bg-indigo-600 text-white rounded-lg font-semibold">接受邀请</button>
                <button onClick={() => handleInvitation(invitation.id, 'reject')} className="px-4 py-2 bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-lg">拒绝</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Team Modal Popup */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-md w-full p-5 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-3">
              <h3 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <Sparkles className="w-5 h-5 text-indigo-500" />
                申请建立新团队
              </h3>
              <button onClick={() => setShowCreateModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateTeamSubmit} className="space-y-4 text-sm">
              <div>
                <label className="block text-slate-700 dark:text-slate-300 font-semibold mb-2">团队名称</label>
                <input
                  type="text"
                  placeholder="如：前端技术委员会"
                  value={newTeamName}
                  onChange={(e) => setNewTeamName(e.target.value)}
                  className="w-full p-3 bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-700 dark:text-slate-300 font-semibold mb-2">团队分类</label>
                <select
                  value={newTeamCategory}
                  onChange={(e) => setNewTeamCategory(e.target.value)}
                  className="w-full p-3 bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl"
                >
                  <option value="技术研发">技术研发</option>
                  <option value="云原生/运维">云原生/运维</option>
                  <option value="人工智能">人工智能</option>
                  <option value="开源项目">开源项目</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-700 dark:text-slate-300 font-semibold mb-2">团队简介与创作方向</label>
                <textarea
                  rows={3}
                  placeholder="描述团队的主要方向、使命与创作规则..."
                  value={newTeamDesc}
                  onChange={(e) => setNewTeamDesc(e.target.value)}
                  className="w-full p-3 bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2 border-t border-slate-100 dark:border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300"
                >
                  取消
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-indigo-600 text-white font-semibold shadow-sm"
                >
                  提交申请
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};
