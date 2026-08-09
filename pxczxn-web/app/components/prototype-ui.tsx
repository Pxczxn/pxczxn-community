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
  Sun,
  UserRound,
  UsersRound,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import {
  CommunitySession,
  NOTIFICATION_EVENT,
  SESSION_EVENT,
  communityApi,
  readSession,
  saveSession,
} from "../lib/community-api";
import { setTheme, THEME_EVENT, ThemeMode } from "./theme-bootstrap";

const primaryNavItems = [
  { href: "/", label: "首页", Icon: House },
  { href: "/discover", label: "发现", Icon: Compass },
  { href: "/articles", label: "文章", Icon: FileText },
  { href: "/moments", label: "动态", Icon: Orbit },
  { href: "/series", label: "系列", Icon: Sparkles },
  { href: "/teams", label: "团队", Icon: UsersRound },
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
      // eslint-disable-next-line @next/next/no-img-element -- 动态用户图片,尺寸由 CSS 控制
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
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const [theme, setCurrentTheme] = useState<ThemeMode>("light");
  const accountMenuRef = useRef<HTMLDivElement>(null);
  const pathname = usePathname();
  const frozenSession = session?.status === "FROZEN";

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

  useEffect(() => {
    if (!accountMenuOpen) return;

    const closeWhenClickAway = (event: PointerEvent) => {
      if (!accountMenuRef.current?.contains(event.target as Node)) {
        setAccountMenuOpen(false);
      }
    };
    const closeWithEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setAccountMenuOpen(false);
    };

    document.addEventListener("pointerdown", closeWhenClickAway);
    document.addEventListener("keydown", closeWithEscape);
    return () => {
      document.removeEventListener("pointerdown", closeWhenClickAway);
      document.removeEventListener("keydown", closeWithEscape);
    };
  }, [accountMenuOpen]);

  useEffect(() => {
    const syncTheme = (event?: Event) => {
      const changedTheme = (event as CustomEvent<ThemeMode> | undefined)?.detail;
      const currentTheme = changedTheme || document.documentElement.dataset.theme;
      setCurrentTheme(currentTheme === "dark" || currentTheme === "starry" ? currentTheme : "light");
    };
    syncTheme();
    window.addEventListener(THEME_EVENT, syncTheme);
    return () => window.removeEventListener(THEME_EVENT, syncTheme);
  }, []);

  function toggleTheme() {
    const nextTheme: ThemeMode = theme === "light" ? "dark" : theme === "dark" ? "starry" : "light";
    setTheme(nextTheme);
    setCurrentTheme(nextTheme);
  }

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
      {!frozenSession && <nav className="community-primary-nav" aria-label="星语社区主导航">
        {primaryNavItems.map(({ href, label, Icon }) => {
          const isActive = href === "/" ? pathname === href : pathname === href || pathname.startsWith(`${href}/`);
          return (
            <Link key={href} href={href} className={isActive ? "is-active" : undefined} aria-current={isActive ? "page" : undefined}>
              <Icon size={15} strokeWidth={2.2} aria-hidden="true" />
              <span>{label}</span>
            </Link>
          );
        })}
      </nav>}
      <div className="app-topbar__group">
        {!frozenSession && <label className="top-search">
          <Search aria-hidden="true" size={17} />
          <input aria-label="搜索" placeholder="搜索文章、动态和团队" />
        </label>}
        {!frozenSession && <div className="topbar-tools" aria-label="快捷工具">
          {session && (
            <Link aria-label="写文章" className="topbar-compose" href="/editor/new" title="写文章">
              <PenLine size={18} /> <span>写文章</span>
            </Link>
          )}
          {session && (
            <Link aria-label="屏蔽管理" className="icon-button" href="/blocks" title="屏蔽管理">
              <ShieldBan size={18} />
            </Link>
          )}
          <button aria-label={`当前${theme === "light" ? "浅色" : theme === "dark" ? "深色" : "星空"}主题，切换主题`} className="icon-button" onClick={toggleTheme} title={`当前${theme === "light" ? "浅色" : theme === "dark" ? "深色" : "星空"}主题，点击切换`} type="button">
            {theme === "light" ? <Sun size={18} /> : theme === "dark" ? <Moon size={18} /> : <Sparkles size={18} />}
          </button>
          <Link
          aria-label={`通知${unreadNotifications ? `，${unreadNotifications} 条未读` : ""}`}
          className="icon-button notification-button"
          href={session ? "/notifications" : "/login"}
          title={unreadNotifications ? `通知：${unreadNotifications} 条未读` : "通知"}
        >
          <Bell size={18} />
          {unreadNotifications > 0 && (
            <span className="notification-badge">
              {unreadNotifications > 99 ? "99+" : unreadNotifications}
            </span>
          )}
        </Link>
          {session && (
          <Link aria-label="即时聊天" className="icon-button" href="/chat" title="即时聊天">
            <MessageCircle size={18} />
          </Link>
          )}
        </div>}
        {frozenSession && <Link className="secondary-button" href="/account-appeals">提交申诉</Link>}
        {session ? (
          <div className="account-menu" ref={accountMenuRef}>
            <button aria-expanded={accountMenuOpen} aria-haspopup="menu" className="user-chip" onClick={() => setAccountMenuOpen((open) => !open)} type="button">
              <Avatar label={(session.displayName || session.username || "?").slice(0, 1)} size="sm" />
              <span>{session.displayName || session.username}</span>
              <ChevronDown size={15} />
            </button>
            {accountMenuOpen && (
              <div className="account-menu__popover" role="menu">
                {!frozenSession && <Link onClick={() => setAccountMenuOpen(false)} href="/me/blog" role="menuitem"><UserRound size={16} />个人中心</Link>}
                {!frozenSession && <Link onClick={() => setAccountMenuOpen(false)} href="/settings" role="menuitem"><Settings size={16} />设置</Link>}
                <button onClick={logout} role="menuitem" type="button"><LogOut size={16} />退出登录</button>
              </div>
            )}
          </div>
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
          <small>团队工作台</small>
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
