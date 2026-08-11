/**
 * NewSeriesPage - Stitch v2.1_18 Shelf + Catalog
 *
 *  - Top layer: "My Shelf" — series the viewer is currently reading or following.
 *    Book-spine visual metaphor with progress and CTAs.
 *  - Bottom layer: Series Catalog — discoverable grid of series.
 * Uses --sw-series-accent as the visual anchor.
 */

"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import {
  ArrowRight,
  BookOpen,
  BookOpenCheck,
  Bookmark,
  ChevronRight,
  Clock,
  Compass,
  Filter,
  LibraryBig,
  PenLine,
  Play,
  Plus,
  Search,
  Sparkles,
  Star,
  TrendingUp,
  Users,
} from "lucide-react";
import { AppNavbar } from "@/components/ui/navbar";

/* ─────────────────────────── Mock Data ─────────────────────────── */

type Series = {
  id: string;
  title: string;
  description: string;
  author: string;
  authorAvatar: string;
  chapters: number;
  readers: number;
  rating: number;
  tags: string[];
  category: string;
  color: string;
  cover: string;
  updatedAt: string;
  isEditorsPick?: boolean;
};

const SHELF: Series[] = [
  {
    id: "shelf-1",
    title: "Rust 系统编程入门",
    description: "从所有权到 async：循序渐进的 Rust 学习曲线。",
    author: "王启明",
    authorAvatar: "王",
    chapters: 12,
    readers: 4820,
    rating: 4.8,
    tags: ["Rust", "系统编程"],
    category: "backend",
    color: "var(--sw-series-accent)",
    cover: "https://images.unsplash.com/photo-1551033406-611cf9a28f67?w=400&h=600&fit=crop",
    updatedAt: "2 天前更新",
  },
  {
    id: "shelf-2",
    title: "前端架构演进史",
    description: "从 jQuery 到微前端：每一代架构的取舍。",
    author: "林书豪",
    authorAvatar: "林",
    chapters: 8,
    readers: 3120,
    rating: 4.6,
    tags: ["前端", "架构"],
    category: "frontend",
    color: "var(--sw-article-accent)",
    cover: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=600&fit=crop",
    updatedAt: "上周更新",
  },
  {
    id: "shelf-3",
    title: "LLM 应用工程化",
    description: "把模型接入生产：评测、监控、回归测试全流程。",
    author: "罗子涵",
    authorAvatar: "罗",
    chapters: 15,
    readers: 6210,
    rating: 4.9,
    tags: ["LLM", "AI", "工程"],
    category: "ai",
    color: "var(--sw-secondary)",
    cover: "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=400&h=600&fit=crop",
    updatedAt: "今天更新",
  },
  {
    id: "shelf-4",
    title: "可访问性手册",
    description: "从语义到色彩：让产品对所有人都友好。",
    author: "苏文慧",
    authorAvatar: "苏",
    chapters: 6,
    readers: 1840,
    rating: 4.7,
    tags: ["A11y", "设计"],
    category: "design",
    color: "var(--sw-team-accent)",
    cover: "https://images.unsplash.com/photo-1481349518771-20055b2a7b24?w=400&h=600&fit=crop",
    updatedAt: "3 天前更新",
  },
];

const CATALOG: Series[] = [
  {
    id: "cat-1",
    title: "PostgreSQL 进阶之路",
    description: "索引、复制、扩展：吃透 PG 的工程能力。",
    author: "陈知行",
    authorAvatar: "陈",
    chapters: 18,
    readers: 5210,
    rating: 4.8,
    tags: ["PostgreSQL", "数据库"],
    category: "backend",
    color: "var(--sw-article-accent)",
    cover: "https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=400&h=600&fit=crop",
    updatedAt: "本周更新",
    isEditorsPick: true,
  },
  {
    id: "cat-2",
    title: "SwiftUI 全景",
    description: "现代 iOS / macOS 开发的体系化教程。",
    author: "何梓琪",
    authorAvatar: "何",
    chapters: 10,
    readers: 2210,
    rating: 4.5,
    tags: ["SwiftUI", "iOS"],
    category: "mobile",
    color: "var(--sw-moment-accent)",
    cover: "https://images.unsplash.com/photo-1535303311164-664fc9ec6532?w=400&h=600&fit=crop",
    updatedAt: "上周更新",
  },
  {
    id: "cat-3",
    title: "可观测性实战",
    description: "日志、指标、追踪：构建生产可观测性体系。",
    author: "高志远",
    authorAvatar: "高",
    chapters: 14,
    readers: 3680,
    rating: 4.7,
    tags: ["可观测性", "DevOps"],
    category: "backend",
    color: "var(--sw-team-accent)",
    cover: "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=400&h=600&fit=crop",
    updatedAt: "5 天前更新",
  },
  {
    id: "cat-4",
    title: "WebGPU 编程入门",
    description: "用现代 GPU 能力解锁浏览器端的新世界。",
    author: "段星河",
    authorAvatar: "段",
    chapters: 9,
    readers: 1480,
    rating: 4.4,
    tags: ["WebGPU", "图形"],
    category: "frontend",
    color: "var(--sw-secondary)",
    cover: "https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=400&h=600&fit=crop",
    updatedAt: "2 周前更新",
  },
  {
    id: "cat-5",
    title: "Bun 1.x 实战手册",
    description: "全栈 Bun：从前端到后端的一体化运行时。",
    author: "邵一鸣",
    authorAvatar: "邵",
    chapters: 7,
    readers: 960,
    rating: 4.3,
    tags: ["Bun", "运行时"],
    category: "backend",
    color: "var(--sw-series-accent)",
    cover: "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400&h=600&fit=crop",
    updatedAt: "3 周前更新",
  },
  {
    id: "cat-6",
    title: "设计系统落地记",
    description: "把 Figma 变量搬进代码，并保持双向同步。",
    author: "潘清漪",
    authorAvatar: "潘",
    chapters: 11,
    readers: 2780,
    rating: 4.6,
    tags: ["设计系统", "Figma"],
    category: "design",
    color: "var(--sw-moment-accent)",
    cover: "https://images.unsplash.com/photo-1559028012-481c04fa702d?w=400&h=600&fit=crop",
    updatedAt: "1 周前更新",
    isEditorsPick: true,
  },
  {
    id: "cat-7",
    title: "团队工程实践",
    description: "Code Review、CI/CD、On-Call：让团队跑得更稳。",
    author: "夏知行",
    authorAvatar: "夏",
    chapters: 13,
    readers: 3140,
    rating: 4.7,
    tags: ["工程", "团队"],
    category: "backend",
    color: "var(--sw-team-accent)",
    cover: "https://images.unsplash.com/photo-1521737852567-6949f3f9f2b5?w=400&h=600&fit=crop",
    updatedAt: "2 周前更新",
  },
  {
    id: "cat-8",
    title: "CSS 新特性速通",
    description: "Container Queries、Anchor Positioning、Cascade Layers。",
    author: "蔡安然",
    authorAvatar: "蔡",
    chapters: 5,
    readers: 1820,
    rating: 4.5,
    tags: ["CSS", "前端"],
    category: "frontend",
    color: "var(--sw-article-accent)",
    cover: "https://images.unsplash.com/photo-1517433670267-08bbd4be890f?w=400&h=600&fit=crop",
    updatedAt: "5 天前更新",
  },
];

const SHELF_PROGRESS: Record<string, number> = {
  "shelf-1": 0.42,
  "shelf-2": 0.18,
  "shelf-3": 0.78,
  "shelf-4": 0.55,
};

const CATEGORIES = [
  { id: "all", label: "全部" },
  { id: "frontend", label: "前端" },
  { id: "backend", label: "后端" },
  { id: "ai", label: "AI" },
  { id: "mobile", label: "移动" },
  { id: "design", label: "设计" },
];

const SORTS = [
  { id: "trending", label: "趋势", icon: TrendingUp },
  { id: "newest", label: "最新", icon: Clock },
  { id: "popular", label: "最热", icon: Star },
  { id: "rating", label: "评分", icon: Sparkles },
] as const;

type SortId = (typeof SORTS)[number]["id"];

/* ─────────────────────────── Sub-Components ─────────────────────────── */

/* Book spine used in the Shelf */
function BookSpine({ series, progress }: { series: Series; progress: number }) {
  return (
    <Link
      href={`/series/${series.id}`}
      className="sw-focus-ring"
      style={{
        position: "relative",
        display: "flex",
        flexDirection: "column",
        width: 96,
        minHeight: 260,
        borderRadius: "var(--sw-radius-md) var(--sw-radius-md) var(--sw-radius-md) var(--sw-radius-md)",
        background: `linear-gradient(180deg, ${series.color} 0%, color-mix(in srgb, ${series.color} 70%, black 30%) 100%)`,
        color: "white",
        textDecoration: "none",
        padding: 16,
        justifyContent: "space-between",
        boxShadow: "var(--sw-shadow-md)",
        transition: "transform 0.2s ease, box-shadow 0.2s ease",
      }}
    >
      {/* Title (rotated) */}
      <div
        style={{
          writingMode: "vertical-rl",
          fontSize: "var(--sw-text-sm)",
          fontWeight: 700,
          letterSpacing: "0.04em",
          lineHeight: 1.2,
          maxHeight: 200,
          overflow: "hidden",
        }}
      >
        {series.title}
      </div>
      {/* Footer */}
      <div className="flex flex-col gap-1" style={{ fontSize: "var(--sw-text-xs)" }}>
        <span style={{ opacity: 0.85 }}>{series.chapters} 章</span>
        <span
          style={{
            height: 4,
            borderRadius: "var(--sw-radius-full)",
            background: "rgba(255,255,255,0.25)",
            overflow: "hidden",
          }}
        >
          <span
            style={{
              display: "block",
              width: `${Math.round(progress * 100)}%`,
              height: "100%",
              background: "rgba(255,255,255,0.9)",
            }}
          />
        </span>
        <span style={{ opacity: 0.8, fontVariantNumeric: "tabular-nums" }}>
          {Math.round(progress * 100)}%
        </span>
      </div>
    </Link>
  );
}

function ShelfCard({ series, progress }: { series: Series; progress: number }) {
  return (
    <article
      className="sw-card"
      style={{
        padding: 16,
        display: "flex",
        flexDirection: "column",
        gap: 12,
        borderTop: "3px solid var(--sw-series-accent)",
      }}
    >
      <div className="flex items-center gap-3">
        <div
          style={{
            width: 56,
            height: 80,
            borderRadius: "var(--sw-radius-md)",
            background: series.color,
            flexShrink: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "white",
          }}
        >
          <BookOpen size={20} />
        </div>
        <div className="min-w-0" style={{ flex: 1 }}>
          <div className="flex items-center gap-2">
            <span className="sw-eyebrow" style={{ color: series.color }}>
              书架
            </span>
            <span
              className="sw-label"
              style={{ color: "var(--sw-outline)", fontSize: 11 }}
            >
              {series.updatedAt}
            </span>
          </div>
          <Link
            href={`/series/${series.id}`}
            className="sw-focus-ring"
            style={{ textDecoration: "none" }}
          >
            <h3
              className="sw-heading-sm"
              style={{
                color: "var(--sw-primary)",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
            >
              {series.title}
            </h3>
          </Link>
          <span
            className="sw-label"
            style={{ color: "var(--sw-outline)", fontSize: 12 }}
          >
            {series.author} · {series.chapters} 章 · {series.readers.toLocaleString()} 读者
          </span>
        </div>
      </div>

      <div>
        <div
          className="flex items-center justify-between"
          style={{ color: "var(--sw-outline)", fontSize: "var(--sw-text-xs)" }}
        >
          <span>已读 {Math.round(progress * series.chapters)} / {series.chapters} 章</span>
          <span>{Math.round(progress * 100)}%</span>
        </div>
        <div
          style={{
            marginTop: 6,
            height: 6,
            borderRadius: "var(--sw-radius-full)",
            background: "var(--sw-surface-container-high)",
            overflow: "hidden",
          }}
        >
          <span
            style={{
              display: "block",
              width: `${Math.round(progress * 100)}%`,
              height: "100%",
              background: series.color,
            }}
          />
        </div>
      </div>

      <div className="flex items-center gap-2">
        <Link
          href={`/series/${series.id}/read`}
          className="sw-button sw-button-primary sw-focus-ring"
          style={{
            flex: 1,
            background: series.color,
            color: "var(--sw-on-secondary)",
            textDecoration: "none",
          }}
        >
          <Play size={14} /> 继续阅读
        </Link>
        <Link
          href={`/series/${series.id}`}
          className="sw-button sw-button-ghost sw-focus-ring"
          style={{ textDecoration: "none" }}
        >
          目录
        </Link>
      </div>
    </article>
  );
}

function CatalogCard({ series }: { series: Series }) {
  return (
    <Link
      href={`/series/${series.id}`}
      className="sw-card"
      style={{
        textDecoration: "none",
        display: "flex",
        flexDirection: "column",
        overflow: "hidden",
      }}
    >
      {/* Cover */}
      <div
        style={{
          position: "relative",
          aspectRatio: "4 / 3",
          background: `linear-gradient(135deg, ${series.color} 0%, color-mix(in srgb, ${series.color} 60%, black 40%) 100%)`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <BookOpen size={36} style={{ color: "rgba(255,255,255,0.85)" }} />
        {series.isEditorsPick && (
          <span
            className="sw-badge"
            style={{
              position: "absolute",
              top: 10,
              left: 10,
              background: "var(--sw-surface-container-lowest)",
              color: series.color,
            }}
          >
            <Star size={11} /> 编辑精选
          </span>
        )}
        <span
          className="sw-label"
          style={{
            position: "absolute",
            bottom: 10,
            right: 10,
            background: "rgba(0,0,0,0.45)",
            color: "white",
            padding: "2px 8px",
            borderRadius: "var(--sw-radius-full)",
            fontSize: 11,
            backdropFilter: "blur(4px)",
          }}
        >
          {series.chapters} 章
        </span>
      </div>
      {/* Body */}
      <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 8 }}>
        <div className="flex items-center gap-2">
          <div
            className="sw-avatar sw-avatar-sm"
            style={{
              background: "var(--sw-surface-container)",
              color: series.color,
              fontSize: "12px",
            }}
          >
            {series.authorAvatar}
          </div>
          <span className="sw-label" style={{ color: "var(--sw-primary)" }}>
            {series.author}
          </span>
        </div>
        <h3 className="sw-heading-sm" style={{ color: "var(--sw-primary)", margin: 0 }}>
          {series.title}
        </h3>
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
          {series.description}
        </p>
        <div
          className="flex items-center justify-between"
          style={{ color: "var(--sw-outline)", fontSize: "var(--sw-text-xs)" }}
        >
          <span className="flex items-center gap-3">
            <span className="flex items-center gap-1">
              <Users size={12} />
              {series.readers.toLocaleString()}
            </span>
            <span className="flex items-center gap-1">
              <Star size={12} style={{ color: series.color }} />
              {series.rating.toFixed(1)}
            </span>
          </span>
          <span>{series.updatedAt}</span>
        </div>
      </div>
    </Link>
  );
}

function SectionHeading({
  title,
  icon: Icon,
  count,
  action,
}: {
  title: string;
  icon: React.ElementType;
  count?: number;
  action?: React.ReactNode;
}) {
  return (
    <div
      className="flex items-center gap-2 pb-3"
      style={{ borderBottom: "1px solid var(--sw-outline-variant)" }}
    >
      <Icon size={18} style={{ color: "var(--sw-series-accent)" }} />
      <h2 className="sw-heading-md" style={{ color: "var(--sw-primary)" }}>
        {title}
      </h2>
      {count !== undefined && (
        <span
          className="sw-label"
          style={{ color: "var(--sw-outline)", fontSize: 12, marginLeft: 4 }}
        >
          {count}
        </span>
      )}
      {action && <div className="ml-auto">{action}</div>}
    </div>
  );
}

/* ─────────────────────────── Main Component ─────────────────────────── */

export default function NewSeriesPage() {
  const [activeCategory, setActiveCategory] = useState("all");
  const [activeSort, setActiveSort] = useState<SortId>("trending");
  const [searchTerm, setSearchTerm] = useState("");

  const sortedCatalog = useMemo(() => {
    let list = CATALOG.filter((s) => {
      if (activeCategory !== "all" && s.category !== activeCategory) return false;
      if (searchTerm && !`${s.title}${s.description}${s.author}`.toLowerCase().includes(searchTerm.toLowerCase())) {
        return false;
      }
      return true;
    });
    switch (activeSort) {
      case "newest":
        list = list.slice().sort((a, b) => (a.updatedAt < b.updatedAt ? 1 : -1));
        break;
      case "rating":
        list = list.slice().sort((a, b) => b.rating - a.rating);
        break;
      case "popular":
        list = list.slice().sort((a, b) => b.readers - a.readers);
        break;
      case "trending":
      default:
        list = list.slice().sort((a, b) => b.readers * b.rating - a.readers * a.rating);
    }
    return list;
  }, [activeCategory, activeSort, searchTerm]);

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
        <header className="mb-6 flex flex-col gap-3">
          <span className="sw-eyebrow" style={{ color: "var(--sw-series-accent)" }}>
            <LibraryBig size={14} style={{ display: "inline", marginRight: 6, verticalAlign: -2 }} />
            系列 / Series
          </span>
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <h1 className="sw-heading-lg" style={{ color: "var(--sw-primary)" }}>
                书架 + 目录
              </h1>
              <p
                className="sw-body"
                style={{
                  color: "var(--sw-on-primary-container)",
                  marginTop: 8,
                  marginBottom: 0,
                }}
              >
                顶部是你的私人书架，下方是社区连载的完整目录。
              </p>
            </div>
            <Link
              href="/series/new"
              className="sw-button sw-button-secondary sw-focus-ring"
              style={{
                textDecoration: "none",
                background: "var(--sw-series-accent)",
                color: "var(--sw-on-secondary)",
              }}
            >
              <Plus size={16} /> 创建系列
            </Link>
          </div>
        </header>

        {/* SHELF */}
        <section
          aria-label="我的书架"
          style={{ marginBottom: 48 }}
        >
          <SectionHeading
            title="我的书架"
            icon={BookOpenCheck}
            count={SHELF.length}
            action={
              <Link
                href="/me/series"
                className="sw-label sw-focus-ring"
                style={{ color: "var(--sw-secondary)", textDecoration: "none", fontSize: 12 }}
              >
                管理书架 <ArrowRight size={12} style={{ display: "inline", marginLeft: 4, verticalAlign: -1 }} />
              </Link>
            }
          />

          {/* Spines */}
          <div
            className="flex gap-4 overflow-x-auto pt-6 pb-4"
            style={{
              background: "linear-gradient(180deg, var(--sw-surface-container-lowest) 0%, var(--sw-surface-container-low) 100%)",
              borderRadius: "var(--sw-radius-xl)",
              padding: "24px 20px",
              boxShadow: "inset 0 -6px 0 var(--sw-outline-variant)",
            }}
          >
            {SHELF.map((s) => (
              <BookSpine key={s.id} series={s} progress={SHELF_PROGRESS[s.id] ?? 0} />
            ))}
            {/* Add a "new" book */}
            <Link
              href="/series/new"
              className="sw-focus-ring"
              style={{
                width: 96,
                minHeight: 260,
                borderRadius: "var(--sw-radius-md)",
                border: "2px dashed var(--sw-outline-variant)",
                background: "transparent",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                gap: 8,
                color: "var(--sw-outline)",
                textDecoration: "none",
                fontSize: "var(--sw-text-xs)",
                fontWeight: 600,
              }}
            >
              <Plus size={20} />
              收藏新系列
            </Link>
          </div>

          {/* Progress cards */}
          <div
            className="grid"
            style={{
              gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
              gap: 14,
              marginTop: 16,
            }}
          >
            {SHELF.map((s) => (
              <ShelfCard
                key={s.id}
                series={s}
                progress={SHELF_PROGRESS[s.id] ?? 0}
              />
            ))}
          </div>
        </section>

        {/* CATALOG */}
        <section aria-label="系列目录">
          <SectionHeading
            title="系列目录"
            icon={Compass}
            count={sortedCatalog.length}
            action={
              <div className="flex items-center gap-2" style={{ color: "var(--sw-outline)" }}>
                <Bookmark size={14} />
                <span className="sw-label" style={{ fontSize: 12 }}>
                  已收录 {CATALOG.length} 个系列
                </span>
              </div>
            }
          />

          {/* Filter strip */}
          <div
            className="sw-card-elevated"
            style={{ padding: 16, marginTop: 16, marginBottom: 16 }}
            aria-label="目录筛选"
          >
            <div className="flex flex-wrap items-center gap-2" style={{ marginBottom: 12 }}>
              <Filter size={16} style={{ color: "var(--sw-series-accent)" }} />
              <span className="sw-label" style={{ color: "var(--sw-primary)" }}>
                分类
              </span>
              <div className="flex flex-wrap gap-1.5 ml-2">
                {CATEGORIES.map((c) => {
                  const active = activeCategory === c.id;
                  return (
                    <button
                      key={c.id}
                      type="button"
                      onClick={() => setActiveCategory(c.id)}
                      className="sw-focus-ring"
                      style={{
                        padding: "6px 12px",
                        borderRadius: "var(--sw-radius-full)",
                        background: active ? "var(--sw-series-accent)" : "var(--sw-surface-container)",
                        color: active ? "var(--sw-on-secondary)" : "var(--sw-on-primary-container)",
                        fontSize: "var(--sw-text-xs)",
                        fontWeight: 600,
                        border: "none",
                        cursor: "pointer",
                      }}
                    >
                      {c.label}
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
                  placeholder="搜索系列、作者、关键词…"
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

              <div className="flex items-center gap-1" role="tablist" aria-label="排序">
                {SORTS.map((s) => {
                  const Icon = s.icon;
                  const active = activeSort === s.id;
                  return (
                    <button
                      key={s.id}
                      type="button"
                      role="tab"
                      aria-selected={active}
                      onClick={() => setActiveSort(s.id)}
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
                      <Icon size={12} /> {s.label}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Catalog grid */}
          <div
            className="grid"
            style={{
              gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))",
              gap: 16,
            }}
          >
            {sortedCatalog.map((s) => (
              <CatalogCard key={s.id} series={s} />
            ))}
          </div>

          {sortedCatalog.length === 0 && (
            <div
              className="sw-card"
              style={{
                padding: 40,
                textAlign: "center",
                color: "var(--sw-on-primary-container)",
              }}
            >
              <p className="sw-body" style={{ margin: 0 }}>
                没有匹配的系列，试试调整筛选条件。
              </p>
            </div>
          )}

          <div className="flex justify-center pt-8">
            <button
              type="button"
              className="sw-button sw-button-ghost sw-focus-ring"
              style={{
                background: "var(--sw-surface-container)",
                color: "var(--sw-primary)",
              }}
            >
              加载更多 <ChevronRight size={14} />
            </button>
          </div>
        </section>
      </main>
    </div>
  );
}
