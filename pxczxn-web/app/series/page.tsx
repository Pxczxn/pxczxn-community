"use client";

import Link from "next/link";
import { BookOpen, Loader2 } from "lucide-react";
import { useEffect, useState } from "react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, type TeamSeries } from "../lib/community-api";

export default function SeriesPage() {
  const [series, setSeries] = useState<TeamSeries[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  useEffect(() => { let active = true; void communityApi.series().then((items) => { if (active) setSeries(items); }).catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "无法加载系列"); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, []);
  return <><UserTopbar title="系列" /><main className="page-shell" style={{ paddingTop: 30 }}><header style={{ marginBottom: 24 }}><h1 style={{ margin: 0 }}>文章系列</h1><p className="secondary">按章节顺序阅读已经通过平台审核的团队连载。</p></header>{loading ? <div className="text-center" style={{ padding: 72 }}><Loader2 className="animate-spin" /></div> : error ? <p role="alert" style={{ color: "var(--danger)" }}>{error}</p> : series.length === 0 ? <p className="secondary">还没有公开系列。</p> : <section style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill,minmax(280px,1fr))", gap: 16 }}>{series.map((item) => <Link className="surface" style={{ padding: 20, color: "inherit", textDecoration: "none" }} key={item.id} href={`/series/${item.id}`}><BookOpen size={22} style={{ color: "var(--primary)" }} /><h2 style={{ fontSize: 18, margin: "14px 0 8px" }}>{item.title}</h2><p className="secondary" style={{ minHeight: 44 }}>{item.summary || "这个系列暂未添加简介。"}</p><span className="muted">{status(item.serializationStatus)} · {item.chapters.length} 篇公开章节</span></Link>)}</section>}</main></>;
}

function status(value: TeamSeries["serializationStatus"]) { return ({ ONGOING: "连载中", COMPLETED: "已完结", PAUSED: "暂缓更新" }[value]); }
