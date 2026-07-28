"use client";

import { Loader2, ShieldBan, X } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, type CommunityBlock } from "../lib/community-api";

const targetTypes: CommunityBlock["targetType"][] = ["USER", "BLOG", "TAG", "CHAT"];

export default function BlocksPage() {
  const [items, setItems] = useState<CommunityBlock[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [targetType, setTargetType] = useState<CommunityBlock["targetType"]>("USER");
  const [targetId, setTargetId] = useState("");

  const load = () => communityApi.myBlocks()
    .then(setItems)
    .catch((error) => setMessage(error instanceof Error ? error.message : "加载失败"));

  useEffect(() => {
    let active = true;
    void communityApi.myBlocks()
      .then((blocks) => { if (active) setItems(blocks); })
      .catch((error) => { if (active) setMessage(error instanceof Error ? error.message : "加载失败"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");
    try {
      await communityApi.createBlock({ targetType, targetId });
      setTargetId("");
      await load();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "保存失败");
    } finally {
      setSubmitting(false);
    }
  }

  async function remove(item: CommunityBlock) {
    setMessage("");
    try {
      await communityApi.removeBlock(item.targetType, item.targetId);
      await load();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "取消屏蔽失败");
    }
  }

  return <>
    <UserTopbar title="屏蔽管理" />
    <main className="page-shell stack" style={{ maxWidth: 960, paddingTop: 32 }}>
      <header>
        <h1 className="section-heading">屏蔽管理</h1>
        <p className="secondary">屏蔽用户、博客、标签或私聊联系人后，相关内容不会出现在你的内容流、评论、私聊和通知中。</p>
      </header>
      <form className="surface stack" style={{ padding: 20 }} onSubmit={submit}>
        <div style={{ display: "grid", gridTemplateColumns: "180px 1fr", gap: 12 }}>
          <select className="field" value={targetType} onChange={(event) => setTargetType(event.target.value as CommunityBlock["targetType"])}>
            {targetTypes.map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
          <input className="field" value={targetId} onChange={(event) => setTargetId(event.target.value)} placeholder="目标 ID" required />
        </div>
        <button className="primary-button" disabled={submitting} type="submit">
          {submitting ? <Loader2 className="animate-spin" size={16} /> : <ShieldBan size={16} />} 屏蔽
        </button>
        {message && <p role="alert" className="secondary">{message}</p>}
      </form>
      <section className="surface stack" style={{ padding: 20 }}>
        <h2 className="card-heading">已屏蔽对象</h2>
        {loading ? <Loader2 className="animate-spin" /> : items.length === 0 ? <p className="secondary">暂无屏蔽项。</p> : items.map((item) => <article className="list-row" key={item.id}>
          <ShieldBan size={17} />
          <div style={{ flex: 1 }}><strong>{item.targetType} #{item.targetId}</strong><p className="secondary" style={{ margin: "2px 0 0" }}>{new Date(item.createdAt).toLocaleString()}</p></div>
          <button aria-label={`取消屏蔽 ${item.targetType} ${item.targetId}`} className="icon-button" onClick={() => void remove(item)} title="取消屏蔽" type="button"><X size={17} /></button>
        </article>)}
      </section>
    </main>
  </>;
}
