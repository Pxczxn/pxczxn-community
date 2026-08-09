"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  ArrowRight,
  Bell,
  BookOpen,
  BookOpenCheck,
  Compass,
  Flame,
  LibraryBig,
  Loader2,
  MessageCircle,
  Orbit,
  PenLine,
  Play,
  Settings,
  Sparkles,
  Users,
} from "lucide-react";
import { UserTopbar, Avatar, EmptyState } from "../components/prototype-ui";
import {
  communityApi,
  readSession,
  type Series,
  type FollowingFeedItem,
  type Moment,
  type MyTeam,
  type PublicArticleSummary,
} from "../lib/community-api";
import { readingPercent } from "../lib/series-labels";

/* ───────────────────────── helpers ───────────────────────── */

function timeLabel(value: string) {
  const distance = Date.now() - new Date(value).getTime();
  if (distance < 60_000) return "刚刚";
  if (distance < 3_600_000) return `${Math.max(1, Math.floor(distance / 60_000))} 分钟前`;
  if (distance < 86_400_000) return `${Math.floor(distance / 3_600_000)} 小时前`;
  return `${Math.floor(distance / 86_400_000)} 天前`;
}

function feedLink(item: FollowingFeedItem): string {
  switch (item.itemType) {
    case "ARTICLE":
    case "TAG_ARTICLE":
      return `/articles/${item.targetId}`;
    case "MOMENT":
      return `/moments/${item.targetId}`;
    case "SERIES":
      return `/series/${item.targetId}`;
    default:
      return "#";
  }
}

/* ────────────────────── 子组件 ────────────────────── */

/** 继续阅读卡片 — 复用 series-page 的结构与视觉 */
function ContinueCard({ item }: { item: Series }) {
  const percent = readingPercent(item.viewerReadChapterCount, item.chapterCount);
  const href = item.viewerLastReadArticleId
    ? `/articles/${item.viewerLastReadArticleId}`
    : `/series/${item.id}`;
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
      <div
        className="series-progress"
        role="progressbar"
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
      >
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

/** 关注更新 — 单条动态 */
function FeedListItem({ item }: { item: FollowingFeedItem }) {
  const icon =
    item.itemType === "ARTICLE" || item.itemType === "TAG_ARTICLE" ? (
      <BookOpen size={15} />
    ) : item.itemType === "SERIES" ? (
      <LibraryBig size={15} />
    ) : (
      <Orbit size={15} />
    );
  const typeLabel =
    item.itemType === "ARTICLE" || item.itemType === "TAG_ARTICLE"
      ? "文章"
      : item.itemType === "SERIES"
        ? "连载"
        : "动态";

  return (
    <Link
      className="surface"
      href={feedLink(item)}
      style={{
        display: "flex",
        alignItems: "flex-start",
        gap: 12,
        padding: "12px 16px",
        borderRadius: 10,
        textDecoration: "none",
        color: "var(--text-primary)",
        transition: "background 0.15s",
      }}
    >
      <span style={{ color: "var(--primary)", flexShrink: 0, marginTop: 2 }}>{icon}</span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, lineHeight: 1.5, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
          {item.title}
        </div>
        <div className="muted" style={{ fontSize: 12, marginTop: 2, display: "flex", gap: 8, flexWrap: "wrap" }}>
          <span>{typeLabel}</span>
          {item.authorName && <span>{item.authorName}</span>}
          {item.blogName && <span>{item.blogName}</span>}
          <span>{timeLabel(item.occurredAt)}</span>
        </div>
      </div>
      <ArrowRight size={15} style={{ color: "var(--text-disabled)", flexShrink: 0, marginTop: 4 }} />
    </Link>
  );
}

/** 动态预览卡片 */
function MomentPreviewCard({ item }: { item: Moment }) {
  const name = item.author.displayName || item.author.username;
  return (
    <article
      className="surface"
      style={{ padding: 14, borderRadius: 12, display: "flex", flexDirection: "column", gap: 8 }}
    >
      <Link
        href={`/moments/${item.momentId}`}
        style={{ display: "flex", alignItems: "center", gap: 8, textDecoration: "none", color: "var(--text-primary)" }}
      >
        <Avatar label={name.slice(0, 1)} size="sm" />
        <span style={{ fontWeight: 600, fontSize: 13 }}>{name}</span>
        <span className="muted" style={{ fontSize: 12, marginLeft: "auto" }}>
          {timeLabel(item.createdAt)}
        </span>
      </Link>
      <Link
        href={`/moments/${item.momentId}`}
        style={{ textDecoration: "none", color: "var(--text-secondary)" }}
      >
        <p style={{ margin: 0, fontSize: 13, lineHeight: 1.6 }}>
          {item.textContent?.slice(0, 120) || "查看这条动态"}
          {item.textContent && item.textContent.length > 120 && "…"}
        </p>
      </Link>
      <div style={{ display: "flex", gap: 12, fontSize: 12, color: "var(--text-tertiary)" }}>
        <span>👍 {item.likeCount}</span>
        <span>💬 {item.commentCount}</span>
        <span>来自 {item.blog.name}</span>
      </div>
    </article>
  );
}

/** 团队卡片 */
function TeamCard({ item }: { item: MyTeam }) {
  return (
    <Link
      href={`/teams/${item.slug}`}
      className="surface"
      style={{
        display: "flex",
        alignItems: "center",
        gap: 12,
        padding: "12px 16px",
        borderRadius: 10,
        textDecoration: "none",
        color: "var(--text-primary)",
      }}
    >
      <span
        style={{
          width: 36,
          height: 36,
          borderRadius: 8,
          background: "var(--primary-soft)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontWeight: 700,
          fontSize: 15,
          color: "var(--primary)",
          flexShrink: 0,
        }}
      >
        {(item.name || "?").slice(0, 1)}
      </span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600 }}>{item.name}</div>
        <div className="muted" style={{ fontSize: 12 }}>
          {item.memberCount} 成员 · {item.articleCount} 文章 · {item.viewerRole}
        </div>
      </div>
      <ArrowRight size={15} style={{ color: "var(--text-disabled)" }} />
    </Link>
  );
}

/** 未登录 — 推荐文章卡片 */
function RecommendedArticleCard({ item }: { item: PublicArticleSummary }) {
  const authorName = item.author.displayName || item.author.username;
  return (
    <Link
      href={item.canonicalPath}
      className="surface"
      style={{
        display: "flex",
        flexDirection: "column",
        gap: 8,
        padding: 16,
        borderRadius: 12,
        textDecoration: "none",
        color: "var(--text-primary)",
      }}
    >
      <h3 style={{ margin: 0, fontSize: 15, fontWeight: 600, lineHeight: 1.4 }}>{item.title}</h3>
      {item.summary && (
        <p style={{ margin: 0, fontSize: 13, color: "var(--text-secondary)", lineHeight: 1.6 }}>
          {item.summary.slice(0, 80)}
          {item.summary.length > 80 && "…"}
        </p>
      )}
      <div className="muted" style={{ fontSize: 12, display: "flex", gap: 8, alignItems: "center" }}>
        <span>{authorName}</span>
        <span>·</span>
        <span>{item.readingTimeMinutes} 分钟</span>
        <span>·</span>
        <span>👍 {item.likeCount}</span>
      </div>
    </Link>
  );
}

/* ═══════════════════════ 主组件 ═══════════════════════ */

export function HomePage() {
  const session = readSession();
  const isLoggedIn = !!session;

  /* ── 已登录状态 ── */
  const [readingSeries, setReadingSeries] = useState<Series[]>([]);
  const [followingFeed, setFollowingFeed] = useState<FollowingFeedItem[]>([]);
  const [moments, setMoments] = useState<Moment[]>([]);
  const [teams, setTeams] = useState<MyTeam[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /* ── 未登录状态 ── */
  const [recommendedArticles, setRecommendedArticles] = useState<PublicArticleSummary[]>([]);
  const [hotSeries, setHotSeries] = useState<Series[]>([]);

  useEffect(() => {
    let active = true;

    if (isLoggedIn) {
      Promise.all([
        communityApi.myReadingSeries(6).catch(() => [] as Series[]),
        communityApi.followingFeed(1, 5).catch(() => ({ records: [] as FollowingFeedItem[] })),
        communityApi.moments(1, 5).catch(() => ({ records: [] as Moment[] })),
        communityApi.myTeams().catch(() => [] as MyTeam[]),
        communityApi.unreadNotifications().catch(() => ({ total: 0, categories: {} })),
      ])
        .then(([reading, feed, momentPage, myTeams, notif]) => {
          if (!active) return;
          setReadingSeries(reading);
          setFollowingFeed(feed.records);
          setMoments(momentPage.records);
          setTeams(myTeams);
          setUnreadCount(notif.total);
        })
        .catch((err: unknown) => {
          if (active) setError(err instanceof Error ? err.message : "内容加载失败");
        })
        .finally(() => active && setLoading(false));
    } else {
      Promise.all([
        communityApi.discoverRankedArticles("QUALITY", 1, 5).catch(() => ({ records: [] as PublicArticleSummary[] })),
        communityApi.series().catch(() => [] as Series[]),
      ])
        .then(([articlePage, seriesList]) => {
          if (!active) return;
          setRecommendedArticles(articlePage.records);
          setHotSeries(seriesList.slice(0, 3));
        })
        .catch((err: unknown) => {
          if (active) setError(err instanceof Error ? err.message : "内容加载失败");
        })
        .finally(() => active && setLoading(false));
    }

    return () => { active = false; };
  }, [isLoggedIn]);

  /* ════════════════ 未登录 Portal ════════════════ */
  if (!isLoggedIn) {
    return (
      <>
        <UserTopbar title="首页" />
        <main className="page-shell" style={{ maxWidth: 1100, margin: "0 auto", padding: "0 20px 60px" }}>
          {/* ── Hero ── */}
          <section
            className="surface-lg shadow-sm"
            style={{ borderRadius: 16, padding: "48px 40px", marginTop: 24 }}
          >
            <div style={{ maxWidth: 720 }}>
              <span className="eyebrow">
                <Sparkles size={15} /> 星语社区 Portal
              </span>
              <h1 style={{ margin: "12px 0 0", fontSize: "clamp(28px, 4.5vw, 44px)", fontWeight: 800, lineHeight: 1.15 }}>
                注册即拥有个人博客
              </h1>
              <p style={{ margin: "12px 0 0", fontSize: 16, color: "var(--text-secondary)", lineHeight: 1.6 }}>
                专为技术与思想创作者设计的公开社区。写作、连载、组建团队，这里是沉淀长远价值的精神家园。
              </p>
              <div style={{ display: "flex", gap: 10, marginTop: 20, flexWrap: "wrap" }}>
                <Link className="primary-button" href="/login">
                  <PenLine size={16} /> 开始创作
                </Link>
                <Link className="secondary-button" href="/discover">
                  <Compass size={16} /> 探索内容
                </Link>
              </div>
            </div>
          </section>

          {/* ── Features Grid ── */}
          <section style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: 16, marginTop: 28 }}>
            <Link href="/editor/new" className="surface" style={{ padding: 24, borderRadius: 14, textDecoration: "none", color: "var(--text-primary)", display: "flex", flexDirection: "column", gap: 10, transition: "box-shadow 0.15s" }}>
              <span style={{ width: 44, height: 44, borderRadius: 10, background: "var(--primary-soft)", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--primary)" }}>
                <BookOpen size={22} />
              </span>
              <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700 }}>个人博客与创作中心</h3>
              <p style={{ margin: 0, fontSize: 13, color: "var(--text-secondary)", lineHeight: 1.6 }}>
                注册即拥有独立博客，支持 Markdown 与沉浸式编辑器，轻松管理你的创作。
              </p>
            </Link>
            <Link href="/series" className="surface" style={{ padding: 24, borderRadius: 14, textDecoration: "none", color: "var(--text-primary)", display: "flex", flexDirection: "column", gap: 10, transition: "box-shadow 0.15s" }}>
              <span style={{ width: 44, height: 44, borderRadius: 10, background: "var(--primary-soft)", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--primary)" }}>
                <LibraryBig size={22} />
              </span>
              <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700 }}>团队协作与连载</h3>
              <p style={{ margin: 0, fontSize: 13, color: "var(--text-secondary)", lineHeight: 1.6 }}>
                按章节顺序搭建深度连载与技术书架，方便读者循序渐进地阅读。
              </p>
            </Link>
            <Link href="/moments" className="surface" style={{ padding: 24, borderRadius: 14, textDecoration: "none", color: "var(--text-primary)", display: "flex", flexDirection: "column", gap: 10, transition: "box-shadow 0.15s" }}>
              <span style={{ width: 44, height: 44, borderRadius: 10, background: "var(--primary-soft)", display: "flex", alignItems: "center", justifyContent: "center", color: "var(--primary)" }}>
                <Orbit size={22} />
              </span>
              <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700 }}>极简动态与同频互动</h3>
              <p style={{ margin: 0, fontSize: 13, color: "var(--text-secondary)", lineHeight: 1.6 }}>
                随手发布想法碎片、技术链接与微动态，与全站创作者交流。
              </p>
            </Link>
          </section>

          {/* ── 加载 / 错误 / 内容 ── */}
          {loading && (
            <div className="surface" style={{ padding: 40, textAlign: "center", marginTop: 28, borderRadius: 12 }}>
              <Loader2 size={22} style={{ animation: "spin 1s linear infinite" }} /> 正在加载…
            </div>
          )}
          {error && !loading && (
            <div className="inline-feedback error" style={{ marginTop: 20 }}>{error}</div>
          )}

          {!loading && !error && (
            <div style={{ marginTop: 28 }}>
              {/* 推荐文章 */}
              {recommendedArticles.length > 0 && (
                <section style={{ marginBottom: 32 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 14 }}>
                    <Sparkles size={18} style={{ color: "var(--primary)" }} />
                    <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>推荐文章</h2>
                  </div>
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: 14 }}>
                    {recommendedArticles.map((article) => (
                      <RecommendedArticleCard item={article} key={article.articleId} />
                    ))}
                  </div>
                </section>
              )}

              {/* 热门系列 */}
              {hotSeries.length > 0 && (
                <section>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 14 }}>
                    <Flame size={18} style={{ color: "var(--warning)" }} />
                    <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>热门连载</h2>
                  </div>
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: 14 }}>
                    {hotSeries.map((item) => (
                      <Link
                        href={`/series/${item.id}`}
                        className="surface"
                        key={item.id}
                        style={{ padding: 16, borderRadius: 12, textDecoration: "none", color: "var(--text-primary)", display: "flex", flexDirection: "column", gap: 8 }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                          <BookOpen size={18} style={{ color: "var(--primary)" }} />
                          <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>{item.title}</h3>
                        </div>
                        {item.summary && (
                          <p className="muted" style={{ margin: 0, fontSize: 12, lineHeight: 1.5 }}>
                            {item.summary.slice(0, 60)}
                            {item.summary.length > 60 && "…"}
                          </p>
                        )}
                        <div style={{ display: "flex", gap: 8, fontSize: 12, color: "var(--text-tertiary)" }}>
                          <span>{item.chapterCount} 章</span>
                          <span>·</span>
                          <span>{item.blogName || "未知博客"}</span>
                        </div>
                      </Link>
                    ))}
                  </div>
                </section>
              )}

              {recommendedArticles.length === 0 && hotSeries.length === 0 && (
                <EmptyState title="社区内容即将上线" description="精彩的文章和连载正在路上，敬请期待。" />
              )}
            </div>
          )}
        </main>
      </>
    );
  }

  /* ════════════════ 已登录 Dashboard ════════════════ */
  return (
    <>
      <UserTopbar title="首页" />
      <main className="page-shell" style={{ maxWidth: 1100, margin: "0 auto", padding: "0 20px 60px" }}>
        {/* ── 快速操作条 ── */}
        <div
          style={{
            display: "flex",
            gap: 10,
            marginTop: 24,
            flexWrap: "wrap",
          }}
        >
          <Link className="primary-button" href="/editor/new">
            <PenLine size={15} /> 写文章
          </Link>
          <Link className="secondary-button" href="/moments">
            <Orbit size={15} /> 发布动态
          </Link>
          <Link className="ghost-button" href="/me/series">
            <LibraryBig size={15} /> 我的连载
          </Link>
        </div>

        {/* ── 加载 / 错误 ── */}
        {loading && (
          <div className="surface" style={{ padding: 40, textAlign: "center", marginTop: 20, borderRadius: 12 }}>
            <Loader2 size={22} style={{ animation: "spin 1s linear infinite" }} /> 正在加载…
          </div>
        )}
        {error && !loading && (
          <div className="inline-feedback error" style={{ marginTop: 16 }}>{error}</div>
        )}

        {!loading && (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 280px",
              gap: 24,
              marginTop: 20,
              alignItems: "start",
            }}
          >
            {/* ── 主内容区 ── */}
            <div style={{ display: "flex", flexDirection: "column", gap: 28, minWidth: 0 }}>
              {/* 继续阅读 */}
              {readingSeries.length > 0 && (
                <section className="series-rail" aria-label="继续阅读">
                  <div className="series-chapters-header">
                    <h2 style={{ margin: 0, display: "flex", alignItems: "center", gap: 8 }}>
                      <BookOpenCheck size={20} /> 继续阅读
                    </h2>
                    <span className="series-toolbar__count">从上次停下的地方接着读</span>
                  </div>
                  <div className="series-rail__track">
                    {readingSeries.map((item) => (
                      <ContinueCard item={item} key={item.id} />
                    ))}
                  </div>
                </section>
              )}

              {/* 关注更新 */}
              {followingFeed.length > 0 && (
                <section aria-label="关注更新">
                  <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 10 }}>
                    <Users size={18} style={{ color: "var(--primary)" }} />
                    <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>关注更新</h2>
                    <span className="muted" style={{ fontSize: 12 }}>来自你关注的创作者</span>
                  </div>
                  <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                    {followingFeed.map((item, idx) => (
                      <FeedListItem item={item} key={`${item.itemType}-${item.targetId}-${idx}`} />
                    ))}
                  </div>
                </section>
              )}

              {/* 最新动态 */}
              {moments.length > 0 && (
                <section aria-label="最新动态">
                  <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 10 }}>
                    <MessageCircle size={18} style={{ color: "var(--accent-secondary)" }} />
                    <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>最新动态</h2>
                  </div>
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: 12 }}>
                    {moments.map((item) => (
                      <MomentPreviewCard item={item} key={item.momentId} />
                    ))}
                  </div>
                </section>
              )}

              {/* 我的团队 */}
              {teams.length > 0 && (
                <section aria-label="我的团队">
                  <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 10 }}>
                    <Users size={18} style={{ color: "var(--accent-cyan)" }} />
                    <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>我的团队</h2>
                  </div>
                  <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                    {teams.map((item) => (
                      <TeamCard item={item} key={item.teamId} />
                    ))}
                  </div>
                </section>
              )}

              {/* 空状态 */}
              {!error && readingSeries.length === 0 && followingFeed.length === 0 && moments.length === 0 && (
                <div className="surface" style={{ borderRadius: 12 }}>
                  <EmptyState title="欢迎加入星语社区" description="开始创作你的第一篇文章，关注感兴趣的内容创作者，你的首页将在这里聚合展示。" />
                </div>
              )}
            </div>

            {/* ── 侧栏 ── */}
            <aside
              style={{
                position: "sticky",
                top: 24,
                display: "flex",
                flexDirection: "column",
                gap: 16,
              }}
            >
              {/* 未读通知 */}
              <Link
                href="/notifications"
                className="surface"
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 10,
                  padding: "14px 16px",
                  borderRadius: 12,
                  textDecoration: "none",
                  color: "var(--text-primary)",
                }}
              >
                <span style={{ position: "relative" }}>
                  <Bell size={18} style={{ color: "var(--text-secondary)" }} />
                  {unreadCount > 0 && (
                    <span
                      style={{
                        position: "absolute",
                        top: -6,
                        right: -8,
                        minWidth: 18,
                        height: 18,
                        borderRadius: 9,
                        background: "var(--danger)",
                        color: "#fff",
                        fontSize: 11,
                        fontWeight: 600,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        padding: "0 4px",
                      }}
                    >
                      {unreadCount > 99 ? "99+" : unreadCount}
                    </span>
                  )}
                </span>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 600 }}>通知</div>
                  <div className="muted" style={{ fontSize: 12 }}>
                    {unreadCount > 0 ? `${unreadCount} 条未读` : "暂无未读通知"}
                  </div>
                </div>
              </Link>

              {/* 快捷入口 */}
              <div className="surface" style={{ padding: 16, borderRadius: 12 }}>
                <h3 style={{ margin: "0 0 10px", fontSize: 14, fontWeight: 700 }}>快捷入口</h3>
                <nav style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                  <Link
                    href="/discover"
                    style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0", fontSize: 13, textDecoration: "none", color: "var(--text-primary)", borderRadius: 6 }}
                  >
                    <Compass size={15} style={{ color: "var(--text-tertiary)" }} /> 发现内容
                  </Link>
                  <Link
                    href="/me/series"
                    style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0", fontSize: 13, textDecoration: "none", color: "var(--text-primary)", borderRadius: 6 }}
                  >
                    <LibraryBig size={15} style={{ color: "var(--text-tertiary)" }} /> 我的连载
                  </Link>
                  <Link
                    href="/moments"
                    style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0", fontSize: 13, textDecoration: "none", color: "var(--text-primary)", borderRadius: 6 }}
                  >
                    <Orbit size={15} style={{ color: "var(--text-tertiary)" }} /> 浏览动态
                  </Link>
                  <Link
                    href="/teams"
                    style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0", fontSize: 13, textDecoration: "none", color: "var(--text-primary)", borderRadius: 6 }}
                  >
                    <Users size={15} style={{ color: "var(--text-tertiary)" }} /> 团队
                  </Link>
                  <Link
                    href="/settings"
                    style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0", fontSize: 13, textDecoration: "none", color: "var(--text-primary)", borderRadius: 6 }}
                  >
                    <Settings size={15} style={{ color: "var(--text-tertiary)" }} /> 设置
                  </Link>
                </nav>
              </div>
            </aside>
          </div>
        )}
      </main>
    </>
  );
}