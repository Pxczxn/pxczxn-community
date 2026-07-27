"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AlertCircle, ArrowUpRight, BookOpen, Loader2, Users } from "lucide-react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, type TeamSummary } from "../lib/community-api";

export default function TeamsPage() {
  const [teams, setTeams] = useState<TeamSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void communityApi.teams()
      .then((value) => { if (active) setTeams(value); })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : "团队列表加载失败"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  return <>
    <UserTopbar title="团队" />
    <main className="page-shell team-directory">
      <header className="team-directory__header">
        <div>
          <span className="team-directory__eyebrow"><Users size={15} /> 团队空间</span>
          <h1>团队博客</h1>
          <p>浏览团队协作创作的公开文章与系列。</p>
        </div>
        <Link href="/team-applications" className="primary-button">
          申请建立团队 <ArrowUpRight size={16} />
        </Link>
      </header>

      <section className="team-directory__bar" aria-label="团队目录状态">
        <span><Users size={16} /> {loading ? "正在加载团队" : `已收录 ${teams.length} 个公开团队`}</span>
        <span>公开协作创作空间</span>
      </section>

      {loading && <div className="team-directory__loading" aria-live="polite"><Loader2 className="animate-spin" size={22} /> 正在加载团队目录</div>}
      {!loading && error && <section className="team-directory__error" role="alert"><AlertCircle size={20} /><div><strong>团队目录暂时无法加载</strong><p>{error}</p></div></section>}
      {!loading && !error && teams.length === 0 && <section className="team-directory__empty"><Users size={28} /><h2>还没有公开团队</h2><p>团队获批后会在这里展示。</p></section>}
      {!loading && !error && teams.length > 0 && <section className="team-directory__grid" aria-label="公开团队">
        {teams.map((team) => <Link key={team.teamId} href={`/teams/${team.slug}`} className="team-directory__card">
          <span className="team-directory__card-icon"><Users size={19} /></span>
          <ArrowUpRight className="team-directory__arrow" size={18} aria-hidden="true" />
          <h2>{team.name}</h2>
          <p>{team.summary || "这个团队还没有添加简介。"}</p>
          <footer><span><BookOpen size={14} /> {team.articleCount} 篇文章</span><span>{team.followerCount} 位关注者</span></footer>
        </Link>)}
      </section>}
    </main>
  </>;
}
