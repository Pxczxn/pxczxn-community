/**
 * 星语社区 (pxczxn-community V2.1) - 消息通知中心 (Notification Center)
 */

import React, { useState } from 'react';
import { useApp } from '../../context/AppContext';
import {
  Bell,
  CheckCheck,
  Heart,
  MessageCircle,
  UserPlus,
  Send,
  Shield,
  Sparkles,
  Bookmark,
} from 'lucide-react';

export const NotificationsView: React.FC = () => {
  const { notifications, markNotificationRead, markAllNotificationsRead, isCompactViewport, navigateTo } = useApp();

  const [filterType, setFilterType] = useState<'ALL' | 'UNREAD' | 'SYSTEM' | 'INTERACTION'>('ALL');

  const filteredNotifications = notifications.filter((n) => {
    if (filterType === 'UNREAD') return !n.isRead;
    if (filterType === 'SYSTEM') return n.type === 'SYSTEM' || n.type === 'GOVERNANCE';
    if (filterType === 'INTERACTION') return n.type === 'LIKE' || n.type === 'COMMENT' || n.type === 'SUBMISSION';
    return true;
  });

  return (
    <div className={`max-w-4xl mx-auto px-3 sm:px-4 transition-all ${isCompactViewport ? 'py-3' : 'py-6'}`}>

      {/* Header */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 shadow-xs mb-4 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="p-2 bg-indigo-50 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-300 rounded-xl">
            <Bell className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-extrabold text-slate-900 dark:text-white">
              社区消息与通知中心
            </h1>
            <p className="text-xs text-slate-400">互动提醒 · 团队投稿审核 · 治理合规公告</p>
          </div>
        </div>

        <button
          onClick={markAllNotificationsRead}
          className="flex items-center space-x-1 px-3 py-1.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-semibold hover:bg-slate-200"
        >
          <CheckCheck className="w-3.5 h-3.5 text-indigo-500" />
          <span>全部已读</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center space-x-2 border-b border-slate-200 dark:border-slate-800 mb-4 pb-1 text-xs font-semibold">
        {[
          { key: 'ALL', label: '全部消息' },
          { key: 'UNREAD', label: '未读消息' },
          { key: 'INTERACTION', label: '互动与投稿' },
          { key: 'SYSTEM', label: '系统与治理' },
        ].map((t) => (
          <button
            key={t.key}
            onClick={() => setFilterType(t.key as any)}
            className={`px-3 py-1.5 rounded-xl transition-colors ${
              filterType === t.key ? 'bg-indigo-600 text-white font-bold' : 'text-slate-600 dark:text-slate-300'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Notifications List */}
      <div className="space-y-2.5">
        {filteredNotifications.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-400 bg-white dark:bg-slate-900 rounded-2xl border">
            暂无相关通知消息
          </div>
        ) : (
          filteredNotifications.map((n) => (
            <div
              key={n.id}
              onClick={() => markNotificationRead(n.id)}
              className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-start justify-between gap-3 text-xs ${
                !n.isRead
                  ? 'bg-indigo-50/40 dark:bg-indigo-950/30 border-indigo-200 dark:border-indigo-900'
                  : 'bg-white dark:bg-slate-900 border-slate-200/80 dark:border-slate-800'
              }`}
            >
              <div className="flex items-start space-x-3">
                <div className="p-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 shrink-0 mt-0.5">
                  {n.type === 'LIKE' && <Heart className="w-4 h-4 text-rose-500" />}
                  {n.type === 'COMMENT' && <MessageCircle className="w-4 h-4 text-indigo-500" />}
                  {n.type === 'SUBMISSION' && <Send className="w-4 h-4 text-amber-500" />}
                  {n.type === 'SYSTEM' && <Bell className="w-4 h-4 text-purple-500" />}
                  {n.type === 'GOVERNANCE' && <Shield className="w-4 h-4 text-emerald-500" />}
                </div>

                <div>
                  <div className="flex items-center space-x-2">
                    <h3 className="font-bold text-slate-900 dark:text-white">{n.title}</h3>
                    {!n.isRead && (
                      <span className="w-2 h-2 rounded-full bg-indigo-600 shrink-0"></span>
                    )}
                  </div>
                  <p className="text-slate-600 dark:text-slate-300 mt-0.5">{n.content}</p>
                  <p className="text-[10px] text-slate-400 mt-1">{n.timestamp}</p>
                </div>
              </div>

              {n.targetUrl && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    if (n.targetUrl) navigateTo(n.targetUrl);
                  }}
                  className="px-2.5 py-1 bg-indigo-600 text-white font-bold rounded-lg text-[11px] shrink-0"
                >
                  去查看
                </button>
              )}
            </div>
          ))
        )}
      </div>

    </div>
  );
};
