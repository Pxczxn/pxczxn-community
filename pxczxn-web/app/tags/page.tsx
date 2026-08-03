"use client";

import Link from "next/link";
import { BookOpen, Compass, Hash, Sparkles, Tag as TagIcon } from "lucide-react";
import { useEffect, useState } from "react";
import { EmptyState, UserTopbar } from "../components/prototype-ui";
import { type PlatformTag, communityApi } from "../lib/community-api";

export default function TagsPage() {
  const [tags, setTags] = useState<PlatformTag[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void communityApi.tags().then(setTags).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "标签加载失败"));
  }, []);

  const sortedTags = [...tags].sort((a, b) => b.usageCount - a.usageCount);

  return (
    <>
      <UserTopbar title="标签" />
      <main className="page-shell series-page">
        <section className="series-hero surface-lg shadow-sm">
          <div className="series-hero__copy">
            <span className="eyebrow"><Hash size={15} /> 平台标签</span>
            <h1>按主题发现全站内容</h1>
            <p>标签来自平台真实创作数据，使用次数按已关联公开内容聚合。</p>
            <div className="series-hero__actions">
              <Link href="/discover" className="primary-button">
                <Compass size={16} /> 去发现内容
              </Link>
            </div>
          </div>
          <div className="series-hero__stats" aria-label="标签统计概览">
            <div><strong>{tags.length}</strong><span>聚合标签</span></div>
            <div><strong>{tags.reduce((acc, t) => acc + t.usageCount, 0)}</strong><span>内容引用</span></div>
            <div><strong>{tags.filter((t) => t.usageCount > 0).length}</strong><span>活跃主题</span></div>
          </div>
        </section>

        {error && <div className="surface inline-feedback error">{error}</div>}

        {!error && !tags.length && (
          <div className="surface" style={{ padding: 40, borderRadius: 16 }}>
            <EmptyState title="正在加载标签" description="公开标签会显示在这里。" />
          </div>
        )}

        {!error && tags.length > 0 && (
          <section className="tags-cloud-section shadow-sm">
            {sortedTags.map((tag) => (
              <Link className="tag-cloud-chip" href={`/articles?tag=${encodeURIComponent(tag.slug)}`} key={tag.tagId}>
                <TagIcon size={13} />
                <span>{tag.name}</span>
                <small>{tag.usageCount}</small>
              </Link>
            ))}
          </section>
        )}
      </main>
    </>
  );
}

