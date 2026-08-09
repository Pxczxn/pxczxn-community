"use client";

import Link from "next/link";
import {
  AlertCircle,
  ArrowDown,
  ArrowUp,
  ArrowUpRight,
  BookOpen,
  Loader2,
  Trash2,
  Users,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { communityApi, type Series, type SeriesChapter } from "../lib/community-api";
import {
  SERIALIZATION_OPTIONS,
  isSeriesEditable,
  serializationLabel,
  seriesReviewLabel,
} from "../lib/series-labels";

/**
 * 作者侧系列管理器。个人博客与团队博客共用同一个组件，唯一差别只有 blogId 与文案。
 *
 * 之所以按 blogId 而不是 teamId 组织：系列的唯一归属事实就是 blog_id，
 * 团队工作台只是把 team.blogId 传进来而已，不需要第二套接口，也不会有第二份交互逻辑。
 */
export interface SeriesManagerProps {
  /** 系列所属博客。团队工作台传 team.blogId，个人中心传当前用户的 personalBlogId。 */
  blogId: string | null;
  /** 已公开系列的对外展示位置，用于"看公开页"跳转。 */
  publicHref?: string;
  /** 用在空态与提示里的称呼，例如"团队"或"你"。 */
  scopeNoun?: string;
  /** 可编排的文章来源说明，个人与团队的口径不同。 */
  articleSourceHint?: string;
}

export function SeriesManager({
  blogId,
  publicHref,
  scopeNoun = "你",
  articleSourceHint = "只有已发布的文章可以编排进连载。",
}: SeriesManagerProps) {
  const [series, setSeries] = useState<Series[]>([]);
  const [articles, setArticles] = useState<SeriesChapter[]>([]);
  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [status, setStatus] = useState<Series["serializationStatus"]>("ONGOING");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    if (!blogId) return;
    const [nextSeries, nextArticles] = await Promise.all([
      communityApi.blogSeries(blogId),
      communityApi.blogSeriesArticles(blogId),
    ]);
    setSeries(nextSeries);
    setArticles(nextArticles);
    setError("");
  }, [blogId]);

  useEffect(() => {
    if (!blogId) return;
    let active = true;
    void Promise.all([communityApi.blogSeries(blogId), communityApi.blogSeriesArticles(blogId)])
      .then(([nextSeries, nextArticles]) => {
        if (!active) return;
        setSeries(nextSeries);
        setArticles(nextArticles);
        setError("");
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : "无法加载连载内容");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [blogId]);

  async function run(action: () => Promise<unknown>) {
    setSaving(true);
    setError("");
    try {
      await action();
      await load();
      return true;
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "操作失败");
      return false;
    } finally {
      setSaving(false);
    }
  }

  async function create() {
    if (!blogId || !title.trim()) return;
    const ok = await run(() =>
      communityApi.createBlogSeries(blogId, {
        title: title.trim(),
        summary: summary.trim() || undefined,
        serializationStatus: status,
      }),
    );
    if (ok) {
      setTitle("");
      setSummary("");
      setStatus("ONGOING");
    }
  }

  function saveChapters(item: Series, ids: string[]) {
    return run(() => communityApi.saveSeriesChapters(item.id, ids, item.lockVersion));
  }

  function submit(item: Series) {
    return run(() => communityApi.submitSeriesReview(item.id, item.lockVersion));
  }

  function update(item: Series, patch: { title: string; summary: string; serializationStatus: Series["serializationStatus"] }) {
    return run(() =>
      communityApi.updateSeries(item.id, {
        title: patch.title,
        summary: patch.summary || undefined,
        serializationStatus: patch.serializationStatus,
        expectedLockVersion: item.lockVersion,
      }),
    );
  }

  if (!blogId || loading) {
    return (
      <div className="series-loading surface" aria-live="polite">
        <Loader2 className="animate-spin" size={22} /> 正在加载连载内容…
      </div>
    );
  }

  if (error && series.length === 0) {
    return (
      <section className="surface inline-feedback error" role="alert">
        <AlertCircle size={20} />
        <div>
          <strong>连载内容暂时无法加载</strong>
          <p>{error}</p>
        </div>
      </section>
    );
  }

  return (
    <div className="workspace-series-page">
      {error && (
        <p className="inline-feedback error" role="alert">
          {error}
        </p>
      )}

      <section className="surface workspace-series-create">
        <h2>创建连载</h2>
        <div className="workspace-series-create__fields">
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="连载标题，例如：Spring Boot 从入门到项目实战"
            maxLength={160}
            aria-label="连载标题"
          />
          <textarea
            value={summary}
            onChange={(event) => setSummary(event.target.value)}
            placeholder="连载简介：读者会先看到这段话，写清楚这部连载讲什么、适合谁读"
            maxLength={1000}
            rows={2}
            aria-label="连载简介"
          />
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value as Series["serializationStatus"])}
            aria-label="连载状态"
          >
            {SERIALIZATION_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <button className="primary-button" disabled={saving || !title.trim()} onClick={() => void create()}>
            创建草稿
          </button>
        </div>
        <p className="muted">{articleSourceHint}</p>
      </section>

      {series.length === 0 ? (
        <section className="series-empty surface">
          <span className="series-empty__icon">
            <BookOpen size={26} />
          </span>
          <div>
            <h2>还没有连载</h2>
            <p>创建连载后，可以把{scopeNoun}已发布的文章按顺序编排成完整连载，提交审核通过即可公开。</p>
          </div>
        </section>
      ) : (
        <div className="workspace-series-list">
          {series.map((item) => (
            <ManagedSeriesCard
              key={item.id}
              item={item}
              articles={articles}
              publicHref={publicHref}
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

function ManagedSeriesCard({
  item,
  articles,
  publicHref,
  saving,
  onSave,
  onSubmit,
  onUpdate,
}: {
  item: Series;
  articles: SeriesChapter[];
  publicHref?: string;
  saving: boolean;
  onSave: (item: Series, ids: string[]) => Promise<boolean>;
  onSubmit: (item: Series) => Promise<boolean>;
  onUpdate: (
    item: Series,
    patch: { title: string; summary: string; serializationStatus: Series["serializationStatus"] },
  ) => Promise<boolean>;
}) {
  const editable = isSeriesEditable(item.reviewStatus);
  const memberIds = item.chapters.map((chapter) => chapter.articleId);
  const available = articles.filter((article) => !memberIds.includes(article.articleId));
  const [editTitle, setEditTitle] = useState(item.title);
  const [editSummary, setEditSummary] = useState(item.summary ?? "");
  const [editStatus, setEditStatus] = useState<Series["serializationStatus"]>(item.serializationStatus);
  const [editing, setEditing] = useState(false);

  function startEditing() {
    setEditTitle(item.title);
    setEditSummary(item.summary ?? "");
    setEditStatus(item.serializationStatus);
    setEditing(true);
  }

  const move = (index: number, offset: number) => {
    const next = [...memberIds];
    const target = index + offset;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    void onSave(item, next);
  };

  const unpublished = item.chapters.filter((chapter) => chapter.publishStatus !== "PUBLISHED");

  return (
    <section className="surface workspace-series-card">
      <header className="workspace-series-card__header">
        {editing ? (
          <div className="workspace-series-card__edit">
            <input value={editTitle} onChange={(event) => setEditTitle(event.target.value)} maxLength={160} aria-label="连载标题" />
            <textarea value={editSummary} onChange={(event) => setEditSummary(event.target.value)} maxLength={1000} rows={2} aria-label="连载简介" />
            <select
              value={editStatus}
              onChange={(event) => setEditStatus(event.target.value as Series["serializationStatus"])}
              aria-label="连载状态"
            >
              {SERIALIZATION_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <div className="workspace-series-card__edit-actions">
              <button
                className="primary-button"
                disabled={saving || !editTitle.trim()}
                onClick={() =>
                  void onUpdate(item, {
                    title: editTitle.trim(),
                    summary: editSummary.trim(),
                    serializationStatus: editStatus,
                  }).then((ok) => {
                    if (ok) setEditing(false);
                  })
                }
              >
                保存
              </button>
              <button className="ghost-button" onClick={() => setEditing(false)}>
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
                <span className={`series-review-chip series-review-chip--${item.reviewStatus.toLowerCase()}`}>
                  {seriesReviewLabel(item.reviewStatus)}
                </span>
                {" · "}
                {serializationLabel(item.serializationStatus)}
                {" · "}
                {item.chapterCount} 章
                {item.followerCount > 0 && (
                  <>
                    {" · "}
                    <Users size={13} /> {item.followerCount} 人追更
                  </>
                )}
              </p>
            </div>
            <div className="workspace-series-card__actions">
              {editable && (
                <button className="ghost-button" onClick={startEditing}>
                  编辑
                </button>
              )}
              {item.reviewStatus === "APPROVED" && (
                <Link className="ghost-button" href={publicHref || `/series/${item.id}`}>
                  公开页 <ArrowUpRight size={14} />
                </Link>
              )}
            </div>
          </>
        )}
      </header>

      {item.reviewStatus === "REJECTED" && item.reviewComment && (
        <p className="inline-feedback warning" role="status">
          <AlertCircle size={16} /> 审核意见：{item.reviewComment}
        </p>
      )}

      <div className="workspace-series-card__chapters">
        {item.chapters.length === 0 ? (
          <p className="secondary">尚未编排章节。</p>
        ) : (
          item.chapters.map((chapter, index) => (
            <div className="workspace-series-chapter" key={chapter.articleId}>
              <strong>{index + 1}</strong>
              <span>
                {chapter.title}
                {chapter.publishStatus !== "PUBLISHED" && <small className="muted"> · 未发布</small>}
              </span>
              {editable && (
                <span className="workspace-series-chapter__tools">
                  <button className="icon-button" aria-label="上移章节" title="上移" disabled={saving || index === 0} onClick={() => move(index, -1)}>
                    <ArrowUp size={16} />
                  </button>
                  <button
                    className="icon-button"
                    aria-label="下移章节"
                    title="下移"
                    disabled={saving || index === item.chapters.length - 1}
                    onClick={() => move(index, 1)}
                  >
                    <ArrowDown size={16} />
                  </button>
                  <button
                    className="icon-button"
                    aria-label="移除章节"
                    title="移除"
                    disabled={saving}
                    onClick={() => void onSave(item, memberIds.filter((id) => id !== chapter.articleId))}
                  >
                    <Trash2 size={16} />
                  </button>
                </span>
              )}
            </div>
          ))
        )}
      </div>

      {editable && (
        <div className="workspace-series-card__footer">
          <select
            aria-label="选择文章加入连载"
            defaultValue=""
            disabled={saving || available.length === 0}
            onChange={(event) => {
              const value = event.target.value;
              if (!value) return;
              event.currentTarget.value = "";
              void onSave(item, [...memberIds, value]);
            }}
          >
            <option value="">{available.length ? "加入文章" : "没有可加入的文章"}</option>
            {available.map((article) => (
              <option key={article.articleId} value={article.articleId}>
                {article.title}
              </option>
            ))}
          </select>
          <button
            className="primary-button"
            disabled={saving || item.chapters.length === 0 || unpublished.length > 0}
            onClick={() => void onSubmit(item)}
            title={unpublished.length > 0 ? "存在未发布章节，无法提交审核" : undefined}
          >
            提交审核
          </button>
        </div>
      )}
      {!editable && <p className="secondary workspace-series-card__note">该连载正在审核或已经公开，章节调整需等待新的审核流程。</p>}
    </section>
  );
}
