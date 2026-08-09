"use client";

import Link from "next/link";
import { Compass, Hash, Search, Tag as TagIcon } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { EmptyState, UserTopbar } from "../components/prototype-ui";
import { type PlatformTag, communityApi } from "../lib/community-api";

export default function TagsPage() {
  const [tags, setTags] = useState<PlatformTag[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");

  useEffect(() => {
    void communityApi.tags().then(setTags).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "标签加载失败"));
  }, []);

  const sortedTags = useMemo(() => [...tags].sort((a, b) => b.usageCount - a.usageCount), [tags]);
  const filteredTags = useMemo(
    () => sortedTags.filter(t => t.name.toLowerCase().includes(search.toLowerCase())),
    [sortedTags, search],
  );

  return (
    <>
      <UserTopbar title="标签" />
      <main className="page-shell tags-page" style={{ maxWidth: 900, margin: "0 auto", padding: "24px 20px" }}>
        <section className="tags-hero surface-lg shadow-sm" style={{ padding: "32px 28px", borderRadius: 14, marginBottom: 24 }}>
          <div className="tags-hero__copy">
            <span className="eyebrow"><Hash size={15} /> 平台标签</span>
            <h1 style={{ margin: "8px 0 0", fontSize: 26, fontWeight: 800 }}>按主题发现全站内容</h1>
            <p style={{ margin: "8px 0 0", fontSize: 14, color: "var(--text-secondary)", lineHeight: 1.6 }}>
              标签来自平台真实创作数据，使用次数按已关联公开内容聚合。
            </p>
            <div className="tags-hero__actions" style={{ marginTop: 12 }}>
              <Link href="/discover" className="primary-button">
                <Compass size={16} /> 去发现内容
              </Link>
            </div>
          </div>
          <div className="tags-hero__stats" style={{ display: "flex", gap: 24, marginTop: 16 }} aria-label="标签统计概览">
            <div><strong>{tags.length}</strong> <span>聚合标签</span></div>
            <div><strong>{tags.reduce((acc, t) => acc + t.usageCount, 0)}</strong> <span>内容引用</span></div>
            <div><strong>{tags.filter((t) => t.usageCount > 0).length}</strong> <span>活跃主题</span></div>
          </div>
        </section>

        {error && <div className="surface inline-feedback error" style={{ padding: 12 }}>{error}</div>}

        {!error && !tags.length && (
          <div className="surface" style={{ padding: 40, borderRadius: 16 }}>
            <EmptyState title="正在加载标签" description="公开标签会显示在这里。" />
          </div>
        )}

        {!error && tags.length > 0 && (
          <>
            <div className="tags-search" style={{ marginBottom: 16 }}>
              <div className="surface" style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 14px", borderRadius: 8 }}>
                <Search size={15} style={{ color: "var(--text-tertiary)" }} />
                <input
                  type="text"
                  placeholder="搜索标签..."
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                  style={{ border: "none", background: "transparent", outline: "none", fontSize: 13, flex: 1, color: "var(--text-primary)" }}
                />
              </div>
            </div>

            <section className="tags-grid shadow-sm" style={{ display: "flex", flexWrap: "wrap", gap: 10, padding: 20, borderRadius: 14 }}>
              {filteredTags.map((tag) => (
                <Link
                  className="tags-cloud-chip"
                  href={`/articles?tag=${encodeURIComponent(tag.slug)}`}
                  key={tag.tagId}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 6,
                    padding: "8px 14px",
                    borderRadius: 8,
                    background: "var(--bg-surface)",
                    border: "1px solid var(--border-default)",
                    textDecoration: "none",
                    color: "var(--text-primary)",
                    fontSize: 13,
                    transition: "border-color 0.15s",
                  }}
                >
                  <TagIcon size={13} style={{ color: "var(--primary)" }} />
                  <span>{tag.name}</span>
                  <small style={{ color: "var(--text-tertiary)", fontSize: 11 }}>{tag.usageCount}</small>
                </Link>
              ))}
              {filteredTags.length === 0 && (
                <p style={{ margin: 0, color: "var(--text-secondary)", fontSize: 13 }}>没有匹配的标签</p>
              )}
            </section>
          </>
        )}
      </main>
    </>
  );
}