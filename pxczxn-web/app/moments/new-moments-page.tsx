/**
 * NewMomentsPage - Stitch v2.1_17 Three-Column Timeline
 *
 * Three columns on lg screens:
 *  - 280px left  : category nav, filters, pinned highlights
 *  - 600px center: composer + chronological moment feed
 *  - 320px right : trending tags / hot discussions / suggested follows
 * Uses --sw-moment-accent as the visual anchor.
 */

"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import {
  AtSign,
  Bookmark,
  ChevronRight,
  Compass,
  Flame,
  Hash,
  Heart,
  Image as ImageIcon,
  MessageCircle,
  Orbit,
  PenLine,
  Pin,
  Radio,
  Repeat2,
  Search,
  Send,
  Smile,
  Sparkles,
  TrendingUp,
  Users,
} from "lucide-react";
import { AppNavbar } from "@/components/ui/navbar";

/* ─────────────────────────── Mock Data ─────────────────────────── */

type Author = {
  id: string;
  name: string;
  handle: string;
  avatar: string;
};

type Moment = {
  id: string;
  author: Author;
  content: string;
  image?: string;
  publishedAt: string;
  topic?: string;
  stats: { likes: number; comments: number; reposts: number; bookmarks: number };
  isFollowingAuthor?: boolean;
  isPinned?: boolean;
};

const AUTHORS: Record<string, Author> = {
  lin: { id: "lin", name: "林书豪", handle: "lin.sh", avatar: "林" },
  chen: { id: "chen", name: "陈知行", handle: "chen.zx", avatar: "陈" },
  su: { id: "su", name: "苏文慧", handle: "su.wh", avatar: "苏" },
  wang: { id: "wang", name: "王启明", handle: "wang.qm", avatar: "王" },
  gao: { id: "gao", name: "高志远", handle: "gao.zy", avatar: "高" },
  he: { id: "he", name: "何梓琪", handle: "he.zq", avatar: "何" },
  luo: { id: "luo", name: "罗子涵", handle: "luo.zh", avatar: "罗" },
  cai: { id: "cai", name: "蔡安然", handle: "cai.ar", avatar: "蔡" },
};

const FEED: Moment[] = [
  {
    id: "m-1",
    author: AUTHORS.lin,
    content:
      "刚把团队内部做了 6 个月的 RSC 改造总结成一篇长文：协议、缓存、Suspense 边界、踩坑全写进去了。下一篇会聊一下 Server Actions 的权限模型。",
    publishedAt: "5 分钟前",
    topic: "前端深潜",
    stats: { likes: 142, comments: 38, reposts: 19, bookmarks: 71 },
    isPinned: true,
  },
  {
    id: "m-2",
    author: AUTHORS.luo,
    content:
      "把 Agent 的评测集换成了基于 LLM-as-judge 的多维度评分，效果比想象中稳。下一步准备做置信区间可视化，欢迎有经验的朋友一起聊聊。",
    image: "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=900&h=400&fit=crop",
    publishedAt: "18 分钟前",
    topic: "AI 工坊",
    stats: { likes: 88, comments: 24, reposts: 12, bookmarks: 30 },
    isFollowingAuthor: true,
  },
  {
    id: "m-3",
    author: AUTHORS.chen,
    content:
      "PostgreSQL 16 logical replication 的冲突检测在 8h 压测中表现稳定，记录一下监控面板的关键指标。",
    publishedAt: "42 分钟前",
    topic: "数据库札记",
    stats: { likes: 56, comments: 11, reposts: 5, bookmarks: 18 },
  },
  {
    id: "m-4",
    author: AUTHORS.su,
    content:
      "为团队整理的 A11y 检查清单更新到 v3，新增 keyboard trap 与 prefers-reduced-motion 两块内容，欢迎拍砖。",
    publishedAt: "1 小时前",
    topic: "UX 工具箱",
    stats: { likes: 73, comments: 16, reposts: 21, bookmarks: 44 },
  },
  {
    id: "m-5",
    author: AUTHORS.gao,
    content:
      "刚帮朋友的创业团队 review 了一套监控方案：eBPF + OpenTelemetry 是真的香，但门槛确实在，建议小团队先从 OTEL 入手。",
    publishedAt: "2 小时前",
    topic: "性能观察",
    stats: { likes: 64, comments: 9, reposts: 4, bookmarks: 11 },
  },
  {
    id: "m-6",
    author: AUTHORS.wang,
    content:
      "我们把 Node 调度器迁到 Rust 之后，平均延迟从 18ms 降到 4.7ms。正在写复盘，月底前会发出来。",
    publishedAt: "3 小时前",
    topic: "工程手记",
    stats: { likes: 132, comments: 27, reposts: 18, bookmarks: 53 },
    isFollowingAuthor: true,
  },
  {
    id: "m-7",
    author: AUTHORS.cai,
    content:
      "CSS Anchor Positioning 已经能在所有主流浏览器中跑通一个简单 demo，准备下个月在公司内部分享。",
    publishedAt: "4 小时前",
    topic: "前端深潜",
    stats: { likes: 41, comments: 7, reposts: 3, bookmarks: 14 },
  },
  {
    id: "m-8",
    author: AUTHORS.he,
    content:
      "SwiftUI 中 @Observable 的依赖追踪比想象中更细致，但也更容易写出死循环，明天录个视频细聊。",
    publishedAt: "昨天",
    topic: "iOS 周刊",
    stats: { likes: 29, comments: 6, reposts: 2, bookmarks: 8 },
  },
];

const CATEGORIES = [
  { id: "all", label: "全部动态", icon: Compass, count: FEED.length },
  { id: "following", label: "我的关注", icon: Users, count: 36 },
  { id: "live", label: "直播与连麦", icon: Radio, count: 4 },
  { id: "topics", label: "话题广场", icon: Hash, count: 18 },
];

const TOPICS = [
  { id: "frontend", label: "前端深潜", count: 12, color: "var(--sw-article-accent)" },
  { id: "ai", label: "AI 工坊", count: 8, color: "var(--sw-secondary)" },
  { id: "backend", label: "数据库札记", count: 6, color: "var(--sw-team-accent)" },
  { id: "design", label: "UX 工具箱", count: 5, color: "var(--sw-series-accent)" },
  { id: "mobile", label: "iOS 周刊", count: 4, color: "var(--sw-moment-accent)" },
];

const TRENDING_HASHTAGS = [
  { tag: "RSC", volume: "12.4k" },
  { tag: "Rust2025", volume: "9.1k" },
  { tag: "LLM评测", volume: "7.6k" },
  { tag: "PostgreSQL16", volume: "5.8k" },
  { tag: "WebGPU", volume: "4.2k" },
];

const HOT_DISCUSSIONS = [
  {
    title: "你们团队的 RSC 边界怎么划？",
    replies: 86,
    excerpt: "Suspense 边界、数据预取、Server Actions 三者如何协同…",
  },
  {
    title: "在 Bun 与 Node 之间反复横跳",
    replies: 62,
    excerpt: "Bun 1.2 的生态真的可以接住生产负载了吗？",
  },
  {
    title: "LLM-as-judge 的局限性",
    replies: 47,
    excerpt: "如何避免 judge 模型自身的偏差污染评分？",
  },
];

const SUGGESTED_FOLLOWS = [
  { name: "段星河", handle: "duan.xh", avatar: "段", focus: "图形学" },
  { name: "邵一鸣", handle: "shao.ym", avatar: "邵", focus: "运行时" },
  { name: "潘清漪", handle: "pan.qy", avatar: "潘", focus: "设计系统" },
];

/* ─────────────────────────── Sub-Components ─────────────────────────── */

function NavItem({
  id,
  label,
  icon: Icon,
  count,
  active,
  onClick,
}: {
  id: string;
  label: string;
  icon: React.ElementType;
  count: number;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="sw-focus-ring"
      style={{
        display: "flex",
        alignItems: "center",
        gap: 10,
        width: "100%",
        padding: "10px 12px",
        borderRadius: "var(--sw-radius-md)",
        background: active ? "var(--sw-surface-container-high)" : "transparent",
        color: active ? "var(--sw-moment-accent)" : "var(--sw-primary)",
        fontSize: "var(--sw-text-sm)",
        fontWeight: active ? 700 : 500,
        border: "none",
        cursor: "pointer",
        textAlign: "left",
      }}
    >
      <Icon size={16} style={{ color: active ? "var(--sw-moment-accent)" : "var(--sw-outline)" }} />
      <span style={{ flex: 1 }}>{label}</span>
      <span
        className="sw-label"
        style={{
          color: "var(--sw-outline)",
          fontSize: "11px",
          fontVariantNumeric: "tabular-nums",
        }}
      >
        {count}
      </span>
    </button>
  );
}

function MomentCard({ moment }: { moment: Moment }) {
  return (
    <article
      className="sw-card"
      style={{
        padding: 20,
        display: "flex",
        flexDirection: "column",
        gap: 12,
        ...(moment.isPinned
          ? { borderLeft: "3px solid var(--sw-moment-accent)" }
          : {}),
      }}
    >
      {moment.isPinned && (
        <div
          className="flex items-center gap-1"
          style={{ color: "var(--sw-moment-accent)", fontSize: "var(--sw-text-xs)" }}
        >
          <Pin size={12} />
          <span className="sw-label">置顶</span>
        </div>
      )}

      <header className="flex items-center gap-3">
        <div
          className="sw-avatar sw-avatar-sm"
          style={{
            background: "var(--sw-moment-accent)",
            color: "var(--sw-on-secondary)",
            fontSize: "12px",
          }}
        >
          {moment.author.avatar}
        </div>
        <div className="flex flex-col" style={{ minWidth: 0 }}>
          <div className="flex items-center gap-2">
            <span className="sw-label" style={{ color: "var(--sw-primary)" }}>
              {moment.author.name}
            </span>
            <span
              className="sw-label"
              style={{ color: "var(--sw-outline)", fontSize: "12px" }}
            >
              @{moment.author.handle}
            </span>
          </div>
          <span
            className="sw-label"
            style={{ color: "var(--sw-outline)", fontSize: "11px" }}
          >
            {moment.publishedAt}
            {moment.topic && (
              <>
                <span style={{ margin: "0 6px" }}>·</span>
                <span style={{ color: "var(--sw-moment-accent)" }}>#{moment.topic}</span>
              </>
            )}
          </span>
        </div>
        <button
          type="button"
          className="sw-focus-ring"
          style={{
            marginLeft: "auto",
            padding: "4px 10px",
            borderRadius: "var(--sw-radius-full)",
            background: moment.isFollowingAuthor ? "transparent" : "var(--sw-moment-accent)",
            color: moment.isFollowingAuthor ? "var(--sw-moment-accent)" : "var(--sw-on-secondary)",
            border: moment.isFollowingAuthor ? "1px solid var(--sw-moment-accent)" : "none",
            fontSize: "var(--sw-text-xs)",
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          {moment.isFollowingAuthor ? "已关注" : "+ 关注"}
        </button>
      </header>

      <p
        className="sw-body"
        style={{
          color: "var(--sw-primary)",
          margin: 0,
          lineHeight: 1.7,
          fontSize: "var(--sw-text-base)",
        }}
      >
        {moment.content}
      </p>

      {moment.image && (
        <div
          style={{
            borderRadius: "var(--sw-radius-md)",
            overflow: "hidden",
            border: "1px solid var(--sw-outline-variant)",
          }}
        >
          <img
            src={moment.image}
            alt="动态配图"
            style={{ width: "100%", display: "block", objectFit: "cover", maxHeight: 360 }}
          />
        </div>
      )}

      <footer
        className="flex items-center justify-between"
        style={{ color: "var(--sw-outline)" }}
      >
        <div className="flex items-center gap-4">
          <button
            type="button"
            className="sw-focus-ring"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 4,
              background: "transparent",
              border: "none",
              color: "inherit",
              fontSize: "var(--sw-text-xs)",
              cursor: "pointer",
            }}
          >
            <Heart size={14} /> {moment.stats.likes}
          </button>
          <button
            type="button"
            className="sw-focus-ring"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 4,
              background: "transparent",
              border: "none",
              color: "inherit",
              fontSize: "var(--sw-text-xs)",
              cursor: "pointer",
            }}
          >
            <MessageCircle size={14} /> {moment.stats.comments}
          </button>
          <button
            type="button"
            className="sw-focus-ring"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 4,
              background: "transparent",
              border: "none",
              color: "inherit",
              fontSize: "var(--sw-text-xs)",
              cursor: "pointer",
            }}
          >
            <Repeat2 size={14} /> {moment.stats.reposts}
          </button>
          <button
            type="button"
            className="sw-focus-ring"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 4,
              background: "transparent",
              border: "none",
              color: "inherit",
              fontSize: "var(--sw-text-xs)",
              cursor: "pointer",
            }}
          >
            <Bookmark size={14} /> {moment.stats.bookmarks}
          </button>
        </div>
        <Link
          href={`/moments/${moment.id}`}
          className="sw-focus-ring"
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: 4,
            color: "var(--sw-moment-accent)",
            fontSize: "var(--sw-text-xs)",
            fontWeight: 600,
            textDecoration: "none",
          }}
        >
          详情 <ChevronRight size={12} />
        </Link>
      </footer>
    </article>
  );
}

function Composer({ defaultTopic }: { defaultTopic: string }) {
  const [value, setValue] = useState("");
  const maxLength = 280;
  const remaining = maxLength - value.length;
  return (
    <section
      className="sw-card"
      style={{
        padding: 16,
        display: "flex",
        flexDirection: "column",
        gap: 12,
        borderTop: "3px solid var(--sw-moment-accent)",
      }}
      aria-label="发布动态"
    >
      <div className="flex items-start gap-3">
        <div
          className="sw-avatar sw-avatar-sm"
          style={{
            background: "var(--sw-moment-accent)",
            color: "var(--sw-on-secondary)",
            fontSize: "12px",
            flexShrink: 0,
          }}
        >
          我
        </div>
        <div style={{ flex: 1 }}>
          <textarea
            value={value}
            onChange={(e) => setValue(e.target.value.slice(0, maxLength))}
            placeholder={`说点什么吧…  话题：${defaultTopic}`}
            className="sw-input"
            rows={3}
            style={{
              resize: "vertical",
              minHeight: 84,
              fontFamily: "inherit",
              lineHeight: 1.6,
            }}
          />
          <div
            className="flex items-center justify-between"
            style={{ marginTop: 8, color: "var(--sw-outline)" }}
          >
            <div className="flex items-center gap-3">
              <button
                type="button"
                className="sw-focus-ring"
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: 4,
                  background: "transparent",
                  border: "none",
                  color: "var(--sw-outline)",
                  cursor: "pointer",
                  fontSize: "var(--sw-text-xs)",
                }}
              >
                <ImageIcon size={14} /> 图片
              </button>
              <button
                type="button"
                className="sw-focus-ring"
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: 4,
                  background: "transparent",
                  border: "none",
                  color: "var(--sw-outline)",
                  cursor: "pointer",
                  fontSize: "var(--sw-text-xs)",
                }}
              >
                <AtSign size={14} /> 提及
              </button>
              <button
                type="button"
                className="sw-focus-ring"
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: 4,
                  background: "transparent",
                  border: "none",
                  color: "var(--sw-outline)",
                  cursor: "pointer",
                  fontSize: "var(--sw-text-xs)",
                }}
              >
                <Smile size={14} /> 表情
              </button>
            </div>
            <div className="flex items-center gap-3">
              <span
                className="sw-label"
                style={{ color: remaining < 20 ? "var(--sw-moment-accent)" : "var(--sw-outline)" }}
              >
                {remaining}
              </span>
              <button
                type="button"
                disabled={value.trim().length === 0}
                className="sw-button sw-button-primary sw-focus-ring"
                style={{
                  background: "var(--sw-moment-accent)",
                  color: "var(--sw-on-secondary)",
                  padding: "8px 16px",
                  opacity: value.trim().length === 0 ? 0.5 : 1,
                }}
              >
                <Send size={14} /> 发布
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function SectionTitle({
  title,
  icon: Icon,
  count,
}: {
  title: string;
  icon: React.ElementType;
  count?: string | number;
}) {
  return (
    <div
      className="flex items-center gap-2 pb-3"
      style={{ borderBottom: "1px solid var(--sw-outline-variant)" }}
    >
      <Icon size={16} style={{ color: "var(--sw-moment-accent)" }} />
      <h3 className="sw-heading-sm" style={{ color: "var(--sw-primary)" }}>
        {title}
      </h3>
      {count !== undefined && (
        <span
          className="sw-label"
          style={{ color: "var(--sw-outline)", fontSize: "12px", marginLeft: "auto" }}
        >
          {count}
        </span>
      )}
    </div>
  );
}

/* ─────────────────────────── Main Component ─────────────────────────── */

export default function NewMomentsPage() {
  const [activeCategory, setActiveCategory] = useState("all");
  const [activeTopic, setActiveTopic] = useState<string>("all");

  const filtered = useMemo(() => {
    return FEED.filter((m) => {
      if (activeTopic !== "all" && m.topic !== activeTopic) return false;
      if (activeCategory === "following" && !m.isFollowingAuthor) return false;
      return true;
    });
  }, [activeCategory, activeTopic]);

  const pinned = filtered.find((m) => m.isPinned);
  const rest = filtered.filter((m) => !m.isPinned);

  return (
    <div
      style={{
        background: "var(--sw-surface)",
        minHeight: "100vh",
      }}
    >
      <AppNavbar />

      <main className="page-shell py-8">
        {/* Header */}
        <header className="mb-6">
          <span className="sw-eyebrow" style={{ color: "var(--sw-moment-accent)" }}>
            <Orbit size={14} style={{ display: "inline", marginRight: 6, verticalAlign: -2 }} />
            动态 / Moments
          </span>
          <div className="flex flex-wrap items-end justify-between gap-4 mt-2">
            <div>
              <h1 className="sw-heading-lg" style={{ color: "var(--sw-primary)" }}>
                三栏时刻流
              </h1>
              <p
                className="sw-body"
                style={{
                  color: "var(--sw-on-primary-container)",
                  marginTop: 8,
                  marginBottom: 0,
                }}
              >
                280 · 600 · 320 黄金分割：导航 / 信息流 / 趋势互不打扰。
              </p>
            </div>
            <Link
              href="/moments/new"
              className="sw-button sw-button-secondary sw-focus-ring"
              style={{ textDecoration: "none", background: "var(--sw-moment-accent)" }}
            >
              <PenLine size={16} /> 写一条动态
            </Link>
          </div>
        </header>

        {/* Three-column grid */}
        <div
          className="grid"
          style={{
            gridTemplateColumns: "minmax(0, 280px) minmax(0, 1fr) minmax(0, 320px)",
            gap: 24,
            alignItems: "start",
          }}
        >
          {/* Left column — 280px */}
          <aside
            className="flex flex-col gap-4"
            style={{ position: "sticky", top: 88 }}
            aria-label="导航与筛选"
          >
            <div className="sw-card" style={{ padding: 16 }}>
              <SectionTitle title="动态分类" icon={Compass} />
              <div className="flex flex-col gap-1 pt-3">
                {CATEGORIES.map((cat) => (
                  <NavItem
                    key={cat.id}
                    id={cat.id}
                    label={cat.label}
                    icon={cat.icon}
                    count={cat.count}
                    active={activeCategory === cat.id}
                    onClick={() => setActiveCategory(cat.id)}
                  />
                ))}
              </div>
            </div>

            <div className="sw-card" style={{ padding: 16 }}>
              <SectionTitle title="话题" icon={Hash} count={TOPICS.length} />
              <div className="flex flex-col gap-1 pt-3">
                <button
                  type="button"
                  onClick={() => setActiveTopic("all")}
                  className="sw-focus-ring"
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    padding: "8px 12px",
                    borderRadius: "var(--sw-radius-md)",
                    background: activeTopic === "all" ? "var(--sw-surface-container-high)" : "transparent",
                    border: "none",
                    cursor: "pointer",
                    color: "var(--sw-primary)",
                    fontSize: "var(--sw-text-sm)",
                    fontWeight: activeTopic === "all" ? 700 : 500,
                  }}
                >
                  <span>全部话题</span>
                  <span style={{ color: "var(--sw-outline)", fontSize: 12 }}>
                    {FEED.length}
                  </span>
                </button>
                {TOPICS.map((t) => (
                  <button
                    key={t.id}
                    type="button"
                    onClick={() => setActiveTopic(t.label)}
                    className="sw-focus-ring"
                    style={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      padding: "8px 12px",
                      borderRadius: "var(--sw-radius-md)",
                      background: activeTopic === t.label ? "var(--sw-surface-container-high)" : "transparent",
                      border: "none",
                      cursor: "pointer",
                      color: "var(--sw-primary)",
                      fontSize: "var(--sw-text-sm)",
                      fontWeight: activeTopic === t.label ? 700 : 500,
                    }}
                  >
                    <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
                      <span
                        style={{
                          width: 6,
                          height: 6,
                          borderRadius: "var(--sw-radius-full)",
                          background: t.color,
                        }}
                      />
                      {t.label}
                    </span>
                    <span style={{ color: "var(--sw-outline)", fontSize: 12 }}>{t.count}</span>
                  </button>
                ))}
              </div>
            </div>

            <div
              className="sw-card"
              style={{
                padding: 16,
                background: "var(--sw-surface-container-low)",
              }}
            >
              <div className="flex items-center gap-2">
                <Sparkles size={16} style={{ color: "var(--sw-moment-accent)" }} />
                <span className="sw-heading-sm" style={{ color: "var(--sw-primary)" }}>
                  每日灵感
                </span>
              </div>
              <p
                className="sw-body"
                style={{
                  color: "var(--sw-on-primary-container)",
                  fontSize: "var(--sw-text-sm)",
                  marginTop: 8,
                  marginBottom: 0,
                }}
              >
                写点什么，会比想象中让你更接近自己的真实想法。
              </p>
            </div>
          </aside>

          {/* Center column — 600px (max-width) */}
          <section
            className="flex flex-col gap-4"
            style={{ maxWidth: 640, width: "100%" }}
            aria-label="动态信息流"
          >
            <Composer defaultTopic={activeTopic === "all" ? "随想" : activeTopic} />

            {pinned && <MomentCard moment={pinned} />}

            {rest.map((m) => (
              <MomentCard key={m.id} moment={m} />
            ))}

            {filtered.length === 0 && (
              <div
                className="sw-card"
                style={{
                  padding: 40,
                  textAlign: "center",
                  color: "var(--sw-on-primary-container)",
                }}
              >
                <p className="sw-body" style={{ margin: 0 }}>
                  当前筛选下还没有动态，试试切换分类或话题。
                </p>
              </div>
            )}
          </section>

          {/* Right column — 320px */}
          <aside
            className="flex flex-col gap-4"
            style={{ position: "sticky", top: 88 }}
            aria-label="趋势与互动"
          >
            <div className="sw-card" style={{ padding: 16 }}>
              <div
                className="sw-input"
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                  padding: "8px 12px",
                }}
              >
                <Search size={14} style={{ color: "var(--sw-outline)" }} />
                <input
                  type="text"
                  placeholder="搜索动态、用户、话题…"
                  style={{
                    flex: 1,
                    border: "none",
                    outline: "none",
                    background: "transparent",
                    color: "var(--sw-primary)",
                    fontSize: "var(--sw-text-sm)",
                  }}
                />
              </div>
            </div>

            <div className="sw-card" style={{ padding: 16 }}>
              <SectionTitle title="热门话题" icon={Flame} count="24h" />
              <div className="flex flex-col pt-3">
                {TRENDING_HASHTAGS.map((t, idx) => (
                  <Link
                    key={t.tag}
                    href={`/moments?tag=${encodeURIComponent(t.tag)}`}
                    className="sw-focus-ring"
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 10,
                      padding: "8px 4px",
                      borderRadius: "var(--sw-radius-md)",
                      textDecoration: "none",
                      color: "var(--sw-primary)",
                    }}
                  >
                    <span
                      className="sw-label"
                      style={{
                        color: idx < 3 ? "var(--sw-moment-accent)" : "var(--sw-outline)",
                        fontSize: 12,
                        fontVariantNumeric: "tabular-nums",
                        minWidth: 16,
                      }}
                    >
                      {idx + 1}
                    </span>
                    <span style={{ flex: 1, fontSize: "var(--sw-text-sm)", fontWeight: 600 }}>
                      #{t.tag}
                    </span>
                    <span
                      className="sw-label"
                      style={{ color: "var(--sw-outline)", fontSize: 12 }}
                    >
                      {t.volume}
                    </span>
                  </Link>
                ))}
              </div>
            </div>

            <div className="sw-card" style={{ padding: 16 }}>
              <SectionTitle title="热门讨论" icon={MessageCircle} count={HOT_DISCUSSIONS.length} />
              <div className="flex flex-col gap-3 pt-3">
                {HOT_DISCUSSIONS.map((d) => (
                  <Link
                    key={d.title}
                    href={`/moments?topic=${encodeURIComponent(d.title)}`}
                    className="sw-focus-ring"
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      gap: 4,
                      padding: 8,
                      borderRadius: "var(--sw-radius-md)",
                      background: "var(--sw-surface-container)",
                      textDecoration: "none",
                      color: "var(--sw-primary)",
                    }}
                  >
                    <span className="sw-label" style={{ color: "var(--sw-primary)", fontSize: 13 }}>
                      {d.title}
                    </span>
                    <span
                      className="sw-label"
                      style={{ color: "var(--sw-outline)", fontSize: 11 }}
                    >
                      {d.excerpt}
                    </span>
                    <span
                      className="sw-label"
                      style={{ color: "var(--sw-moment-accent)", fontSize: 11 }}
                    >
                      {d.replies} 条回复 →
                    </span>
                  </Link>
                ))}
              </div>
            </div>

            <div className="sw-card" style={{ padding: 16 }}>
              <SectionTitle title="推荐关注" icon={TrendingUp} />
              <div className="flex flex-col gap-3 pt-3">
                {SUGGESTED_FOLLOWS.map((u) => (
                  <div
                    key={u.handle}
                    className="flex items-center gap-3"
                    style={{ padding: "4px 0" }}
                  >
                    <div
                      className="sw-avatar sw-avatar-sm"
                      style={{
                        background: "var(--sw-secondary)",
                        color: "var(--sw-on-secondary)",
                        fontSize: "12px",
                      }}
                    >
                      {u.avatar}
                    </div>
                    <div className="min-w-0" style={{ flex: 1 }}>
                      <div className="sw-label" style={{ color: "var(--sw-primary)" }}>
                        {u.name}
                      </div>
                      <div
                        className="sw-label"
                        style={{ color: "var(--sw-outline)", fontSize: 11 }}
                      >
                        @{u.handle} · {u.focus}
                      </div>
                    </div>
                    <button
                      type="button"
                      className="sw-focus-ring"
                      style={{
                        padding: "4px 10px",
                        borderRadius: "var(--sw-radius-full)",
                        background: "var(--sw-moment-accent)",
                        color: "var(--sw-on-secondary)",
                        border: "none",
                        fontSize: "var(--sw-text-xs)",
                        fontWeight: 600,
                        cursor: "pointer",
                      }}
                    >
                      关注
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
}
