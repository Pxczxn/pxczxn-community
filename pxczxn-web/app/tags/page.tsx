"use client";

import Link from "next/link";
import { Hash } from "lucide-react";
import { useEffect, useState } from "react";
import { EmptyState, UserTopbar } from "../components/prototype-ui";
import { type PlatformTag, communityApi } from "../lib/community-api";

export default function TagsPage() {
  const [tags, setTags] = useState<PlatformTag[]>([]);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { void communityApi.tags().then(setTags).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "标签加载失败")); }, []);
  return <><UserTopbar title="标签" /><main className="page-shell tag-page"><section className="surface-lg shadow-sm tag-page__hero"><span className="eyebrow"><Hash size={15} /> 平台标签</span><h1>按主题发现内容</h1><p>标签来自平台真实数据，使用次数仅统计已关联内容。</p></section><section className="surface tag-grid">{error && <p className="inline-feedback error">{error}</p>}{!error && !tags.length && <EmptyState title="正在加载标签" description="公开标签会显示在这里。" />}{tags.sort((a,b) => b.usageCount - a.usageCount).map(tag => <Link className="tag-card" href={`/articles?tag=${encodeURIComponent(tag.slug)}`} key={tag.tagId}><Hash size={18}/><strong>{tag.name}</strong><span>{tag.description || "浏览带有这个标签的公开内容"}</span><small>{tag.usageCount} 次使用</small></Link>)}</section></main></>;
}
