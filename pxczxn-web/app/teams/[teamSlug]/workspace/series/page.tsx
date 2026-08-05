"use client";

import Link from "next/link";
import { AlertCircle, ArrowDown, ArrowUp, ArrowUpRight, Loader2, Trash2 } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { communityApi, type TeamSeries, type TeamSeriesChapter } from "../../../../lib/community-api";
import { SERIALIZATION_LABELS, SERIES_REVIEW_LABELS } from "../../../team-labels";
import { useWorkspace } from "../workspace-context";

export default function WorkspaceSeriesPage() {
  const { teamId, teamSlug } = useWorkspace();
  const [series, setSeries] = useState<TeamSeries[]>([]);
  const [articles, setArticles] = useState<TeamSeriesChapter[]>([]);
  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback(() => {
    if (!teamId) return;
    setLoading(true);
    Promise.all([communityApi.teamSeries(teamId), communityApi.teamSeriesArticles(teamId)])
      .then(([nextSeries, nextArticles]) => { setSeries(nextSeries); setArticles(nextArticles); setError(""); })
      .catch((cause: unknown) => setError(cause instanceof Error ? cause.message : "无法加载团队系列"))
      .finally(() => setLoading(false));
  }, [teamId]);

  useEffect(() => {
    if (!teamId) return;
    let active = true;
    void Promise.all([communityApi.teamSeries(teamId), communityApi.teamSeriesArticles(teamId)])
      .then(([nextSeries, nextArticles]) => { if (active) { setSeries(nextSeries); setArticles(nextArticles); setError(""); } })
      .catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "无法加载团队系列"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [teamId]);

  async function run(action: () => Promise<unknown>) {
    setSaving(true);
    setError("");
    try {
      await action();
      load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "操作失败");
    } finally {
      setSaving(false);
    }
  }

  async function create() {
    if (!teamId || !title.trim()) return;
    await run(() => communityApi.createTeamSeries(teamId, { title: title.trim(), summary: summary.trim() || undefined }));
    setTitle("");
    setSummary("");
  }

  function saveChapters(item: TeamSeries, ids: string[]) {
    return run(() => communityApi.saveSeriesChapters(item.id, ids, item.lockVersion));
  }

  function submit(item: TeamSeries) {
    return run(() => communityApi.submitSeriesReview(item.id, item.lockVersion));
  }

  function update(item: TeamSeries, nextTitle: string, nextSummary: string) {
    return run(() => communityApi.updateTeamSeries(item.id, {
      title: nextTitle, summary: nextSummary || undefined, serializationStatus: item.serializationStatus,
      expectedLockVersion: item.lockVersion,
    }));
  }

  if (loading || !teamId) {
    return <div className="series-loading surface" aria-live="polite"><Loader2 className="animate-spin" size={22} /> 正在加载团队系列…</div>;
  }
  if (error && series.length === 0) {
    return (
      <section className="surface inline-feedback error" role="alert">
        <AlertCircle size={20} />
        <div><strong>系列暂时无法加载</strong><p>{error}</p></div>
      </section>
    );
  }

  return (
    <div className="workspace-series-page">
      <header className="workspace-content-page__header">
        <div>
          <span className="eyebrow">系列管理</span>
          <p>将团队文章编排成系列、专栏与连载，提交审核后公开展示。</p>
        </div>
      </header>

      {error && <p className="inline-feedback error" role="alert">{error}</p>}

      <section className="surface workspace-series-create">
        <h2>创建系列</h2>
        <div className="workspace-series-create__fields">
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="系列标题，例如：Spring Boot 从入门到项目实战"
            maxLength={160}
            aria-label="系列标题"
          />
          <textarea
            value={summary}
            onChange={(event) => setSummary(event.target.value)}
            placeholder="系列简介"
            maxLength={1000}
            rows={2}
            aria-label="系列简介"
          />
          <button className="primary-button" disabled={saving || !title.trim()} onClick={() => void create()}>
            创建草稿
          </button>
        </div>
      </section>

      {series.length === 0 ? (
        <section className="series-empty surface">
          <div>
            <h2>还没有团队系列</h2>
            <p>创建系列后，可以把团队成员的文章编排成完整知识内容。</p>
          </div>
        </section>
      ) : (
        <div className="workspace-series-list">
          {series.map((item) => (
            <SeriesCard
              key={item.id}
              item={item}
              articles={articles}
              teamSlug={teamSlug}
              saving={saving}
              onSave={saveChapters}
              onSubmit={submit}
              onUpdate={update}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function SeriesCard({
  item,
  articles,
  teamSlug,
  saving,
  onSave,
  onSubmit,
  onUpdate,
}: {
  item: TeamSeries;
  articles: TeamSeriesChapter[];
  teamSlug: string;
  saving: boolean;
  onSave: (item: TeamSeries, ids: string[]) => Promise<void>;
  onSubmit: (item: TeamSeries) => Promise<void>;
  onUpdate: (item: TeamSeries, title: string, summary: string) => Promise<void>;
}) {
  const editable = item.reviewStatus === "DRAFT" || item.reviewStatus === "REJECTED";
  const memberIds = item.chapters.map((chapter) => chapter.articleId);
  const available = articles.filter((article) => !memberIds.includes(article.articleId));
  const [editTitle, setEditTitle] = useState(item.title);
  const [editSummary, setEditSummary] = useState(item.summary ?? "");
  const [editing, setEditing] = useState(false);

  const move = (index: number, offset: number) => {
    const next = [...memberIds];
    const target = index + offset;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    void onSave(item, next);
  };

  return (
    <section className="surface workspace-series-card">
      <header className="workspace-series-card__header">
        {editing ? (
          <div className="workspace-series-card__edit">
            <input value={editTitle} onChange={(event) => setEditTitle(event.target.value)} maxLength={160} aria-label="系列标题" />
            <textarea value={editSummary} onChange={(event) => setEditSummary(event.target.value)} maxLength={1000} rows={2} aria-label="系列简介" />
            <div className="workspace-series-card__edit-actions">
              <button
                className="primary-button"
                disabled={saving || !editTitle.trim()}
                onClick={() => void onUpdate(item, editTitle.trim(), editSummary.trim()).then(() => setEditing(false))}
              >
                保存
              </button>
              <button className="ghost-button" onClick={() => { setEditing(false); setEditTitle(item.title); setEditSummary(item.summary ?? ""); }}>
                取消
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="workspace-series-card__identity">
              <h3>{item.title}</h3>
              <p className="secondary">{item.summary || "暂无简介"}</p>
              <p className="muted">
                {SERIES_REVIEW_LABELS[item.reviewStatus] || item.reviewStatus}
                {" · "}{SERIALIZATION_LABELS[item.serializationStatus] || item.serializationStatus}
                {" · "}{item.chapters.length} 章
              </p>
            </div>
            <div className="workspace-series-card__actions">
              {editable && <button className="ghost-button" onClick={() => { setEditing(true); setEditTitle(item.title); setEditSummary(item.summary ?? ""); }}>编辑</button>}
              {item.reviewStatus === "APPROVED" && (
                <Link className="ghost-button" href={`/teams/${teamSlug}?tab=series`}>
                  公开页 <ArrowUpRight size={14} />
                </Link>
              )}
            </div>
          </>
        )}
      </header>

      <div className="workspace-series-card__chapters">
        {item.chapters.length === 0 ? (
          <p className="secondary">尚未编排章节。</p>
        ) : (
          item.chapters.map((chapter, index) => (
            <div className="workspace-series-chapter" key={chapter.articleId}>
              <strong>{index + 1}</strong>
              <span>{chapter.title}<small className="muted"> · {chapter.publishStatus}</small></span>
              {editable && (
                <span className="workspace-series-chapter__tools">
                  <button className="icon-button" aria-label="上移章节" title="上移" disabled={saving || index === 0} onClick={() => move(index, -1)}><ArrowUp size={16} /></button>
                  <button className="icon-button" aria-label="下移章节" title="下移" disabled={saving || index === item.chapters.length - 1} onClick={() => move(index, 1)}><ArrowDown size={16} /></button>
                  <button className="icon-button" aria-label="移除章节" title="移除" disabled={saving} onClick={() => void onSave(item, memberIds.filter((id) => id !== chapter.articleId))}><Trash2 size={16} /></button>
                </span>
              )}
            </div>
          ))
        )}
      </div>

      {editable && (
        <div className="workspace-series-card__footer">
          <select
            aria-label="选择团队文章"
            defaultValue=""
            disabled={saving || available.length === 0}
            onChange={(event) => { if (event.target.value) { void onSave(item, [...memberIds, event.target.value]); event.currentTarget.value = ""; } }}
          >
            <option value="">{available.length ? "加入团队文章" : "没有可加入的文章"}</option>
            {available.map((article) => (
              <option key={article.articleId} value={article.articleId}>{article.title}（{article.publishStatus}）</option>
            ))}
          </select>
          <button
            className="primary-button"
            disabled={saving || item.chapters.length === 0 || item.chapters.some((chapter) => chapter.publishStatus !== "PUBLISHED")}
            onClick={() => void onSubmit(item)}
          >
            提交审核
          </button>
        </div>
      )}
      {!editable && <p className="secondary workspace-series-card__note">该系列正在审核或已经公开，章节调整需等待新的审核流程。</p>}
    </section>
  );
}
