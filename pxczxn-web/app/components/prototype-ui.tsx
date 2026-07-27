"use client";

import Link from "next/link";
import {
  Bell,
  ChevronDown,
  LogIn,
  LogOut,
  MessageCircle,
  Moon,
  PenLine,
  Search,
  Settings,
  Sparkles,
} from "lucide-react";
import { useEffect, useState } from "react";
import {
  CommunitySession,
  NOTIFICATION_EVENT,
  SESSION_EVENT,
  communityApi,
  readSession,
  saveSession,
} from "../lib/community-api";

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <Link className="app-topbar__brand" href="/discover">
      <span className="brand-mark">星</span>
      {!compact && <span>星语社区</span>}
    </Link>
  );
}

export function Avatar({
  label = "程",
  size = "md",
}: {
  label?: string;
  size?: "sm" | "md" | "lg";
}) {
  return <span className={`avatar avatar-${size}`}>{label}</span>;
}

export function UserTopbar({ title }: { title?: string }) {
  const [session, setSession] = useState<CommunitySession | null>(null);
  const [unreadNotifications, setUnreadNotifications] = useState(0);

  useEffect(() => {
    const sync = () => {
      const nextSession = readSession();
      setSession(nextSession);
      if (!nextSession) {
        setUnreadNotifications(0);
        return;
      }
      void communityApi.unreadNotifications()
        .then((result) => setUnreadNotifications(result.total))
        .catch(() => setUnreadNotifications(0));
    };
    sync();
    window.addEventListener(SESSION_EVENT, sync);
    window.addEventListener(NOTIFICATION_EVENT, sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener(SESSION_EVENT, sync);
      window.removeEventListener(NOTIFICATION_EVENT, sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  async function logout() {
    try {
      await communityApi.logout();
    } catch {
      // Clearing the local session is still the safe result when the server is offline.
    } finally {
      saveSession(null);
      window.location.assign("/login");
    }
  }

  return (
    <header className="app-topbar">
      <div className="app-topbar__group">
        <Brand />
        {title && <span className="secondary">/</span>}
        {title && <strong>{title}</strong>}
      </div>
      <nav className="community-primary-nav" aria-label="星语社区主导航">
        <Link href="/">首页</Link>
        <Link href="/discover">发现</Link>
        <Link href="/articles">文章</Link>
        <Link href="/moments">动态</Link>
        <Link href="/series">系列</Link>
        <Link href="/teams">团队</Link>
        <Link href="/tags">标签</Link>
      </nav>
      <div className="app-topbar__group">
        <label className="top-search">
          <Search aria-hidden="true" size={17} />
          <input aria-label="搜索" placeholder="搜索文章、动态和团队" />
        </label>
        {session && (
          <Link aria-label="写文章" className="icon-button" href="/editor/new">
            <PenLine size={18} />
          </Link>
        )}
        <Link aria-label="主题设置" className="icon-button" href="/settings">
          <Moon size={18} />
        </Link>
        <Link
          aria-label={`通知${unreadNotifications ? `，${unreadNotifications} 条未读` : ""}`}
          className="icon-button notification-button"
          href={session ? "/notifications" : "/login"}
        >
          <Bell size={18} />
          {unreadNotifications > 0 && (
            <span className="notification-badge">
              {unreadNotifications > 99 ? "99+" : unreadNotifications}
            </span>
          )}
        </Link>
        {session && (
          <Link aria-label="即时聊天" className="icon-button" href="/chat">
            <MessageCircle size={18} />
          </Link>
        )}
        {session ? (
          <>
            <Link className="user-chip" href="/me/blog">
              <Avatar label={(session.displayName || session.username).slice(0, 1)} size="sm" />
              <span>{session.displayName || session.username}</span>
              <ChevronDown size={15} />
            </Link>
            <button
              aria-label="退出登录"
              className="icon-button topbar-logout"
              onClick={logout}
              title="退出登录"
              type="button"
            >
              <LogOut size={17} />
            </button>
          </>
        ) : (
          <Link className="user-chip" href="/login">
            <LogIn size={17} />
            <span>登录</span>
          </Link>
        )}
      </div>
    </header>
  );
}

export function Metric({
  label,
  value,
  trend,
}: {
  label: string;
  value: string;
  trend?: string;
}) {
  return (
    <div className="surface metric-card">
      <span className="secondary">{label}</span>
      <strong className="metric">{value}</strong>
      {trend && <small className="metric-trend">{trend}</small>}
    </div>
  );
}

export function ArticleThumb({ variant = 1 }: { variant?: number }) {
  return (
    <span
      aria-hidden="true"
      className={`article-thumb article-thumb--${((variant - 1) % 4) + 1}`}
    >
      <Sparkles size={17} />
    </span>
  );
}

export function SideNavigation({
  active,
}: {
  active:
    | "overview"
    | "submissions"
    | "articles"
    | "moments"
    | "analytics"
    | "members"
    | "roles"
    | "settings";
}) {
  const links = [
    ["overview", "团队主页", "/teams"],
    ["submissions", "投稿管理", "/submissions"],
    ["articles", "文章管理", "/articles"],
    ["moments", "动态管理", "/moments"],
    ["analytics", "数据统计", "/discover"],
    ["members", "成员管理", "/teams"],
    ["roles", "角色权限", "/teams"],
    ["settings", "团队设置", "/settings"],
  ] as const;

  return (
    <aside className="workspace-sidebar">
      <Link className="workspace-brand" href="/teams">
        <span className="brand-mark">星</span>
        <span>
          <strong>星语社区</strong>
          <small>团队工作台（M3）</small>
        </span>
      </Link>
      <nav aria-label="团队工作台导航">
        {links.map(([key, label, href]) => (
          <Link
            className={`workspace-nav-item ${active === key ? "active" : ""}`}
            href={href}
            key={key}
          >
            <Settings aria-hidden="true" size={17} />
            {label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}

export function EmptyState({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="empty-state">
      <span className="empty-state__icon">
        <Sparkles size={28} />
      </span>
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  );
}
