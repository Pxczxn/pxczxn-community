/**
 * 星语社区 (pxczxn-community V2.1) - 顶部导航栏 (Navbar)
 * 支持 600-700px 视口紧凑排版、主题切换、快捷创作、通知、私信与用户菜单
 */

import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useApp } from '../../context/AppContext';
import { RoutePath } from '../../types';
import {
  Sparkles,
  Compass,
  FileText,
  MessageSquare,
  BookOpen,
  Users,
  Search,
  PenSquare,
  Bell,
  MessageCircle,
  Sun,
  Moon,
  Star,
  User,
  Settings,
  LayoutDashboard,
  ShieldAlert,
  LogOut,
  Maximize2,
  Minimize2,
  Menu,
  X,
  HelpCircle,
  Lightbulb,
  CheckCheck,
  ChevronRight,
  ChevronLeft,
  Send,
  Paperclip,
  Smile,
  ArrowRight,
  Loader2,
} from 'lucide-react';

export const Navbar: React.FC = () => {
  const {
    currentRoute,
    navigateTo,
    isCompactViewport,
    toggleCompactViewport,
    theme,
    setTheme,
    user,
    isLoggedIn,
    toggleLogin,
    notifications,
    conversations,
    chatMessages,
    selectConversation,
    sendChatMessage,
    markNotificationRead,
    markAllNotificationsRead,
    markAllConversationsRead,
    globalSearchQuery,
    setGlobalSearchQuery,
    triggerSearch,
    mobileMenuOpen,
    setMobileMenuOpen,
  } = useApp();

  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [themeDropdownOpen, setThemeDropdownOpen] = useState(false);
  const [searchInput, setSearchInput] = useState(globalSearchQuery);

  const headerRef = useRef<HTMLElement | null>(null);
  const [activePopover, setActivePopover] = useState<'notifications' | 'chat' | null>(null);
  const [notifFilter, setNotifFilter] = useState<'ALL' | 'INTERACTION' | 'SUBMISSION' | 'SYSTEM'>('ALL');
  const popoverTimerRef = useRef<NodeJS.Timeout | null>(null);
  const ignoreHoverRef = useRef<'notifications' | 'chat' | null>(null);

  // 点击外部收起所有下拉菜单与弹窗
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (headerRef.current && !headerRef.current.contains(event.target as Node)) {
        setActivePopover(null);
        setUserDropdownOpen(false);
        setThemeDropdownOpen(false);
        ignoreHoverRef.current = null;
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // 无限滚动状态与动态截取
  const [visibleNotifCount, setVisibleNotifCount] = useState(6);
  const [isNotifLoading, setIsNotifLoading] = useState(false);

  const [visibleChatCount, setVisibleChatCount] = useState(5);
  const [isChatLoading, setIsChatLoading] = useState(false);

  // 抖音式展开私信聊天状态
  const [selectedChatConv, setSelectedChatConv] = useState<any | null>(null);
  const [chatInputText, setChatInputText] = useState('');
  const [isChatInputFocused, setIsChatInputFocused] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (selectedChatConv) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [selectedChatConv, chatMessages]);

  const handleSendChatMessage = () => {
    if (!chatInputText.trim() || !selectedChatConv) return;
    sendChatMessage(selectedChatConv.peerUser.id, chatInputText.trim());
    setChatInputText('');
  };

  const handleMouseEnter = (type: 'notifications' | 'chat') => {
    if (popoverTimerRef.current) clearTimeout(popoverTimerRef.current);
    setActivePopover(type);
  };

  const handleMouseLeave = () => {
    // 若输入框处于 Focus 状态、有未发出的输入内容、或正处于与特定好友私信对话中，鼠标移出不自动收起，避免中断打字
    if (isChatInputFocused || chatInputText.trim().length > 0 || selectedChatConv) return;
    if (popoverTimerRef.current) clearTimeout(popoverTimerRef.current);
    popoverTimerRef.current = setTimeout(() => {
      setActivePopover(null);
    }, 250);
  };

  const unreadNotificationsCount = notifications.filter((n) => !n.isRead).length;
  const unreadChatCount = conversations.reduce((acc, c) => acc + c.unreadCount, 0);
  const selectedChatMessages = selectedChatConv
    ? chatMessages.filter((message) => (
      message.senderId === selectedChatConv.peerUser.id
      || message.receiverId === selectedChatConv.peerUser.id
    ))
    : [];

  const filteredNotifs = notifications.filter((n) => {
    if (notifFilter === 'ALL') return true;
    if (notifFilter === 'INTERACTION') return n.category === 'COMMENT' || n.category === 'INTERACTION';
    if (notifFilter === 'SUBMISSION') return n.category === 'SUBMISSION' || n.category === 'REVIEW';
    if (notifFilter === 'SYSTEM') return n.category === 'SYSTEM' || n.category === 'FOLLOW';
    return true;
  });

  const extendedNotifs = filteredNotifs;
  const extendedConversations = conversations;

  const handleNotifScroll = (e: React.UIEvent<HTMLDivElement>) => {
    e.stopPropagation();
    const { scrollTop, scrollHeight, clientHeight } = e.currentTarget;
    if (scrollTop + clientHeight >= scrollHeight - 25 && !isNotifLoading) {
      if (visibleNotifCount < extendedNotifs.length) {
        setIsNotifLoading(true);
        setTimeout(() => {
          setVisibleNotifCount((prev) => Math.min(prev + 4, extendedNotifs.length));
          setIsNotifLoading(false);
        }, 350);
      }
    }
  };

  const handleChatScroll = (e: React.UIEvent<HTMLDivElement>) => {
    e.stopPropagation();
    const { scrollTop, scrollHeight, clientHeight } = e.currentTarget;
    if (scrollTop + clientHeight >= scrollHeight - 25 && !isChatLoading) {
      if (visibleChatCount < extendedConversations.length) {
        setIsChatLoading(true);
        setTimeout(() => {
          setVisibleChatCount((prev) => Math.min(prev + 3, extendedConversations.length));
          setIsChatLoading(false);
        }, 350);
      }
    }
  };

  const mainNavItems: { label: string; route: RoutePath; icon: React.ReactNode }[] = [
    { label: '首页', route: '/', icon: <Sparkles className="w-4 h-4" /> },
    { label: '发现', route: '/discover', icon: <Compass className="w-4 h-4" /> },
    { label: '文章', route: '/articles', icon: <FileText className="w-4 h-4" /> },
    { label: '动态', route: '/moments', icon: <MessageSquare className="w-4 h-4" /> },
    { label: '系列', route: '/series', icon: <BookOpen className="w-4 h-4" /> },
    { label: '团队', route: '/teams', icon: <Users className="w-4 h-4" /> },
  ];

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      triggerSearch(searchInput.trim());
    }
  };

  return (
    <header ref={headerRef} className="sticky top-0 z-50 bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border-b border-slate-200/80 dark:border-slate-800 transition-colors">
      <div className="max-w-7xl mx-auto px-3 sm:px-4 lg:px-6">
        <div className={`flex items-center justify-between transition-all ${isCompactViewport ? 'h-12 sm:h-13' : 'h-15'}`}>

          {/* Brand Logo */}
          <div className="flex items-center space-x-3">
            <button
              onClick={() => navigateTo('/')}
              className="flex items-center space-x-2 group focus:outline-none"
            >
              <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-indigo-600 via-purple-600 to-pink-500 flex items-center justify-center text-white shadow-sm shadow-indigo-500/30 group-hover:scale-105 transition-transform">
                <Sparkles className="w-4 h-4" />
              </div>
              <div className="flex flex-col text-left">
                <span className="font-bold text-slate-900 dark:text-white text-base tracking-tight group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors flex items-center gap-1">
                  星语社区
                  <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 border border-indigo-200 dark:border-indigo-800 font-mono font-medium">
                    V2.1
                  </span>
                </span>
              </div>
            </button>

            {/* Desktop Navigation Links */}
            <nav className="hidden md:flex items-center space-x-1 ml-4 border-l border-slate-200 dark:border-slate-800 pl-4">
              {mainNavItems.map((item) => {
                const isActive = currentRoute === item.route;
                return (
                  <motion.button
                    key={item.route}
                    onClick={() => navigateTo(item.route)}
                    whileHover={{ scale: 1.04, y: -1 }}
                    whileTap={{ scale: 0.96 }}
                    className={`flex items-center space-x-1.5 px-2.5 py-1.5 rounded-md text-xs font-medium transition-all cursor-pointer ${
                      isActive
                        ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400 font-semibold shadow-xs'
                        : 'text-slate-600 dark:text-slate-300 hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-slate-100/70 dark:hover:bg-slate-800/60'
                    }`}
                  >
                    {item.icon}
                    <span>{item.label}</span>
                  </motion.button>
                );
              })}
            </nav>
          </div>

          {/* Search & Actions */}
          <div className="flex items-center space-x-2">
            {/* Quick Search Input */}
            <form onSubmit={handleSearchSubmit} className="hidden lg:flex items-center relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 pointer-events-none" />
              <input
                type="text"
                placeholder="搜索文章/动态/系列/团队..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="w-48 xl:w-56 pl-8 pr-3 py-1 text-xs bg-slate-100 dark:bg-slate-800/80 border border-transparent focus:border-indigo-500 rounded-full text-slate-800 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:bg-white dark:focus:bg-slate-900 transition-all"
              />
            </form>

            {/* Write Article Button (Shown on sm+ since mobile has central BottomNav button) */}
            <motion.button
              whileHover={{ scale: 1.03 }}
              whileTap={{ scale: 0.96 }}
              onClick={() => navigateTo('/editor/new')}
              className="hidden sm:flex items-center space-x-1 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white px-2.5 sm:px-3 py-1 sm:py-1.5 rounded-lg text-xs font-medium shadow-sm hover:shadow transition-all cursor-pointer"
            >
              <PenSquare className="w-3.5 h-3.5" />
              <span>写文章</span>
            </motion.button>

            {/* Notifications Icon with Hover Popover */}
            <div
              className="relative"
              onMouseEnter={() => handleMouseEnter('notifications')}
              onMouseLeave={handleMouseLeave}
            >
              <motion.button
                whileHover={{ scale: 1.08 }}
                whileTap={{ scale: 0.92 }}
                type="button"
                className={`relative p-1.5 rounded-lg transition-colors cursor-pointer ${
                  activePopover === 'notifications'
                    ? 'bg-indigo-50 text-indigo-600 dark:bg-slate-800 dark:text-indigo-400 font-semibold'
                    : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
                title="通知中心"
              >
                <Bell className="w-4 h-4" />
                {unreadNotificationsCount > 0 && (
                  <span className="absolute top-1 right-1 w-2 h-2 bg-rose-500 rounded-full ring-2 ring-white dark:ring-slate-900" />
                )}
              </motion.button>

              <AnimatePresence>
                {activePopover === 'notifications' && (
                  <motion.div
                    initial={{ opacity: 0, y: 8, scale: 0.96 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 6, scale: 0.96 }}
                    transition={{ duration: 0.16, ease: 'easeOut' }}
                    onMouseEnter={() => handleMouseEnter('notifications')}
                    onMouseLeave={handleMouseLeave}
                    className="absolute right-0 top-full mt-2 w-80 sm:w-96 bg-white/95 dark:bg-slate-900/95 border border-slate-200/90 dark:border-slate-800 rounded-2xl shadow-2xl backdrop-blur-xl z-50 text-xs overflow-hidden"
                  >
                  {/* Header */}
                  <div className="flex items-center justify-between px-3.5 py-2.5 bg-slate-50/80 dark:bg-slate-800/60 border-b border-slate-100 dark:border-slate-800">
                    <div className="flex items-center space-x-2">
                      <div className="flex items-center gap-1.5 font-bold text-slate-900 dark:text-white text-sm">
                        <Bell className="w-4 h-4 text-indigo-500" />
                        <span>互动与系统通知</span>
                      </div>
                      {unreadNotificationsCount > 0 ? (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500 text-white shadow-xs">
                          {unreadNotificationsCount}条未读
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800">
                          已全部阅读
                        </span>
                      )}
                    </div>

                    <div className="flex items-center space-x-2">
                      {unreadNotificationsCount > 0 && (
                        <button
                          onClick={() => markAllNotificationsRead()}
                          className="flex items-center gap-1 text-[11px] text-slate-500 hover:text-indigo-600 dark:text-slate-400 dark:hover:text-indigo-400 font-medium transition-colors"
                          title="一键已读"
                        >
                          <CheckCheck className="w-3.5 h-3.5 text-indigo-500" />
                          <span>已读</span>
                        </button>
                      )}
                      <button
                        onClick={() => {
                          setActivePopover(null);
                          navigateTo('/notifications');
                        }}
                        className="flex items-center gap-0.5 text-[11px] text-indigo-600 dark:text-indigo-400 hover:underline font-semibold"
                      >
                        <span>全部</span>
                        <ChevronRight className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>



                  {/* Notification Filter Pills */}
                  <div className="flex items-center gap-1 px-3 py-1.5 border-b border-slate-100 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50">
                    {[
                      { id: 'ALL', label: '全部' },
                      { id: 'INTERACTION', label: '互动/评论' },
                      { id: 'SUBMISSION', label: '团队/投稿' },
                      { id: 'SYSTEM', label: '系统/关注' },
                    ].map((tab) => (
                      <button
                        key={tab.id}
                        onClick={() => setNotifFilter(tab.id as any)}
                        className={`px-2 py-0.5 rounded-full text-[10px] sm:text-[11px] font-medium transition-all ${
                          notifFilter === tab.id
                            ? 'bg-indigo-600 text-white shadow-xs'
                            : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
                        }`}
                      >
                        {tab.label}
                      </button>
                    ))}
                  </div>

                  {/* Notification Item List with Independent Infinite Scroll */}
                  <div
                    onWheel={(e) => e.stopPropagation()}
                    onScroll={handleNotifScroll}
                    className="max-h-[360px] overflow-y-auto overscroll-contain divide-y divide-slate-100 dark:divide-slate-800/60 p-1.5"
                  >
                    {filteredNotifs.length === 0 ? (
                      <div className="py-8 text-center text-slate-400 text-xs">暂无相关通知</div>
                    ) : (
                      <>
                        {extendedNotifs.slice(0, visibleNotifCount).map((notif) => (
                          <div
                            key={notif.id}
                            onClick={() => {
                              markNotificationRead(notif.id);
                              setActivePopover(null);
                              if (notif.targetUrl) navigateTo(notif.targetUrl as any);
                            }}
                            className={`group p-2.5 rounded-xl transition-all cursor-pointer flex items-start space-x-2.5 ${
                              notif.isRead
                                ? 'hover:bg-slate-50 dark:hover:bg-slate-800/50 text-slate-600 dark:text-slate-300'
                                : 'bg-indigo-50/40 dark:bg-indigo-950/30 hover:bg-indigo-50/70 dark:hover:bg-indigo-950/50 text-slate-900 dark:text-slate-100'
                            }`}
                          >
                            <div className="relative shrink-0 mt-0.5">
                              {notif.sender ? (
                                <img
                                  src={notif.sender.avatar}
                                  alt={notif.sender.displayName}
                                  className="w-8 h-8 rounded-full object-cover ring-2 ring-white dark:ring-slate-800"
                                />
                              ) : (
                                <div className="w-8 h-8 rounded-full bg-indigo-100 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold">
                                  <Bell className="w-4 h-4" />
                                </div>
                              )}
                              {!notif.isRead && (
                                <span className="absolute -top-0.5 -right-0.5 w-2.5 h-2.5 bg-rose-500 rounded-full ring-2 ring-white dark:ring-slate-900" />
                              )}
                            </div>

                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between gap-1 mb-0.5">
                                <span className="font-semibold text-xs truncate text-slate-900 dark:text-white">
                                  {notif.title}
                                </span>
                                <span className="text-[10px] text-slate-400 shrink-0 font-mono">{notif.createdAt}</span>
                              </div>
                              <p className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-2 leading-relaxed">
                                {notif.content}
                              </p>
                            </div>
                          </div>
                        ))}

                        {/* Infinite Scroll Footer Indicator */}
                        <div className="py-2.5 text-center text-slate-400 dark:text-slate-500 border-t border-slate-100 dark:border-slate-800/40">
                          {visibleNotifCount < extendedNotifs.length ? (
                            <div className="flex items-center justify-center gap-1.5 text-[11px]">
                              {isNotifLoading ? (
                                <>
                                  <Loader2 className="w-3.5 h-3.5 animate-spin text-indigo-500" />
                                  <span className="text-indigo-600 dark:text-indigo-400 font-medium">加载更多通知中...</span>
                                </>
                              ) : (
                                <span>滑动加载更多历史通知</span>
                              )}
                            </div>
                          ) : (
                            <div className="text-[10px]">已为您呈现全部通知消息</div>
                          )}
                        </div>
                      </>
                    )}
                  </div>
                </motion.div>
              )}
              </AnimatePresence>
            </div>

            {/* Chat Icon with Hover Popover */}
            <div
              className="relative"
              onMouseEnter={() => handleMouseEnter('chat')}
              onMouseLeave={handleMouseLeave}
            >
              <motion.button
                whileHover={{ scale: 1.08 }}
                whileTap={{ scale: 0.92 }}
                type="button"
                className={`relative p-1.5 rounded-lg transition-colors cursor-pointer ${
                  activePopover === 'chat'
                    ? 'bg-purple-50 text-purple-600 dark:bg-slate-800 dark:text-purple-400 font-semibold'
                    : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
                title="私信消息"
              >
                <MessageCircle className="w-4 h-4" />
                {unreadChatCount > 0 && (
                  <span className="absolute top-1 right-1 w-2 h-2 bg-indigo-500 rounded-full ring-2 ring-white dark:ring-slate-900" />
                )}
              </motion.button>

              <AnimatePresence>
                {activePopover === 'chat' && (
                  <motion.div
                    initial={{ opacity: 0, y: 8, scale: 0.96 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 6, scale: 0.96 }}
                    transition={{ duration: 0.16, ease: 'easeOut' }}
                    onMouseEnter={() => handleMouseEnter('chat')}
                    onMouseLeave={handleMouseLeave}
                    className={`absolute right-0 top-full mt-2 bg-white dark:bg-slate-900 border border-slate-200/90 dark:border-slate-800 rounded-2xl shadow-2xl z-50 text-xs overflow-hidden transition-all ${
                      selectedChatConv ? 'w-[560px] sm:w-[620px]' : 'w-80 sm:w-96'
                    }`}
                  >
                  {/* Header */}
                  <div className="flex items-center justify-between px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800/80 border-b border-slate-100 dark:border-slate-800 shrink-0">
                    <div className="flex items-center space-x-2">
                      <div className="flex items-center gap-1.5 font-bold text-slate-900 dark:text-white text-sm">
                        <MessageCircle className="w-4 h-4 text-purple-500" />
                        <span>私信与团队频道</span>
                      </div>
                      {unreadChatCount > 0 ? (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-600 text-white shadow-xs">
                          {unreadChatCount}条未读
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400">
                          无未读
                        </span>
                      )}
                    </div>

                    <div className="flex items-center space-x-1.5">
                      {unreadChatCount > 0 && (
                        <motion.button
                          whileHover={{ scale: 1.05 }}
                          whileTap={{ scale: 0.95 }}
                          onClick={() => markAllConversationsRead()}
                          className="flex items-center gap-1 px-2 py-0.5 bg-purple-50 hover:bg-purple-100 dark:bg-purple-950/60 dark:hover:bg-purple-900/60 text-[11px] text-purple-700 dark:text-purple-300 font-semibold rounded-lg transition-colors mr-1 cursor-pointer border border-purple-200/60 dark:border-purple-800/60 shadow-xs"
                          title="一键已读所有会话"
                        >
                          <CheckCheck className="w-3.5 h-3.5 text-purple-500" />
                          <span>一键已读</span>
                        </motion.button>
                      )}
                      {selectedChatConv && (
                        <button
                          onClick={() => setSelectedChatConv(null)}
                          className="flex items-center space-x-1 px-2 py-1 text-[11px] font-semibold text-slate-600 dark:text-slate-300 bg-slate-200/60 dark:bg-slate-700/60 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors"
                          title="收起右侧会话"
                        >
                          <ChevronLeft className="w-3.5 h-3.5" />
                          <span>收起会话</span>
                        </button>
                      )}
                      <button
                        onClick={() => {
                          setActivePopover(null);
                          navigateTo('/chat');
                        }}
                        className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg transition-colors"
                        title="打开全屏独立消息中心"
                      >
                        <Maximize2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => {
                          setActivePopover(null);
                          setSelectedChatConv(null);
                        }}
                        className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg transition-colors"
                        title="关闭"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {/* Single Column or Douyin-Style Expanded Dual Column */}
                  {!selectedChatConv ? (
                    /* Conversations List Only */
                    <div
                      onWheel={(e) => e.stopPropagation()}
                      onScroll={handleChatScroll}
                      className="max-h-[360px] overflow-y-auto overscroll-contain divide-y divide-slate-100 dark:divide-slate-800/60 p-1.5 bg-white dark:bg-slate-900"
                    >
                      {conversations.length === 0 ? (
                        <div className="py-8 text-center text-slate-400 text-xs">暂无私信会话</div>
                      ) : (
                        <>
                          {extendedConversations.slice(0, visibleChatCount).map((conv) => (
                            <div
                              key={conv.id}
                              onClick={(e) => {
                                e.stopPropagation();
                                setSelectedChatConv(conv);
                              }}
                              className={`group p-2.5 rounded-xl transition-all cursor-pointer flex items-center space-x-3 ${
                                conv.unreadCount > 0
                                  ? 'bg-purple-50/40 dark:bg-purple-950/20 hover:bg-purple-50/70 dark:hover:bg-purple-950/40'
                                  : 'hover:bg-slate-50 dark:hover:bg-slate-800/50'
                              }`}
                            >
                              <div className="relative shrink-0">
                                <img
                                  src={conv.peerUser.avatar}
                                  alt={conv.peerUser.displayName}
                                  className="w-9 h-9 rounded-full object-cover ring-2 ring-white dark:ring-slate-800"
                                />
                                <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-emerald-500 rounded-full ring-2 ring-white dark:ring-slate-900" />
                              </div>

                              <div className="flex-1 min-w-0">
                                <div className="flex items-center justify-between gap-1 mb-0.5">
                                  <span className="font-bold text-xs truncate text-slate-900 dark:text-white group-hover:text-purple-600 dark:group-hover:text-purple-400 transition-colors">
                                    {conv.peerUser.displayName}
                                  </span>
                                  <span className="text-[10px] text-slate-400 shrink-0 font-mono">{conv.lastTime}</span>
                                </div>
                                <p className="text-[11px] text-slate-500 dark:text-slate-400 truncate">
                                  {conv.lastMessage}
                                </p>
                              </div>

                              {conv.unreadCount > 0 && (
                                <span className="px-1.5 py-0.5 text-[10px] font-bold bg-rose-500 text-white rounded-full shrink-0 shadow-xs">
                                  {conv.unreadCount}
                                </span>
                              )}
                            </div>
                          ))}

                          {/* Footer */}
                          <div className="py-2.5 text-center text-slate-400 dark:text-slate-500 border-t border-slate-100 dark:border-slate-800/40">
                            {visibleChatCount < extendedConversations.length ? (
                              <div className="flex items-center justify-center gap-1.5 text-[11px]">
                                {isChatLoading ? (
                                  <>
                                    <Loader2 className="w-3.5 h-3.5 animate-spin text-purple-500" />
                                    <span className="text-purple-600 dark:text-purple-400 font-medium">加载历史会话...</span>
                                  </>
                                ) : (
                                  <span>滑动加载更多历史会话</span>
                                )}
                              </div>
                            ) : (
                              <div className="text-[10px]">已为您呈现全部私信消息</div>
                            )}
                          </div>
                        </>
                      )}
                    </div>
                  ) : (
                    /* Douyin Style Dual Column: Left List + Right Embedded Chat Box */
                    <div className="grid grid-cols-12 h-[380px] divide-x divide-slate-100 dark:divide-slate-800 bg-white dark:bg-slate-900">
                      {/* Left Sidebar List */}
                      <div className="col-span-4 overflow-y-auto p-1.5 space-y-1 bg-slate-50/50 dark:bg-slate-900/40 shrink-0">
                        {conversations.map((conv) => (
                          <div
                            key={conv.id}
                            onClick={() => {
                              setSelectedChatConv(conv);
                              selectConversation(conv.peerUser.id);
                            }}
                            className={`p-2 rounded-xl transition-all cursor-pointer flex items-center space-x-2 ${
                              selectedChatConv.id === conv.id
                                ? 'bg-purple-100/80 dark:bg-purple-900/50 text-purple-900 dark:text-purple-100 font-semibold'
                                : 'hover:bg-slate-100 dark:hover:bg-slate-800/60'
                            }`}
                          >
                            <img
                              src={conv.peerUser.avatar}
                              alt={conv.peerUser.displayName}
                              className="w-7 h-7 rounded-full object-cover shrink-0"
                            />
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between">
                                <span className="font-bold text-[11px] truncate text-slate-900 dark:text-white">
                                  {conv.peerUser.displayName}
                                </span>
                              </div>
                              <p className="text-[10px] text-slate-400 truncate">{conv.lastMessage}</p>
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Right Embedded Chat Area */}
                      <div className="col-span-8 flex flex-col min-w-0 bg-white dark:bg-slate-900 overflow-hidden">
                        {/* Chat Header */}
                        <div className="p-2.5 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-white dark:bg-slate-900 shrink-0">
                          <div className="flex items-center space-x-2 min-w-0">
                            <img
                              src={selectedChatConv.peerUser.avatar}
                              alt={selectedChatConv.peerUser.displayName}
                              className="w-7 h-7 rounded-full object-cover shrink-0"
                            />
                            <div className="min-w-0">
                              <div className="font-bold text-xs text-slate-900 dark:text-white flex items-center gap-1 truncate">
                                <span className="truncate">{selectedChatConv.peerUser.displayName}</span>
                                <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full shrink-0" />
                              </div>
                              <div className="text-[9px] text-slate-400 truncate">在线 • 星语协同成员</div>
                            </div>
                          </div>
                        </div>

                        {/* Messages Stream */}
                        <div
                          onWheel={(e) => e.stopPropagation()}
                          className="flex-1 p-3 overflow-y-auto space-y-2.5 text-xs min-w-0 bg-slate-50/40 dark:bg-slate-950/30"
                        >
                          {selectedChatMessages.length === 0 ? (
                            <p className="py-8 text-center text-slate-400">暂无私信记录</p>
                          ) : selectedChatMessages.map((msg) => (
                            <div
                              key={msg.id}
                              className={`flex flex-col ${msg.isSelf ? 'items-end' : 'items-start'} max-w-full`}
                            >
                              <div
                                className={`max-w-[85%] px-3 py-2 rounded-2xl leading-relaxed text-[11px] break-words whitespace-pre-wrap overflow-hidden ${
                                  msg.isSelf
                                    ? 'bg-purple-600 text-white rounded-br-none shadow-xs'
                                    : 'bg-white dark:bg-slate-800 border border-slate-200/80 dark:border-slate-700 text-slate-800 dark:text-slate-100 rounded-bl-none shadow-xs'
                                }`}
                              >
                                {msg.text}
                              </div>
                              <span className="text-[9px] text-slate-400 mt-0.5 px-1 font-mono">{msg.timestamp}</span>
                            </div>
                          ))}
                          <div ref={messagesEndRef} />
                        </div>

                        {/* Chat Input Bar */}
                        <div className="p-2 border-t border-slate-100 dark:border-slate-800 bg-white dark:bg-slate-900 flex items-center gap-1.5 shrink-0">
                          <button type="button" className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 shrink-0">
                            <Smile className="w-4 h-4" />
                          </button>
                          <button type="button" className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 shrink-0">
                            <Paperclip className="w-4 h-4" />
                          </button>
                          <input
                            type="text"
                            value={chatInputText}
                            onFocus={() => setIsChatInputFocused(true)}
                            onBlur={() => setIsChatInputFocused(false)}
                            onChange={(e) => setChatInputText(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && handleSendChatMessage()}
                            placeholder="发送消息..."
                            className="flex-1 min-w-0 px-2.5 py-1.5 bg-slate-100 dark:bg-slate-800 text-slate-900 dark:text-white rounded-xl text-xs focus:outline-hidden focus:ring-1 focus:ring-purple-500"
                          />
                          <button
                            type="button"
                            onClick={handleSendChatMessage}
                            className="p-1.5 bg-purple-600 hover:bg-purple-700 text-white rounded-xl transition-colors shrink-0"
                            title="发送消息"
                          >
                            <Send className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    </div>
                  )}
                </motion.div>
              )}
              </AnimatePresence>
            </div>

            {/* Theme Picker Dropdown */}
            <div className="relative">
              <motion.button
                whileHover={{ scale: 1.08 }}
                whileTap={{ scale: 0.92 }}
                onClick={() => setThemeDropdownOpen(!themeDropdownOpen)}
                className="p-1.5 rounded-lg text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                title="切换主题 (浅色/深色/星空)"
              >
                {theme === 'light' && <Sun className="w-4 h-4 text-amber-500" />}
                {theme === 'dark' && <Moon className="w-4 h-4 text-indigo-400" />}
                {theme === 'starlight' && <Star className="w-4 h-4 text-purple-400 fill-purple-400/30" />}
              </motion.button>

              <AnimatePresence>
                {themeDropdownOpen && (
                  <motion.div
                    initial={{ opacity: 0, y: 8, scale: 0.95 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 6, scale: 0.95 }}
                    transition={{ duration: 0.15, ease: 'easeOut' }}
                    className="absolute right-0 mt-2 w-32 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-xl py-1 text-xs z-50"
                  >
                    <button
                      onClick={() => {
                        setTheme('light');
                        setThemeDropdownOpen(false);
                      }}
                      className={`w-full flex items-center space-x-2 px-3 py-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer ${
                        theme === 'light' ? 'text-amber-600 font-semibold' : 'text-slate-700 dark:text-slate-200'
                      }`}
                    >
                      <Sun className="w-3.5 h-3.5" />
                      <span>浅色模式</span>
                    </button>
                    <button
                      onClick={() => {
                        setTheme('dark');
                        setThemeDropdownOpen(false);
                      }}
                      className={`w-full flex items-center space-x-2 px-3 py-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer ${
                        theme === 'dark' ? 'text-indigo-400 font-semibold' : 'text-slate-700 dark:text-slate-200'
                      }`}
                    >
                      <Moon className="w-3.5 h-3.5" />
                      <span>深色模式</span>
                    </button>
                    <button
                      onClick={() => {
                        setTheme('starlight');
                        setThemeDropdownOpen(false);
                      }}
                      className={`w-full flex items-center space-x-2 px-3 py-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer ${
                        theme === 'starlight' ? 'text-purple-400 font-semibold' : 'text-slate-700 dark:text-slate-200'
                      }`}
                    >
                      <Star className="w-3.5 h-3.5" />
                      <span>星空主题</span>
                    </button>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* User Dropdown Profile Menu */}
            <div className="relative">
              {isLoggedIn && user ? (
                <motion.button
                  whileHover={{ scale: 1.06 }}
                  whileTap={{ scale: 0.94 }}
                  onClick={() => setUserDropdownOpen(!userDropdownOpen)}
                  className="flex items-center space-x-1.5 p-1 rounded-full border border-slate-200 dark:border-slate-700 hover:ring-2 hover:ring-indigo-500/30 transition-all cursor-pointer"
                >
                  <img src={user.avatar} alt={user.displayName} className="w-7 h-7 rounded-full object-cover" />
                </motion.button>
              ) : (
                <button
                  onClick={toggleLogin}
                  className="px-3 py-1 text-xs font-medium bg-indigo-50 dark:bg-indigo-950/70 text-indigo-600 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 rounded-lg hover:bg-indigo-100 transition-colors cursor-pointer"
                >
                  登录/注册
                </button>
              )}

              <AnimatePresence>
                {userDropdownOpen && user && (
                  <motion.div
                    initial={{ opacity: 0, y: 8, scale: 0.95 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 6, scale: 0.95 }}
                    transition={{ duration: 0.15, ease: 'easeOut' }}
                    className="absolute right-0 mt-2 w-56 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl py-1.5 text-xs z-50 divide-y divide-slate-100 dark:divide-slate-800"
                  >
                  <div className="px-3.5 py-2.5">
                    <p className="font-semibold text-slate-900 dark:text-white">{user.displayName}</p>
                    <p className="text-slate-400 text-[11px]">@{user.username}</p>
                  </div>

                  <div className="py-1">
                    <button
                      onClick={() => {
                        navigateTo('/me');
                        setUserDropdownOpen(false);
                      }}
                      className="w-full flex items-center space-x-2 px-3.5 py-1.5 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
                    >
                      <User className="w-3.5 h-3.5 text-indigo-500" />
                      <span>我的空间 (Asset Center)</span>
                    </button>
                    <button
                      onClick={() => {
                        navigateTo('/creator');
                        setUserDropdownOpen(false);
                      }}
                      className="w-full flex items-center space-x-2 px-3.5 py-1.5 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
                    >
                      <LayoutDashboard className="w-3.5 h-3.5 text-purple-500" />
                      <span>创作者中心 (Creator Studio)</span>
                    </button>
                    <button
                      onClick={() => {
                        navigateTo(`/profile/${user.username}`);
                        setUserDropdownOpen(false);
                      }}
                      className="w-full flex items-center space-x-2 px-3.5 py-1.5 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
                    >
                      <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                      <span>个人公开主页</span>
                    </button>
                  </div>

                  <div className="py-1">
                    <button
                      onClick={() => {
                        navigateTo('/governance');
                        setUserDropdownOpen(false);
                      }}
                      className="w-full flex items-center space-x-2 px-3.5 py-1.5 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
                    >
                      <ShieldAlert className="w-3.5 h-3.5 text-rose-500" />
                      <span>治理与申诉记录</span>
                    </button>
                    <button
                      onClick={() => {
                        navigateTo('/settings');
                        setUserDropdownOpen(false);
                      }}
                      className="w-full flex items-center space-x-2 px-3.5 py-1.5 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
                    >
                      <Settings className="w-3.5 h-3.5 text-slate-500" />
                      <span>设置中心</span>
                    </button>
                  </div>

                  <div className="py-1">
                    <button
                      onClick={() => {
                        toggleLogin();
                        setUserDropdownOpen(false);
                      }}
                      className="w-full flex items-center space-x-2 px-3.5 py-1.5 text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40"
                    >
                      <LogOut className="w-3.5 h-3.5" />
                      <span>退出登录</span>
                    </button>
                  </div>
                </motion.div>
              )}
              </AnimatePresence>
            </div>

            {/* Mobile Navigation Toggle Button */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="md:hidden p-1.5 rounded-lg text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Drawer Overlay */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2, ease: 'easeInOut' }}
            className="md:hidden border-t border-slate-200 dark:border-slate-800 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md px-4 py-3 space-y-3 overflow-hidden"
          >
            <form onSubmit={handleSearchSubmit} className="relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="全站搜索..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-100 dark:bg-slate-800 rounded-xl text-slate-800 dark:text-slate-100 focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
              />
            </form>

            <div className="grid grid-cols-3 gap-2">
              {mainNavItems.map((item) => (
                <motion.button
                  key={item.route}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => {
                    navigateTo(item.route);
                    setMobileMenuOpen(false);
                  }}
                  className={`flex flex-col items-center justify-center p-2.5 rounded-xl text-xs font-medium cursor-pointer transition-all ${
                    currentRoute === item.route
                      ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/80 dark:text-indigo-400 font-bold shadow-2xs'
                      : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                  }`}
                >
                  {item.icon}
                  <span className="mt-1">{item.label}</span>
                </motion.button>
              ))}
            </div>

            <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
              <span className="font-semibold text-indigo-600 dark:text-indigo-400">星语社区</span>
              <span className="font-mono text-[11px]">pxczxn V2.1</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
};
