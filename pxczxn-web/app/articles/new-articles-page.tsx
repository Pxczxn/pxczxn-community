/**
 * NewArticlesPage - Stitch v2.1_16 High-Density Index
 *
 * 12-column grid layout showcasing a dense, scannable article list:
 * - Top filter strip (categories, sort, view modes)
 * - Compact list rows with: number, tags, title, summary, metrics
 * - Right rail: tag filters, reading time, contributor index
 * - Uses --sw-article-accent as the visual anchor
 *
 * Mock data is used until the data layer is wired up.
 */

"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import {
  ArrowDownAZ,
  ArrowRight,
  Bookmark,
  Clock,
  Filter,
  Flame,
  Grid3X3,
  Hash,
  Heart,
  List,
  PenLine,
  Search,
  SlidersHorizontal,
  TrendingUp,
} from "lucide-react";
import { AppNavbar } from "@/components/ui/navbar";

/* ─────────────────────────── Mock Data ─────────────────────────── */

type ArticleListItem = {
  id: string;
  rank: number;
  title: string;
  summary: string;
  author: string;
  authorAvatar: string;
  blog: string;
  tags: string[];
  publishedAt: string;
  readTime: number;
  likes: number;
  bookmarks: number;
  views: number;
  category: string;
};

const ARTICLES: ArticleListItem[] = [
  {
    id: "a-1",
    rank: 1,
    title: "深入理解 React Server Components 的渲染边界",
    summary:
      "Server Components 改变了组件的渲染时机与数据获取方式。本文梳理 RSC 的协议、Suspense 边界与缓存策略，并讨论在生产环境中如何避免常见陷阱。",
    author: "林书豪",
    authorAvatar: "林",
    blog: "前端深潜",
    tags: ["React", "RSC", "架构"],
    publishedAt: "30 分钟前",
    readTime: 14,
    likes: 286,
    bookmarks: 142,
    views: 4321,
    category: "frontend",
  },
  {
    id: "a-2",
    rank: 2,
    title: "PostgreSQL 16 中的逻辑复制：从原理到生产实践",
    summary:
      "结合实际案例讲解 logical replication 的拓扑设计、冲突检测与监控告警，帮助你搭建可平滑升级的数据库镜像。",
    author: "陈知行",
    authorAvatar: "陈",
    blog: "数据库札记",
    tags: ["PostgreSQL", "数据库", "复制"],
    publishedAt: "1 小时前",
    readTime: 18,
    likes: 197,
    bookmarks: 118,
    views: 2980,
    category: "backend",
  },
  {
    id: "a-3",
    rank: 3,
    title: "设计系统中的可访问性优先：A11y 的工程化落地",
    summary:
      "可访问性是产品体验的最低保障。从语义化标签、键盘导航到色彩对比，本文整理出一份团队可立即采用的检查清单。",
    author: "苏文慧",
    authorAvatar: "苏",
    blog: "UX 工具箱",
    tags: ["可访问性", "设计系统", "A11y"],
    publishedAt: "2 小时前",
    readTime: 9,
    likes: 154,
    bookmarks: 96,
    views: 1842,
    category: "design",
  },
  {
    id: "a-4",
    rank: 4,
    title: "为什么我们用 Rust 重写了内部的任务调度器",
    summary:
      "从 Node.js 到 Rust 的迁移实录：性能提升了 3.8 倍，内存占用下降到 1/5，但学习曲线与团队结构带来了完全不同的挑战。",
    author: "王启明",
    authorAvatar: "王",
    blog: "工程手记",
    tags: ["Rust", "性能", "工程"],
    publishedAt: "3 小时前",
    readTime: 12,
    likes: 312,
    bookmarks: 188,
    views: 5620,
    category: "backend",
  },
  {
    id: "a-5",
    rank: 5,
    title: "可视化分析：拆解 eBPF 在生产环境中的故事",
    summary:
      "通过一组真实火焰图与函数追踪数据，揭示 eBPF 如何在零侵入的前提下帮助我们定位一个 12 小时的卡顿问题。",
    author: "高志远",
    authorAvatar: "高",
    blog: "性能观察",
    tags: ["eBPF", "Linux", "性能"],
    publishedAt: "5 小时前",
    readTime: 16,
    likes: 221,
    bookmarks: 134,
    views: 3120,
    category: "backend",
  },
  {
    id: "a-6",
    rank: 6,
    title: "SwiftUI 中的状态管理：从 @State 到 @Observable",
    summary:
      "Swift 5.9 引入的 Observation 框架让数据流更直观。结合示例，我们来对比旧有范式与现代写法的差异。",
    author: "何梓琪",
    authorAvatar: "何",
    blog: "iOS 周刊",
    tags: ["SwiftUI", "iOS", "状态管理"],
    publishedAt: "昨天",
    readTime: 8,
    likes: 87,
    bookmarks: 41,
    views: 1120,
    category: "mobile",
  },
  {
    id: "a-7",
    rank: 7,
    title: "从 0 到 1 写一个 LLM 评测沙盒：踩坑与对策",
    summary:
      "想为自己的 Agent 跑出可信的评测？这篇文章记录了数据集选型、裁判模型、超时控制与成本控制的关键点。",
    author: "罗子涵",
    authorAvatar: "罗",
    blog: "AI 工坊",
    tags: ["LLM", "评测", "Agent"],
    publishedAt: "昨天",
    readTime: 22,
    likes: 408,
    bookmarks: 256,
    views: 7120,
    category: "ai",
  },
  {
    id: "a-8",
    rank: 8,
    title: "用 CSS Anchor Positioning 重构悬浮提示体系",
    summary:
      "新特性让 tooltip、菜单等组件不再依赖 JavaScript 计算位置，原生浏览器定位大幅降低闪烁与偏差。",
    author: "蔡安然",
    authorAvatar: "蔡",
    blog: "前端深潜",
    tags: ["CSS", "前端", "新特性"],
    publishedAt: "2 天前",
    readTime: 6,
    likes: 132,
    bookmarks: 73,
    views: 1920,
    category: "frontend",
  },
  {
    id: "a-9",
    rank: 9,
    title: "Bun 1.2 生产可用性评估报告",
    summary:
      "从冷启动、HTTP/2、TypeScript 支持到生态兼容，Bun 在 Node.js 之外走出了怎样的曲线？我们跑了 6 个常见 Web 框架作为对照。",
    author: "邵一鸣",
    authorAvatar: "邵",
    blog: "工程手记",
    tags: ["Bun", "运行时", "评测"],
    publishedAt: "2 天前",
    readTime: 11,
    likes: 156,
    bookmarks: 89,
    views: 2410,
    category: "backend",
  },
  {
    id: "a-10",
    rank: 10,
    title: "图神经网络入门：从概念到 PyG 实现",
    summary:
      "用最朴素的类比解释消息传递、聚合与 readout，配合一个社交网络推荐的小型示例带你跑通完整流程。",
    author: "夏知行",
    authorAvatar: "夏",
    blog: "AI 工坊",
    tags: ["GNN", "深度学习", "PyG"],
    publishedAt: "3 天前",
    readTime: 15,
    likes: 245,
    bookmarks: 162,
    views: 3580,
    category: "ai",
  },
  {
    id: "a-11",
    rank: 11,
    title: "Tailwind v4 中的设计令牌：原子化 CSS 的下一步",
    summary:
      "Token-driven 配置让 Tailwind 终于可以与 Figma Variables 双向同步。实测在大型设计系统中的收益与权衡。",
    author: "潘清漪",
    authorAvatar: "潘",
    blog: "设计系统月报",
    tags: ["Tailwind", "设计系统", "CSS"],
    publishedAt: "3 天前",
    readTime: 7,
    likes: 119,
    bookmarks: 64,
    views: 1640,
    category: "design",
  },
  {
    id: "a-12",
    rank: 12,
    title: "WebGPU 在浏览器中落地：从纹理到计算管线",
    summary:
      "对比 WebGL 的能力差异，并展示一个用 Compute Shader 编写的浏览器内 AI 推理 demo。",
    author: "段星河",
    authorAvatar: "段",
    blog: "图形学笔记",
    tags: ["WebGPU", "图形", "前端"],
    publishedAt: "4 天前",
    readTime: 19,
    likes: 178,
    bookmarks: 121,
    views: 2870,
    category: "frontend",
  },
];

const CATEGORIES = [
  { id: "all", label: "全部", count: ARTICLES.length },
  { id: "frontend", label: "前端", count: ARTICLES.filter((a) => a.category === "frontend").length },
  { id: "backend", label: "后端", count: ARTICLES.filter((a) => a.category === "backend").length },
  { id: "ai", label: "AI / 机器学习", count: ARTICLES.filter((a) => a.category === "ai").length },
  { id: "mobile", label: "移动开发", count: ARTICLES.filter((a) => a.category === "mobile").length },
  { id: "design", label: "设计", count: ARTICLES.filter((a) => a.category === "design").length },
];

const SORTS = [
  { id: "hot", label: "最热", icon: Flame },
  { id: "latest", label: "最新", icon: Clock },
  { id: "trending", label: "趋势", icon: TrendingUp },
  { id: "alpha", label: "A → Z", icon: ArrowDownAZ },
] as const;

type SortId = (typeof SORTS)[number]["id"];
type ViewMode = "list" | "grid";

const TAG_POOL = Array.from(
  ARTICLES.flatMap((a) => a.tags).reduce((map, tag) => {
    map.set(tag, (map.get(tag) ?? 0) + 1);
    return map;
  }, new Map<string, number>())
).map(([name, count]) => ({ name, count }));

const READING_TIME_BUCKETS = [
  { id: "any", label: "不限", min: 0, max: 999 },
  { id: "short", label: "< 10 分钟", min: 0, max: 10 },
  { id: "mid", label: "10 - 20 分钟", min: 10, max: 20 },
  { id: "long", label: "> 20 分钟", min: 20, max: 999 },
];

const TOP_CONTRIBUTORS = [
  { name: "林书豪", articles: 42, specialty: "前端架构" },
  { name: "陈知行", articles: 36, specialty: "数据库" },
  { name: "罗子涵", articles: 28, specialty: "AI / Agent" },
  { name: "王启明", articles: 24, specialty: "系统编程" },
  { name: "苏文慧", articles: 21, specialty: "可访问性" },
];

/* ─────────────────────────── Sub-Components ─────────────────────────── */

function CompactRow({ article }: { article: ArticleListItem }) {
  return (
    <article
      className="sw-card"
      style={{
        padding: "16px 20px",
        display: "grid",
        gridTemplateColumns: "40px 1fr auto",
        gap: 20,
        alignItems: "start",
        borderLeft: "3px solid var(--sw-article-accent)",
      }}
    >
      <div
        className="sw-heading-md"
        style={{
          color: article.rank <= 3 ? "var(--sw-article-accent)" : "var(--sw-outline)",
          fontVariantNumeric: "tabular-nums",
          textAlign: "right",
        }}
      >
        {String(article.rank).padStart(2, "0")}
      </div>

      <div className="flex flex-col gap-2 min-w-0">
        <Link
          href={`/articles/${article.id}`}
          className="sw-focus-ring"
          style={{ textDecoration: "none" }}
        >
          <h3
            className="sw-heading-sm"
            style={{
              color: "var(--sw-primary)",
              display: "-webkit-box",
              WebkitLineClamp: 1,
              WebkitBoxOrient: "vertical",
              overflow: "hidden",
            }}
          >
            {article.title}
          </h3>
        </Link>
        <p
          className="sw-body"
          style={{
            color: "var(--sw-on-primary-container)",
            fontSize: "var(--sw-text-sm)",
            display: "-webkit-box",
            WebkitLineClamp: 2,
            WebkitBoxOrient: "vertical",
            overflow: "hidden",
            margin: 0,
          }}
        >
          {article.summary}
        </p>
        <div
          className="flex flex-wrap items-center gap-3"
          style={{ color: "var(--sw-outline)", fontSize: "var(--sw-text-xs)" }}
        >
          <span className="sw-label" style={{ color: "var(--sw-primary)" }}>
            {article.author}
          </span>
          <span>·</span>
          <span>{article.blog}</span>
          <span>·</span>
          <span>{article.publishedAt}</span>
          <div className="flex gap-1.5 flex-wrap ml-2">
            {article.tags.slice(0, 3).map((tag) => (
              <span key={tag} className="sw-tag" style={{ fontSize: "11px" }}>
                {tag}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div
        className="flex flex-col items-end gap-2"
        style={{ color: "var(--sw-outline)", fontSize: "var(--sw-text-xs)", minWidth: 88 }}
      >
        <div className="flex items-center gap-1">
          <Clock size={12} />
          <span>{article.readTime} 分钟</span>
        </div>
        <div className="flex items-center gap-3">
          <span className="flex items-center gap-1">
            <Heart size={12} style={{ color: "var(--sw-article-accent)" }} />
            {article.likes}
          </span>
          <span className="flex items-center gap-1">
            <Bookmark size={12} />
            {article.bookmarks}
          </span>
        </div>
        <span>{article.views.toLocaleString()} 阅读</span>
      </div>
    </article>
  );
}

function GridCard({ article }: { article: ArticleListItem }) {
  return (
    <article
      className="sw-card"
      style={{
        padding: 20,
        display: "flex",
        flexDirection: "column",
        gap: 12,
        borderTop: "3px solid var(--sw-article-accent)",
      }}
    >
      <div className="flex items-center gap-2">
        <div
          className="sw-avatar sw-avatar-sm"
          style={{
            background: "var(--sw-secondary)",
            color: "var(--sw-on-secondary)",
            fontSize: "12px",
          }}
        >
          {article.authorAvatar}
        </div>
        <span className="sw-label" style={{ color: "var(--sw-primary)" }}>
          {article.author}
        </span>
        <span
          className="sw-label"
          style={{ color: "var(--sw-outline)", fontSize: "12px", marginLeft: "auto" }}
        >
          {article.publishedAt}
        </span>
      </div>
      <Link href={`/articles/${article.id}`} style={{ textDecoration: "none" }}>
        <h3 className="sw-heading-sm" style={{ color: "var(--sw-primary)" }}>
          {article.title}
        </h3>
      </Link>
      <p
        className="sw-body"
        style={{
          color: "var(--sw-on-primary-container)",
          fontSize: "var(--sw-text-sm)",
          margin: 0,
          display: "-webkit-box",
          WebkitLineClamp: 2,
          WebkitBoxOrient: "vertical",
          overflow: "hidden",
        }}
      >
        {article.summary}
      </p>
      <div className="flex flex-wrap gap-1.5">
        {article.tags.map((tag) => (
          <span key={tag} className="sw-tag" style={{ fontSize: "11px" }}>
            {tag}
          </span>
        ))}
      </div>
      <div
        className="flex items-center justify-between"
        style={{ color: "var(--sw-outline)", fontSize: "var(--sw-text-xs)" }}
      >
        <span className="flex items-center gap-1">
          <Clock size={12} />
          {article.readTime} 分钟
        </span>
        <span className="flex items-center gap-3">
          <span className="flex items-center gap-1">
            <Heart size={12} style={{ color: "var(--sw-article-accent)" }} />
            {article.likes}
          </span>
          <span>{article.views.toLocaleString()} 阅读</span>
        </span>
      </div>
    </article>
  );
}

function SectionHeader({
  title,
  count,
  icon: Icon,
  action,
}: {
  title: string;
  count?: number;
  icon: React.ElementType;
  action?: React.ReactNode;
}) {
  return (
    <div
      className="flex items-center gap-2 pb-3"
      style={{ borderBottom: "1px solid var(--sw-outline-variant)" }}
    >
      <Icon size={18} style={{ color: "var(--sw-article-accent)" }} />
      <h3
        className="sw-heading-sm"
        style={{ color: "var(--sw-primary)" }}
      >
        {title}
      </h3>
      {count !== undefined && (
        <span
          className="sw-label"
          style={{ color: "var(--sw-outline)", fontSize: "12px", marginLeft: 4 }}
        >
          ({count})
        </span>
      )}
      {action && <div className="ml-auto">{action}</div>}
    </div>
  );
}

/* ─────────────────────────── Main Component ─────────────────────────── */

export default function NewArticlesPage() {
  const [activeCategory, setActiveCategory] = useState<string>("all");
  const [activeSort, setActiveSort] = useState<SortId>("hot");
  const [viewMode, setViewMode] = useState<ViewMode>("list");
  const [searchTerm, setSearchTerm] = useState("");
  const [readingBucket, setReadingBucket] = useState("any");
  const [activeTags, setActiveTags] = useState<string[]>([]);

  const filteredArticles = useMemo(() => {
    const bucket = READING_TIME_BUCKETS.find((b) => b.id === readingBucket) ?? READING_TIME_BUCKETS[0];
    let list = ARTICLES.filter((a) => {
      if (activeCategory !== "all" && a.category !== activeCategory) return false;
      if (a.readTime < bucket.min || a.readTime >= bucket.max) return false;
      if (activeTags.length > 0 && !activeTags.some((t) => a.tags.includes(t))) return false;
      if (searchTerm && !`${a.title}${a.summary}${a.author}`.toLowerCase().includes(searchTerm.toLowerCase())) {
        return false;
      }
      return true;
    });

    switch (activeSort) {
      case "latest":
        list = list
          .slice()
          .sort((a, b) => (a.publishedAt < b.publishedAt ? 1 : -1));
        break;
      case "trending":
        list = list
          .slice()
          .sort((a, b) => b.likes / Math.max(b.readTime, 1) - a.likes / Math.max(a.readTime, 1));
        break;
      case "alpha":
        list = list.slice().sort((a, b) => a.title.localeCompare(b.title, "zh"));
        break;
      case "hot":
      default:
        list = list.slice().sort((a, b) => b.views - a.views);
    }
    return list.map((a, idx) => ({ ...a, rank: idx + 1 }));
  }, [activeCategory, activeSort, searchTerm, readingBucket, activeTags]);

  const toggleTag = (tag: string) => {
    setActiveTags((prev) =>
      prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]
    );
  };

  return (
    <div
      style={{
        background: "var(--sw-surface)",
        minHeight: "100vh",
      }}
    >
      <AppNavbar />

      <main className="page-shell py-8">
        {/* Page header */}
        <header className="mb-6 flex flex-col gap-3">
          <span className="sw-eyebrow" style={{ color: "var(--sw-article-accent)" }}>
            <PenLine size={14} style={{ display: "inline", marginRight: 6, verticalAlign: -2 }} />
            专栏 / Articles
          </span>
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <h1 className="sw-heading-lg" style={{ color: "var(--sw-primary)" }}>
                高密度文章索引
              </h1>
              <p
                className="sw-body"
                style={{ color: "var(--sw-on-primary-container)", marginTop: 8, marginBottom: 0 }}
              >
                12 栏网格 × 信息密度优先的浏览体验，快速发现值得阅读的长文。
              </p>
            </div>
            <Link
              href="/editor/new"
              className="sw-button sw-button-primary sw-focus-ring"
              style={{ textDecoration: "none" }}
            >
              <PenLine size={16} />
              写新文章
            </Link>
          </div>
        </header>

        {/* Filter strip */}
        <section
          className="sw-card-elevated"
          style={{ padding: 16, marginBottom: 24 }}
          aria-label="筛选与排序"
        >
          <div className="flex flex-wrap items-center gap-2" style={{ marginBottom: 12 }}>
            <Filter size={16} style={{ color: "var(--sw-article-accent)" }} />
            <span className="sw-label" style={{ color: "var(--sw-primary)" }}>
              分类
            </span>
            <div className="flex flex-wrap gap-1.5 ml-2">
              {CATEGORIES.map((cat) => {
                const active = activeCategory === cat.id;
                return (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => setActiveCategory(cat.id)}
                    className="sw-focus-ring"
                    style={{
                      padding: "6px 12px",
                      borderRadius: "var(--sw-radius-full)",
                      background: active ? "var(--sw-article-accent)" : "var(--sw-surface-container)",
                      color: active ? "var(--sw-on-secondary)" : "var(--sw-on-primary-container)",
                      fontSize: "var(--sw-text-xs)",
                      fontWeight: 600,
                      border: "none",
                      cursor: "pointer",
                    }}
                  >
                    {cat.label} · {cat.count}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div
              className="sw-input"
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                flex: "1 1 240px",
                minWidth: 200,
                padding: "8px 12px",
              }}
            >
              <Search size={14} style={{ color: "var(--sw-outline)" }} />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="搜索标题、作者或正文…"
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

            <div className="flex items-center gap-1" role="tablist" aria-label="排序方式">
              {SORTS.map((sort) => {
                const Icon = sort.icon;
                const active = activeSort === sort.id;
                return (
                  <button
                    key={sort.id}
                    type="button"
                    role="tab"
                    aria-selected={active}
                    onClick={() => setActiveSort(sort.id)}
                    className="sw-focus-ring"
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: 4,
                      padding: "6px 10px",
                      borderRadius: "var(--sw-radius-md)",
                      background: active ? "var(--sw-secondary-container)" : "transparent",
                      color: active ? "var(--sw-on-secondary-container)" : "var(--sw-on-primary-container)",
                      fontSize: "var(--sw-text-xs)",
                      fontWeight: 600,
                      border: "1px solid var(--sw-outline-variant)",
                      cursor: "pointer",
                    }}
                  >
                    <Icon size={12} />
                    {sort.label}
                  </button>
                );
              })}
            </div>

            <div
              className="flex"
              style={{
                border: "1px solid var(--sw-outline-variant)",
                borderRadius: "var(--sw-radius-md)",
                overflow: "hidden",
              }}
              role="group"
              aria-label="视图切换"
            >
              {(
                [
                  { id: "list", icon: List, label: "列表" },
                  { id: "grid", icon: Grid3X3, label: "网格" },
                ] as const
              ).map(({ id, icon: Icon, label }) => {
                const active = viewMode === id;
                return (
                  <button
                    key={id}
                    type="button"
                    onClick={() => setViewMode(id)}
                    aria-pressed={active}
                    className="sw-focus-ring"
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: 4,
                      padding: "6px 10px",
                      background: active ? "var(--sw-surface-container-high)" : "transparent",
                      color: "var(--sw-primary)",
                      fontSize: "var(--sw-text-xs)",
                      fontWeight: 600,
                      border: "none",
                      cursor: "pointer",
                    }}
                  >
                    <Icon size={12} />
                    {label}
                  </button>
                );
              })}
            </div>
          </div>
        </section>

        {/* 12-column grid */}
        <div
          className="grid"
          style={{
            gridTemplateColumns: "minmax(0, 1fr) 320px",
            gap: 24,
          }}
        >
          {/* Index column */}
          <section aria-label="文章列表">
            <div
              className="flex items-center justify-between mb-3"
              style={{ color: "var(--sw-outline)" }}
            >
              <span className="sw-label" style={{ color: "var(--sw-on-primary-container)" }}>
                共 {filteredArticles.length} 篇
              </span>
              <button
                type="button"
                className="sw-focus-ring"
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: 4,
                  background: "transparent",
                  border: "1px solid var(--sw-outline-variant)",
                  borderRadius: "var(--sw-radius-md)",
                  padding: "6px 10px",
                  fontSize: "var(--sw-text-xs)",
                  color: "var(--sw-on-primary-container)",
                  cursor: "pointer",
                }}
              >
                <SlidersHorizontal size={12} />
                高级筛选
              </button>
            </div>

            {viewMode === "list" ? (
              <div
                className="grid"
                style={{ gridTemplateColumns: "1fr", gap: 10 }}
              >
                {filteredArticles.map((article) => (
                  <CompactRow key={article.id} article={article} />
                ))}
              </div>
            ) : (
              <div
                className="grid"
                style={{ gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 14 }}
              >
                {filteredArticles.map((article) => (
                  <GridCard key={article.id} article={article} />
                ))}
              </div>
            )}

            {filteredArticles.length === 0 && (
              <div
                className="sw-card"
                style={{
                  padding: 32,
                  textAlign: "center",
                  color: "var(--sw-on-primary-container)",
                }}
              >
                <p className="sw-body" style={{ margin: 0 }}>
                  没有匹配的文章，试试调整筛选条件吧。
                </p>
              </div>
            )}
          </section>

          {/* Sidebar */}
          <aside className="flex flex-col gap-6" aria-label="侧栏筛选">
            <div className="sw-card" style={{ padding: 20 }}>
              <SectionHeader title="热门标签" icon={Hash} count={TAG_POOL.length} />
              <div className="flex flex-wrap gap-1.5 pt-4">
                {TAG_POOL.map((tag) => {
                  const active = activeTags.includes(tag.name);
                  return (
                    <button
                      key={tag.name}
                      type="button"
                      onClick={() => toggleTag(tag.name)}
                      className="sw-focus-ring"
                      style={{
                        padding: "4px 10px",
                        borderRadius: "var(--sw-radius-full)",
                        background: active ? "var(--sw-article-accent)" : "var(--sw-surface-container)",
                        color: active ? "var(--sw-on-secondary)" : "var(--sw-on-primary-container)",
                        fontSize: "var(--sw-text-xs)",
                        fontWeight: 600,
                        border: "none",
                        cursor: "pointer",
                      }}
                    >
                      {tag.name} · {tag.count}
                    </button>
                  );
                })}
              </div>
              {activeTags.length > 0 && (
                <button
                  type="button"
                  onClick={() => setActiveTags([])}
                  className="sw-focus-ring"
                  style={{
                    marginTop: 12,
                    background: "transparent",
                    border: "none",
                    color: "var(--sw-secondary)",
                    fontSize: "var(--sw-text-xs)",
                    fontWeight: 600,
                    cursor: "pointer",
                    padding: 0,
                  }}
                >
                  清除标签筛选
                </button>
              )}
            </div>

            <div className="sw-card" style={{ padding: 20 }}>
              <SectionHeader title="阅读时长" icon={Clock} />
              <div className="flex flex-col gap-1 pt-4">
                {READING_TIME_BUCKETS.map((bucket) => {
                  const active = readingBucket === bucket.id;
                  return (
                    <button
                      key={bucket.id}
                      type="button"
                      onClick={() => setReadingBucket(bucket.id)}
                      className="sw-focus-ring"
                      style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        padding: "8px 12px",
                        borderRadius: "var(--sw-radius-md)",
                        background: active ? "var(--sw-surface-container-high)" : "transparent",
                        color: "var(--sw-primary)",
                        fontSize: "var(--sw-text-sm)",
                        border: "none",
                        cursor: "pointer",
                        textAlign: "left",
                      }}
                    >
                      <span>{bucket.label}</span>
                      {active && (
                        <span style={{ color: "var(--sw-article-accent)" }}>●</span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="sw-card" style={{ padding: 20 }}>
              <SectionHeader
                title="活跃作者"
                icon={TrendingUp}
                action={
                  <Link
                    href="/creators"
                    className="sw-label sw-focus-ring"
                    style={{ color: "var(--sw-secondary)", textDecoration: "none", fontSize: 12 }}
                  >
                    全部
                    <ArrowRight size={12} style={{ display: "inline", marginLeft: 4, verticalAlign: -1 }} />
                  </Link>
                }
              />
              <div className="flex flex-col pt-4">
                {TOP_CONTRIBUTORS.map((c) => (
                  <Link
                    key={c.name}
                    href={`/users/${c.name}`}
                    className="sw-focus-ring"
                    style={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      padding: "8px 4px",
                      borderRadius: "var(--sw-radius-md)",
                      textDecoration: "none",
                    }}
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <div
                        className="sw-avatar sw-avatar-sm"
                        style={{
                          background: "var(--sw-secondary)",
                          color: "var(--sw-on-secondary)",
                          fontSize: "12px",
                        }}
                      >
                        {c.name.slice(0, 1)}
                      </div>
                      <div className="min-w-0">
                        <div
                          className="sw-label"
                          style={{ color: "var(--sw-primary)" }}
                        >
                          {c.name}
                        </div>
                        <div
                          className="sw-label"
                          style={{ color: "var(--sw-outline)", fontSize: "11px" }}
                        >
                          {c.specialty}
                        </div>
                      </div>
                    </div>
                    <span
                      className="sw-label"
                      style={{ color: "var(--sw-outline)", fontSize: "12px" }}
                    >
                      {c.articles} 篇
                    </span>
                  </Link>
                ))}
              </div>
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
}
