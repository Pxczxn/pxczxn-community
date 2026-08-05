"use client";

import { Loader2, ShieldBan, X, PlusCircle } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, type CommunityBlock } from "../lib/community-api";

const targetTypes: { value: CommunityBlock["targetType"]; label: string }[] = [
  { value: "USER", label: "用户 (USER)" },
  { value: "BLOG", label: "博客 (BLOG)" },
  { value: "TAG", label: "标签 (TAG)" },
  { value: "CHAT", label: "私聊 (CHAT)" },
];

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

  return (
    <>
      <UserTopbar title="屏蔽管理" />
      <main className="blocks-container page-shell">
        <header className="page-header-box">
          <h1>屏蔽管理</h1>
          <p>屏蔽用户、博客、标签或私聊联系人后，相关内容将不会出现在您的推荐流、动态、评论与私信通知中。</p>
        </header>

        <form className="card-surface stack" onSubmit={submit}>
          <h2 className="card-heading" style={{ fontSize: 16, margin: 0, marginBottom: 12 }}>添加屏蔽项</h2>
          <div className="block-form-grid">
            <select
              className="field"
              onChange={(event) => setTargetType(event.target.value as CommunityBlock["targetType"])}
              value={targetType}
            >
              {targetTypes.map((item) => (
                <option key={item.value} value={item.value}>{item.label}</option>
              ))}
            </select>
            <input
              className="field"
              onChange={(event) => setTargetId(event.target.value)}
              placeholder="请输入目标 ID、用户名或标识"
              required
              value={targetId}
            />
          </div>
          <button className="primary-button" disabled={submitting} type="submit" style={{ alignSelf: "flex-start" }}>
            {submitting ? <Loader2 className="spin" size={16} /> : <PlusCircle size={16} />}
            <span>添加屏蔽</span>
          </button>
          {message && <p className="notice" role="alert" style={{ marginTop: 12 }}>{message}</p>}
        </form>

        <section className="card-surface stack">
          <h2 className="card-heading" style={{ fontSize: 16, margin: 0, marginBottom: 12 }}>已屏蔽列表</h2>
          {loading ? (
            <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "16px 0", color: "var(--text-secondary)" }}>
              <Loader2 className="spin" size={16} />
              <span>正在加载屏蔽数据…</span>
            </div>
          ) : items.length === 0 ? (
            <p className="secondary" style={{ padding: "16px 0", margin: 0 }}>暂无屏蔽项。</p>
          ) : (
            <div className="stack" style={{ gap: 10 }}>
              {items.map((item) => (
                <article className="list-row" key={item.id} style={{ padding: "12px 16px", borderRadius: 10 }}>
                  <ShieldBan size={18} style={{ color: "var(--primary)", flexShrink: 0 }} />
                  <div style={{ flex: 1 }}>
                    <strong style={{ fontSize: 14 }}>{item.targetType} #{item.targetId}</strong>
                    <p className="secondary" style={{ margin: "2px 0 0", fontSize: 12 }}>
                      屏蔽于 {new Date(item.createdAt).toLocaleString()}
                    </p>
                  </div>
                  <button
                    aria-label={`取消屏蔽 ${item.targetType} ${item.targetId}`}
                    className="icon-button"
                    onClick={() => void remove(item)}
                    title="取消屏蔽"
                    type="button"
                  >
                    <X size={17} />
                  </button>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>
    </>
  );
}
