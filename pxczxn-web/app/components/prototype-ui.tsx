"use client";

import Link from "next/link";
import {
  Bell,
  ChevronDown,
  Compass,
  FileText,
  House,
  LibraryBig,
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
import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { Badge, Tooltip } from "@/components/ui/community-ui";
import { Avatar as ShadcnAvatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import {
  CommunitySession,
  CurrentCommunityUser,
  NOTIFICATION_EVENT,
  SESSION_EVENT,
  communityApi,
  publicFileUrl,
  readSession,
  saveSession,
} from "../lib/community-api";
import { setTheme, THEME_EVENT, ThemeMode } from "./theme-bootstrap";

const primaryNavItems = [
  { href: "/", label: "首页", Icon: House },
  { href: "/discover", label: "发现", Icon: Compass },
  { href: "/articles", label: "文章", Icon: FileText },
  { href: "/moments", label: "动态", Icon: Orbit },
  { href: "/series", label: "书架", Icon: LibraryBig },
  { href: "/teams", label: "团队", Icon: UsersRound },
] as const;

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <Link className="app-topbar__brand flex items-center gap-2 text-slate-900 dark:text-slate-100" href="/">
      <span className="w-7 h-7 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-xs">
        <Sparkles size={16} className="fill-white stroke-white" />
      </span>
      {!compact && <span className="font-bold text-lg tracking-tight text-slate-900 dark:text-slate-100">星语社区</span>}
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
      <ShadcnAvatar size={size === "sm" ? "sm" : size === "lg" ? "lg" : "default"} className="avatar-image" aria-label={alt}>
        <AvatarImage src={src} alt={alt} />
        <AvatarFallback>{label}</AvatarFallback>
      </ShadcnAvatar>
    );
  }

  return (
    <ShadcnAvatar
      size={size === "sm" ? "sm" : size === "lg" ? "lg" : "default"}
      style={{
        backgroundColor: "var(--primary, #1677ff)",
        color: "#fff",
        fontWeight: 600,
        fontSize: size === "sm" ? 13 : size === "lg" ? 18 : 15,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <AvatarFallback>{label}</AvatarFallback>
    </ShadcnAvatar>
  );
}

export function UserTopbar({ title }: { title?: string }) {
  const [session, setSession] = useState<CommunitySession | null>(null);
  const [currentUser, setCurrentUser] = useState<CurrentCommunityUser | null>(null);
  const [unreadNotifications, setUnreadNotifications] = useState(0);
  const [theme, setCurrentTheme] = useState<ThemeMode>("light");
  const [searchQuery, setSearchQuery] = useState("");
  const pathname = usePathname();
  const router = useRouter();
  const frozenSession = session?.status === "FROZEN";

  useEffect(() => {
    const sync = () => {
      const nextSession = readSession();
      setSession(nextSession);
      if (!nextSession) {
        setCurrentUser(null);
        setUnreadNotifications(0);
        return;
      }
      void communityApi.me()
        .then((user) => setCurrentUser(user))
        .catch(() => setCurrentUser(null));

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
      // Server offline fallback
    } finally {
      saveSession(null);
      window.location.assign("/login");
    }
  }

  function handleSearchSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const trimmed = searchQuery.trim();
    if (trimmed) {
      router.push(`/search?q=${encodeURIComponent(trimmed)}`);
    }
  }

  const userMenuItems = [
    ...(!frozenSession
      ? [
          {
            key: "blog",
            label: (
              <Link href="/me/blog" style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <UserRound size={16} /> 个人中心
              </Link>
            ),
          },
          {
            key: "settings",
            label: (
              <Link href="/settings" style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <Settings size={16} /> 设置
              </Link>
            ),
          },
          { type: "divider" as const },
        ]
      : []),
    {
      key: "logout",
      label: (
        <span onClick={logout} style={{ display: "flex", alignItems: "center", gap: 8, color: "var(--danger, #ef4444)" }}>
          <LogOut size={16} /> 退出登录
        </span>
      ),
    },
  ];

  return (
    <header className="app-topbar">
      <div className="app-topbar__group">
        <Brand />
      </div>
      {!frozenSession && (
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
      )}
      <div className="app-topbar__group">
        {!frozenSession && (
          <form onSubmit={handleSearchSubmit} className="top-search">
            <Search aria-hidden="true" size={16} />
            <input
              aria-label="搜索"
              name="q"
              placeholder="搜索文章、动态、系列、用户..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </form>
        )}
        {!frozenSession && (
          <div className="topbar-tools" aria-label="快捷工具">
            {session && (
              <Tooltip title="写文章">
                <Link className="topbar-compose" href="/editor/new">
                  <PenLine size={16} /> <span>写文章</span>
                </Link>
              </Tooltip>
            )}
            {session && (
              <Tooltip title="屏蔽管理">
                <Link aria-label="屏蔽管理" className="icon-button" href="/blocks">
                  <ShieldBan size={18} />
                </Link>
              </Tooltip>
            )}
            <Tooltip title={`当前${theme === "light" ? "浅色" : theme === "dark" ? "深色" : "星空"}主题，点击切换`}>
              <button
                aria-label={`当前${theme === "light" ? "浅色" : theme === "dark" ? "深色" : "星空"}主题，切换主题`}
                className="icon-button"
                onClick={toggleTheme}
                type="button"
              >
                {theme === "light" ? <Sun size={18} /> : theme === "dark" ? <Moon size={18} /> : <Sparkles size={18} />}
              </button>
            </Tooltip>
            <Tooltip title={unreadNotifications ? `通知：${unreadNotifications} 条未读` : "通知"}>
              <Link
                aria-label={`通知${unreadNotifications ? `，${unreadNotifications} 条未读` : ""}`}
                className="icon-button notification-button"
                href={session ? "/notifications" : "/login"}
              >
                <Badge count={unreadNotifications} overflowCount={99} size="small">
                  <Bell size={18} />
                </Badge>
              </Link>
            </Tooltip>
            {session && (
              <Tooltip title="即时聊天">
                <Link aria-label="即时聊天" className="icon-button" href="/chat">
                  <MessageCircle size={18} />
                </Link>
              </Tooltip>
            )}
          </div>
        )}
        {frozenSession && <Link className="secondary-button" href="/account-appeals">提交申诉</Link>}
        {session ? (
          <DropdownMenu>
            <DropdownMenuTrigger render={<button className="user-chip" type="button"><Avatar label={(currentUser?.displayName || session.displayName || session.username || "?").slice(0, 1)} src={currentUser?.avatarFileId ? publicFileUrl(currentUser.avatarFileId) : null} size="sm" /><span>{currentUser?.displayName || session.displayName || session.username}</span><ChevronDown size={14} /></button>} />
            <DropdownMenuContent align="end">
              {userMenuItems.map((item) => ("type" in item && item.type === "divider") ? <DropdownMenuSeparator key="divider" /> : <DropdownMenuItem key={item.key}>{item.label}</DropdownMenuItem>)}
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <Link className="user-chip" href="/login">
            <LogIn size={16} />
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
  teamSlug,
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
  teamSlug?: string;
}) {
  const base = teamSlug ? `/teams/${teamSlug}/workspace` : "/teams";
  const links = [
    ["overview", "团队主页", teamSlug ? `/teams/${teamSlug}` : "/teams"],
    ["submissions", "投稿管理", teamSlug ? `${base}/submissions` : "/submissions"],
    ["articles", "文章管理", teamSlug ? `${base}/content` : "/articles"],
    ["moments", "动态管理", teamSlug ? `${base}/content` : "/moments"],
    ["analytics", "数据统计", "/discover"],
    ["members", "成员管理", teamSlug ? `${base}/members` : "/teams"],
    ["roles", "角色权限", teamSlug ? `${base}/members` : "/teams"],
    ["settings", "团队设置", teamSlug ? `${base}/settings` : "/settings"],
  ] as const;

  return (
    <aside className="workspace-sidebar">
      <Link className="workspace-brand" href={teamSlug ? `/teams/${teamSlug}` : "/teams"}>
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
