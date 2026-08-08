"use client";

import Link from "next/link";
import {
  ArrowRight,
  BookOpen,
  BookOpenCheck,
  Layers,
  LibraryBig,
  ListTree,
  Loader2,
  Play,
  Sparkles,
  Users,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, readSession, type Series } from "../lib/community-api";
import { blogTypeLabel, readingPercent, serializationLabel } from "../lib/series-labels";

type StatusFilter = "ALL" | "ONGOING" | "COMPLETED" | "PAUSED";

export default function SeriesPage() {
  const [series, setSeries] = useState<Series[]>([]);
  const [reading, setReading] = useState<Series[]>([]);
  const [following, setFollowing] = useState<Series[]>([]);
  const [filter, setFilter] = useState<StatusFilter>("ALL");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [signedIn, setSignedIn] = useState(false);

  useEffect(() => {
    let active = true;
    const session = readSession();
    // 个人书架依赖登录态；游客只拉公开列表，避免制造必然 401 的请求。
    const personal = session
      ? Promise.all([
          communityApi.myReadingSeries(6).catch(() => [] as Series[]),
          communityApi.myFollowedSeries().catch(() => [] as Series[]),
        ])
      : Promise.resolve([[], []] as [Series[], Series[]]);

    void Promise.all([communityApi.series(), personal])
      .then(([all, [readingList, followingList]]) => {
        if (!active) return;
        setSignedIn(Boolean(session));
        setSeries(all);
        setReading(readingList);
        setFollowing(followingList);
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : "无法加载系列");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const summary = useMemo(
    () => ({
      series: series.length,
      chapters: series.reduce((total, item) => total + item.chapterCount, 0),
      ongoing: series.filter((item) => item.serializationStatus === "ONGOING").length,
    }),
    [series],
  );

  const filteredSeries = useMemo(
    () => (filter === "ALL" ? series : series.filter((item) => item.serializationStatus === filter)),
    [series, filter],
  );

  return (
    <>
      <UserTopbar title="系列" />
      <main className="series-page page-shell">
        <section className="series-hero surface-lg shadow-sm">
          <div className="series-hero__copy">
            <span className="eyebrow">
              <LibraryBig size={15} /> 连载系列
            </span>
            <h1>把一篇篇好内容，读成完整的故事</h1>
            <p>这里收录来自个人博客与团队博客的公开连载。按章节顺序展开，方便你从第一篇读到最后一篇。</p>
            <div className="series-hero__actions">
              <Link className="primary-button" href="/discover">
                <Sparkles size={16} /> 去发现文章
              </Link>
              {signedIn && (
                <Link className="ghost-button" href="/me/series">
                  管理我的系列 <ArrowRight size={16} />
                </Link>
              )}
            </div>
          </div>
          <div className="series-hero__stats" aria-label="公开系列概览">
            <div>
              <strong>{summary.series}</strong>
              <span>公开系列</span>
            </div>
            <div>
              <strong>{summary.chapters}</strong>
              <span>公开章节</span>
            </div>
            <div>
              <strong>{summary.ongoing}</strong>
              <span>正在连载</span>
            </div>
          </div>
        </section>

        {!loading && reading.length > 0 && (
          <section className="series-rail" aria-label="继续阅读">
            <div className="series-chapters-header">
              <h2>
                <BookOpenCheck size={20} /> 继续阅读
              </h2>
              <span className="series-toolbar__count">从上次停下的地方接着读</span>
            </div>
            <div className="series-rail__track">
              {reading.map((item) => (
                <ContinueCard item={item} key={item.id} />
              ))}
            </div>
          </section>
        )}

        {!loading && following.length > 0 && (
          <section className="series-rail" aria-label="我的追更">
            <div className="series-chapters-header">
              <h2>
                <Users size={20} /> 我的追更
              </h2>
              <span className="series-toolbar__count">共 {following.length} 个系列</span>
            </div>
            <div className="series-rail__track">
              {following.map((item) => (
                <Link className="series-chip surface" href={`/series/${item.id}`} key={item.id}>
                  <span className="series-chip__title">{item.title}</span>
                  <span className="muted">
                    {serializationLabel(item.serializationStatus)} · {item.chapterCount} 章
                  </span>
                </Link>
              ))}
            </div>
          </section>
        )}

        <section className="series-toolbar" aria-label="系列列表说明">
          <div>
            <span className="eyebrow">
              <ListTree size={15} /> 按最近更新排序
            </span>
            <h2>全部系列</h2>
          </div>
          <div className="series-filters">
            {(
              [
                ["ALL", "全部"],
                ["ONGOING", "连载中"],
                ["COMPLETED", "已完结"],
                ["PAUSED", "已暂停"],
              ] as const
            ).map(([value, label]) => {
              const count = value === "ALL" ? series.length : series.filter((s) => s.serializationStatus === value).length;
              return (
                <button
                  className={`series-filter-btn ${filter === value ? "active" : ""}`}
                  key={value}
                  onClick={() => setFilter(value)}
                  type="button"
                >
                  {label} ({count})
                </button>
              );
            })}
            {!loading && !error && <span className="series-toolbar__count">共 {filteredSeries.length} 个公开系列</span>}
          </div>
        </section>

        {loading && (
          <div className="series-loading surface" aria-busy="true">
            <Loader2 className="animate-spin" size={22} /> 正在整理连载系列…
          </div>
        )}
        {error && (
          <p className="inline-feedback error" role="alert">
            {error}
          </p>
        )}
        {!loading && !error && filteredSeries.length === 0 && (
          <section className="series-empty surface">
            <span className="series-empty__icon">
              <BookOpen size={28} />
            </span>
            <div>
              <h2>{filter === "ALL" ? "还没有公开系列" : "未找到匹配条件的系列"}</h2>
              <p>
                {filter === "ALL"
                  ? "个人博客和团队博客发布并通过审核的连载都会展示在这里。你可以先在发现页阅读已经公开的文章。"
                  : "尝试切换分类过滤器查看其他连载内容。"}
              </p>
            </div>
            <Link className="secondary-button" href="/discover">
              浏览公开文章 <ArrowRight size={16} />
            </Link>
          </section>
        )}
        {!loading && !error && filteredSeries.length > 0 && (
          <section className="series-shelf" aria-label="公开文章系列">
            {filteredSeries.map((item) => (
              <SeriesCard item={item} key={item.id} />
            ))}
          </section>
        )}
      </main>
    </>
  );
}

/** "继续阅读"卡片：唯一目标是让读者一键回到上次那一章。 */
function ContinueCard({ item }: { item: Series }) {
  const percent = readingPercent(item.viewerReadChapterCount, item.chapterCount);
  const href = item.viewerLastReadArticleId ? `/articles/${item.viewerLastReadArticleId}` : `/series/${item.id}`;
  return (
    <article className="series-continue-card surface">
      <div className="series-continue-card__head">
        <span className="series-card__icon">
          <BookOpen size={18} />
        </span>
        <div>
          <h3>{item.title}</h3>
          <span className="muted">
            已读 {item.viewerReadChapterCount}/{item.chapterCount} 章
          </span>
        </div>
      </div>
      <div className="series-progress" role="progressbar" aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100}>
        <span style={{ width: `${percent}%` }} />
      </div>
      <div className="series-continue-card__actions">
        <Link className="primary-button" href={href}>
          <Play size={14} fill="currentColor" /> 继续阅读
        </Link>
        <Link className="ghost-button" href={`/series/${item.id}`}>
          目录
        </Link>
      </div>
    </article>
  );
}

function SeriesCard({ item }: { item: Series }) {
  const percent = readingPercent(item.viewerReadChapterCount, item.chapterCount);
  return (
    <Link className="series-card surface" href={`/series/${item.id}`}>
      <div className="series-card__topline">
        <span className={`series-status series-status--${item.serializationStatus.toLowerCase()}`}>
          {serializationLabel(item.serializationStatus)}
        </span>
        <span className="series-card__chapter-count">
          <Layers size={14} /> {item.chapterCount} 篇章节
        </span>
      </div>
      <span className="series-card__icon">
        <BookOpen size={22} />
      </span>
      <h3>{item.title}</h3>
      <p>{item.summary || "这个系列暂未添加简介。"}</p>
      {percent > 0 && (
        <div className="series-progress series-progress--slim" role="progressbar" aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100}>
          <span style={{ width: `${percent}%` }} />
        </div>
      )}
      <footer>
        <span className="series-card__owner">
          {item.blogName || "未署名博客"}
          <small className="muted"> · {blogTypeLabel(item.blogType)}</small>
        </span>
        <span className="series-card__more">
          {item.followerCount > 0 && (
            <span className="muted">
              <Users size={13} /> {item.followerCount}
            </span>
          )}
          查看系列 <ArrowRight size={15} />
        </span>
      </footer>
    </Link>
  );
}
