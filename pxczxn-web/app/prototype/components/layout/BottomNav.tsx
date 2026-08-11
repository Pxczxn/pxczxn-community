/**
 * 星语社区 (pxczxn-community V2.1) - 微信风格移动端底部导航栏 (BottomNav)
 * 具备 5 大经典 Tab 切换、高亮动画、中央悬浮创作按钮与红点提示
 */

import React from 'react';
import { motion } from 'motion/react';
import { useApp } from '../../context/AppContext';
import {
  Sparkles,
  Compass,
  PenSquare,
  MessageSquare,
  User,
  BookOpen,
  Users,
} from 'lucide-react';

export const BottomNav: React.FC = () => {
  const { currentRoute, navigateTo, notifications, conversations, user } = useApp();

  const unreadNotifsCount = notifications.filter((n) => !n.isRead).length;
  const unreadChatCount = conversations.reduce((acc, c) => acc + c.unreadCount, 0);
  const totalUnread = unreadNotifsCount + unreadChatCount;

  const isTabActive = (route: string) => {
    if (route === '/') return currentRoute === '/';
    return currentRoute.startsWith(route);
  };

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 md:hidden bg-white/95 dark:bg-slate-900/95 backdrop-blur-xl border-t border-slate-200/80 dark:border-slate-800 px-2 py-1.5 shadow-2xl transition-all">
      <div className="grid grid-cols-5 items-center justify-items-center text-center max-w-md mx-auto">

        {/* 1. 首页 */}
        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={() => navigateTo('/')}
          className={`flex flex-col items-center justify-center py-1 w-full cursor-pointer transition-colors ${
            isTabActive('/') && currentRoute === '/'
              ? 'text-indigo-600 dark:text-indigo-400 font-bold'
              : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
          }`}
        >
          <Sparkles className={`w-5 h-5 ${isTabActive('/') && currentRoute === '/' ? 'stroke-[2.5px]' : 'stroke-[1.75px]'}`} />
          <span className="text-[10px] mt-0.5 tracking-tight">首页</span>
        </motion.button>

        {/* 2. 发现 */}
        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={() => navigateTo('/discover')}
          className={`flex flex-col items-center justify-center py-1 w-full cursor-pointer transition-colors ${
            isTabActive('/discover')
              ? 'text-indigo-600 dark:text-indigo-400 font-bold'
              : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
          }`}
        >
          <Compass className={`w-5 h-5 ${isTabActive('/discover') ? 'stroke-[2.5px]' : 'stroke-[1.75px]'}`} />
          <span className="text-[10px] mt-0.5 tracking-tight">发现</span>
        </motion.button>

        {/* 3. 创作 (微信/小红书风格中央悬浮核心按钮) */}
        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.9 }}
          onClick={() => navigateTo('/editor/new')}
          className="flex flex-col items-center justify-center -mt-4 cursor-pointer focus:outline-none"
        >
          <div className="w-11 h-11 rounded-full bg-gradient-to-tr from-indigo-600 via-purple-600 to-pink-500 text-white flex items-center justify-center shadow-lg shadow-indigo-500/40 border-2 border-white dark:border-slate-900">
            <PenSquare className="w-5 h-5 stroke-[2.2px]" />
          </div>
          <span className="text-[10px] mt-0.5 font-bold text-indigo-600 dark:text-indigo-400">写文章</span>
        </motion.button>

        {/* 4. 动态 */}
        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={() => navigateTo('/moments')}
          className={`flex flex-col items-center justify-center py-1 w-full cursor-pointer transition-colors relative ${
            isTabActive('/moments')
              ? 'text-indigo-600 dark:text-indigo-400 font-bold'
              : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
          }`}
        >
          <div className="relative">
            <MessageSquare className={`w-5 h-5 ${isTabActive('/moments') ? 'stroke-[2.5px]' : 'stroke-[1.75px]'}`} />
            {totalUnread > 0 && (
              <span className="absolute -top-1 -right-1.5 min-w-[14px] h-[14px] bg-rose-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center px-0.5 ring-2 ring-white dark:ring-slate-900">
                {totalUnread > 99 ? '99+' : totalUnread}
              </span>
            )}
          </div>
          <span className="text-[10px] mt-0.5 tracking-tight">动态</span>
        </motion.button>

        {/* 5. 我的 */}
        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={() => navigateTo('/me')}
          className={`flex flex-col items-center justify-center py-1 w-full cursor-pointer transition-colors ${
            isTabActive('/me') || isTabActive('/space') || isTabActive('/profile')
              ? 'text-indigo-600 dark:text-indigo-400 font-bold'
              : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
          }`}
        >
          {user?.avatar ? (
            <img
              src={user.avatar}
              alt={user.displayName}
              className={`w-5 h-5 rounded-full object-cover border ${
                isTabActive('/me') || isTabActive('/space')
                  ? 'border-indigo-600 dark:border-indigo-400 ring-2 ring-indigo-500/20'
                  : 'border-slate-300 dark:border-slate-700'
              }`}
            />
          ) : (
            <User className={`w-5 h-5 ${isTabActive('/me') ? 'stroke-[2.5px]' : 'stroke-[1.75px]'}`} />
          )}
          <span className="text-[10px] mt-0.5 tracking-tight">我的</span>
        </motion.button>

      </div>
    </div>
  );
};
