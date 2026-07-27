"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { ArrowDown, ArrowUp, Loader2, Trash2 } from "lucide-react";
import { UserTopbar } from "../../../components/prototype-ui";
import { communityApi, type TeamSeries, type TeamSeriesChapter } from "../../../lib/community-api";

export default function TeamSeriesWorkspacePage() {
  const params = useSearchParams(); const teamId = params.get("teamId");
  const [series, setSeries] = useState<TeamSeries[]>([]); const [articles, setArticles] = useState<TeamSeriesChapter[]>([]); const [title, setTitle] = useState(""); const [summary, setSummary] = useState(""); const [error, setError] = useState(""); const [loading, setLoading] = useState(true); const [saving, setSaving] = useState(false);
  const load = () => { if (!teamId) return; setLoading(true); Promise.all([communityApi.teamSeries(teamId), communityApi.teamSeriesArticles(teamId)]).then(([nextSeries, nextArticles]) => { setSeries(nextSeries); setArticles(nextArticles); setError(""); }).catch((cause: unknown) => setError(cause instanceof Error ? cause.message : "无法加载团队系列")).finally(() => setLoading(false)); };
  useEffect(() => {
    if (!teamId) return;
    let active = true;
    void Promise.all([communityApi.teamSeries(teamId), communityApi.teamSeriesArticles(teamId)])
      .then(([nextSeries, nextArticles]) => {
        if (!active) return;
        setSeries(nextSeries);
        setArticles(nextArticles);
        setError("");
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : "无法加载团队系列");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [teamId]);
  async function create() { if (!teamId || !title.trim()) return; setSaving(true); try { await communityApi.createTeamSeries(teamId, { title: title.trim(), summary: summary.trim() || undefined }); setTitle(""); setSummary(""); load(); } catch (cause) { setError(cause instanceof Error ? cause.message : "创建系列失败"); } finally { setSaving(false); } }
  async function saveChapters(item: TeamSeries, ids: string[]) { setSaving(true); try { await communityApi.saveSeriesChapters(item.id, ids, item.lockVersion); load(); } catch (cause) { setError(cause instanceof Error ? cause.message : "保存章节失败"); } finally { setSaving(false); } }
  async function submit(item: TeamSeries) { setSaving(true); try { await communityApi.submitSeriesReview(item.id, item.lockVersion); load(); } catch (cause) { setError(cause instanceof Error ? cause.message : "提交审核失败"); } finally { setSaving(false); } }
  return <><UserTopbar title="团队系列" /><main className="page-shell" style={{ paddingTop: 28 }}>{!teamId ? <p role="alert" style={{ color: "var(--danger)" }}>请选择团队工作台。</p> : loading ? <div className="text-center" style={{ padding: 72 }}><Loader2 className="animate-spin" /></div> : <><header><h1>团队系列</h1><p className="secondary">系列只能由获得团队授权的成员管理，公开前必须通过平台审核。提交前所有章节都必须已经发布。</p></header>{error && <p role="alert" style={{ color: "var(--danger)" }}>{error}</p>}<section className="surface" style={{ padding: 20, margin: "20px 0" }}><h2 style={{ fontSize: 18, marginTop: 0 }}>创建系列</h2><div className="stack"><input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="系列标题" maxLength={160} /><textarea value={summary} onChange={(event) => setSummary(event.target.value)} placeholder="系列简介" maxLength={1000} rows={3} /><button className="primary-button" disabled={saving || !title.trim()} onClick={() => void create()}>创建草稿</button></div></section>{series.length === 0 ? <p className="secondary">还没有团队系列。</p> : <div className="stack">{series.map((item) => <SeriesCard key={item.id} item={item} articles={articles} saving={saving} onSave={saveChapters} onSubmit={submit} />)}</div>}</>}</main></>;
}

function SeriesCard({ item, articles, saving, onSave, onSubmit }: { item: TeamSeries; articles: TeamSeriesChapter[]; saving: boolean; onSave: (item: TeamSeries, ids: string[]) => Promise<void>; onSubmit: (item: TeamSeries) => Promise<void> }) {
  const editable = item.reviewStatus === "DRAFT" || item.reviewStatus === "REJECTED";
  const memberIds = item.chapters.map((chapter) => chapter.articleId); const available = articles.filter((article) => !memberIds.includes(article.articleId));
  const move = (index: number, offset: number) => { const next = [...memberIds]; const target = index + offset; [next[index], next[target]] = [next[target], next[index]]; void onSave(item, next); };
  return <section className="surface" style={{ padding: 20 }}><h2 style={{ fontSize: 19, marginTop: 0 }}>{item.title}</h2><p className="secondary">{item.summary || "暂无简介"}</p><p className="muted">{labelStatus(item.reviewStatus)} · {labelSerialization(item.serializationStatus)}</p><div className="stack" style={{ marginTop: 14 }}>{item.chapters.length === 0 ? <p className="secondary">尚未编排章节。</p> : item.chapters.map((chapter, index) => <div key={chapter.articleId} className="surface" style={{ padding: 12, display: "flex", alignItems: "center", gap: 10 }}><strong>{index + 1}</strong><span style={{ flex: 1 }}>{chapter.title}<small className="muted"> · {chapter.publishStatus}</small></span>{editable && <><button className="icon-button" aria-label="上移章节" title="上移" disabled={saving || index === 0} onClick={() => move(index, -1)}><ArrowUp size={16} /></button><button className="icon-button" aria-label="下移章节" title="下移" disabled={saving || index === item.chapters.length - 1} onClick={() => move(index, 1)}><ArrowDown size={16} /></button><button className="icon-button" aria-label="移除章节" title="移除" disabled={saving} onClick={() => void onSave(item, memberIds.filter((id) => id !== chapter.articleId))}><Trash2 size={16} /></button></>}</div>)}</div>{editable && <div style={{ display: "flex", gap: 10, flexWrap: "wrap", marginTop: 16 }}><select aria-label="选择团队文章" defaultValue="" disabled={saving || available.length === 0} onChange={(event) => { if (event.target.value) { void onSave(item, [...memberIds, event.target.value]); event.currentTarget.value = ""; } }}><option value="">{available.length ? "加入团队文章" : "没有可加入的文章"}</option>{available.map((article) => <option key={article.articleId} value={article.articleId}>{article.title} ({article.publishStatus})</option>)}</select><button className="primary-button" disabled={saving || item.chapters.length === 0 || item.chapters.some((chapter) => chapter.publishStatus !== "PUBLISHED")} onClick={() => void onSubmit(item)}>提交审核</button></div>}{!editable && <p className="secondary" style={{ marginTop: 16 }}>该系列正在审核或已经公开，章节调整需等待新的审核流程。</p>}</section>;
}

function labelStatus(value: TeamSeries["reviewStatus"]) { return ({ DRAFT: "草稿", PENDING_REVIEW: "审核中", APPROVED: "已公开", REJECTED: "已驳回" }[value]); }
function labelSerialization(value: TeamSeries["serializationStatus"]) { return ({ ONGOING: "连载中", COMPLETED: "已完结", PAUSED: "暂缓更新" }[value]); }
