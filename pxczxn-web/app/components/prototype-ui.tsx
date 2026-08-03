"use client";

import Link from "next/link";
import {
  Bell,
  ChevronDown,
  Compass,
  FileText,
  House,
  LogIn,
  LogOut,
  MessageCircle,
  Moon,
  Orbit,
  PenLine,
  Search,
  Settings,
  ShieldBan,
  Sparkles,
  Tags,
  UsersRound,
} from "lucide-react";
import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import {
  CommunitySession,
  NOTIFICATION_EVENT,
  SESSION_EVENT,
  communityApi,
  readSession,
  saveSession,
} from "../lib/community-api";

const primaryNavItems = [
  { href: "/", label: "首页", Icon: House },
  { href: "/discover", label: "发现", Icon: Compass },
  { href: "/articles", label: "文章", Icon: FileText },
  { href: "/moments", label: "动态", Icon: Orbit },
  { href: "/series", label: "系列", Icon: Sparkles },
  { href: "/teams", label: "团队", Icon: UsersRound },
  { href: "/tags", label: "标签", Icon: Tags },
] as const;

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
  src,
  alt = "",
}: {
  label?: string;
  size?: "sm" | "md" | "lg";
  src?: string | null;
  alt?: string;
}) {
  if (src) {
    return (
      <img
        alt={alt}
        className={`avatar avatar-${size} avatar-image`}
        src={src}
      />
    );
  }

  return <span className={`avatar avatar-${size}`}>{label}</span>;
}

export function UserTopbar({ title }: { title?: string }) {
  const [session, setSession] = useState<CommunitySession | null>(null);
  const [unreadNotifications, setUnreadNotifications] = useState(0);
  const pathname = usePathname();

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
        {primaryNavItems.map(({ href, label, Icon }) => {
          const isActive = href === "/" ? pathname === href : pathname === href || pathname.startsWith(`${href}/`);
          return (
            <Link key={href} href={href} className={isActive ? "is-active" : undefined} aria-current={isActive ? "page" : undefined}>
              <Icon size={15} strokeWidth={2.2} aria-hidden="true" />
              <span>{label}</span>
            </Link>
          );
        })}
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
        {session && (
          <Link aria-label="屏蔽管理" className="icon-button" href="/blocks">
            <ShieldBan size={18} />
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
