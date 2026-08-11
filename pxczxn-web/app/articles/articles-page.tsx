"use client";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { Search, Loader2, ArrowLeft, ArrowRight, Clock, Flame, Sparkles, Hash } from "lucide-react";
import { UserTopbar, Avatar, EmptyState } from "../components/prototype-ui";
import { communityApi, type PublicArticleSummary, type PlatformTag, type DiscoverySort } from "../lib/community-api";

/* ---------- 子组件 ---------- */

function ArticleListItem({ item }: { item: PublicArticleSummary }) {
  const authorName = item.author.displayName || item.author.username;
  return (
    <article className="surface" style={{ padding: "14px 18px", borderRadius: 10, transition: "background 0.15s" }}>
      <Link href={item.canonicalPath} style={{ textDecoration: "none", color: "inherit" }}>
        <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, lineHeight: 1.4 }}>{item.title}</h3>
      </Link>
      {item.summary && (
        <p style={{ margin: "6px 0 0", fontSize: 13, color: "var(--text-secondary)", lineHeight: 1.6, display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>
          {item.summary}
        </p>
      )}
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 8, fontSize: 12, color: "var(--text-tertiary)" }}>
        <span style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <Avatar label={authorName.slice(0, 1)} size="sm" /> {authorName}
        </span>
        <span>{new Date(item.publishedAt).toLocaleDateString("zh-CN")}</span>
        <span>{item.viewCount} 阅读</span>
        <span>{item.likeCount} 赞</span>
      </div>
      {item.tags.length > 0 && (
        <div style={{ display: "flex", gap: 6, marginTop: 8, flexWrap: "wrap" }}>
          {item.tags.map(tag => (
            <Link key={tag.tagId} href={`/articles?tag=${encodeURIComponent(tag.slug)}`} className="chip" style={{ fontSize: 11, padding: "2px 8px", textDecoration: "none" }}>
              {tag.name}
            </Link>
          ))}
        </div>
      )}
    </article>
  );
}

function SidebarTags({ tags, currentTag, onSelect }: { tags: PlatformTag[]; currentTag: string; onSelect: (slug: string) => void }) {
  const sorted = [...tags].sort((a, b) => b.usageCount - a.usageCount).slice(0, 12);
  return (
    <div className="surface" style={{ padding: 16, borderRadius: 12 }}>
      <h3 style={{ margin: "0 0 10px", fontSize: 14, fontWeight: 700 }}>热门标签</h3>
      <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
        {sorted.map(tag => (
          <button
            key={tag.tagId}
            onClick={() => onSelect(tag.slug === currentTag ? "" : tag.slug)}
            className={tag.slug === currentTag ? "primary-button" : "chip"}
            style={{ fontSize: 12, padding: "3px 10px", cursor: "pointer" }}
            type="button"
          >
            {tag.name} ({tag.usageCount})
          </button>
        ))}
      </div>
    </div>
  );
}

/* ---------- 主组件 ---------- */

export function ArticlesPageView() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const currentSort = (searchParams.get("sort") || "QUALITY") as DiscoverySort;
  const currentTag = searchParams.get("tag") || "";
  const currentQ = searchParams.get("q") || "";
  const currentPage = Math.max(1, Number(searchParams.get("page")) || 1);

  const [articles, setArticles] = useState<PublicArticleSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [tags, setTags] = useState<PlatformTag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchInput, setSearchInput] = useState(currentQ);

  // Fetch articles
  useEffect(() => {
    let active = true;
    setLoading(true);
    communityApi.discoverRankedArticles(currentSort, currentPage, 20, currentTag || undefined, currentQ || undefined)
      .then(page => {
        if (!active) return;
        setArticles(page.records);
        setTotal(page.total);
      })
      .catch(err => {
        if (active) setError(err instanceof Error ? err.message : "加载失败");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [currentSort, currentTag, currentQ, currentPage]);

  // Fetch tags for sidebar
  useEffect(() => {
    communityApi.tags().then(setTags).catch(() => {});
  }, []);

  // URL update helper
  const updateParam = useCallback((key: string, value: string) => {
    const params = new URLSearchParams(searchParams.toString());
    if (value) params.set(key, value);
    else params.delete(key);
    if (key !== "page") params.delete("page"); // reset page on filter change
    router.push(`/articles?${params.toString()}`);
  }, [searchParams, router]);

  // Handle search submit
  const handleSearch = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    updateParam("q", searchInput.trim());
  }, [searchInput, updateParam]);

  const totalPages = Math.ceil(total / 20);

  return (
    <>
      <UserTopbar title="文章" />
      <main className="page-shell" style={{ maxWidth: 1100, margin: "0 auto", padding: "24px 20px" }}>
        {/* Compact Header */}
        <div style={{ marginBottom: 20 }}>
          <h1 style={{ margin: 0, fontSize: 22, fontWeight: 700 }}>文章</h1>
          <p style={{ margin: "4px 0 0", fontSize: 13, color: "var(--text-secondary)" }}>
            按公开时间浏览社区文章
          </p>
        </div>

        {/* Toolbar */}
        <div className="surface" style={{ padding: "12px 16px", borderRadius: 10, marginBottom: 20, display: "flex", flexWrap: "wrap", gap: 12, alignItems: "center" }}>
          <form onSubmit={handleSearch} style={{ display: "flex", alignItems: "center", gap: 6, flex: "1 1 200px" }}>
            <Search size={15} style={{ color: "var(--text-tertiary)" }} />
            <input
              type="text"
              placeholder="搜索文章..."
              value={searchInput}
              onChange={e => setSearchInput(e.target.value)}
              style={{ border: "none", background: "transparent", outline: "none", fontSize: 13, flex: 1, color: "var(--text-primary)" }}
            />
          </form>
          <div style={{ display: "flex", gap: 4 }}>
            {([["QUALITY", "综合", Sparkles], ["LATEST", "最新", Clock], ["LIKES", "热门", Flame]] as const).map(([key, label, Icon]) => (
              <button
                key={key}
                onClick={() => updateParam("sort", key)}
                className={currentSort === key ? "primary-button" : "ghost-button"}
                style={{ fontSize: 12, padding: "4px 10px", display: "flex", alignItems: "center", gap: 4 }}
                type="button"
              >
                <Icon size={13} /> {label}
              </button>
            ))}
          </div>
          {currentTag && (
            <span className="chip" style={{ fontSize: 12, display: "flex", alignItems: "center", gap: 4 }}>
              <Hash size={12} /> {tags.find(t => t.slug === currentTag)?.name || currentTag}
              <button onClick={() => updateParam("tag", "")} style={{ background: "none", border: "none", cursor: "pointer", padding: 0, color: "var(--text-tertiary)" }} type="button">×</button>
            </span>
          )}
        </div>

        {/* Content + Sidebar */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 260px", gap: 24 }}>
          <div>
            {loading && <div className="surface" style={{ padding: 32, textAlign: "center" }}><Loader2 className="spin" size={20} /> 加载中...</div>}
            {error && <div className="surface" style={{ padding: 16, color: "var(--color-error, #ef4444)" }}>{error}</div>}
            {!loading && !error && articles.length === 0 && (
              <EmptyState title="暂无文章" description="没有找到匹配的文章。" />
            )}
            {!loading && !error && articles.length > 0 && (
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                {articles.map(item => (
                  <ArticleListItem item={item} key={item.articleId} />
                ))}
              </div>
            )}
            {/* Pagination */}
            {totalPages > 1 && (
              <div style={{ display: "flex", justifyContent: "center", gap: 8, marginTop: 20 }}>
                <button
                  className="ghost-button"
                  disabled={currentPage <= 1}
                  onClick={() => updateParam("page", String(currentPage - 1))}
                  style={{ fontSize: 13, padding: "6px 12px" }}
                  type="button"
                >
                  <ArrowLeft size={14} /> 上一页
                </button>
                <span style={{ fontSize: 13, color: "var(--text-secondary)", lineHeight: "32px" }}>{currentPage} / {totalPages}</span>
                <button
                  className="ghost-button"
                  disabled={currentPage >= totalPages}
                  onClick={() => updateParam("page", String(currentPage + 1))}
                  style={{ fontSize: 13, padding: "6px 12px" }}
                  type="button"
                >
                  下一页 <ArrowRight size={14} />
                </button>
              </div>
            )}
          </div>

          {/* Sidebar */}
          <aside style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <SidebarTags tags={tags} currentTag={currentTag} onSelect={(slug) => updateParam("tag", slug)} />
          </aside>
        </div>
      </main>
    </>
  );
}
