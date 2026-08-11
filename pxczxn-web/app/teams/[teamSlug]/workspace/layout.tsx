"use client";

import Link from "next/link";
import { useParams, usePathname, useRouter } from "next/navigation";
import {
  ArrowUpRight,
  BookOpen,
  BookText,
  ClipboardList,
  FileText,
  LayoutDashboard,
  Send,
  Settings,
  Users,
} from "lucide-react";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { Button, Card, Menu, Space, Tag, Typography } from "@/components/ui/community-ui";
import { Avatar, UserTopbar } from "../../../components/prototype-ui";
import { communityApi, publicFileUrl, type MyTeam, type TeamWorkspace } from "../../../lib/community-api";
import { ROLE_LABELS } from "../../team-labels";
import { TeamSwitcher } from "../../team-switcher";
import { TeamError, TeamLoading } from "../../team-ui";
import { WorkspaceContextProvider } from "./workspace-context";

const { Text, Title } = Typography;

interface NavItem {
  key: string;
  label: string;
  href: string;
  icon: typeof LayoutDashboard;
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

const PLANNED_NAV_ITEMS = [
  { key: "TASKS", label: "任务", icon: ClipboardList },
  { key: "DOCS", label: "文档", icon: BookText },
] as const;

export default function TeamWorkspaceLayout({ children }: { children: ReactNode }) {
  const { teamSlug: slug } = useParams<{ teamSlug: string }>();
  const pathname = usePathname();
  const router = useRouter();
  const [workspace, setWorkspace] = useState<TeamWorkspace | null>(null);
  const [myTeams, setMyTeams] = useState<MyTeam[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const load = useCallback((reset: boolean) => {
    if (reset) {
      setLoading(true);
      setError("");
      setWorkspace(null);
    }
    let cancelled = false;
    void communityApi.team(slug)
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
    const timer = window.setTimeout(() => { cleanup = load(true); }, 0);
    return () => { window.clearTimeout(timer); cleanup?.(); };
  }, [load]);

  const team = workspace?.team.team;
  const capabilities = new Set(workspace?.capabilities ?? []);
  const visibleNav = NAV_ITEMS.filter((item) => !item.capability || capabilities.has(item.capability));
  const selectedKey = visibleNav.find((item) => pathname === `/teams/${slug}${item.href}`)?.key ?? "OVERVIEW";

  return (
    <>
      <UserTopbar title="团队" />
      <main className="page-shell workspace-page prototype-team-workspace">
        {loading && <TeamLoading label="正在进入团队工作台…" />}
        {!loading && error && (
          <TeamError
            title={error.includes("Not allowed") || error.includes("权限") ? "你不是该团队成员" : "工作台暂时无法打开"}
            description={error.includes("Not allowed") || error.includes("权限") ? "团队工作台仅向团队成员开放。" : error}
            extra={<Space wrap><Link href={`/teams/${slug}`}><Button>访问公开主页</Button></Link><Link href="/teams"><Button type="primary">返回团队列表</Button></Link></Space>}
          />
        )}
        {!loading && !error && workspace && team && (
          <WorkspaceContextProvider value={{ teamSlug: slug, teamId: team.teamId, workspace, reloadWorkspace: () => load(false) }}>
            <Card className="workspace-header" bordered={false}>
              <div className="workspace-header__identity">
                <Avatar alt={`${team.name}头像`} label={team.name.slice(0, 1)} size="lg" src={publicFileUrl(team.avatarFileId)} />
                <div className="workspace-header__copy">
                  <Space size={8} wrap>
                    <Title level={3}>{team.name}</Title>
                    <Tag color="blue">{ROLE_LABELS[workspace.viewerRole] || workspace.viewerRole}</Tag>
                  </Space>
                  <Text type="secondary">@{team.slug} · 团队协作工作台</Text>
                </div>
              </div>
              <Space wrap className="workspace-header__actions">
                <TeamSwitcher teams={myTeams} currentTeamId={team.teamId} loading={loading} onNavigate={(target) => router.push(`/teams/${target.slug}/workspace`)} />
                <Link href={`/teams/${team.slug}`}><Button icon={<ArrowUpRight size={15} />}>公开主页</Button></Link>
              </Space>
            </Card>
            <Card className="workspace-navigation" bordered={false} bodyStyle={{ padding: 0 }}>
              <nav aria-label="工作台导航">
                <Menu
                  className="workspace-nav"
                  mode="horizontal"
                  selectedKeys={[selectedKey]}
                  items={[
                    ...visibleNav.map((item) => {
                      const Icon = item.icon;
                      return { key: item.key, icon: <Icon size={16} />, label: <Link href={`/teams/${slug}${item.href}`}>{item.label}</Link> };
                    }),
                    ...PLANNED_NAV_ITEMS.map((item) => {
                      const Icon = item.icon;
                      return { key: item.key, disabled: true, icon: <Icon size={16} />, label: <span className="workspace-nav__item--planned">{item.label}<small>规划中</small></span> };
                    }),
                  ]}
                />
              </nav>
            </Card>
            <section className="workspace-content">{children}</section>
          </WorkspaceContextProvider>
        )}
      </main>
    </>
  );
}
