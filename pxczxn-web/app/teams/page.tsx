"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AlertCircle, ArrowUpRight, BookOpen, Loader2, Sparkles, Users } from "lucide-react";
import { Avatar, UserTopbar } from "../components/prototype-ui";
import { communityApi, publicFileUrl, readSession, type TeamSummary } from "../lib/community-api";

export default function TeamsPage() {
  const [teams, setTeams] = useState<TeamSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  function requireLoginForApplication(event: React.MouseEvent<HTMLAnchorElement>) {
    if (readSession()) return;
    event.preventDefault();
    window.location.assign("/login?returnTo=%2Fteam-applications");
  }

  useEffect(() => {
    let active = true;
    void communityApi.teams()
      .then((value) => { if (active) setTeams(value); })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : "团队列表加载失败"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  return (
    <>
      <UserTopbar title="团队" />
      <main className="page-shell series-page">
        <section className="series-hero surface-lg shadow-sm">
          <div className="series-hero__copy">
            <span className="eyebrow"><Users size={15} /> 团队空间</span>
            <h1>团队博客</h1>
            <p>浏览社区内创作者共同搭建的团队博客，沉淀主题专栏与深度技术系列。</p>
            <div className="series-hero__actions">
              <Link href="/team-applications" className="primary-button" onClick={requireLoginForApplication}>
                <Sparkles size={16} /> 申请建立团队
              </Link>
              <Link href="/series" className="ghost-button">
                <BookOpen size={16} /> 连载书架
              </Link>
            </div>
          </div>
          <div className="series-hero__stats" aria-label="公开团队概览">
            <div><strong>{teams.length}</strong><span>公开团队</span></div>
            <div><strong>{teams.reduce((acc, t) => acc + (Number(t.articleCount) || 0), 0)}</strong><span>团队文章</span></div>
            <div><strong>{teams.reduce((acc, t) => acc + (Number(t.followerCount) || 0), 0)}</strong><span>团队关注</span></div>
          </div>
        </section>

        <section className="series-toolbar" aria-label="团队目录状态">
          <div>
            <span className="eyebrow"><Users size={15} /> 公开协作空间</span>
            <h2>全部团队</h2>
          </div>
          {!loading && !error && <span className="series-toolbar__count">已收录 {teams.length} 个公开团队</span>}
        </section>

        {loading && <div className="series-loading surface" aria-live="polite"><Loader2 className="animate-spin" size={22} /> 正在整理团队列表…</div>}
        {!loading && error && (
          <section className="surface inline-feedback error" role="alert">
            <AlertCircle size={20} />
            <div>
              <strong>团队目录暂时无法加载</strong>
              <p>{error}</p>
            </div>
          </section>
        )}
        {!loading && !error && teams.length === 0 && (
          <section className="series-empty surface">
            <span className="series-empty__icon"><Users size={28} /></span>
            <div>
              <h2>还没有公开团队</h2>
              <p>团队获批后会自动展示在这里。你可以发起团队建立申请。</p>
            </div>
            <Link href="/team-applications" className="secondary-button" onClick={requireLoginForApplication}>申请建立团队 <ArrowUpRight size={16} /></Link>
          </section>
        )}
        {!loading && !error && teams.length > 0 && (
          <section className="teams-grid" aria-label="公开团队">
            {teams.map((team) => (
              <Link key={team.teamId} href={`/teams/${team.slug}`} className="team-card surface">
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <Avatar
                      alt={`${team.name}头像`}
                      label={team.name.slice(0, 1)}
                      size="md"
                      src={publicFileUrl(team.avatarFileId)}
                    />
                    <div>
                      <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>{team.name}</h2>
                      <span className="secondary" style={{ fontSize: 12 }}>@{team.slug}</span>
                    </div>
                  </div>
                  <ArrowUpRight className="series-card__more" size={20} />
                </div>
                <p style={{ margin: 0, color: "var(--text-secondary)", fontSize: 14, lineHeight: 1.6, flex: 1 }}>
                  {team.summary || "这个团队还没有添加简介。"}
                </p>
                <div className="discover-feed-card__footer">
                  <div className="discover-feed-card__meta">
                    <span className="discover-feed-card__meta-item"><BookOpen size={13} /> {team.articleCount} 篇公开文章</span>
                    <span className="discover-feed-card__meta-item"><Users size={13} /> {team.followerCount} 位关注者</span>
                  </div>
                </div>
              </Link>
            ))}
          </section>
        )}
      </main>
    </>
  );
}
