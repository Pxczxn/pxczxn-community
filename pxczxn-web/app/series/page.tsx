"use client";

import Link from "next/link";
import {
  ArrowRight,
  BookOpen,
  BookOpenCheck,
  Clock,
  Layers,
  LibraryBig,
  ListTree,
  Loader2,
  Play,
  Users,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, readSession, type Series } from "../lib/community-api";
import { blogTypeLabel, readingPercent, serializationLabel } from "../lib/series-labels";

type StatusFilter = "ALL" | "ONGOING" | "COMPLETED";

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
        if (active) setError(cause instanceof Error ? cause.message : "无法加载连载");
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

  const ongoingSeries = useMemo(
    () => series.filter((item) => item.serializationStatus === "ONGOING"),
    [series],
  );

  const completedSeries = useMemo(
    () => series.filter((item) => item.serializationStatus === "COMPLETED"),
    [series],
  );

  return (
    <>
      <UserTopbar title="书架" />
      <main className="series-page page-shell">
        {/* ── 紧凑标题行（弱化 Hero） ── */}
        <section className="series-page__header">
          <div style={{ display: "flex", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
            <h1 style={{ fontSize: "1.25rem", fontWeight: 700, margin: 0 }}>
              <LibraryBig size={20} style={{ verticalAlign: -3, marginRight: 6 }} />
              书架
            </h1>
            <span className="muted" style={{ fontSize: "0.85rem" }}>
              {summary.series} 部连载 · {summary.chapters} 章 · {summary.ongoing} 部连载中
            </span>
          </div>
          {signedIn && (
            <Link className="ghost-button" href="/me/series" style={{ fontSize: "0.85rem", whiteSpace: "nowrap" }}>
              管理连载 <ArrowRight size={14} />
            </Link>
          )}
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
              <span className="series-toolbar__count">共 {following.length} 部连载</span>
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

        {/* ── 正在连载 ── */}
        {!loading && ongoingSeries.length > 0 && (
          <section className="series-toolbar" aria-label="正在连载">
            <div>
              <span className="eyebrow">
                <Play size={15} /> 正在连载
              </span>
              <h2>连载中 · {ongoingSeries.length} 部</h2>
            </div>
          </section>
        )}
        {!loading && ongoingSeries.length > 0 && (
          <section className="series-shelf" aria-label="连载中的作品">
            {ongoingSeries.map((item) => (
              <SeriesCard item={item} key={`ongoing-${item.id}`} />
            ))}
          </section>
        )}

        {/* ── 最近完结 ── */}
        {!loading && completedSeries.length > 0 && (
          <section className="series-toolbar" aria-label="最近完结">
            <div>
              <span className="eyebrow">
                <BookOpenCheck size={15} /> 最近完结
              </span>
              <h2>已完结 · {completedSeries.length} 部</h2>
            </div>
          </section>
        )}
        {!loading && completedSeries.length > 0 && (
          <section className="series-shelf" aria-label="已完结的连载">
            {completedSeries.map((item) => (
              <SeriesCard item={item} key={`completed-${item.id}`} />
            ))}
          </section>
        )}

        <section className="series-toolbar" aria-label="连载列表说明">
          <div>
            <span className="eyebrow">
              <ListTree size={15} /> 按最近更新排序
            </span>
            <h2>全部连载</h2>
          </div>
          <div className="series-filters">
            {(
              [
                ["ALL", "全部"],
                ["ONGOING", "连载中"],
                ["COMPLETED", "已完结"],
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
            {!loading && !error && <span className="series-toolbar__count">共 {filteredSeries.length} 部公开连载</span>}
          </div>
        </section>

        {loading && (
          <div className="series-loading surface" aria-busy="true">
            <Loader2 className="animate-spin" size={22} /> 正在整理书架…
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
              <h2>{filter === "ALL" ? "还没有公开连载" : "未找到匹配条件的连载"}</h2>
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
          <section className="series-shelf" aria-label="公开连载作品">
            {filteredSeries.map((item) => (
              <SeriesCard item={item} key={item.id} />
            ))}
          </section>
        )}
      </main>
    </>
  );
}

/** 将 ISO 时间字符串格式化为相对时间（如"3 天前"）。 */
function formatRelativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "刚刚";
  if (mins < 60) return `${mins} 分钟前`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} 小时前`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days} 天前`;
  const months = Math.floor(days / 30);
  if (months < 12) return `${months} 个月前`;
  return `${Math.floor(months / 12)} 年前`;
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
        <span className="series-card__chapter-count" style={{ fontWeight: 600, fontSize: "0.9rem" }}>
          <Layers size={15} /> {item.chapterCount} 章
        </span>
      </div>
      <span className="series-card__icon">
        <BookOpen size={22} />
      </span>
      <h3>{item.title}</h3>
      <p>{item.summary || "这部连载暂未添加简介。"}</p>
      <div className="series-progress series-progress--slim" role="progressbar" aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100}>
        <span style={{ width: `${percent}%` }} />
      </div>
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
          <span className="muted" style={{ fontSize: "0.78rem", marginRight: 6 }}>
            <Clock size={12} /> {formatRelativeTime(item.updatedAt)}
          </span>
          查看连载 <ArrowRight size={15} />
        </span>
      </footer>
    </Link>
  );
}
