"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { Loader2, Users } from "lucide-react";
import { UserTopbar } from "../../components/prototype-ui";
import { communityApi, type PublicArticlePage, type TeamPortal } from "../../lib/community-api";

export default function TeamDetailPage() {
  const params = useParams<{ teamSlug: string }>(); const slug = params.teamSlug; const [team, setTeam] = useState<TeamPortal | null>(null); const [articles, setArticles] = useState<PublicArticlePage | null>(null); const [error, setError] = useState("");
  useEffect(() => { let active=true; void Promise.all([communityApi.team(slug), communityApi.publicArticles(slug,1,10)]).then(([portal,page])=>{if(active){setTeam(portal);setArticles(page);}}).catch(e=>{if(active)setError(e instanceof Error?e.message:"加载失败");}); return()=>{active=false;}; },[slug]);
  if (error) return <><UserTopbar title="团队"/><main className="page-shell" style={{paddingTop:32}}><p role="alert" style={{color:"var(--danger)"}}>{error}</p></main></>;
  if (!team) return <><UserTopbar title="团队"/><main className="page-shell text-center" style={{paddingTop:80}}><Loader2 className="animate-spin"/></main></>;
  return <><UserTopbar title="团队"/><main className="page-shell" style={{paddingTop:28}}><section className="surface" style={{padding:28, marginBottom:20}}><Users size={26} style={{color:"var(--primary)"}}/><h1 style={{margin:"12px 0 6px"}}>{team.team.name}</h1><p className="secondary">{team.team.summary || "这个团队还没有简介。"}</p><p className="muted">由 {team.ownerDisplayName || "团队成员"} 维护 · {team.team.articleCount} 篇文章 · {team.team.followerCount} 位关注者</p></section><div style={{display:"grid",gridTemplateColumns:"minmax(0,2fr) minmax(220px,1fr)",gap:20}}><section><h2 style={{fontSize:18}}>最新文章</h2>{articles?.records.map(article=><Link key={article.articleId} href={`/articles/${article.articleId}`} className="surface" style={{display:"block",padding:18,marginBottom:10,textDecoration:"none",color:"inherit"}}><strong>{article.title}</strong><p className="secondary" style={{marginBottom:0}}>{article.summary}</p></Link>)}{articles?.records.length===0&&<p className="secondary">暂无公开文章。</p>}</section><aside className="surface" style={{padding:18}}><h2 style={{fontSize:18,marginTop:0}}>团队成员</h2>{team.members.map(member=><div key={member.userId} style={{padding:"9px 0",borderBottom:"1px solid var(--border)"}}><strong>{member.displayName || member.username}</strong><span className="muted" style={{marginLeft:8}}>{member.roleCode}</span></div>)}</aside></div></main></>;
}
