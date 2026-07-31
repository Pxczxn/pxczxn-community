"use client";

import Link from "next/link";
import { ArrowRight, BookOpen, Clock3, LibraryBig, ListTree, Loader2, Sparkles } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, type TeamSeries } from "../lib/community-api";

export default function SeriesPage() {
  const [series, setSeries] = useState<TeamSeries[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void communityApi.series()
      .then((items) => { if (active) setSeries(items); })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : "无法加载系列");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const summary = useMemo(() => ({
    series: series.length,
    chapters: series.reduce((total, item) => total + item.chapters.length, 0),
    ongoing: series.filter((item) => item.serializationStatus === "ONGOING").length,
  }), [series]);

  return (
    <>
      <UserTopbar title="系列" />
      <main className="series-page page-shell">
        <section className="series-hero surface-lg shadow-sm">
          <div className="series-hero__copy">
            <span className="eyebrow"><LibraryBig size={15} /> 连载书架</span>
            <h1>把一篇篇好内容，读成完整的故事</h1>
            <p>这里收录已公开的团队系列。按章节顺序展开，方便你从第一篇读到最后一篇。</p>
            <div className="series-hero__actions">
              <Link className="primary-button" href="/discover"><Sparkles size={16} /> 去发现文章</Link>
              <Link className="ghost-button" href="/teams">浏览团队 <ArrowRight size={16} /></Link>
            </div>
          </div>
          <div className="series-hero__stats" aria-label="公开系列概览">
            <div><strong>{summary.series}</strong><span>公开系列</span></div>
            <div><strong>{summary.chapters}</strong><span>公开章节</span></div>
            <div><strong>{summary.ongoing}</strong><span>正在连载</span></div>
          </div>
        </section>

        <section className="series-toolbar" aria-label="系列列表说明">
          <div>
            <span className="eyebrow"><ListTree size={15} /> 按最近更新排序</span>
            <h2>全部系列</h2>
          </div>
          {!loading && !error && <span className="series-toolbar__count">共 {summary.series} 个公开系列</span>}
        </section>

        {loading && <div className="series-loading surface" aria-busy="true"><Loader2 className="animate-spin" size={22} /> 正在整理连载书架…</div>}
        {error && <p className="inline-feedback error" role="alert">{error}</p>}
        {!loading && !error && series.length === 0 && (
          <section className="series-empty surface">
            <span className="series-empty__icon"><BookOpen size={28} /></span>
            <div>
              <h2>还没有公开系列</h2>
              <p>团队发布并通过审核的连载会自动展示在这里。你可以先在发现页阅读已经公开的文章。</p>
            </div>
            <Link className="secondary-button" href="/discover">浏览公开文章 <ArrowRight size={16} /></Link>
          </section>
        )}
        {!loading && !error && series.length > 0 && (
          <section className="series-shelf" aria-label="公开文章系列">
            {series.map((item) => <SeriesCard item={item} key={item.id} />)}
          </section>
        )}
      </main>
    </>
  );
}

function SeriesCard({ item }: { item: TeamSeries }) {
  return (
    <Link className="series-card surface" href={`/series/${item.id}`}>
      <div className="series-card__topline">
        <span className={`series-status series-status--${item.serializationStatus.toLowerCase()}`}>{status(item.serializationStatus)}</span>
        <span className="series-card__chapter-count"><ListTree size={14} /> {item.chapters.length} 篇章节</span>
      </div>
      <span className="series-card__icon"><BookOpen size={22} /></span>
      <h3>{item.title}</h3>
      <p>{item.summary || "这个系列暂未添加简介。"}</p>
      <footer>
        <span><Clock3 size={14} /> 按章节顺序阅读</span>
        <span className="series-card__more">查看系列 <ArrowRight size={15} /></span>
      </footer>
    </Link>
  );
}

function status(value: TeamSeries["serializationStatus"]) {
  return ({ ONGOING: "连载中", COMPLETED: "已完结", PAUSED: "暂缓更新" } as const)[value] || "系列";
}
