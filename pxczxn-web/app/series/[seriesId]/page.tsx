"use client";

import Link from "next/link";
import { ArrowLeft, BookOpen, Loader2 } from "lucide-react";
import { useEffect, useState } from "react";
import { UserTopbar } from "../../components/prototype-ui";
import { communityApi, type TeamSeries } from "../../lib/community-api";

export default function SeriesDetailPage({ params }: { params: Promise<{ seriesId: string }> }) {
  const [series, setSeries] = useState<TeamSeries | null>(null); const [error, setError] = useState("");
  useEffect(() => { let active = true; void params.then(({ seriesId }) => communityApi.seriesDetail(seriesId)).then((value) => { if (active) setSeries(value); }).catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "无法加载系列"); }); return () => { active = false; }; }, [params]);
  return <><UserTopbar title="系列" /><main className="page-shell" style={{ paddingTop: 30 }}>{error ? <p role="alert" style={{ color: "var(--danger)" }}>{error}</p> : !series ? <div className="text-center" style={{ padding: 72 }}><Loader2 className="animate-spin" /></div> : <><Link className="ghost-button" href="/series"><ArrowLeft size={16} /> 返回系列</Link><section className="surface" style={{ padding: 26, marginTop: 16 }}><BookOpen size={24} style={{ color: "var(--primary)" }} /><h1 style={{ marginBottom: 8 }}>{series.title}</h1><p className="secondary">{series.summary || "这个系列暂未添加简介。"}</p><p className="muted">{series.serializationStatus === "COMPLETED" ? "已完结" : series.serializationStatus === "PAUSED" ? "暂缓更新" : "连载中"}</p></section><section style={{ marginTop: 20 }}><h2 style={{ fontSize: 20 }}>章节</h2>{series.chapters.length === 0 ? <p className="secondary">暂时没有公开章节。</p> : <div className="stack">{series.chapters.map((chapter) => <Link key={chapter.articleId} className="surface" style={{ padding: 16, display: "flex", gap: 12, textDecoration: "none", color: "inherit" }} href={`/articles/${chapter.articleId}`}><strong>{chapter.chapterOrder}</strong><span>{chapter.title}</span></Link>)}</div>}</section></>}</main></>;
}
