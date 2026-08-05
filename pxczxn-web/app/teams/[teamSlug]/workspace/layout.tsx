"use client";

import Link from "next/link";
import { useParams, usePathname, useRouter } from "next/navigation";
import {
  AlertCircle,
  ArrowUpRight,
  BookOpen,
  FileText,
  LayoutDashboard,
  Loader2,
  Send,
  Settings,
  Users,
} from "lucide-react";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { Avatar, UserTopbar } from "../../../components/prototype-ui";
import { communityApi, publicFileUrl, type MyTeam, type TeamWorkspace } from "../../../lib/community-api";
import { ROLE_LABELS } from "../../team-labels";
import { TeamSwitcher } from "../../team-switcher";
import { WorkspaceContextProvider } from "./workspace-context";

interface NavItem {
  key: string;
  label: string;
  href: string;
  icon: typeof LayoutDashboard;
  /** 需要的 capability；投稿对所有成员可见。 */
  capability?: string;
}

const NAV_ITEMS: NavItem[] = [
  { key: "OVERVIEW", label: "概览", href: "/workspace", icon: LayoutDashboard, capability: "OVERVIEW" },
  { key: "ARTICLES", label: "内容", href: "/workspace/content", icon: FileText, capability: "ARTICLES" },
  { key: "SERIES", label: "系列", href: "/workspace/series", icon: BookOpen, capability: "SERIES" },
  { key: "SUBMISSIONS", label: "投稿", href: "/workspace/submissions", icon: Send },
  { key: "MEMBERS", label: "成员", href: "/workspace/members", icon: Users, capability: "MEMBERS" },
  { key: "SETTINGS", label: "设置", href: "/workspace/settings", icon: Settings, capability: "SETTINGS" },
];

export default function TeamWorkspaceLayout({ children }: { children: ReactNode }) {
  const params = useParams<{ teamSlug: string }>();
  const pathname = usePathname();
  const router = useRouter();
  const slug = params.teamSlug;
  const [workspace, setWorkspace] = useState<TeamWorkspace | null>(null);
  const [myTeams, setMyTeams] = useState<MyTeam[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const load = useCallback((reset: boolean) => {
    if (reset) {
      // 切换团队/首次进入：立即清掉上一个团队的标题与内容，避免短暂残留。
      setLoading(true);
      setError("");
      setWorkspace(null);
    }
    let cancelled = false;
    communityApi.team(slug)
      .then((portal) => Promise.all([
        communityApi.teamWorkspace(portal.team.teamId),
        communityApi.myTeams().catch(() => [] as MyTeam[]),
      ]))
      .then(([value, mine]) => {
        if (!cancelled) {
          setWorkspace(value);
          setMyTeams(mine);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) setError(cause instanceof Error ? cause.message : "工作台加载失败");
      })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [slug]);

  useEffect(() => {
    let cleanup: (() => void) | undefined;
    const timer = window.setTimeout(() => {
      cleanup = load(true);
    }, 0);
    return () => {
      window.clearTimeout(timer);
      cleanup?.();
    };
  }, [load]);

  const capabilities = new Set(workspace?.capabilities ?? []);
  const visibleNav = NAV_ITEMS.filter((item) => !item.capability || capabilities.has(item.capability));
  const team = workspace?.team.team;

  return (
    <>
      <UserTopbar title={team ? team.name : "团队工作台"} />
      <main className="page-shell workspace-page">
        {loading && (
          <div className="series-loading surface" aria-live="polite">
            <Loader2 className="animate-spin" size={22} /> 正在进入团队工作台…
          </div>
        )}

        {!loading && error && (
          <section className="surface inline-feedback error" role="alert">
            <AlertCircle size={20} />
            <div>
              <strong>{error.includes("Not allowed") || error.includes("权限") ? "你不是该团队成员" : "工作台暂时无法打开"}</strong>
              <p>
                {error.includes("Not allowed") || error.includes("权限")
                  ? "团队工作台仅对团队成员开放。可以浏览公开主页，或先接受团队邀请。"
                  : `${error} 请稍后重试，或返回团队列表。`}
              </p>
              <div style={{ display: "flex", gap: 10, marginTop: 6 }}>
                <Link className="secondary-button" href={`/teams/${slug}`}>访问公开主页</Link>
                <Link className="ghost-button" href="/teams">返回团队列表</Link>
              </div>
            </div>
          </section>
        )}

        {!loading && !error && workspace && team && (
          <WorkspaceContextProvider
            value={{
              teamSlug: slug,
              teamId: workspace.team.team.teamId,
              workspace,
              reloadWorkspace: () => load(false),
            }}
          >
            <header className="workspace-header surface">
              <Avatar
                alt={`${team.name}头像`}
                label={team.name.slice(0, 1)}
                size="md"
                src={publicFileUrl(team.avatarFileId)}
              />
              <div className="workspace-header__copy">
                <h1>{team.name}</h1>
                <p className="secondary">
                  @{team.slug} · 你的角色：{ROLE_LABELS[workspace.viewerRole] || workspace.viewerRole}
                </p>
              </div>
              <TeamSwitcher
                teams={myTeams}
                currentTeamId={workspace.team.team.teamId}
                loading={loading}
                onNavigate={(target) => router.push(`/teams/${target.slug}/workspace`)}
              />
              <Link className="ghost-button" href={`/teams/${team.slug}`}>
                公开主页 <ArrowUpRight size={14} />
              </Link>
            </header>

            <div className="workspace-layout">
              <nav className="workspace-nav surface" aria-label="工作台导航">
                {visibleNav.map((item) => {
                  const href = `/teams/${slug}${item.href}`;
                  const active = pathname === href;
                  const Icon = item.icon;
                  return (
                    <Link
                      key={item.key}
                      href={href}
                      className={`workspace-nav__item${active ? " active" : ""}`}
                      aria-current={active ? "page" : undefined}
                    >
                      <Icon size={17} /> {item.label}
                    </Link>
                  );
                })}
              </nav>
              <div className="workspace-content">{children}</div>
            </div>
          </WorkspaceContextProvider>
        )}
      </main>
    </>
  );
}
