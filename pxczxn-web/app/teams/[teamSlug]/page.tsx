"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  ArrowUpRight,
  Loader2,
  Sparkles,
  Users,
} from "lucide-react";
import { Avatar, UserTopbar } from "../../components/prototype-ui";
import {
  communityApi,
  publicFileUrl,
  readSession,
  type MyTeam,
  type PublicArticlePage,
  type TeamPortal,
  type TeamSeries,
} from "../../lib/community-api";
import { formatCount, ROLE_LABELS, SERIALIZATION_LABELS } from "../team-labels";

type PortalTab = "home" | "articles" | "series" | "members" | "about";
const TABS: Array<{ key: PortalTab; label: string }> = [
  { key: "home", label: "主页" },
  { key: "articles", label: "文章" },
  { key: "series", label: "系列" },
  { key: "members", label: "成员" },
  { key: "about", label: "关于" },
];

function tabFromUrl(): PortalTab {
  if (typeof window === "undefined") return "home";
  const value = new URLSearchParams(window.location.search).get("tab");
  return TABS.some((item) => item.key === value) ? (value as PortalTab) : "home";
}

export default function TeamDetailPage() {
  const params = useParams<{ teamSlug: string }>();
  const slug = params.teamSlug;
  const [team, setTeam] = useState<TeamPortal | null>(null);
  const [tab, setTab] = useState<PortalTab>("home");
  const [articles, setArticles] = useState<PublicArticlePage | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [series, setSeries] = useState<TeamSeries[]>([]);
  const [mine, setMine] = useState<MyTeam[] | null>(null);
  const [following, setFollowing] = useState(false);
  const [followBusy, setFollowBusy] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const timer = window.setTimeout(() => setTab(tabFromUrl()), 0);
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    let active = true;
    void communityApi.team(slug)
      .then(async (portal) => {
        if (!active) return;
        setTeam(portal);
        const teamId = portal.team.teamId;
        const jobs: Array<Promise<unknown>> = [
          communityApi.publicArticles(slug, pageNum, 10).then((value) => { if (active) setArticles(value); }),
          communityApi.teamPublicSeries(teamId).then((value) => { if (active) setSeries(value); }),
        ];
        if (readSession()) {
          jobs.push(
            communityApi.myTeams()
              .then((value) => { if (active) setMine(value); })
              .catch(() => { if (active) setMine(null); }),
            communityApi.blogFollowRelationship(portal.team.blogId)
              .then((value) => { if (active) setFollowing(value.following); })
              .catch(() => undefined),
          );
        }
        await Promise.all(jobs);
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : "加载失败");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [slug, pageNum]);

  const isMember = useMemo(
    () => Boolean(mine && team && mine.some((item) => item.teamId === team!.team.teamId)),
    [mine, team],
  );

  async function toggleFollow() {
    if (!team || !readSession()) {
      window.location.assign(`/login?returnTo=${encodeURIComponent(`/teams/${slug}`)}`);
      return;
    }
    setFollowBusy(true);
    try {
      await communityApi.setBlogFollow(team.team.blogId, !following);
      setFollowing((value) => !value);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "关注操作失败，请稍后重试");
    } finally {
      setFollowBusy(false);
    }
  }

  if (loading && !team) {
    return (
      <>
        <UserTopbar title="团队" />
        <main className="page-shell team-detail-loading">
          <Loader2 aria-label="加载团队" className="animate-spin" />
        </main>
      </>
    );
  }

  if (error && !team) {
    return (
      <>
        <UserTopbar title="团队" />
        <main className="page-shell team-detail-page">
          <section className="surface inline-feedback error" role="alert">
            <AlertCircle size={20} />
            <div>
              <strong>团队主页暂时无法打开</strong>
              <p>{error}</p>
              <Link className="secondary-button" href="/teams">返回团队列表</Link>
            </div>
          </section>
        </main>
      </>
    );
  }

  if (!team) return null;

  const teamAvatar = publicFileUrl(team.team.avatarFileId);
  const publicArticles = articles?.records ?? [];

  function selectTab(next: PortalTab) {
    setTab(next);
    window.history.replaceState(null, "", `/teams/${slug}?tab=${next}`);
  }

  return (
    <>
      <UserTopbar title="团队" />
      <main className="page-shell team-detail-page">
        <section className="surface team-detail-hero">
          <Avatar
            alt={`${team.team.name}头像`}
            label={team.team.name.slice(0, 1)}
            size="lg"
            src={teamAvatar}
          />
          <div className="team-detail-hero__copy">
            <span className="eyebrow"><Users size={15} /> 团队空间</span>
            <h1>{team.team.name}</h1>
            <p className="secondary">@{team.team.slug}</p>
            <p>{team.team.summary || "这个团队还没有简介。"}</p>
            <p className="muted">
              由 {team.ownerDisplayName || "团队成员"} 维护 · {team.team.articleCount} 篇文章 · {team.team.followerCount} 位关注者
            </p>
          </div>
          <div className="team-detail-hero__actions">
            {isMember && (
              <Link className="primary-button" href={`/teams/${slug}/workspace`}>
                <Sparkles size={15} /> 进入工作台
              </Link>
            )}
            <button type="button" className={following ? "ghost-button" : "secondary-button"} disabled={followBusy} onClick={() => void toggleFollow()}>
              {following ? "已关注" : "关注团队"}
            </button>
          </div>
        </section>

        <nav className="team-portal-tabs" role="tablist" aria-label="团队主页页签">
          {TABS.map((item) => (
            <button
              type="button"
              role="tab"
              key={item.key}
              aria-selected={tab === item.key}
              className={tab === item.key ? "active" : ""}
              onClick={() => selectTab(item.key)}
            >
              {item.label}
            </button>
          ))}
        </nav>

        {tab === "home" && (
          <div className="team-detail-layout">
            <section className="team-detail-articles">
              <h2>最新文章</h2>
              {publicArticles.map((article) => (
                <Link
                  className="surface team-detail-article"
                  href={`/articles/${article.articleId}`}
                  key={article.articleId}
                >
                  <strong>{article.title}</strong>
                  <p className="secondary">{article.summary}</p>
                </Link>
              ))}
              {publicArticles.length === 0 && <p className="secondary">暂无公开文章。</p>}
            </section>
            <aside className="surface team-detail-members">
              <h2>核心成员</h2>
              {team.members.slice(0, 8).map((member) => {
                const name = member.displayName || member.username;
                return (
                  <div className="team-member-row" key={member.userId}>
                    <Avatar alt={name} label={name.slice(0, 1)} size="sm" src={publicFileUrl(member.avatarFileId)} />
                    <div className="team-member-copy">
                      <strong>{name}</strong>
                      <span>@{member.username}</span>
                    </div>
                    <span className="chip team-member-role">{ROLE_LABELS[member.roleCode] || member.roleCode}</span>
                  </div>
                );
              })}
              {team.members.length > 8 && (
                <button type="button" className="ghost-button" onClick={() => selectTab("members")}>
                  查看全部 {team.members.length} 位成员 <ArrowUpRight size={14} />
                </button>
              )}
            </aside>
          </div>
        )}

        {tab === "articles" && (
          <section className="surface team-portal-panel">
            <header className="workspace-panel__header">
              <h2>团队文章</h2>
              <span className="secondary">共 {articles?.total ?? 0} 篇公开文章</span>
            </header>
            {publicArticles.length === 0 ? (
              <p className="workspace-panel__empty">暂无公开文章。</p>
            ) : (
              <div className="team-portal-article-list">
                {publicArticles.map((article) => (
                  <Link className="team-portal-article" href={`/articles/${article.articleId}`} key={article.articleId}>
                    <strong>{article.title}</strong>
                    <p className="secondary">{article.summary}</p>
                  </Link>
                ))}
              </div>
            )}
            {(() => {
              const currentPage = articles?.pageNum ?? 1;
              const pageSize = articles?.pageSize ?? 10;
              const total = articles?.total ?? 0;
              const hasPrev = currentPage > 1;
              const hasNext = currentPage * pageSize < total;
              if (!hasPrev && !hasNext) return null;
              return (
                <div className="team-portal-pagination">
                  <button type="button" className="ghost-button" disabled={!hasPrev} onClick={() => setPageNum((value) => value - 1)}>
                    上一页
                  </button>
                  <span className="secondary">第 {currentPage} 页 / 共 {total} 篇</span>
                  <button type="button" className="ghost-button" disabled={!hasNext} onClick={() => setPageNum((value) => value + 1)}>
                    下一页
                  </button>
                </div>
              );
            })()}
          </section>
        )}

        {tab === "series" && (
          <section className="surface team-portal-panel">
            <header className="workspace-panel__header">
              <h2>团队系列</h2>
              <span className="secondary">仅展示公开且通过审核的系列</span>
            </header>
            {series.length === 0 ? (
              <p className="workspace-panel__empty">团队还没有公开系列。</p>
            ) : (
              <div className="team-portal-series-grid">
                {series.map((item) => (
                  <div className="surface team-portal-series-card" key={item.id}>
                    <h3>{item.title}</h3>
                    <p className="secondary">{item.summary || "暂无简介"}</p>
                    <p className="muted">
                      {SERIALIZATION_LABELS[item.serializationStatus] || item.serializationStatus} · {item.chapters.length} 章
                    </p>
                    {item.chapters.length > 0 && (
                      <Link className="ghost-button" href={`/series/${item.id}`}>
                        查看系列 <ArrowUpRight size={14} />
                      </Link>
                    )}
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {tab === "members" && (
          <section className="surface team-portal-panel">
            <header className="workspace-panel__header">
              <h2>团队成员</h2>
              <span className="secondary">共 {team.members.length} 位成员</span>
            </header>
            <div className="team-portal-member-grid">
              {team.members.map((member) => {
                const name = member.displayName || member.username;
                return (
                  <div className="team-member-row" key={member.userId}>
                    <Avatar alt={name} label={name.slice(0, 1)} size="sm" src={publicFileUrl(member.avatarFileId)} />
                    <div className="team-member-copy">
                      <strong>{name}</strong>
                      <span>@{member.username}</span>
                    </div>
                    <span className="chip team-member-role">{ROLE_LABELS[member.roleCode] || member.roleCode}</span>
                  </div>
                );
              })}
            </div>
          </section>
        )}

        {tab === "about" && (
          <div className="team-detail-layout">
            <section className="surface team-portal-panel">
              <h2>团队介绍</h2>
              <p>{team.team.summary || "这个团队还没有添加简介。"}</p>
            </section>
            <aside className="surface team-portal-panel">
              <h2>团队数据</h2>
              <div className="team-portal-about-stats">
                <div><strong>{team.team.articleCount}</strong><span>公开文章</span></div>
                <div><strong>{team.members.length}</strong><span>团队成员</span></div>
                <div><strong>{formatCount(Number(team.team.followerCount))}</strong><span>关注者</span></div>
                <div><strong>{series.length}</strong><span>公开系列</span></div>
              </div>
              <p className="muted">
                团队文章保留真实作者归属；想加入团队可以关注团队动态或等待团队邀请。
              </p>
            </aside>
          </div>
        )}
      </main>
    </>
  );
}
