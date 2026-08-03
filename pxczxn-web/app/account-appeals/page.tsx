"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { AccountEnforcementCase, communityApi } from "../lib/community-api";

export default function AccountAppealsPage() {
  const [cases, setCases] = useState<AccountEnforcementCase[]>([]);
  const [selected, setSelected] = useState("");
  const [statement, setStatement] = useState("");
  const [evidence, setEvidence] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => { communityApi.myAccountEnforcements().then(setCases).catch((error) => setMessage(error instanceof Error ? error.message : "加载失败")); }, []);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!selected || !statement.trim()) { setMessage("请选择措施并填写申诉说明"); return; }
    try { await communityApi.appealAccountEnforcement(selected, { statement: statement.trim(), evidenceSnapshot: evidence.trim() || undefined }); setMessage("申诉已提交，平台会按权限逐级复核"); setStatement(""); setEvidence(""); }
    catch (error) { setMessage(error instanceof Error ? error.message : "提交失败"); }
  }
  return <main className="page-shell stack"><Link className="link" href="/settings">返回账号与安全</Link><section className="surface stack"><div><p className="eyebrow">账号安全</p><h1>账号措施与申诉</h1><p>你可以查看对本人账号生效的强制措施，并在申诉期内提交说明和证据。</p></div>{message && <p className="notice">{message}</p>}<div className="stack">{cases.length === 0 ? <p>当前没有账号强制措施。</p> : cases.map((item) => <article className="list-row" key={item.id}><div><strong>{item.measureType}</strong><p>{item.userVisibleReason}</p><small>状态：{item.status}　到期：{item.expiresAt || "以平台通知为准"}</small></div><button className="secondary-button" onClick={() => setSelected(item.id)} type="button">选择申诉</button></article>)}</div></section><section className="surface stack"><h2>提交申诉</h2><form className="stack" onSubmit={submit}><label><span>所选措施</span><input className="field" readOnly value={selected} /></label><label><span>申诉说明</span><textarea className="field textarea" maxLength={2000} onChange={(event) => setStatement(event.target.value)} required value={statement} /></label><label><span>补充证据（可选）</span><textarea className="field textarea" maxLength={20000} onChange={(event) => setEvidence(event.target.value)} value={evidence} /></label><button className="primary-button" type="submit">提交申诉</button></form></section></main>;
}
