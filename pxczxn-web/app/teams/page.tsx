"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Users, Loader2 } from "lucide-react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, type TeamSummary } from "../lib/community-api";

export default function TeamsPage() {
  const [teams, setTeams] = useState<TeamSummary[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  useEffect(() => { let active = true; void communityApi.teams().then(v => { if (active) setTeams(v); }).catch(e => { if (active) setError(e instanceof Error ? e.message : "加载失败"); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, []);
  return <><UserTopbar title="团队" /><main className="page-shell" style={{ paddingTop: 30 }}><header style={{ marginBottom: 24 }}><h1 style={{ margin: 0 }}>团队博客</h1><p className="secondary">浏览团队正在创作的公开内容</p></header>{loading ? <div className="text-center" style={{ padding: 64 }}><Loader2 className="animate-spin" /></div> : error ? <p role="alert" style={{ color: "var(--danger)" }}>{error}</p> : teams.length === 0 ? <p className="secondary">暂无公开团队。</p> : <section style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill,minmax(260px,1fr))", gap: 16 }}>{teams.map(team => <Link key={team.teamId} href={`/teams/${team.slug}`} className="surface" style={{ padding: 20, textDecoration: "none", color: "inherit" }}><Users size={22} style={{ color: "var(--primary)" }} /><h2 style={{ fontSize: 18, margin: "14px 0 8px" }}>{team.name}</h2><p className="secondary" style={{ minHeight: 42 }}>{team.summary || "这个团队还没有简介。"}</p><span className="muted">{team.articleCount} 篇文章 · {team.followerCount} 位关注者</span></Link>)}</section>}</main></>;
}
