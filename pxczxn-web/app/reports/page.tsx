"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Flag, Loader2 } from "lucide-react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, type CommunityReport } from "../lib/community-api";

const targetTypes: CommunityReport["targetType"][] = ["ARTICLE", "MOMENT", "COMMENT", "BLOG", "USER", "TEAM", "CHAT"];

export default function ReportsPage() {
  const [items, setItems] = useState<CommunityReport[]>([]); const [loading, setLoading] = useState(true); const [message, setMessage] = useState("");
  const [targetType, setTargetType] = useState<CommunityReport["targetType"]>("ARTICLE"); const [targetId, setTargetId] = useState(""); const [reasonCode, setReasonCode] = useState(""); const [description, setDescription] = useState("");
  const load = () => { setLoading(true); return communityApi.myReports().then(setItems).catch((error) => setMessage(error instanceof Error ? error.message : "加载失败")).finally(() => setLoading(false)); };
  useEffect(() => { let active = true; void communityApi.myReports().then((reports) => { if (active) setItems(reports); }).catch((error) => { if (active) setMessage(error instanceof Error ? error.message : "加载失败"); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, []);
  async function submit(event: FormEvent) { event.preventDefault(); setMessage(""); try { await communityApi.createReport({ targetType, targetId, reasonCode, description }); setTargetId(""); setReasonCode(""); setDescription(""); await load(); } catch (error) { setMessage(error instanceof Error ? error.message : "提交失败"); } }
  return <><UserTopbar title="举报中心" /><main className="page-shell stack" style={{ maxWidth: 960, paddingTop: 32 }}><header><h1 className="section-heading">举报中心</h1><p className="secondary">提交需要运营人员处理的社区内容或账户问题。</p></header><form className="surface stack" style={{ padding: 20 }} onSubmit={submit}><div style={{ display: "grid", gridTemplateColumns: "160px 1fr 1fr", gap: 12 }}><select className="field" value={targetType} onChange={(event) => setTargetType(event.target.value as CommunityReport["targetType"])}>{targetTypes.map((value) => <option key={value}>{value}</option>)}</select><input className="field" value={targetId} onChange={(event) => setTargetId(event.target.value)} placeholder="目标 ID" required /><input className="field" value={reasonCode} onChange={(event) => setReasonCode(event.target.value)} placeholder="举报原因代码" required /></div><textarea className="field textarea" value={description} onChange={(event) => setDescription(event.target.value)} placeholder="补充说明（可选）" /><button className="primary-button" type="submit"><Flag size={16} />提交举报</button>{message && <p role="alert" className="secondary">{message}</p>}</form><section className="surface stack" style={{ padding: 20 }}><h2 className="card-heading">我的举报</h2>{loading ? <Loader2 className="animate-spin" /> : items.length === 0 ? <p className="secondary">暂无举报记录。</p> : items.map((item) => <article key={item.id} className="list-row"><Flag size={17} /><div style={{ flex: 1 }}><strong>{item.targetType} #{item.targetId}</strong><p className="secondary" style={{ margin: "2px 0 0" }}>{item.reasonCode}{item.description ? ` · ${item.description}` : ""}</p></div><span className="chip">{item.status}</span></article>)}</section></main></>;
}
