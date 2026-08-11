/**
 * NewTeamsPage - Stitch v2.1_19 Collaboration Hub
 *
 *  - Hero banner introducing the team experience + join CTA
 *  - Featured team strip
 *  - Filterable team card grid (by focus, size, openness)
 *  - Right rail: my teams, recommended creators, recent activity
 * Uses --sw-team-accent as the visual anchor.
 */

"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import {
  ArrowRight,
  Compass,
  Filter,
  Globe2,
  Layers,
  Lock,
  Plus,
  Search,
  Sparkles,
  TrendingUp,
  UserPlus,
  Users,
} from "lucide-react";
import { AppNavbar } from "@/components/ui/navbar";

/* ─────────────────────────── Mock Data ─────────────────────────── */

type Team = {
  id: string;
  slug: string;
  name: string;
  description: string;
  focus: string;
  members: number;
  articles: number;
  series: number;
  cover: string;
  logoColor: string;
  isOpen: boolean;
  isVerified?: boolean;
  tags: string[];
  category: string;
  recentActivity: string;
  topMembers: string[];
};

const FEATURED: Team = {
  id: "feat-1",
  slug: "stellar-research",
  name: "星辰研究室",
  description:
    "围绕 LLM 评测与 Agent 工程化的研究小组，定期发布论文导读与开源工具。",
  focus: "AI / 评测",
  members: 18,
  articles: 42,
  series: 6,
  cover: "https://images.unsplash.com/photo-1465101046530-73398c7f28ca?w=1200&h=420&fit=crop",
  logoColor: "var(--sw-team-accent)",
  isOpen: true,
  isVerified: true,
  tags: ["AI", "LLM", "Agent"],
  category: "ai",
  recentActivity: "今天 14:32 新增 1 篇评测",
  topMembers: ["星", "辰", "研"],
};

const TEAMS: Team[] = [
  {
    id: "t-1",
    slug: "frontend-collective",
    name: "前端联盟",
    description: "聚焦现代前端工程与设计系统的协作型团队。",
    focus: "前端工程",
    members: 28,
    articles: 96,
    series: 9,
    cover: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&h=400&fit=crop",
    logoColor: "var(--sw-article-accent)",
    isOpen: true,
    isVerified: true,
    tags: ["React", "CSS", "设计系统"],
    category: "frontend",
    recentActivity: "昨天 21:08 团队新成员加入",
    topMembers: ["前", "端", "联"],
  },
  {
    id: "t-2",
    slug: "infra-pioneers",
    name: "基础设施先锋",
    description: "分布式系统、可观测性、可靠性工程的经验共享小组。",
    focus: "后端 / 基础设施",
    members: 22,
    articles: 71,
    series: 5,
    cover: "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&h=400&fit=crop",
    logoColor: "var(--sw-secondary)",
    isOpen: true,
    tags: ["分布式", "可观测性"],
    category: "backend",
    recentActivity: "3 小时前发布新系列",
    topMembers: ["先", "锋", "基"],
  },
  {
    id: "t-3",
    slug: "ux-craft",
    name: "UX 工坊",
    description: "设计师与研究员的协作小组，关注可访问性与可用性。",
    focus: "设计 / 可访问性",
    members: 14,
    articles: 38,
    series: 3,
    cover: "https://images.unsplash.com/photo-1559028012-481c04fa702d?w=600&h=400&fit=crop",
    logoColor: "var(--sw-series-accent)",
    isOpen: false,
    tags: ["UX", "A11y"],
    category: "design",
    recentActivity: "本周整理 4 份 A11y 报告",
    topMembers: ["U", "X", "工"],
  },
  {
    id: "t-4",
    slug: "mobile-monday",
    name: "Mobile Monday",
    description: "iOS / Android / 跨端的工程师与产品协作小组。",
    focus: "移动开发",
    members: 19,
    articles: 55,
    series: 4,
    cover: "https://images.unsplash.com/photo-1535303311164-664fc9ec6532?w=600&h=400&fit=crop",
    logoColor: "var(--sw-moment-accent)",
    isOpen: true,
    tags: ["iOS", "Android"],
    category: "mobile",
    recentActivity: "本周上线 2 篇 SwiftUI 实战",
    topMembers: ["M", "M", "移"],
  },
  {
    id: "t-5",
    slug: "data-tribe",
    name: "数据部落",
    description: "围绕数据工程、BI 与数据产品的实践分享。",
    focus: "数据 / 数据库",
    members: 16,
    articles: 47,
    series: 5,
    cover: "https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=600&h=400&fit=crop",
    logoColor: "var(--sw-team-accent)",
    isOpen: true,
    tags: ["数据", "PostgreSQL"],
    category: "backend",
    recentActivity: "今天 09:12 上线新书",
    topMembers: ["数", "据", "部"],
  },
  {
    id: "t-6",
    slug: "ai-ethics-circle",
    name: "AI 伦理圈",
    description: "关注 AI 安全、伦理与社会影响的读书与讨论小组。",
    focus: "AI / 伦理",
    members: 11,
    articles: 23,
    series: 2,
    cover: "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=600&h=400&fit=crop",
    logoColor: "var(--sw-secondary)",
    isOpen: false,
    isVerified: true,
    tags: ["AI", "伦理"],
    category: "ai",
    recentActivity: "上周组织 1 场读书会",
    topMembers: ["A", "I", "伦"],
  },
  {
    id: "t-7",
    slug: "rust-hub",
    name: "Rust 驿站",
    description: "Rust 中文社区的技术分享与开源协作小组。",
    focus: "Rust / 系统编程",
    members: 24,
    articles: 64,
    series: 6,
    cover: "https://images.unsplash.com/photo-1551033406-611cf9a28f67?w=600&h=400&fit=crop",
    logoColor: "var(--sw-series-accent)",
    isOpen: true,
    tags: ["Rust", "系统"],
    category: "backend",
    recentActivity: "昨天 16:00 直播分享",
    topMembers: ["R", "U", "S"],
  },
  {
    id: "t-8",
    slug: "design-systems-co",
    name: "设计系统协作社",
    description: "面向设计师与前端的设计系统共建组织。",
    focus: "设计系统",
    members: 13,
    articles: 29,
    series: 3,
    cover: "https://images.unsplash.com/photo-1481349518771-20055b2a7b24?w=600&h=400&fit=crop",
    logoColor: "var(--sw-moment-accent)",
    isOpen: true,
    tags: ["设计系统", "Figma"],
    category: "design",
    recentActivity: "本周更新 2 份规范",
    topMembers: ["设", "计", "系"],
  },
];

const CATEGORIES = [
  { id: "all", label: "全部" },
  { id: "frontend", label: "前端" },
  { id: "backend", label: "后端" },
  { id: "ai", label: "AI" },
  { id: "mobile", label: "移动" },
  { id: "design", label: "设计" },
];

const SIZES = [
  { id: "any", label: "不限" },
  { id: "small", label: "≤ 15 人", min: 0, max: 15 },
  { id: "mid", label: "16 - 30 人", min: 16, max: 30 },
  { id: "large", label: "> 30 人", min: 31, max: 9999 },
];

const SORTS = [
  { id: "trending", label: "趋势", icon: TrendingUp },
  { id: "active", label: "活跃", icon: Sparkles },
  { id: "size", label: "规模", icon: Users },
  { id: "newest", label: "最新", icon: Compass },
] as const;

type SortId = (typeof SORTS)[number]["id"];

const MY_TEAMS = [TEAMS[0], TEAMS[4]];

const RECOMMENDED_CREATORS = [
  { name: "高志远", focus: "eBPF / 性能", avatar: "高" },
  { name: "罗子涵", focus: "LLM 评测", avatar: "罗" },
  { name: "蔡安然", focus: "CSS 新特性", avatar: "蔡" },
  { name: "段星河", focus: "WebGPU", avatar: "段" },
];

const RECENT_ACTIVITY = [
  { team: "前端联盟", text: "新成员 \"王启明\" 加入", time: "2 小时前" },
  { team: "星辰研究室", text: "发布新系列「LLM 评测沙盒」", time: "今天 09:24" },
  { team: "数据部落", text: "更新团队章程", time: "昨天" },
];

/* ─────────────────────────── Sub-Components ─────────────────────────── */

function HeroBanner({ team }: { team: Team }) {
  return (
    <section
      className="sw-card-elevated"
      style={{
        position: "relative",
        overflow: "hidden",
        marginBottom: 32,
        background: "var(--sw-surface-container-lowest)",
      }}
      aria-label="精选团队"
    >
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: `linear-gradient(120deg, ${team.logoColor} 0%, color-mix(in srgb, ${team.logoColor} 60%, var(--sw-primary) 40%) 100%)`,
          opacity: 0.92,
        }}
      />
      <div
        style={{
          position: "relative",
          padding: "40px 32px",
          color: "white",
          display: "grid",
          gridTemplateColumns: "1fr auto",
          gap: 24,
          alignItems: "center",
        }}
      >
        <div className="flex flex-col gap-3" style={{ maxWidth: 640 }}>
          <span
            className="sw-eyebrow"
            style={{ color: "rgba(255,255,255,0.85)" }}
          >
            <Sparkles size={12} style={{ display: "inline", marginRight: 6, verticalAlign: -2 }} />
            本周精选
          </span>
          <h1
            className="sw-heading-lg"
            style={{ color: "white", margin: 0 }}
          >
            {team.name}
          </h1>
          <p
            className="sw-body"
            style={{ color: "rgba(255,255,255,0.92)", margin: 0 }}
          >
            {team.description}
          </p>
          <div
            className="flex flex-wrap items-center gap-3"
            style={{ color: "rgba(255,255,255,0.85)", fontSize: "var(--sw-text-sm)" }}
          >
            <span className="flex items-center gap-1">
              <Users size={14} /> {team.members} 位成员
            </span>
            <span>·</span>
            <span>{team.articles} 篇文章</span>
            <span>·</span>
            <span>{team.series} 个系列</span>
            <span>·</span>
            <span>{team.focus}</span>
          </div>
          <div className="flex flex-wrap items-center gap-2 mt-2">
            <Link
              href={`/teams/${team.slug}`}
              className="sw-button sw-focus-ring"
              style={{
                background: "var(--sw-surface-container-lowest)",
                color: team.logoColor,
                textDecoration: "none",
              }}
            >
              了解团队 <ArrowRight size={14} />
            </Link>
            <Link
              href={`/teams/${team.slug}/join`}
              className="sw-button sw-focus-ring"
              style={{
                background: "rgba(255,255,255,0.2)",
                color: "white",
                border: "1px solid rgba(255,255,255,0.4)",
                textDecoration: "none",
              }}
            >
              <UserPlus size={14} /> 申请加入
            </Link>
          </div>
        </div>

        {/* Stat cards */}
        <div
          className="grid"
          style={{
            gridTemplateColumns: "repeat(2, minmax(120px, 1fr))",
            gap: 12,
            minWidth: 280,
          }}
        >
          {[
            { label: "成员", value: team.members },
            { label: "文章", value: team.articles },
            { label: "系列", value: team.series },
            { label: "活动", value: "12 场 / 月" },
          ].map((s) => (
            <div
              key={s.label}
              style={{
                background: "rgba(255,255,255,0.16)",
                border: "1px solid rgba(255,255,255,0.25)",
                borderRadius: "var(--sw-radius-md)",
                padding: "12px 16px",
                backdropFilter: "blur(8px)",
                color: "white",
              }}
            >
              <div className="sw-label" style={{ color: "rgba(255,255,255,0.85)", fontSize: 11 }}>
                {s.label}
              </div>
              <div
                className="sw-heading-md"
                style={{ color: "white", fontSize: "var(--sw-text-2xl)", marginTop: 2 }}
              >
                {s.value}
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function TeamCard({ team }: { team: Team }) {
  return (
    <Link
      href={`/teams/${team.slug}`}
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
          aspectRatio: "16 / 7",
          background: `linear-gradient(135deg, ${team.logoColor} 0%, color-mix(in srgb, ${team.logoColor} 50%, var(--sw-primary) 50%) 100%)`,
        }}
      >
        <div
          style={{
            position: "absolute",
            inset: 0,
            background: "linear-gradient(180deg, rgba(0,0,0,0) 50%, rgba(0,0,0,0.45) 100%)",
          }}
        />
        <div
          className="flex items-center gap-2"
          style={{
            position: "absolute",
            top: 12,
            left: 12,
            color: "white",
          }}
        >
          <div
            className="sw-avatar sw-avatar-sm"
            style={{
              background: "var(--sw-surface-container-lowest)",
              color: team.logoColor,
              fontSize: "12px",
            }}
          >
            {team.topMembers[0]}
          </div>
          {team.isVerified && (
            <span
              className="sw-badge"
              style={{
                background: "rgba(255,255,255,0.85)",
                color: team.logoColor,
                fontSize: 11,
              }}
            >
              官方认证
            </span>
          )}
        </div>
        <div
          className="flex items-center gap-1"
          style={{
            position: "absolute",
            top: 12,
            right: 12,
            color: "white",
            fontSize: "var(--sw-text-xs)",
            background: "rgba(0,0,0,0.35)",
            padding: "2px 8px",
            borderRadius: "var(--sw-radius-full)",
            backdropFilter: "blur(4px)",
          }}
        >
          {team.isOpen ? (
            <>
              <Globe2 size={11} /> 公开招募
            </>
          ) : (
            <>
              <Lock size={11} /> 邀请制
            </>
          )}
        </div>
      </div>

      {/* Body */}
      <div style={{ padding: 18, display: "flex", flexDirection: "column", gap: 10 }}>
        <div>
          <h3
            className="sw-heading-sm"
            style={{ color: "var(--sw-primary)", margin: 0 }}
          >
            {team.name}
          </h3>
          <span
            className="sw-label"
            style={{ color: "var(--sw-outline)", fontSize: 12 }}
          >
            {team.focus}
          </span>
        </div>
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
          {team.description}
        </p>
        <div className="flex flex-wrap gap-1.5">
          {team.tags.map((t) => (
            <span key={t} className="sw-tag" style={{ fontSize: "11px" }}>
              {t}
            </span>
          ))}
        </div>
        <div
          className="flex items-center justify-between"
          style={{ color: "var(--sw-outline)", fontSize: "var(--sw-text-xs)" }}
        >
          <span className="flex items-center gap-3">
            <span className="flex items-center gap-1">
              <Users size={12} /> {team.members}
            </span>
            <span className="flex items-center gap-1">
              <Layers size={12} /> {team.articles}
            </span>
          </span>
          <span style={{ color: "var(--sw-team-accent)" }}>{team.recentActivity}</span>
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
      <Icon size={18} style={{ color: "var(--sw-team-accent)" }} />
      <h2 className="sw-heading-md" style={{ color: "var(--sw-primary)" }}>
        {title}
      </h2>
      {count !== undefined && (
        <span
          className="sw-label"
          style={{ color: "var(--sw-outline)", fontSize: 12, marginLeft: 4 }}
        >
          ({count})
        </span>
      )}
      {action && <div className="ml-auto">{action}</div>}
    </div>
  );
}

function SidebarCard({ title, icon: Icon, children }: { title: string; icon: React.ElementType; children: React.ReactNode }) {
  return (
    <div className="sw-card" style={{ padding: 18 }}>
      <div
        className="flex items-center gap-2 pb-3"
        style={{ borderBottom: "1px solid var(--sw-outline-variant)" }}
      >
        <Icon size={16} style={{ color: "var(--sw-team-accent)" }} />
        <h3 className="sw-heading-sm" style={{ color: "var(--sw-primary)" }}>
          {title}
        </h3>
      </div>
      <div className="pt-3 flex flex-col gap-3">{children}</div>
    </div>
  );
}

/* ─────────────────────────── Main Component ─────────────────────────── */

export default function NewTeamsPage() {
  const [activeCategory, setActiveCategory] = useState("all");
  const [activeSize, setActiveSize] = useState("any");
  const [openOnly, setOpenOnly] = useState(false);
  const [activeSort, setActiveSort] = useState<SortId>("trending");
  const [searchTerm, setSearchTerm] = useState("");

  const filteredTeams = useMemo(() => {
    const size = SIZES.find((s) => s.id === activeSize) ?? SIZES[0];
    const minMembers = size.min ?? 0;
    const maxMembers = size.max ?? Number.POSITIVE_INFINITY;
    let list = TEAMS.filter((t) => {
      if (activeCategory !== "all" && t.category !== activeCategory) return false;
      if (openOnly && !t.isOpen) return false;
      if (t.members < minMembers || t.members > maxMembers) return false;
      if (searchTerm && !`${t.name}${t.description}${t.focus}`.toLowerCase().includes(searchTerm.toLowerCase())) {
        return false;
      }
      return true;
    });
    switch (activeSort) {
      case "active":
        list = list.slice().sort((a, b) => b.articles + b.series - (a.articles + a.series));
        break;
      case "size":
        list = list.slice().sort((a, b) => b.members - a.members);
        break;
      case "newest":
        list = list.slice().sort((a, b) => (a.recentActivity < b.recentActivity ? 1 : -1));
        break;
      case "trending":
      default:
        list = list.slice().sort((a, b) => b.members * b.articles - a.members * a.articles);
    }
    return list;
  }, [activeCategory, activeSize, openOnly, activeSort, searchTerm]);

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
          <span className="sw-eyebrow" style={{ color: "var(--sw-team-accent)" }}>
            <Users size={14} style={{ display: "inline", marginRight: 6, verticalAlign: -2 }} />
            团队 / Teams
          </span>
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <h1 className="sw-heading-lg" style={{ color: "var(--sw-primary)" }}>
                协作中心
              </h1>
              <p
                className="sw-body"
                style={{
                  color: "var(--sw-on-primary-container)",
                  marginTop: 8,
                  marginBottom: 0,
                }}
              >
                找到一群同频的创作者，一起把想法做成长期作品。
              </p>
            </div>
            <Link
              href="/teams/new"
              className="sw-button sw-button-primary sw-focus-ring"
              style={{
                background: "var(--sw-team-accent)",
                color: "var(--sw-on-secondary)",
                textDecoration: "none",
              }}
            >
              <Plus size={16} /> 创建团队
            </Link>
          </div>
        </header>

        {/* Hero banner */}
        <HeroBanner team={FEATURED} />

        {/* Main grid */}
        <div
          className="grid"
          style={{
            gridTemplateColumns: "minmax(0, 1fr) 320px",
            gap: 24,
            alignItems: "start",
          }}
        >
          {/* Teams column */}
          <section aria-label="团队列表">
            <SectionHeading
              title="发现团队"
              icon={Compass}
              count={filteredTeams.length}
              action={
                <span
                  className="sw-label"
                  style={{ color: "var(--sw-outline)", fontSize: 12 }}
                >
                  共 {TEAMS.length} 个公开团队
                </span>
              }
            />

            {/* Filter strip */}
            <div
              className="sw-card-elevated"
              style={{ padding: 16, marginTop: 16, marginBottom: 16 }}
              aria-label="筛选"
            >
              <div className="flex flex-wrap items-center gap-2" style={{ marginBottom: 12 }}>
                <Filter size={16} style={{ color: "var(--sw-team-accent)" }} />
                <span className="sw-label" style={{ color: "var(--sw-primary)" }}>
                  方向
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
                          background: active ? "var(--sw-team-accent)" : "var(--sw-surface-container)",
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
                    placeholder="搜索团队、方向…"
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

                <select
                  aria-label="团队规模"
                  value={activeSize}
                  onChange={(e) => setActiveSize(e.target.value)}
                  className="sw-input"
                  style={{
                    padding: "8px 12px",
                    minWidth: 140,
                    color: "var(--sw-primary)",
                    fontSize: "var(--sw-text-sm)",
                  }}
                >
                  {SIZES.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.label}
                    </option>
                  ))}
                </select>

                <label
                  className="flex items-center gap-2"
                  style={{ color: "var(--sw-on-primary-container)", fontSize: "var(--sw-text-sm)" }}
                >
                  <input
                    type="checkbox"
                    checked={openOnly}
                    onChange={(e) => setOpenOnly(e.target.checked)}
                    style={{ accentColor: "var(--sw-team-accent)" }}
                  />
                  仅显示公开招募
                </label>

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

            {/* Team cards grid */}
            <div
              className="grid"
              style={{
                gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
                gap: 16,
              }}
            >
              {filteredTeams.map((t) => (
                <TeamCard key={t.id} team={t} />
              ))}
            </div>

            {filteredTeams.length === 0 && (
              <div
                className="sw-card"
                style={{
                  padding: 40,
                  textAlign: "center",
                  color: "var(--sw-on-primary-container)",
                }}
              >
                <p className="sw-body" style={{ margin: 0 }}>
                  没有匹配的团队，试试调整筛选条件。
                </p>
              </div>
            )}

            <div className="flex justify-center pt-6">
              <button
                type="button"
                className="sw-button sw-button-ghost sw-focus-ring"
                style={{
                  background: "var(--sw-surface-container)",
                  color: "var(--sw-primary)",
                }}
              >
                加载更多 <ArrowRight size={14} />
              </button>
            </div>
          </section>

          {/* Sidebar */}
          <aside
            className="flex flex-col gap-4"
            style={{ position: "sticky", top: 88 }}
            aria-label="个人与动态"
          >
            <SidebarCard title="我的团队" icon={Users}>
              {MY_TEAMS.map((t) => (
                <Link
                  key={t.id}
                  href={`/teams/${t.slug}`}
                  className="sw-focus-ring"
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 10,
                    padding: 8,
                    borderRadius: "var(--sw-radius-md)",
                    background: "var(--sw-surface-container)",
                    textDecoration: "none",
                    color: "var(--sw-primary)",
                  }}
                >
                  <div
                    className="sw-avatar sw-avatar-sm"
                    style={{
                      background: t.logoColor,
                      color: "var(--sw-on-secondary)",
                      fontSize: "12px",
                    }}
                  >
                    {t.topMembers[0]}
                  </div>
                  <div className="min-w-0" style={{ flex: 1 }}>
                    <div className="sw-label" style={{ color: "var(--sw-primary)" }}>
                      {t.name}
                    </div>
                    <div
                      className="sw-label"
                      style={{ color: "var(--sw-outline)", fontSize: 11 }}
                    >
                      {t.members} 位成员 · {t.articles} 篇文章
                    </div>
                  </div>
                  <ArrowRight size={14} style={{ color: "var(--sw-outline)" }} />
                </Link>
              ))}
              <Link
                href="/teams/requests"
                className="sw-button sw-button-ghost sw-focus-ring"
                style={{ justifyContent: "flex-start", textDecoration: "none" }}
              >
                <UserPlus size={14} /> 查看加入申请
              </Link>
            </SidebarCard>

            <SidebarCard title="推荐创作者" icon={Sparkles}>
              {RECOMMENDED_CREATORS.map((c) => (
                <div key={c.name} className="flex items-center gap-3">
                  <div
                    className="sw-avatar sw-avatar-sm"
                    style={{
                      background: "var(--sw-secondary)",
                      color: "var(--sw-on-secondary)",
                      fontSize: "12px",
                    }}
                  >
                    {c.avatar}
                  </div>
                  <div className="min-w-0" style={{ flex: 1 }}>
                    <div className="sw-label" style={{ color: "var(--sw-primary)" }}>
                      {c.name}
                    </div>
                    <div
                      className="sw-label"
                      style={{ color: "var(--sw-outline)", fontSize: 11 }}
                    >
                      {c.focus}
                    </div>
                  </div>
                  <button
                    type="button"
                    className="sw-focus-ring"
                    style={{
                      padding: "4px 10px",
                      borderRadius: "var(--sw-radius-full)",
                      background: "var(--sw-team-accent)",
                      color: "var(--sw-on-secondary)",
                      border: "none",
                      fontSize: "var(--sw-text-xs)",
                      fontWeight: 600,
                      cursor: "pointer",
                    }}
                  >
                    邀请
                  </button>
                </div>
              ))}
            </SidebarCard>

            <SidebarCard title="最近动态" icon={TrendingUp}>
              {RECENT_ACTIVITY.map((a, i) => (
                <div
                  key={`${a.team}-${i}`}
                  style={{
                    padding: 8,
                    borderRadius: "var(--sw-radius-md)",
                    background: "var(--sw-surface-container)",
                  }}
                >
                  <div
                    className="sw-label"
                    style={{ color: "var(--sw-team-accent)", fontSize: 12 }}
                  >
                    {a.team}
                  </div>
                  <div
                    className="sw-body"
                    style={{
                      color: "var(--sw-primary)",
                      fontSize: "var(--sw-text-sm)",
                      marginTop: 2,
                    }}
                  >
                    {a.text}
                  </div>
                  <div
                    className="sw-label"
                    style={{ color: "var(--sw-outline)", fontSize: 11, marginTop: 4 }}
                  >
                    {a.time}
                  </div>
                </div>
              ))}
            </SidebarCard>
          </aside>
        </div>
      </main>
    </div>
  );
}
