/**
 * 星语社区 (pxczxn-community V2.1) - 设置中心 (Settings View)
 * 8大核心导航模块：账号资料 | 安全设置 | 通知矩阵 | 隐私权限 | 外观设计 | 使用偏好 | 屏蔽静音 | 数据与账号
 */

import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useApp } from '../../context/AppContext';
import { communityApi, publicFileUrl, type CommunityBlock } from '../../../lib/community-api';
import {
  User,
  Globe,
  Palette,
  Bell,
  Shield,
  Save,
  CheckCircle2,
  Moon,
  Sun,
  Star,
  Key,
  Eye,
  EyeOff,
  Lock,
  Copy,
  Check,
  CheckCircle,
  ExternalLink,
  Clock,
  HardDrive,
  SlidersHorizontal,
  Ban,
  Database,
  Smartphone,
  Laptop,
  LogOut,
  Mail,
  AlertTriangle,
  Download,
  Trash2,
  Image as ImageIcon,
  Sparkles,
  Search,
  Sliders,
  Type,
  Layout,
  VolumeX,
  FileText,
  X,
  Plus,
  Upload,
  RotateCcw,
  MapPin,
  Tv,
} from 'lucide-react';

type ManagedSession = {
  id: string;
  device: string;
  os: string;
  browser: string;
  ip: string;
  location: string;
  lastActive: string;
  current: boolean;
};

export const SettingsView: React.FC = () => {
  const { user, setUser, theme, setTheme, isCompactViewport, navigateTo } = useApp();
  const userId = user?.id;

  // 8 Main Navigation Tabs
  type TabType =
    | 'ACCOUNT'
    | 'SECURITY'
    | 'NOTIFICATIONS'
    | 'PRIVACY'
    | 'APPEARANCE'
    | 'PREFERENCES'
    | 'BLOCKS'
    | 'DATA';

  const [activeTab, setActiveTab] = useState<TabType>('ACCOUNT');

  // ================= 1. ACCOUNT & PROFILE STATE =================
  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [username, setUsername] = useState(user?.username || '');
  const [bio, setBio] = useState(user?.bio || '');
  const [blogName, setBlogName] = useState(user?.blogName || '');
  const [blogSummary, setBlogSummary] = useState('');
  const [blogSlug, setBlogSlug] = useState(user?.blogSlug || '');
  const [location, setLocation] = useState(user?.location || '');
  const [website, setWebsite] = useState(user?.website || '');
  const [githubUrl, setGithubUrl] = useState('');
  const [bilibiliUrl, setBilibiliUrl] = useState('');
  const [avatar, setAvatar] = useState(user?.avatar || '');
  const [coverImage, setCoverImage] = useState('');

  useEffect(() => {
    if (!userId) return;
    setDisplayName(user?.displayName || '');
    setUsername(user?.username || '');
    setBio(user?.bio || '');
    setBlogName(user?.blogName || '');
    setBlogSlug(user?.blogSlug || '');
    setLocation(user?.location || '');
    setWebsite(user?.website || '');
    setAvatar(user?.avatar || '');

    let cancelled = false;
    void communityApi.myBlog().then((blog) => {
      if (cancelled) return;
      setBlogName(blog.name || '');
      setBlogSlug(blog.slug || '');
      setBlogSummary(blog.summary || '');
      setAvatar(publicFileUrl(blog.avatarFileId) || user?.avatar || '');
      setCoverImage(publicFileUrl(blog.backgroundFileId) || '');
    }).catch(() => undefined);
    return () => { cancelled = true; };
  }, [userId, user?.avatar, user?.bio, user?.blogName, user?.blogSlug, user?.displayName, user?.location, user?.username, user?.website]);

  const persistBlogImage = async (file: File, field: 'avatarFileId' | 'backgroundFileId') => {
    const uploaded = await communityApi.uploadFile(file);
    const blog = await communityApi.updateMyBlog({ [field]: uploaded.fileId });
    const imageUrl = publicFileUrl(field === 'avatarFileId' ? blog.avatarFileId : blog.backgroundFileId);
    if (field === 'avatarFileId') setAvatar(imageUrl || '');
    else setCoverImage(imageUrl || '');
  };

  const clearBlogImage = async (field: 'avatarFileId' | 'backgroundFileId') => {
    await communityApi.updateMyBlog(field === 'avatarFileId' ? { clearAvatar: true } : { clearBackground: true });
    if (field === 'avatarFileId') setAvatar('');
    else setCoverImage('');
  };

  // 本地文件上传处理函数
  const handleAvatarFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        alert('头像图片文件大小不能超过 5MB');
        return;
      }
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setAvatar(event.target.result as string);
        }
      };
      reader.readAsDataURL(file);
      void persistBlogImage(file, 'avatarFileId').catch(() => undefined);
    }
  };

  const handleAvatarDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith('image/')) {
      if (file.size > 5 * 1024 * 1024) {
        alert('头像图片文件大小不能超过 5MB');
        return;
      }
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setAvatar(event.target.result as string);
        }
      };
      reader.readAsDataURL(file);
      void persistBlogImage(file, 'avatarFileId').catch(() => undefined);
    }
  };

  const handleCoverFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 8 * 1024 * 1024) {
        alert('背景图文件大小不能超过 8MB');
        return;
      }
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setCoverImage(event.target.result as string);
        }
      };
      reader.readAsDataURL(file);
      void persistBlogImage(file, 'backgroundFileId').catch(() => undefined);
    }
  };

  const handleCoverDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith('image/')) {
      if (file.size > 8 * 1024 * 1024) {
        alert('背景图文件大小不能超过 8MB');
        return;
      }
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setCoverImage(event.target.result as string);
        }
      };
      reader.readAsDataURL(file);
      void persistBlogImage(file, 'backgroundFileId').catch(() => undefined);
    }
  };

  // ================= 2. SECURITY STATE =================
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [apiKey, setApiKey] = useState('sk-••••••••••••••••••••');
  const [showKey, setShowKey] = useState(false);
  const [copiedKey, setCopiedKey] = useState(false);
  const [enable2FA, setEnable2FA] = useState(false);

  const [sessions, setSessions] = useState<ManagedSession[]>([]);

  // ================= 3. NOTIFICATION MATRIX STATE =================
  const [dndMode, setDndMode] = useState(true);
  const [emailFrequency, setEmailFrequency] = useState<'REALTIME' | 'DAILY' | 'WEEKLY' | 'OFF'>('WEEKLY');

  // Matrix rows and initial toggles
  const [notifMatrix, setNotifMatrix] = useState<{ [key: string]: { site: boolean; email: boolean; push: boolean } }>({
    likes: { site: true, email: false, push: true },
    comments: { site: true, email: true, push: true },
    replies: { site: true, email: true, push: true },
    mentions: { site: true, email: true, push: true },
    followers: { site: true, email: false, push: true },
    messages: { site: true, email: true, push: true },
    coauthor: { site: true, email: true, push: true },
    audit: { site: true, email: true, push: true },
    updates: { site: true, email: false, push: false },
    security: { site: true, email: true, push: true },
  });

  const toggleNotifChannel = (rowKey: string, channel: 'site' | 'email' | 'push') => {
    setNotifMatrix((prev) => ({
      ...prev,
      [rowKey]: {
        ...prev[rowKey],
        [channel]: !prev[rowKey][channel],
      },
    }));
  };

  // ================= 4. PRIVACY STATE =================
  const [likesPrivacy, setLikesPrivacy] = useState<'PUBLIC' | 'FOLLOWERS' | 'MUTUAL' | 'PRIVATE'>('MUTUAL');
  const [bookmarksPrivacy, setBookmarksPrivacy] = useState<'PUBLIC' | 'FOLLOWERS' | 'MUTUAL' | 'PRIVATE'>('PRIVATE');
  const [followingPrivacy, setFollowingPrivacy] = useState<'PUBLIC' | 'FOLLOWERS' | 'MUTUAL' | 'PRIVATE'>('PUBLIC');
  const [readingStatusPrivacy, setReadingStatusPrivacy] = useState<'PUBLIC' | 'FOLLOWERS' | 'MUTUAL' | 'PRIVATE'>('MUTUAL');

  const [whoCanMessage, setWhoCanMessage] = useState<'ALL' | 'MUTUAL' | 'NONE'>('MUTUAL');
  const [whoCanMention, setWhoCanMention] = useState<'ALL' | 'FOLLOWING' | 'NONE'>('ALL');
  const [allowSearchIndex, setAllowSearchIndex] = useState(true);
  const [allowRecommendation, setAllowRecommendation] = useState(true);

  useEffect(() => {
    if (!userId) return;
    let cancelled = false;
    void communityApi.likeListPrivacy()
      .then(({ visibility }) => {
        if (!cancelled) {
          if (visibility === 'FOLLOWERS_ONLY') setLikesPrivacy('FOLLOWERS');
          else if (visibility === 'MUTUAL_ONLY') setLikesPrivacy('MUTUAL');
          else if (visibility === 'PUBLIC' || visibility === 'PRIVATE') setLikesPrivacy(visibility);
        }
      })
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, [userId]);

  const updateLikesPrivacy = (next: 'PUBLIC' | 'FOLLOWERS' | 'MUTUAL' | 'PRIVATE') => {
    const previous = likesPrivacy;
    setLikesPrivacy(next);
    const visibility = next === 'FOLLOWERS'
      ? 'FOLLOWERS_ONLY'
      : next === 'MUTUAL'
        ? 'MUTUAL_ONLY'
        : next;
    void communityApi.updateLikeListPrivacy(visibility)
      .catch(() => setLikesPrivacy(previous));
  };

  // ================= 5. APPEARANCE STATE =================
  const [fontSize, setFontSize] = useState<'SMALL' | 'MEDIUM' | 'LARGE'>('MEDIUM');
  const [uiDensity, setUiDensity] = useState<'COMFORTABLE' | 'STANDARD' | 'COMPACT'>('STANDARD');
  const [codeTheme, setCodeTheme] = useState<'GITHUB' | 'ONE_DARK' | 'DRACULA'>('ONE_DARK');
  const [reduceMotion, setReduceMotion] = useState(false);

  // ================= 6. USAGE PREFERENCES STATE =================
  const [defaultHomeFeed, setDefaultHomeFeed] = useState<'RECOMMEND' | 'FOLLOWING' | 'RELATED'>('RECOMMEND');
  const [defaultArticleSort, setDefaultArticleSort] = useState<'HOT' | 'LATEST' | 'COMMENT'>('HOT');
  const [defaultPostVisibility, setDefaultPostVisibility] = useState<'PUBLIC' | 'PRIVATE' | 'TEAM_ONLY'>('PUBLIC');
  const [defaultCommentScope, setDefaultCommentScope] = useState<'EVERYONE' | 'MEMBERS_ONLY' | 'CLOSED'>('EVERYONE');
  const [allowRepost, setAllowRepost] = useState(true);

  // ================= 7. BLOCKS & MUTING STATE =================
  const [blockCategory, setBlockCategory] = useState<'USER' | 'BLOG' | 'TAG' | 'KEYWORD'>('USER');
  const [keywords, setKeywords] = useState<string[]>(['加密货币', '低俗垃圾', '纯营销']);
  const [newKeywordInput, setNewKeywordInput] = useState('');

  const [blocks, setBlocks] = useState<CommunityBlock[]>([]);

  useEffect(() => {
    let cancelled = false;
    communityApi.myBlocks()
      .then((items) => {
        if (!cancelled) setBlocks(items);
      })
      .catch(() => {
        if (!cancelled) setBlocks([]);
      });
    return () => { cancelled = true; };
  }, []);

  const blockedUsers = blocks.filter((block) => block.targetType === 'USER').map((block) => ({
    id: block.id,
    targetId: block.targetId,
    name: `用户 ${block.targetId.slice(0, 8)}`,
    handle: `@${block.targetId}`,
    avatar: user?.avatar || '',
    date: block.createdAt,
  }));

  // ================= 8. DATA & ACCOUNT LIFECYCLE =================
  const [exportingData, setExportingData] = useState(false);
  const [deactivateModalOpen, setDeactivateModalOpen] = useState(false);

  // Feedback State
  const [isSaved, setIsSaved] = useState(false);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (user) {
      const [account, blog] = await Promise.all([
        communityApi.updateProfile({ displayName, bio }),
        communityApi.updateMyBlog({ name: blogName, summary: blogSummary }),
        communityApi.updateBlogSettings({
          commentScope: {
            EVERYONE: 'ALL_LOGGED_IN',
            MEMBERS_ONLY: 'FOLLOWERS_ONLY',
            CLOSED: 'DISABLED',
          }[defaultCommentScope],
          defaultVisibility: defaultPostVisibility === 'TEAM_ONLY' ? 'PRIVATE' : defaultPostVisibility,
          allowRepost: allowRepost ? 'ALLOW' : 'DISALLOW',
          themeKey: theme === 'starlight' ? 'starry' : theme,
        }),
      ]);
      setUser({
        ...user,
        displayName: account.displayName || user.displayName,
        bio: account.bio || '',
        location,
        website,
        avatar,
        blogName: blog.name,
        blogSlug: blog.slug,
      });
    }
    setIsSaved(true);
    setTimeout(() => setIsSaved(false), 3000);
  };

  const handleCopyKey = () => {
    navigator.clipboard.writeText(apiKey);
    setCopiedKey(true);
    setTimeout(() => setCopiedKey(false), 2000);
  };

  const handlePasswordChange = async () => {
    if (!currentPassword || !newPassword || newPassword !== confirmPassword) return;
    try {
      await communityApi.changePassword(currentPassword, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch { /* Keep the inputs available when the password change is rejected. */ }
  };

  const handleAddKeyword = () => {
    if (newKeywordInput.trim() && !keywords.includes(newKeywordInput.trim())) {
      setKeywords([...keywords, newKeywordInput.trim()]);
      setNewKeywordInput('');
    }
  };

  const handleRemoveKeyword = (word: string) => {
    setKeywords(keywords.filter((k) => k !== word));
  };

  const handleUnblockUser = async (targetId: string) => {
    try {
      await communityApi.removeBlock('USER', targetId);
      setBlocks((current) => current.filter((block) => !(block.targetType === 'USER' && block.targetId === targetId)));
    } catch { /* Keep the block visible when the server rejects the change. */ }
  };

  const handleLogoutSession = (id: string) => {
    setSessions(sessions.filter((s) => s.id !== id));
  };

  // Nav item list definition
  const navTabs: { id: TabType; label: string; icon: React.ReactNode; desc: string }[] = [
    { id: 'ACCOUNT', label: '账号与资料', icon: <User className="w-4 h-4" />, desc: '显示名称、Blog与社交外链' },
    { id: 'SECURITY', label: '安全设置', icon: <Shield className="w-4 h-4" />, desc: '密码、2FA与设备会话' },
    { id: 'NOTIFICATIONS', label: '通知矩阵', icon: <Bell className="w-4 h-4" />, desc: '站内、邮件与 Push 规则' },
    { id: 'PRIVACY', label: '隐私权限', icon: <Lock className="w-4 h-4" />, desc: '喜欢、动态与互动范围' },
    { id: 'APPEARANCE', label: '外观设计', icon: <Palette className="w-4 h-4" />, desc: '浅色/深色/星空与字体密度' },
    { id: 'PREFERENCES', label: '使用偏好', icon: <SlidersHorizontal className="w-4 h-4" />, desc: '默认 Feed、排序与发文权限' },
    { id: 'BLOCKS', label: '屏蔽与静音', icon: <Ban className="w-4 h-4" />, desc: '黑名单用户、Blog 与关键词' },
    { id: 'DATA', label: '数据与账号', icon: <Database className="w-4 h-4" />, desc: '数据导出与账号生命周期' },
  ];

  return (
    <div className={`max-w-6xl mx-auto px-3 sm:px-4 transition-all ${isCompactViewport ? 'py-3' : 'py-5'}`}>

      {/* Top Banner Header */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs mb-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center space-x-3.5">
          <img
            src={avatar}
            alt={displayName}
            className="w-13 h-13 rounded-xl object-cover border border-slate-200 dark:border-slate-700 shrink-0"
          />
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white tracking-tight">{displayName}</h1>
              <span className="px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-[11px] font-mono text-slate-500 dark:text-slate-400">
                @{username}
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 line-clamp-1">{bio}</p>
          </div>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
          <button
            type="button"
            onClick={() => navigateTo('/me')}
            className="px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 text-xs font-semibold transition-all flex items-center gap-1.5"
          >
            <span>公开主页预览</span>
            <ExternalLink className="w-3.5 h-3.5 text-slate-400" />
          </button>
          {isSaved && (
            <span className="flex items-center space-x-1 px-2.5 py-1.5 bg-emerald-50 dark:bg-emerald-950/80 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800 text-xs font-semibold rounded-xl animate-in fade-in">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>设置已更新</span>
            </span>
          )}
        </div>
      </div>

      {/* Main Grid Layout: Left Nav (3 cols) + Right Content Panel (9 cols) */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-4">

        {/* Left Navigation Sidebar */}
        <div className="md:col-span-3 space-y-3 md:sticky md:top-20 self-start z-10">
          <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-1.5 shadow-xs flex md:flex-col overflow-x-auto no-scrollbar gap-1 md:gap-1">
            <p className="hidden md:block text-[10px] font-bold text-slate-400 dark:text-slate-500 px-3 pt-2 pb-1 uppercase tracking-wider">
              设置中心导航
            </p>
            {navTabs.map((tab) => {
              const isActive = activeTab === tab.id;
              return (
                <motion.button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  whileHover={{ x: isActive ? 0 : 2 }}
                  whileTap={{ scale: 0.98 }}
                  className={`shrink-0 md:shrink md:w-full flex items-center space-x-2.5 px-3 py-2.5 rounded-xl text-left relative transition-all cursor-pointer ${
                    isActive
                      ? 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900 font-semibold shadow-xs'
                      : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800/60'
                  }`}
                >
                  <span className={`relative z-10 shrink-0 ${isActive ? 'text-white dark:text-slate-900' : 'text-slate-400'}`}>
                    {tab.icon}
                  </span>
                  <div className="min-w-0 relative z-10">
                    <p className="text-xs leading-none font-medium whitespace-nowrap">{tab.label}</p>
                    <p className={`hidden sm:block text-[10px] mt-1 truncate ${isActive ? 'text-slate-300 dark:text-slate-600' : 'text-slate-400 dark:text-slate-500'}`}>
                      {tab.desc}
                    </p>
                  </div>
                </motion.button>
              );
            })}
          </div>

          <div className="px-3 py-2 text-[11px] text-slate-400 dark:text-slate-500 flex items-center justify-between bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-xl">
            <span className="flex items-center gap-1.5">
              <HardDrive className="w-3.5 h-3.5 text-slate-400" />
              <span>离线快照</span>
            </span>
            <span className="font-mono text-[10px] font-semibold text-slate-600 dark:text-slate-400">1.2 MB / 10 MB</span>
          </div>
        </div>

        {/* Content Panel */}
        <div className="md:col-span-9 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl p-4 sm:p-5 shadow-xs text-xs overflow-hidden">
          <form onSubmit={handleSave} className="space-y-6">
            <AnimatePresence mode="wait">
              <motion.div
                key={activeTab}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -12 }}
                transition={{ duration: 0.18, ease: "easeOut" }}
                className="space-y-6"
              >
            {activeTab === 'ACCOUNT' && (
              <div className="space-y-4 animate-in fade-in">
                <div className="pb-2.5 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
                  <div>
                    <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                      <User className="w-4 h-4 text-indigo-500" />
                      <span>账号与资料设置</span>
                    </h2>
                    <p className="text-[11px] text-slate-400 mt-0.5">管理个人基本信息、博客公开身份与多社交平台外链</p>
                  </div>
                </div>

                {/* Avatar & Cover Photo Row (仅本地上传模式) */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {/* 个人头像上传 */}
                  <div className="p-3.5 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-2.5">
                    <div className="flex items-center justify-between">
                      <label className="font-semibold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                        <ImageIcon className="w-3.5 h-3.5 text-indigo-500" />
                        <span>个人头像</span>
                      </label>
                      <span className="text-[10px] text-indigo-600 dark:text-indigo-400 font-medium">仅支持本地上传</span>
                    </div>

                    <div
                      onDragOver={(e) => e.preventDefault()}
                      onDrop={handleAvatarDrop}
                      className="p-3 bg-white dark:bg-slate-900 border border-dashed border-slate-200 dark:border-slate-700 rounded-xl flex items-center gap-3 transition-colors hover:border-indigo-400 dark:hover:border-indigo-500"
                    >
                      <img src={avatar} alt="Avatar" className="w-12 h-12 rounded-xl object-cover border border-slate-200 dark:border-slate-700 shrink-0 shadow-xs" />
                      <div className="flex-1 min-w-0 space-y-1.5">
                        <div className="flex flex-wrap items-center gap-2">
                          <label className="cursor-pointer px-2.5 py-1 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium text-[11px] flex items-center gap-1.5 transition-colors shadow-xs">
                            <Upload className="w-3 h-3" />
                            <span>选择本地图片</span>
                            <input
                              type="file"
                              accept="image/*"
                              onChange={handleAvatarFileUpload}
                              className="hidden"
                            />
                          </label>
                          <button
                            type="button"
                            onClick={() => void clearBlogImage('avatarFileId').catch(() => undefined)}
                            className="px-2 py-1 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 font-medium rounded-lg text-[10px] transition-colors flex items-center gap-1"
                          >
                            <RotateCcw className="w-3 h-3" />
                            <span>恢复默认</span>
                          </button>
                        </div>
                        <p className="text-[10px] text-slate-400">点击或拖拽本地图片文件到此处（&lt;5MB）</p>
                      </div>
                    </div>
                  </div>

                  {/* 博客主页背景图上传 */}
                  <div className="p-3.5 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-2.5">
                    <div className="flex items-center justify-between">
                      <label className="font-semibold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                        <ImageIcon className="w-3.5 h-3.5 text-indigo-500" />
                        <span>博客主页背景图</span>
                      </label>
                      <span className="text-[10px] text-indigo-600 dark:text-indigo-400 font-medium">仅支持本地上传</span>
                    </div>

                    <div
                      onDragOver={(e) => e.preventDefault()}
                      onDrop={handleCoverDrop}
                      className="p-3 bg-white dark:bg-slate-900 border border-dashed border-slate-200 dark:border-slate-700 rounded-xl flex items-center gap-3 transition-colors hover:border-indigo-400 dark:hover:border-indigo-500"
                    >
                      <img src={coverImage} alt="Cover" className="w-16 h-12 rounded-xl object-cover border border-slate-200 dark:border-slate-700 shrink-0 shadow-xs" />
                      <div className="flex-1 min-w-0 space-y-1.5">
                        <div className="flex flex-wrap items-center gap-2">
                          <label className="cursor-pointer px-2.5 py-1 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium text-[11px] flex items-center gap-1.5 transition-colors shadow-xs">
                            <Upload className="w-3 h-3" />
                            <span>选择本地图片</span>
                            <input
                              type="file"
                              accept="image/*"
                              onChange={handleCoverFileUpload}
                              className="hidden"
                            />
                          </label>
                          <button
                            type="button"
                            onClick={() => void clearBlogImage('backgroundFileId').catch(() => undefined)}
                            className="px-2 py-1 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 font-medium rounded-lg text-[10px] transition-colors flex items-center gap-1"
                          >
                            <RotateCcw className="w-3 h-3" />
                            <span>恢复默认</span>
                          </button>
                        </div>
                        <p className="text-[10px] text-slate-400">点击或拖拽本地背景图到此处（建议1200×400, &lt;8MB）</p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Display Name & Locked Username */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                  <div className="space-y-1">
                    <label className="block font-semibold text-slate-700 dark:text-slate-300">显示昵称 (Display Name)</label>
                    <input
                      type="text"
                      value={displayName}
                      onChange={(e) => setDisplayName(e.target.value)}
                      className="w-full px-3 py-2 bg-slate-50/50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-xl font-medium focus:bg-white"
                    />
                  </div>

                  <div className="space-y-1">
                    <div className="flex items-center justify-between">
                      <label className="block font-semibold text-slate-700 dark:text-slate-300">唯一 Handle ID (@)</label>
                      <span className="text-[10px] text-slate-400 font-mono flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        <span>30 天限制 1 次</span>
                      </span>
                    </div>
                    <div className="relative">
                      <input
                        type="text"
                        value={username}
                        disabled
                        readOnly
                        className="w-full px-3 py-2 pr-24 bg-slate-100/80 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700 rounded-xl font-mono text-slate-500 dark:text-slate-400 cursor-not-allowed select-none"
                      />
                      <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1 text-[10px] text-slate-400 dark:text-slate-500 bg-slate-200/60 dark:bg-slate-700/60 px-2 py-0.5 rounded font-mono">
                        <Lock className="w-3 h-3" />
                        <span>21 天后可改</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Blog Name & Summary */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                  <div className="space-y-1">
                    <label className="block font-semibold text-slate-700 dark:text-slate-300">博客名称 (Blog Name)</label>
                    <input
                      type="text"
                      value={blogName}
                      onChange={(e) => setBlogName(e.target.value)}
                      className="w-full px-3 py-2 bg-slate-50/50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-xl font-medium"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="block font-semibold text-slate-700 dark:text-slate-300">专栏路由标识 (Blog Slug)</label>
                    <div className="flex items-center">
                      <span className="px-2.5 py-2 bg-slate-100 dark:bg-slate-800 border border-r-0 border-slate-200 dark:border-slate-700 rounded-l-xl text-slate-400 font-mono text-[11px]">
                        star.community/blogs/
                      </span>
                      <input
                        type="text"
                        value={blogSlug}
                        onChange={(e) => setBlogSlug(e.target.value)}
                        className="flex-1 px-3 py-2 bg-slate-50/50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-r-xl font-mono text-xs font-semibold"
                      />
                    </div>
                  </div>
                </div>

                {/* User Bio */}
                <div className="space-y-1">
                  <label className="block font-semibold text-slate-700 dark:text-slate-300">个人简介 (Bio)</label>
                  <textarea
                    rows={2}
                    value={bio}
                    onChange={(e) => setBio(e.target.value)}
                    className="w-full p-2.5 bg-slate-50/50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-xl leading-relaxed"
                  />
                </div>

                {/* Blog Summary */}
                <div className="space-y-1">
                  <label className="block font-semibold text-slate-700 dark:text-slate-300">博客专栏描述 (Blog Summary)</label>
                  <textarea
                    rows={2}
                    value={blogSummary}
                    onChange={(e) => setBlogSummary(e.target.value)}
                    className="w-full p-2.5 bg-slate-50/50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 rounded-xl leading-relaxed"
                  />
                </div>

                {/* 社交与联系方式 (Social & Links) - 紧凑四列/双列布局 */}
                <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-2">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                      <Globe className="w-3.5 h-3.5 text-indigo-500" />
                      <span>社交与联系方式</span>
                    </label>
                    <span className="text-[10px] text-slate-400">展示在个人主页与博客名片</span>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2">
                    {/* 常驻城市 */}
                    <div className="space-y-1">
                      <span className="text-[10px] font-semibold text-slate-500 dark:text-slate-400">常驻城市 / 地区</span>
                      <div className="relative">
                        <div className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400">
                          <MapPin className="w-3.5 h-3.5" />
                        </div>
                        <input
                          type="text"
                          value={location}
                          onChange={(e) => setLocation(e.target.value)}
                          placeholder="例如：杭州 / 远程"
                          className="w-full pl-8 pr-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-medium focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
                        />
                      </div>
                    </div>

                    {/* 个人主页 */}
                    <div className="space-y-1">
                      <span className="text-[10px] font-semibold text-slate-500 dark:text-slate-400">个人主页 (URL)</span>
                      <div className="relative">
                        <div className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400">
                          <Globe className="w-3.5 h-3.5" />
                        </div>
                        <input
                          type="text"
                          value={website}
                          onChange={(e) => setWebsite(e.target.value)}
                          placeholder="https://..."
                          className="w-full pl-8 pr-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-mono focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
                        />
                      </div>
                    </div>

                    {/* GitHub */}
                    <div className="space-y-1">
                      <span className="text-[10px] font-semibold text-slate-500 dark:text-slate-400">GitHub 链接</span>
                      <div className="relative">
                        <div className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400">
                          <ExternalLink className="w-3.5 h-3.5" />
                        </div>
                        <input
                          type="text"
                          value={githubUrl}
                          onChange={(e) => setGithubUrl(e.target.value)}
                          placeholder="https://github.com/..."
                          className="w-full pl-8 pr-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-mono focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
                        />
                      </div>
                    </div>

                    {/* Bilibili */}
                    <div className="space-y-1">
                      <span className="text-[10px] font-semibold text-slate-500 dark:text-slate-400">Bilibili 主页</span>
                      <div className="relative">
                        <div className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400">
                          <Tv className="w-3.5 h-3.5" />
                        </div>
                        <input
                          type="text"
                          value={bilibiliUrl}
                          onChange={(e) => setBilibiliUrl(e.target.value)}
                          placeholder="https://space.bilibili.com/..."
                          className="w-full pl-8 pr-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-mono focus:outline-hidden focus:ring-1 focus:ring-indigo-500"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* ================= 2. SECURITY SETTINGS ================= */}
            {activeTab === 'SECURITY' && (
              <div className="space-y-4 animate-in fade-in">
                <div className="pb-2.5 border-b border-slate-100 dark:border-slate-800">
                  <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <Shield className="w-4 h-4 text-emerald-500" />
                    <span>账号安全与鉴权中心</span>
                  </h2>
                  <p className="text-[11px] text-slate-400 mt-0.5">管理登录密码、设备会话、邮箱验证及 OpenAPI 密钥</p>
                </div>

                {/* Account Status Card */}
                <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between">
                  <div className="space-y-0.5">
                    <p className="font-semibold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                      <Mail className="w-3.5 h-3.5 text-slate-500" />
                      <span>绑定邮箱：pxczxn@gmail.com</span>
                    </p>
                    <p className="text-[10px] text-slate-400">已于 2026-03-12 完成验证，可用于找回密码与安全提醒</p>
                  </div>
                  <span className="px-2.5 py-1 bg-emerald-50 dark:bg-emerald-950/80 text-emerald-600 dark:text-emerald-400 font-semibold rounded-lg text-[10px] border border-emerald-200 dark:border-emerald-800 shrink-0">
                    已认证
                  </span>
                </div>

                {/* Change Password Section */}
                <div className="p-3.5 bg-slate-50/70 dark:bg-slate-800/40 border border-slate-200/60 dark:border-slate-800 rounded-xl space-y-3">
                  <p className="font-bold text-slate-800 dark:text-slate-200">修改登录密码</p>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div className="space-y-1">
                      <label className="block text-[11px] text-slate-600 dark:text-slate-400">当前旧密码</label>
                      <input
                        type="password"
                        placeholder="••••••••"
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        className="w-full px-3 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="block text-[11px] text-slate-600 dark:text-slate-400">新密码</label>
                      <input
                        type="password"
                        placeholder="至少 8 位包含字母与数字"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        className="w-full px-3 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="block text-[11px] text-slate-600 dark:text-slate-400">确认新密码</label>
                      <input
                        type="password"
                        placeholder="重复输入新密码"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        className="w-full px-3 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs"
                      />
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={handlePasswordChange}
                    disabled={!currentPassword || !newPassword || newPassword !== confirmPassword}
                    className="px-3 py-1.5 bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900 rounded-lg text-xs font-semibold disabled:opacity-50"
                  >
                    更新密码
                  </button>
                </div>

                {/* Active Device Sessions List */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <p className="font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                      <Laptop className="w-4 h-4 text-slate-500" />
                      <span>活跃登录设备与会话</span>
                    </p>
                    <button
                      type="button"
                      onClick={() => setSessions(sessions.filter((s) => s.current))}
                      className="text-[11px] text-rose-600 dark:text-rose-400 font-semibold hover:underline"
                    >
                      下线其他所有设备
                    </button>
                  </div>

                  <div className="space-y-2">
                    {sessions.map((sess) => (
                      <div
                        key={sess.id}
                        className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between"
                      >
                        <div className="flex items-center space-x-3">
                          <div className="w-8 h-8 rounded-lg bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-300">
                            {sess.device.includes('iPhone') ? <Smartphone className="w-4 h-4" /> : <Laptop className="w-4 h-4" />}
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-semibold text-slate-800 dark:text-slate-200">{sess.device}</span>
                              {sess.current && (
                                <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-indigo-100 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-400">
                                  当前设备
                                </span>
                              )}
                            </div>
                            <p className="text-[10px] text-slate-400 mt-0.5 font-mono">
                              {sess.os} • {sess.browser} • {sess.ip} ({sess.location})
                            </p>
                          </div>
                        </div>

                        {!sess.current && (
                          <button
                            type="button"
                            onClick={() => handleLogoutSession(sess.id)}
                            className="p-1.5 text-slate-400 hover:text-rose-500 transition-colors"
                            title="强行下线"
                          >
                            <LogOut className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                </div>

                {/* 2FA & API Key */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-slate-800 dark:text-slate-200">TOTP 二次身份验证 (2FA)</p>
                      <p className="text-[10px] text-slate-400">绑定 Google Authenticator 校验码</p>
                    </div>
                    <input
                      type="checkbox"
                      checked={enable2FA}
                      onChange={(e) => setEnable2FA(e.target.checked)}
                      className="w-4 h-4 accent-slate-900 dark:accent-indigo-500 rounded cursor-pointer"
                    />
                  </div>

                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                        <Key className="w-3.5 h-3.5 text-slate-500" />
                        <span>API Access Token</span>
                      </span>
                      <span className="px-1.5 py-0.5 rounded text-[9px] font-mono bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400">
                        ACTIVE
                      </span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <input
                        type={showKey ? 'text' : 'password'}
                        value={apiKey}
                        readOnly
                        className="flex-1 px-2.5 py-1 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono text-[11px]"
                      />
                      <button
                        type="button"
                        onClick={() => setShowKey(!showKey)}
                        className="p-1.5 text-slate-400 hover:text-slate-600"
                      >
                        {showKey ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                      </button>
                      <button
                        type="button"
                        onClick={handleCopyKey}
                        className="px-2.5 py-1 bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900 font-semibold rounded-lg text-[10px]"
                      >
                        {copiedKey ? '已复制' : '复制'}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* ================= 3. NOTIFICATIONS MATRIX ================= */}
            {activeTab === 'NOTIFICATIONS' && (
              <div className="space-y-4 animate-in fade-in">
                <div className="pb-2.5 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
                  <div>
                    <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                      <Bell className="w-4 h-4 text-amber-500" />
                      <span>消息通知矩阵与规则</span>
                    </h2>
                    <p className="text-[11px] text-slate-400 mt-0.5">细粒度按通知类型与触达渠道 (站内 / 邮件 / Push) 进行定制</p>
                  </div>
                </div>

                {/* Top Quick Settings: DND & Email Digest */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-slate-800 dark:text-slate-200">夜间免打扰模式 (DND)</p>
                      <p className="text-[10px] text-slate-400">每日 23:00 ~ 08:00 静音非紧急 Push</p>
                    </div>
                    <input
                      type="checkbox"
                      checked={dndMode}
                      onChange={(e) => setDndMode(e.target.checked)}
                      className="w-4 h-4 accent-slate-900 dark:accent-indigo-500 rounded cursor-pointer"
                    />
                  </div>

                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-slate-800 dark:text-slate-200">邮件摘要频次</p>
                      <p className="text-[10px] text-slate-400">避免邮件过频繁打扰</p>
                    </div>
                    <select
                      value={emailFrequency}
                      onChange={(e) => setEmailFrequency(e.target.value as any)}
                      className="px-2.5 py-1 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-medium"
                    >
                      <option value="REALTIME">实时发送</option>
                      <option value="DAILY">每日汇总</option>
                      <option value="WEEKLY">每周精选</option>
                      <option value="OFF">关闭邮件</option>
                    </select>
                  </div>
                </div>

                {/* Matrix Table */}
                <div className="border border-slate-200/80 dark:border-slate-800 rounded-xl overflow-hidden">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-100/70 dark:bg-slate-800/80 border-b border-slate-200/80 dark:border-slate-800 font-bold text-slate-700 dark:text-slate-300">
                      <tr>
                        <th className="py-2.5 px-3">通知事件类型</th>
                        <th className="py-2.5 px-3 text-center">站内通知</th>
                        <th className="py-2.5 px-3 text-center">邮件通知</th>
                        <th className="py-2.5 px-3 text-center">App Push</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                      {[
                        { key: 'likes', label: '文章/动态 赞踩与收藏', mandatory: false },
                        { key: 'comments', label: '评论与直接回复', mandatory: false },
                        { key: 'mentions', label: '@提及 与讨论区引用', mandatory: false },
                        { key: 'followers', label: '新增关注粉丝通知', mandatory: false },
                        { key: 'messages', label: '私信与直连消息', mandatory: false },
                        { key: 'coauthor', label: '共创与团队协作邀请', mandatory: false },
                        { key: 'audit', label: '投稿审阅与审核结果', mandatory: false },
                        { key: 'updates', label: '关注专栏与系列更新', mandatory: false },
                        { key: 'security', label: '账号安全与异地登录警报', mandatory: true },
                      ].map((item) => {
                        const matrixRow = notifMatrix[item.key] || { site: true, email: true, push: true };
                        return (
                          <tr key={item.key} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30">
                            <td className="py-2.5 px-3 font-medium text-slate-800 dark:text-slate-200">
                              {item.label}
                              {item.mandatory && (
                                <span className="ml-2 text-[9px] text-rose-500 font-normal">(强管控)</span>
                              )}
                            </td>
                            <td className="py-2.5 px-3 text-center">
                              <input
                                type="checkbox"
                                disabled={item.mandatory}
                                checked={matrixRow.site}
                                onChange={() => toggleNotifChannel(item.key, 'site')}
                                className="w-3.5 h-3.5 accent-slate-900 dark:accent-indigo-500 rounded cursor-pointer disabled:opacity-50"
                              />
                            </td>
                            <td className="py-2.5 px-3 text-center">
                              <input
                                type="checkbox"
                                disabled={item.mandatory}
                                checked={matrixRow.email}
                                onChange={() => toggleNotifChannel(item.key, 'email')}
                                className="w-3.5 h-3.5 accent-slate-900 dark:accent-indigo-500 rounded cursor-pointer disabled:opacity-50"
                              />
                            </td>
                            <td className="py-2.5 px-3 text-center">
                              <input
                                type="checkbox"
                                checked={matrixRow.push}
                                onChange={() => toggleNotifChannel(item.key, 'push')}
                                className="w-3.5 h-3.5 accent-slate-900 dark:accent-indigo-500 rounded cursor-pointer"
                              />
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* ================= 4. PRIVACY SETTINGS ================= */}
            {activeTab === 'PRIVACY' && (
              <div className="space-y-4 animate-in fade-in">
                <div className="pb-2.5 border-b border-slate-100 dark:border-slate-800">
                  <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <Lock className="w-4 h-4 text-purple-500" />
                    <span>隐私权限与可见范围</span>
                  </h2>
                  <p className="text-[11px] text-slate-400 mt-0.5">控制个人数据公开度、互动权限与搜索引擎索引</p>
                </div>

                {/* Privacy Visibility Matrix (4 options: PUBLIC | FOLLOWERS | MUTUAL | PRIVATE) */}
                <div className="space-y-3">
                  <p className="font-bold text-slate-800 dark:text-slate-200">社交列表与数据可见范围</p>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">

                    <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-1.5">
                      <label className="block font-semibold text-slate-800 dark:text-slate-200">谁能看我的喜欢列表</label>
                      <select
                        value={likesPrivacy}
                        onChange={(e) => updateLikesPrivacy(e.target.value as 'PUBLIC' | 'FOLLOWERS' | 'MUTUAL' | 'PRIVATE')}
                        className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs"
                      >
                        <option value="PUBLIC">所有人公开</option>
                        <option value="FOLLOWERS">仅我的粉丝</option>
                        <option value="MUTUAL">互相关注好友</option>
                        <option value="PRIVATE">仅自己可见</option>
                      </select>
                    </div>

                    <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-1.5">
                      <label className="block font-semibold text-slate-800 dark:text-slate-200">谁能看我的公开收藏夹</label>
                      <select
                        value={bookmarksPrivacy}
                        onChange={(e) => setBookmarksPrivacy(e.target.value as any)}
                        className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs"
                      >
                        <option value="PUBLIC">所有人公开</option>
                        <option value="FOLLOWERS">仅我的粉丝</option>
                        <option value="MUTUAL">互相关注好友</option>
                        <option value="PRIVATE">仅自己可见</option>
                      </select>
                    </div>

                    <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-1.5">
                      <label className="block font-semibold text-slate-800 dark:text-slate-200">谁能看我的关注/粉丝列表</label>
                      <select
                        value={followingPrivacy}
                        onChange={(e) => setFollowingPrivacy(e.target.value as any)}
                        className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs"
                      >
                        <option value="PUBLIC">所有人公开</option>
                        <option value="FOLLOWERS">仅我的粉丝</option>
                        <option value="MUTUAL">互相关注好友</option>
                        <option value="PRIVATE">仅自己可见</option>
                      </select>
                    </div>

                    <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-1.5">
                      <label className="block font-semibold text-slate-800 dark:text-slate-200">展示系列阅读进度</label>
                      <select
                        value={readingStatusPrivacy}
                        onChange={(e) => setReadingStatusPrivacy(e.target.value as any)}
                        className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs"
                      >
                        <option value="PUBLIC">所有人公开</option>
                        <option value="FOLLOWERS">仅我的粉丝</option>
                        <option value="MUTUAL">互相关注好友</option>
                        <option value="PRIVATE">仅自己可见</option>
                      </select>
                    </div>

                  </div>
                </div>

                {/* Interaction Restrictions */}
                <div className="space-y-2">
                  <p className="font-bold text-slate-800 dark:text-slate-200">互动与推荐索引控制</p>

                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-slate-800 dark:text-slate-200">谁能向我发送私信</p>
                      <p className="text-[10px] text-slate-400">防范恶意骚扰与垃圾推广</p>
                    </div>
                    <select
                      value={whoCanMessage}
                      onChange={(e) => setWhoCanMessage(e.target.value as any)}
                      className="px-2.5 py-1 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs"
                    >
                      <option value="ALL">所有人</option>
                      <option value="MUTUAL">仅互相关注</option>
                      <option value="NONE">拒绝所有私信</option>
                    </select>
                  </div>

                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-slate-800 dark:text-slate-200">允许搜索引擎 (Google/Baidu) 索引个人主页</p>
                      <p className="text-[10px] text-slate-400">开启后生成 Sitemap 提交至站外搜索引擎</p>
                    </div>
                    <input
                      type="checkbox"
                      checked={allowSearchIndex}
                      onChange={(e) => setAllowSearchIndex(e.target.checked)}
                      className="w-4 h-4 accent-slate-900 dark:accent-indigo-500 rounded cursor-pointer"
                    />
                  </div>

                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-slate-800 dark:text-slate-200">允许出现在社区“推荐创作者”榜单</p>
                      <p className="text-[10px] text-slate-400">算法根据高质量文章推荐给感兴趣的读者</p>
                    </div>
                    <input
                      type="checkbox"
                      checked={allowRecommendation}
                      onChange={(e) => setAllowRecommendation(e.target.checked)}
                      className="w-4 h-4 accent-slate-900 dark:accent-indigo-500 rounded cursor-pointer"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* ================= 5. APPEARANCE DESIGN ================= */}
            {activeTab === 'APPEARANCE' && (
              <div className="space-y-4 animate-in fade-in">
                <div className="pb-2.5 border-b border-slate-100 dark:border-slate-800">
                  <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <Palette className="w-4 h-4 text-pink-500" />
                    <span>视觉呈现与外观设计</span>
                  </h2>
                  <p className="text-[11px] text-slate-400 mt-0.5">定制专属主题皮肤、界面字号与无障碍渲染</p>
                </div>

                {/* Theme Cards */}
                <div>
                  <label className="block font-semibold text-slate-800 dark:text-slate-200 mb-2">皮肤主题模式</label>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                    <button
                      type="button"
                      onClick={() => setTheme('light')}
                      className={`p-3.5 rounded-xl border flex flex-col items-center gap-1.5 transition-all ${
                        theme === 'light'
                          ? 'border-indigo-600 bg-indigo-50/40 text-indigo-950 font-semibold'
                          : 'border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/30'
                      }`}
                    >
                      <Sun className="w-5 h-5 text-amber-500" />
                      <span className="font-semibold">日间浅色</span>
                      <span className="text-[10px] text-slate-400">高清晰度对比</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setTheme('dark')}
                      className={`p-3.5 rounded-xl border flex flex-col items-center gap-1.5 transition-all ${
                        theme === 'dark'
                          ? 'border-indigo-600 bg-slate-800 text-white font-semibold'
                          : 'border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/30'
                      }`}
                    >
                      <Moon className="w-5 h-5 text-indigo-400" />
                      <span className="font-semibold">夜间深色</span>
                      <span className="text-[10px] text-slate-400">暗光护眼无疲劳</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setTheme('starlight')}
                      className={`p-3.5 rounded-xl border flex flex-col items-center gap-1.5 transition-all ${
                        theme === 'starlight'
                          ? 'border-purple-600 bg-slate-900 text-purple-200 font-semibold'
                          : 'border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/30'
                      }`}
                    >
                      <Star className="w-5 h-5 text-purple-400" />
                      <span className="font-semibold">星空深邃</span>
                      <span className="text-[10px] text-slate-400">极简暗沉基调</span>
                    </button>
                  </div>
                </div>

                {/* Font Size & UI Density */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-2">
                    <label className="block font-semibold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                      <Type className="w-3.5 h-3.5 text-slate-500" />
                      <span>界面正文字体大小</span>
                    </label>
                    <div className="grid grid-cols-3 gap-2">
                      {[
                        { key: 'SMALL', label: '小 (13px)' },
                        { key: 'MEDIUM', label: '标准 (14px)' },
                        { key: 'LARGE', label: '大 (15px)' },
                      ].map((item) => (
                        <button
                          key={item.key}
                          type="button"
                          onClick={() => setFontSize(item.key as any)}
                          className={`py-1.5 rounded-lg border text-[11px] font-semibold transition-all ${
                            fontSize === item.key
                              ? 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900 border-slate-900 dark:border-slate-100'
                              : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300'
                          }`}
                        >
                          {item.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-2">
                    <label className="block font-semibold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                      <Layout className="w-3.5 h-3.5 text-slate-500" />
                      <span>页面间距布局密度</span>
                    </label>
                    <div className="grid grid-cols-3 gap-2">
                      {[
                        { key: 'COMFORTABLE', label: '舒适' },
                        { key: 'STANDARD', label: '标准' },
                        { key: 'COMPACT', label: '紧凑' },
                      ].map((item) => (
                        <button
                          key={item.key}
                          type="button"
                          onClick={() => setUiDensity(item.key as any)}
                          className={`py-1.5 rounded-lg border text-[11px] font-semibold transition-all ${
                            uiDensity === item.key
                              ? 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900 border-slate-900 dark:border-slate-100'
                              : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300'
                          }`}
                        >
                          {item.label}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Accessibility & Code Theme */}
                <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between">
                  <div>
                    <p className="font-semibold text-slate-800 dark:text-slate-200">无障碍：减少动画过度效果</p>
                    <p className="text-[10px] text-slate-400">关闭复杂 CSS 缩放与位移过渡 animations</p>
                  </div>
                  <input
                    type="checkbox"
                    checked={reduceMotion}
                    onChange={(e) => setReduceMotion(e.target.checked)}
                    className="w-4 h-4 accent-slate-900 dark:accent-indigo-500 rounded cursor-pointer"
                  />
                </div>
              </div>
            )}

            {/* ================= 6. USAGE PREFERENCES ================= */}
            {activeTab === 'PREFERENCES' && (
              <div className="space-y-4 animate-in fade-in">
                <div className="pb-2.5 border-b border-slate-100 dark:border-slate-800">
                  <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <SlidersHorizontal className="w-4 h-4 text-blue-500" />
                    <span>使用偏好与发文默认规则</span>
                  </h2>
                  <p className="text-[11px] text-slate-400 mt-0.5">定制默认首页 Feed、文章排序规则与创作许可预设</p>
                </div>

                {/* Feed & Sorting Defaults */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-1.5">
                    <label className="block font-semibold text-slate-800 dark:text-slate-200">默认首页 Feed Tab</label>
                    <select
                      value={defaultHomeFeed}
                      onChange={(e) => setDefaultHomeFeed(e.target.value as any)}
                      className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-medium"
                    >
                      <option value="RECOMMEND">综合推荐</option>
                      <option value="FOLLOWING">关注动态</option>
                      <option value="RELATED">与我相关</option>
                    </select>
                  </div>

                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-1.5">
                    <label className="block font-semibold text-slate-800 dark:text-slate-200">文章列表默认排序</label>
                    <select
                      value={defaultArticleSort}
                      onChange={(e) => setDefaultArticleSort(e.target.value as any)}
                      className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-medium"
                    >
                      <option value="HOT">热门推荐</option>
                      <option value="LATEST">最新发布</option>
                      <option value="COMMENT">最新讨论</option>
                    </select>
                  </div>
                </div>

                {/* Creator Presets (利用后端 Blog Settings) */}
                <div className="space-y-3">
                  <p className="font-bold text-slate-800 dark:text-slate-200">创作与发文默认设置 (Blog Settings 预设)</p>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                    <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-1.5">
                      <label className="block font-semibold text-slate-800 dark:text-slate-200">默认文章可见性 (Visibility)</label>
                      <select
                        value={defaultPostVisibility}
                        onChange={(e) => setDefaultPostVisibility(e.target.value as any)}
                        className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-medium"
                      >
                        <option value="PUBLIC">公开发布 (PUBLIC)</option>
                        <option value="PRIVATE">私密草稿 (PRIVATE)</option>
                        <option value="TEAM_ONLY">团队内部 (TEAM_ONLY)</option>
                      </select>
                    </div>

                    <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 space-y-1.5">
                      <label className="block font-semibold text-slate-800 dark:text-slate-200">默认评论范围 (Comment Scope)</label>
                      <select
                        value={defaultCommentScope}
                        onChange={(e) => setDefaultCommentScope(e.target.value as any)}
                        className="w-full px-2.5 py-1.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-xs font-medium"
                      >
                        <option value="EVERYONE">所有人可评论</option>
                        <option value="MEMBERS_ONLY">仅注册成员可评论</option>
                        <option value="CLOSED">禁止评论</option>
                      </select>
                    </div>
                  </div>

                  <div className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-slate-800 dark:text-slate-200">默认允许他人转载与分享 (Allow Repost)</p>
                      <p className="text-[10px] text-slate-400">保留原作者署名与文章来源 URL 链接</p>
                    </div>
                    <input
                      type="checkbox"
                      checked={allowRepost}
                      onChange={(e) => setAllowRepost(e.target.checked)}
                      className="w-4 h-4 accent-slate-900 dark:accent-indigo-500 rounded cursor-pointer"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* ================= 7. BLOCKS & MUTING ================= */}
            {activeTab === 'BLOCKS' && (
              <div className="space-y-4 animate-in fade-in">
                <div className="pb-2.5 border-b border-slate-100 dark:border-slate-800">
                  <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <Ban className="w-4 h-4 text-rose-500" />
                    <span>屏蔽与黑名单管理</span>
                  </h2>
                  <p className="text-[11px] text-slate-400 mt-0.5">管理已屏蔽的用户、专栏、标签与自定义关键词</p>
                </div>

                {/* Sub category tabs */}
                <div className="flex border-b border-slate-200 dark:border-slate-800 space-x-4 text-xs font-medium">
                  {[
                    { id: 'USER', label: '屏蔽用户' },
                    { id: 'BLOG', label: '屏蔽专栏' },
                    { id: 'TAG', label: '屏蔽标签' },
                    { id: 'KEYWORD', label: '关键词静音' },
                  ].map((cat) => (
                    <button
                      key={cat.id}
                      type="button"
                      onClick={() => setBlockCategory(cat.id as any)}
                      className={`pb-2 transition-all ${
                        blockCategory === cat.id
                          ? 'border-b-2 border-slate-900 dark:border-slate-100 text-slate-900 dark:text-white font-bold'
                          : 'text-slate-400 hover:text-slate-600'
                      }`}
                    >
                      {cat.label}
                    </button>
                  ))}
                </div>

                {/* USER BLOCK LIST */}
                {blockCategory === 'USER' && (
                  <div className="space-y-2">
                    {blockedUsers.length === 0 ? (
                      <p className="text-slate-400 text-center py-6">暂无屏蔽用户</p>
                    ) : (
                      blockedUsers.map((u) => (
                        <div
                          key={u.id}
                          className="p-3 bg-slate-50/70 dark:bg-slate-800/40 rounded-xl border border-slate-200/60 dark:border-slate-800 flex items-center justify-between"
                        >
                          <div className="flex items-center space-x-3">
                            <img src={u.avatar} alt={u.name} className="w-9 h-9 rounded-lg object-cover" />
                            <div>
                              <p className="font-semibold text-slate-800 dark:text-slate-200">{u.name}</p>
                              <p className="text-[10px] text-slate-400 font-mono">{u.handle} • 屏蔽于 {u.date}</p>
                            </div>
                          </div>
                          <button
                            type="button"
                            onClick={() => handleUnblockUser(u.targetId)}
                            className="px-2.5 py-1 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-rose-50 text-slate-600 dark:text-slate-300 hover:text-rose-600 text-[11px] font-semibold rounded-lg transition-all"
                          >
                            解除屏蔽
                          </button>
                        </div>
                      ))
                    )}
                  </div>
                )}

                {/* KEYWORD MUTING */}
                {blockCategory === 'KEYWORD' && (
                  <div className="space-y-3">
                    <div className="flex gap-2">
                      <input
                        type="text"
                        placeholder="输入需要静音屏蔽的关键词..."
                        value={newKeywordInput}
                        onChange={(e) => setNewKeywordInput(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddKeyword())}
                        className="flex-1 px-3 py-1.5 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-xs"
                      />
                      <button
                        type="button"
                        onClick={handleAddKeyword}
                        className="px-3 py-1.5 bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900 font-semibold rounded-xl flex items-center gap-1 shrink-0"
                      >
                        <Plus className="w-3.5 h-3.5" />
                        <span>添加词条</span>
                      </button>
                    </div>

                    <div className="flex flex-wrap gap-2">
                      {keywords.map((word) => (
                        <span
                          key={word}
                          className="px-2.5 py-1 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-lg border border-slate-200 dark:border-slate-700 text-xs font-mono flex items-center gap-1.5"
                        >
                          <VolumeX className="w-3 h-3 text-rose-500" />
                          <span>{word}</span>
                          <button
                            type="button"
                            onClick={() => handleRemoveKeyword(word)}
                            className="hover:text-rose-500"
                          >
                            <X className="w-3 h-3" />
                          </button>
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                {(blockCategory === 'BLOG' || blockCategory === 'TAG') && (
                  <p className="text-slate-400 text-center py-6">暂无屏蔽的{blockCategory === 'BLOG' ? '专栏' : '标签'}</p>
                )}
              </div>
            )}

            {/* ================= 8. DATA & ACCOUNT LIFECYCLE ================= */}
            {activeTab === 'DATA' && (
              <div className="space-y-4 animate-in fade-in">
                <div className="pb-2.5 border-b border-slate-100 dark:border-slate-800">
                  <h2 className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <Database className="w-4 h-4 text-cyan-500" />
                    <span>数据导出与账号生命周期</span>
                  </h2>
                  <p className="text-[11px] text-slate-400 mt-0.5">打包导出全部个人内容备份或进行账号注销</p>
                </div>

                {/* Export Card */}
                <div className="p-3.5 bg-slate-50/70 dark:bg-slate-800/40 border border-slate-200/60 dark:border-slate-800 rounded-xl space-y-2">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-bold text-slate-800 dark:text-slate-200 flex items-center gap-1.5">
                        <Download className="w-4 h-4 text-indigo-500" />
                        <span>导出全部创作与社交数据 (.zip)</span>
                      </p>
                      <p className="text-[10px] text-slate-400 mt-0.5">
                        包含 Markdown 格式全量文章、动态 JSON、收藏夹与关系链清单
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setExportingData(true);
                        setTimeout(() => setExportingData(false), 2000);
                      }}
                      disabled={exportingData}
                      className="px-3 py-1.5 bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900 font-semibold rounded-xl text-xs shrink-0 flex items-center gap-1.5"
                    >
                      {exportingData ? <Clock className="w-3.5 h-3.5 animate-spin" /> : <Download className="w-3.5 h-3.5" />}
                      <span>{exportingData ? '打包生成中...' : '立即打包导出'}</span>
                    </button>
                  </div>
                </div>

                {/* Lifecycle Danger Zone */}
                <div className="p-4 bg-rose-50/50 dark:bg-rose-950/20 border border-rose-200/80 dark:border-rose-900/50 rounded-xl space-y-3">
                  <div className="flex items-center gap-2 text-rose-600 dark:text-rose-400 font-bold">
                    <AlertTriangle className="w-4 h-4" />
                    <span>危险区域 (Danger Zone)</span>
                  </div>

                  <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pt-2 border-t border-rose-200/60 dark:border-rose-900/30">
                    <div>
                      <p className="font-semibold text-slate-800 dark:text-slate-200">暂停 / 停用账号</p>
                      <p className="text-[10px] text-slate-400">临时隐藏个人主页与内容，重新登录即可立刻恢复</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => setDeactivateModalOpen(true)}
                      className="px-3 py-1.5 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 hover:bg-rose-50 text-rose-600 font-semibold rounded-xl text-xs shrink-0"
                    >
                      停用当前账号
                    </button>
                  </div>

                  <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pt-2 border-t border-rose-200/60 dark:border-rose-900/30">
                    <div>
                      <p className="font-semibold text-rose-600 dark:text-rose-400">永久注销与清空账号数据</p>
                      <p className="text-[10px] text-slate-400">二次身份验证后进入 14 天冷静期，期满永久清除</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => setDeactivateModalOpen(true)}
                      className="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white font-semibold rounded-xl text-xs shrink-0"
                    >
                      申请永久注销
                    </button>
                  </div>
                </div>
              </div>
            )}

              </motion.div>
            </AnimatePresence>

            {/* Bottom Submit Action Bar */}
            <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <span className="text-[11px] text-slate-400">修改后点击按钮更新本地与远程配置</span>
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.96 }}
                type="submit"
                className="flex items-center space-x-1.5 px-5 py-2 bg-slate-900 hover:bg-slate-800 text-white dark:bg-slate-100 dark:text-slate-900 dark:hover:bg-slate-200 font-semibold rounded-xl shadow-xs transition-all text-xs cursor-pointer"
              >
                <Save className="w-3.5 h-3.5" />
                <span>保存全局设置</span>
              </motion.button>
            </div>

          </form>
        </div>

      </div>

      {/* Floating Save Toast Notification */}
      <AnimatePresence>
        {isSaved && (
          <motion.div
            initial={{ opacity: 0, y: 20, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.9 }}
            transition={{ duration: 0.2 }}
            className="fixed bottom-6 right-6 z-50 flex items-center gap-2.5 px-4 py-3 bg-slate-900/90 dark:bg-white/90 text-white dark:text-slate-900 backdrop-blur-md border border-slate-800 dark:border-slate-200 rounded-2xl shadow-2xl text-xs font-semibold"
          >
            <CheckCircle2 className="w-4 h-4 text-emerald-400 dark:text-emerald-600 shrink-0" />
            <span>个人配置与全局设置已成功保存！</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Deactivate / Danger Modal */}
      {deactivateModalOpen && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-in fade-in">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-md w-full p-5 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-2 text-rose-600 font-bold">
                <AlertTriangle className="w-4 h-4" />
                <span>确认停用 / 注销账号？</span>
              </div>
              <button onClick={() => setDeactivateModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-4 h-4" />
              </button>
            </div>
            <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
              停用后您的公开博客、文章与评论将暂时对站外用户隐身。您随时可以通过重新登录此账号激活恢复。若申请永久注销，需先确认您非任何活跃团队的 Owner。
            </p>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setDeactivateModalOpen(false)}
                className="px-3.5 py-1.5 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-semibold rounded-xl text-xs"
              >
                取消
              </button>
              <button
                type="button"
                onClick={() => {
                  setDeactivateModalOpen(false);
                  alert('已提交申请，验证邮件已发送至绑定的邮箱');
                }}
                className="px-3.5 py-1.5 bg-rose-600 text-white font-semibold rounded-xl text-xs"
              >
                确认提交
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
