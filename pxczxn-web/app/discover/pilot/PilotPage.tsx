/**
 * PilotPage - Stitch v2.1_15 Editorial Mosaic Layout
 * 
 * A pilot implementation of the new Discover page layout featuring:
 * - Large Hero article with gradient overlay and editor's choice badge
 * - Mosaic grid for secondary articles
 * - Featured series section with book stack visual
 * - Right sidebar with popular tags, notable creators, recommended teams
 * 
 * Uses new Design Tokens (sw-) and AppNavbar component.
 * Accessible at /discover/pilot
 */

"use client";

import Link from "next/link";
import Image from "next/image";
import { 
  ArrowRight, 
  Clock, 
  TrendingUp,
  BookOpen,
  Users,
  Sparkles,
  Star,
  ChevronRight
} from "lucide-react";
import { AppNavbar } from "@/components/ui/navbar";

/* ─────────────────────────── Mock Data ─────────────────────────── */

const HERO_ARTICLE = {
  id: "hero-1",
  title: "深度学习在自然语言处理中的突破：从 Transformer 到 GPT 的演进",
  summary: "探索近年来 NLP 领域的重大技术变革，从注意力机制的提出到大型语言模型的崛起，剖析背后的核心原理与未来发展趋势。",
  author: {
    name: "李明远",
    avatar: "李",
  },
  coverImage: "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=1200&h=600&fit=crop",
  publishedAt: "2小时前",
  tags: ["深度学习", "NLP", "GPT", "AI"],
  isEditorsChoice: true,
  readTime: 12,
};

const MOSAIC_ARTICLES = [
  {
    id: "mosaic-1",
    title: "Rust 异步编程实战：Tokio 框架深度解析",
    summary: "深入理解 Rust 的异步运行时机制，掌握 Tokio 的核心用法。",
    author: "张伟",
    avatar: "张",
    publishedAt: "1小时前",
    tags: ["Rust", "异步编程"],
    readTime: 8,
    accent: "article",
  },
  {
    id: "mosaic-2",
    title: "TypeScript 5.0 装饰器：现代 Web 开发新范式",
    summary: "装饰器提案进入 Stage 3，探索其在框架和库中的实践应用。",
    author: "王芳",
    avatar: "王",
    publishedAt: "3小时前",
    tags: ["TypeScript", "Web"],
    readTime: 6,
    accent: "article",
  },
  {
    id: "mosaic-3",
    title: "分布式系统一致性：Raft 算法可视化指南",
    summary: "通过交互式图解深入理解 Raft 共识算法的核心机制。",
    author: "陈强",
    avatar: "陈",
    publishedAt: "5小时前",
    tags: ["分布式", "算法"],
    readTime: 10,
    accent: "article",
  },
  {
    id: "mosaic-4",
    title: "WebAssembly 在前端的无限可能",
    summary: "探索 WASM 如何突破 JavaScript 性能瓶颈，开启前端新纪元。",
    author: "刘洋",
    avatar: "刘",
    publishedAt: "7小时前",
    tags: ["WebAssembly", "前端"],
    readTime: 7,
    accent: "article",
  },
];

const FEATURED_SERIES = [
  {
    id: "series-1",
    title: "Rust 系统编程入门",
    description: "从零开始掌握 Rust 编程语言",
    articles: 12,
    accent: "series",
    color: "var(--sw-series-accent)",
  },
  {
    id: "series-2",
    title: "前端架构设计模式",
    description: "深入剖析 React/Vue 架构设计",
    articles: 8,
    accent: "series",
    color: "var(--sw-series-accent)",
  },
  {
    id: "series-3",
    title: "微服务治理实践",
    description: "完整覆盖服务网格、限流、熔断",
    articles: 15,
    accent: "series",
    color: "var(--sw-series-accent)",
  },
];

const POPULAR_TAGS = [
  { name: "深度学习", count: 234 },
  { name: "Rust", count: 189 },
  { name: "TypeScript", count: 156 },
  { name: "Kubernetes", count: 142 },
  { name: "Go", count: 128 },
  { name: "DevOps", count: 115 },
];

const NOTABLE_CREATORS = [
  { name: "李明远", specialty: "AI / 机器学习", avatar: "李" },
  { name: "张伟", specialty: "系统编程", avatar: "张" },
  { name: "王芳", specialty: "前端框架", avatar: "王" },
  { name: "陈强", specialty: "分布式系统", avatar: "陈" },
];

const RECOMMENDED_TEAMS = [
  { name: "星辰工作室", members: 12, focus: "AI 研究" },
  { name: "前端联盟", members: 8, focus: "Web 技术" },
  { name: "开源先锋", members: 15, focus: "基础设施" },
];

/* ─────────────────────────── Components ─────────────────────────── */

/* Hero Article Card */
function HeroCard({ article }: { article: typeof HERO_ARTICLE }) {
  return (
    <article
      className="sw-card sw-fade-in relative overflow-hidden"
      style={{
        minHeight: 420,
        display: "flex",
        flexDirection: "column",
        justifyContent: "flex-end",
      }}
    >
      {/* Cover Image */}
      <div className="absolute inset-0">
        <Image
          src={article.coverImage}
          alt={article.title}
          fill
          className="object-cover"
          priority
        />
        {/* Gradient Overlay */}
        <div
          className="absolute inset-0"
          style={{
            background: "linear-gradient(to top, rgba(9, 20, 38, 0.95) 0%, rgba(9, 20, 38, 0.6) 50%, rgba(9, 20, 38, 0.2) 100%)",
          }}
        />
      </div>

      {/* Content */}
      <div className="relative p-6 flex flex-col gap-4" style={{ color: "white" }}>
        {/* Badge Row */}
        <div className="flex items-center gap-2">
          {article.isEditorsChoice && (
            <span
              className="sw-badge"
              style={{
                background: "var(--sw-series-accent)",
                color: "white",
              }}
            >
              <Star size={12} />
              编辑精选
            </span>
          )}
          <span
            className="sw-badge accent-article"
          >
            深度学习
          </span>
        </div>

        {/* Title */}
        <Link href={`/articles/${article.id}`} style={{ textDecoration: "none" }}>
          <h2
            className="sw-heading-lg"
            style={{
              color: "white",
              fontFamily: "var(--font-heading), var(--sw-font-heading-fallback)",
            }}
          >
            {article.title}
          </h2>
        </Link>

        {/* Summary */}
        <p
          className="sw-body"
          style={{
            color: "rgba(255, 255, 255, 0.8)",
            display: "-webkit-box",
            WebkitLineClamp: 2,
            WebkitBoxOrient: "vertical",
            overflow: "hidden",
          }}
        >
          {article.summary}
        </p>

        {/* Meta Row */}
        <div className="flex items-center gap-4 pt-2">
          {/* Author */}
          <div className="flex items-center gap-2">
            <div
              className="sw-avatar sw-avatar-sm"
              style={{
                background: "var(--sw-secondary)",
                color: "var(--sw-on-secondary)",
                fontSize: "12px",
              }}
            >
              {article.author.avatar}
            </div>
            <span className="sw-label" style={{ color: "white" }}>
              {article.author.name}
            </span>
          </div>

          {/* Read Time */}
          <div className="flex items-center gap-1" style={{ color: "rgba(255, 255, 255, 0.6)" }}>
            <Clock size={14} />
            <span className="sw-label" style={{ fontSize: "12px" }}>
              {article.readTime} 分钟阅读
            </span>
          </div>

          {/* Published At */}
          <span className="sw-label" style={{ color: "rgba(255, 255, 255, 0.6)", fontSize: "12px" }}>
            {article.publishedAt}
          </span>
        </div>
      </div>
    </article>
  );
}

/* Mosaic Article Card */
function MosaicCard({ article }: { article: typeof MOSAIC_ARTICLES[0] }) {
  return (
    <article
      className="sw-card sw-fade-in"
      style={{
        padding: 20,
        display: "flex",
        flexDirection: "column",
        gap: 12,
      }}
    >
      {/* Author Row */}
      <div className="flex items-center gap-2">
        <div
          className="sw-avatar sw-avatar-sm"
          style={{
            background: "var(--sw-surface-container)",
            color: "var(--sw-primary)",
          }}
        >
          {article.avatar}
        </div>
        <span className="sw-label" style={{ color: "var(--sw-primary)" }}>
          {article.author}
        </span>
        <span
          className="sw-label"
          style={{ color: "var(--sw-outline)", fontSize: "12px" }}
        >
          · {article.publishedAt}
        </span>
      </div>

      {/* Title */}
      <Link href={`/articles/${article.id}`} style={{ textDecoration: "none" }}>
        <h3
          className="sw-heading-sm"
          style={{
            color: "var(--sw-primary)",
            display: "-webkit-box",
            WebkitLineClamp: 2,
            WebkitBoxOrient: "vertical",
            overflow: "hidden",
          }}
        >
          {article.title}
        </h3>
      </Link>

      {/* Summary */}
      <p
        className="sw-body"
        style={{
          color: "var(--sw-on-primary-container)",
          fontSize: "var(--sw-text-sm)",
          display: "-webkit-box",
          WebkitLineClamp: 2,
          WebkitBoxOrient: "vertical",
          overflow: "hidden",
        }}
      >
        {article.summary}
      </p>

      {/* Tags Row */}
      <div className="flex flex-wrap gap-2 pt-2">
        {article.tags.map((tag) => (
          <span key={tag} className="sw-tag">
            {tag}
          </span>
        ))}
        <span
          className="sw-label"
          style={{ color: "var(--sw-outline)", fontSize: "12px", marginLeft: "auto" }}
        >
          {article.readTime} 分钟
        </span>
      </div>
    </article>
  );
}

/* Series Card */
function SeriesCard({ series }: { series: typeof FEATURED_SERIES[0] }) {
  return (
    <Link
      href={`/series/${series.id}`}
      className="sw-card sw-fade-in"
      style={{
        padding: 20,
        display: "flex",
        flexDirection: "column",
        gap: 12,
        textDecoration: "none",
        borderLeft: `3px solid ${series.color}`,
      }}
    >
      {/* Header */}
      <div className="flex items-center gap-2">
        <BookOpen size={16} style={{ color: series.color }} />
        <span className="sw-eyebrow" style={{ color: series.color }}>
          系列
        </span>
      </div>

      {/* Title */}
      <h3
        className="sw-heading-sm"
        style={{ color: "var(--sw-primary)" }}
      >
        {series.title}
      </h3>

      {/* Description */}
      <p
        className="sw-body"
        style={{
          color: "var(--sw-on-primary-container)",
          fontSize: "var(--sw-text-sm)",
        }}
      >
        {series.description}
      </p>

      {/* Footer */}
      <div className="flex items-center justify-between pt-2">
        <span
          className="sw-label"
          style={{ color: "var(--sw-outline)", fontSize: "12px" }}
        >
          {series.articles} 篇文章
        </span>
        <ArrowRight size={16} style={{ color: series.color }} />
      </div>
    </Link>
  );
}

/* Sidebar Section */
function SidebarSection({ 
  title, 
  icon: Icon,
  children 
}: { 
  title: string; 
  icon: React.ElementType;
  children: React.ReactNode;
}) {
  return (
    <div className="sw-card" style={{ padding: 20 }}>
      {/* Header */}
      <div className="flex items-center gap-2 pb-4" style={{ borderBottom: "1px solid var(--sw-outline-variant)" }}>
        <Icon size={18} style={{ color: "var(--sw-secondary)" }} />
        <h3 className="sw-heading-sm" style={{ color: "var(--sw-primary)" }}>
          {title}
        </h3>
      </div>
      {/* Content */}
      <div className="pt-4">
        {children}
      </div>
    </div>
  );
}

/* Tag Item */
function TagItem({ tag }: { tag: typeof POPULAR_TAGS[0] }) {
  return (
    <Link
      href={`/articles?tag=${encodeURIComponent(tag.name)}`}
      className="flex items-center justify-between py-2 sw-focus-ring rounded-lg transition-colors"
      style={{ padding: "8px 12px", textDecoration: "none" }}
    >
      <div className="flex items-center gap-2">
        <span className="sw-tag" style={{ fontSize: "var(--sw-text-xs)" }}>
          {tag.name}
        </span>
      </div>
      <span className="sw-label" style={{ color: "var(--sw-outline)", fontSize: "11px" }}>
        {tag.count}
      </span>
    </Link>
  );
}

/* Creator Item */
function CreatorItem({ creator }: { creator: typeof NOTABLE_CREATORS[0] }) {
  return (
    <Link
      href={`/users/${creator.name}`}
      className="flex items-center gap-3 py-2 sw-focus-ring rounded-lg transition-colors"
      style={{ padding: "8px 12px", textDecoration: "none" }}
    >
      <div
        className="sw-avatar sw-avatar-sm"
        style={{
          background: "var(--sw-secondary)",
          color: "var(--sw-on-secondary)",
        }}
      >
        {creator.avatar}
      </div>
      <div className="flex flex-col">
        <span className="sw-label" style={{ color: "var(--sw-primary)" }}>
          {creator.name}
        </span>
        <span className="sw-label" style={{ color: "var(--sw-outline)", fontSize: "11px" }}>
          {creator.specialty}
        </span>
      </div>
    </Link>
  );
}

/* Team Item */
function TeamItem({ team }: { team: typeof RECOMMENDED_TEAMS[0] }) {
  return (
    <Link
      href={`/teams/${team.name}`}
      className="sw-focus-ring rounded-lg transition-colors"
      style={{
        padding: "12px",
        textDecoration: "none",
        background: "var(--sw-surface-container)",
        display: "flex",
        flexDirection: "column",
        gap: 8,
      }}
    >
      <div className="flex items-center justify-between">
        <span className="sw-heading-sm" style={{ color: "var(--sw-primary)" }}>
          {team.name}
        </span>
        <ChevronRight size={16} style={{ color: "var(--sw-outline)" }} />
      </div>
      <div className="flex items-center gap-3">
        <span className="sw-label" style={{ color: "var(--sw-outline)", fontSize: "12px" }}>
          {team.members} 位成员
        </span>
        <span
          className="sw-badge accent-team"
          style={{ fontSize: "10px", padding: "1px 6px" }}
        >
          {team.focus}
        </span>
      </div>
    </Link>
  );
}

/* ─────────────────────────── Main Component ─────────────────────────── */

export default function PilotPage() {
  return (
    <div
      className="sw-scrollbar"
      style={{
        background: "var(--sw-surface)",
        minHeight: "100vh",
      }}
    >
      {/* Navigation */}
      <AppNavbar />

      {/* Main Content */}
      <main className="page-shell py-8">
        {/* Page Header */}
        <header className="mb-8">
          <h1 className="sw-heading-lg" style={{ color: "var(--sw-primary)" }}>
            发现
          </h1>
          <p className="sw-body mt-2" style={{ color: "var(--sw-on-primary-container)" }}>
            探索最新文章、系列与创作者内容
          </p>
        </header>

        {/* Editorial Mosaic Layout */}
        <div
          className="grid gap-8"
          style={{
            gridTemplateColumns: "1fr 320px",
          }}
        >
          {/* Main Column */}
          <div className="flex flex-col gap-8">
            {/* Hero Section */}
            <section>
              <HeroCard article={HERO_ARTICLE} />
            </section>

            {/* Mosaic Grid */}
            <section className="grid gap-4" style={{ gridTemplateColumns: "repeat(2, 1fr)" }}>
              {MOSAIC_ARTICLES.map((article) => (
                <MosaicCard key={article.id} article={article} />
              ))}
            </section>

            {/* Featured Series */}
            <section>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Sparkles size={20} style={{ color: "var(--sw-series-accent)" }} />
                  <h2 className="sw-heading-md" style={{ color: "var(--sw-primary)" }}>
                    热门系列
                  </h2>
                </div>
                <Link
                  href="/series"
                  className="sw-label flex items-center gap-1 sw-focus-ring"
                  style={{ color: "var(--sw-secondary)", textDecoration: "none" }}
                >
                  查看全部
                  <ArrowRight size={14} />
                </Link>
              </div>
              <div className="grid gap-4" style={{ gridTemplateColumns: "repeat(3, 1fr)" }}>
                {FEATURED_SERIES.map((series) => (
                  <SeriesCard key={series.id} series={series} />
                ))}
              </div>
            </section>

            {/* Trending Section */}
            <section>
              <div className="flex items-center gap-2 mb-4">
                <TrendingUp size={20} style={{ color: "var(--sw-article-accent)" }} />
                <h2 className="sw-heading-md" style={{ color: "var(--sw-primary)" }}>
                  热门文章
                </h2>
              </div>
              <div className="flex flex-col gap-3">
                {MOSAIC_ARTICLES.slice(0, 3).map((article, index) => (
                  <Link
                    key={article.id}
                    href={`/articles/${article.id}`}
                    className="sw-card sw-fade-in flex items-center gap-4"
                    style={{
                      padding: 16,
                      textDecoration: "none",
                    }}
                  >
                    <span
                      className="sw-heading-lg"
                      style={{
                        color: index < 3 ? "var(--sw-article-accent)" : "var(--sw-outline)",
                        minWidth: 32,
                      }}
                    >
                      {index + 1}
                    </span>
                    <div className="flex-1">
                      <h3 className="sw-heading-sm" style={{ color: "var(--sw-primary)" }}>
                        {article.title}
                      </h3>
                      <span className="sw-label" style={{ color: "var(--sw-outline)", fontSize: "12px" }}>
                        {article.author} · {article.readTime} 分钟阅读
                      </span>
                    </div>
                  </Link>
                ))}
              </div>
            </section>
          </div>

          {/* Sidebar Column */}
          <aside className="flex flex-col gap-6">
            {/* Popular Tags */}
            <SidebarSection title="热门标签" icon={TrendingUp}>
              <div className="flex flex-col">
                {POPULAR_TAGS.map((tag) => (
                  <TagItem key={tag.name} tag={tag} />
                ))}
              </div>
              <Link
                href="/tags"
                className="sw-label flex items-center justify-center gap-1 mt-4 sw-focus-ring"
                style={{ 
                  color: "var(--sw-secondary)", 
                  textDecoration: "none",
                  padding: "8px",
                  borderRadius: "var(--sw-radius-md)",
                  background: "var(--sw-surface-container)",
                }}
              >
                查看全部标签
                <ArrowRight size={14} />
              </Link>
            </SidebarSection>

            {/* Notable Creators */}
            <SidebarSection title="优秀创作者" icon={Users}>
              <div className="flex flex-col">
                {NOTABLE_CREATORS.map((creator) => (
                  <CreatorItem key={creator.name} creator={creator} />
                ))}
              </div>
              <Link
                href="/creators"
                className="sw-label flex items-center justify-center gap-1 mt-4 sw-focus-ring"
                style={{ 
                  color: "var(--sw-secondary)", 
                  textDecoration: "none",
                  padding: "8px",
                  borderRadius: "var(--sw-radius-md)",
                  background: "var(--sw-surface-container)",
                }}
              >
                发现更多创作者
                <ArrowRight size={14} />
              </Link>
            </SidebarSection>

            {/* Recommended Teams */}
            <SidebarSection title="推荐团队" icon={BookOpen}>
              <div className="flex flex-col gap-3">
                {RECOMMENDED_TEAMS.map((team) => (
                  <TeamItem key={team.name} team={team} />
                ))}
              </div>
            </SidebarSection>
          </aside>
        </div>
      </main>

      {/* Footer */}
      <footer
        className="mt-16 py-8"
        style={{
          borderTop: "1px solid var(--sw-outline-variant)",
          background: "var(--sw-surface-container-lowest)",
        }}
      >
        <div className="page-shell flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div
              className="w-8 h-8 rounded-lg flex items-center justify-center"
              style={{ background: "var(--sw-primary)" }}
            >
              <Star size={16} style={{ color: "var(--sw-on-primary)" }} />
            </div>
            <span className="sw-label" style={{ color: "var(--sw-primary)" }}>
              星语社区 · 发现有趣的内容
            </span>
          </div>
          <div className="flex items-center gap-6">
            <Link href="/about" className="sw-label" style={{ color: "var(--sw-outline)", textDecoration: "none" }}>
              关于
            </Link>
            <Link href="/terms" className="sw-label" style={{ color: "var(--sw-outline)", textDecoration: "none" }}>
              服务条款
            </Link>
            <Link href="/privacy" className="sw-label" style={{ color: "var(--sw-outline)", textDecoration: "none" }}>
              隐私政策
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
