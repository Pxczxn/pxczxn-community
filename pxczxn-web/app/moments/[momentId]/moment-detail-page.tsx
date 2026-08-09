"use client";

import Link from "next/link";
import {
  AlertTriangle,
  BookOpen,
  Check,
  Edit3,
  ExternalLink,
  Hash,
  Heart,
  LayoutGrid,
  LoaderCircle,
  MessageCircle,
  Share2,
  TrendingUp,
} from "lucide-react";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { Avatar, EmptyState, UserTopbar } from "../../components/prototype-ui";
import {
  CommentThread,
  Moment,
  PlatformTag,
  communityApi,
  publicFileUrl,
} from "../../lib/community-api";
import {
  CommentRow,
  messageOf,
  relativeTime,
} from "../moment-parts";

/* ─── 右侧栏数据从 API 动态加载 ─── */

/* ─── 快捷操作（静态导航） ─── */
const QUICK_ACTIONS = [
  { icon: Edit3, label: "发布动态", desc: "分享你的想法与生活", href: "/moments" },
  { icon: BookOpen, label: "写文章", desc: "记录内容与分享", href: "/editor/new" },
  { icon: LayoutGrid, label: "返回动态广场", desc: "浏览更多精彩动态", href: "/moments" },
];

/** 格式化数字为简洁显示 */
function formatDetailCount(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}k`;
  return String(n);
}

export function MomentDetailPage({ momentId }: { momentId: string }) {
  const [moment, setMoment] = useState<Moment | null>(null);
  const [comments, setComments] = useState<CommentThread[]>([]);
  const [commentText, setCommentText] = useState("");
  const [busyAction, setBusyAction] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [commentSort, setCommentSort] = useState<"hot" | "new">("hot");
  const [hotTopics, setHotTopics] = useState<PlatformTag[]>([]);

  const updateMoment = useCallback((patch: Partial<Moment>) => {
    setMoment((current) => current ? { ...current, ...patch } : current);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    setNotFound(false);
    try {
      const detail = await communityApi.moment(momentId);
      setMoment(detail);
      const page = await communityApi.comments("MOMENT", momentId, 1, 20);
      setComments(page.records);
    } catch (requestError) {
      if (requestError instanceof Error && /404|不存在|已删除/.test(requestError.message)) {
        setNotFound(true);
      } else {
        setError(messageOf(requestError, "动态加载失败"));
      }
    } finally {
      setLoading(false);
    }
  }, [momentId]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      communityApi.tags().then((tags) => setHotTopics(tags.slice(0, 6))).catch(() => {});
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  async function toggleLike() {
    if (!moment) return;
    const action = `like-${moment.momentId}`;
    setBusyAction(action);
    setError("");
    try {
      const relationship = await communityApi.setLike("MOMENT", moment.momentId, !moment.liked);
      updateMoment({ liked: relationship.liked, likeCount: relationship.likeCount });
    } catch (requestError) {
      setError(messageOf(requestError, "点赞失败"));
    } finally {
      setBusyAction("");
    }
  }

  async function toggleFavorite() {
    if (!moment) return;
    const action = `favorite-${moment.momentId}`;
    setBusyAction(action);
    setError("");
    try {
      const relationship = await communityApi.setFavorite(
        "MOMENT",
        moment.momentId,
        !moment.favorited,
      );
      updateMoment({ favorited: relationship.favorited, favoriteCount: relationship.favoriteCount });
    } catch (requestError) {
      setError(messageOf(requestError, "收藏失败"));
    } finally {
      setBusyAction("");
    }
  }

  async function copyShare() {
    if (!moment) return;
    const url = new URL(`/moments/${moment.momentId}`, window.location.origin).toString();
    await navigator.clipboard.writeText(url);
    setNotice("动态链接已复制。");
  }

  async function submitComment(event: FormEvent) {
    event.preventDefault();
    if (!moment || !commentText.trim()) return;
    setBusyAction("comment");
    setError("");
    try {
      await communityApi.createComment("MOMENT", moment.momentId, commentText.trim());
      setCommentText("");
      const page = await communityApi.comments("MOMENT", moment.momentId, 1, 20);
      setComments(page.records);
      updateMoment({ commentCount: moment.commentCount + 1 });
    } catch (requestError) {
      setError(messageOf(requestError, "评论发布失败"));
    } finally {
      setBusyAction("");
    }
  }

  /* ─── 作者信息（从 moment 中提取） ─── */
  const authorName = moment?.author.displayName || moment?.author.username || "";
  const authorAvatar = moment ? publicFileUrl(moment.author.avatarFileId) : null;

  return (
    <>
      <UserTopbar title="动态详情" />
      <main className="page-shell moment-detail-page">
        {/* ════════════════ 主内容区 ════════════════ */}
        <div className="detail-main">
          {/* 页面标题 */}
          <h1 className="detail-main__title">动态详情</h1>

          {loading && (
            <div className="surface feed-loading" aria-busy="true">
              <LoaderCircle className="spin" size={23} /> 正在加载动态…
            </div>
          )}

          {!loading && notFound && (
            <div className="surface">
              <EmptyState title="动态不存在" description="这条动态可能已被删除，或链接有误。" />
            </div>
          )}

          {!loading && !notFound && !moment && error && (
            <div className="surface">
              <EmptyState title="加载失败" description={error} />
            </div>
          )}

          {!loading && !notFound && moment && (
            <>
              {/* ── 动态正文卡片 ── */}
              <article className="surface detail-article">
                {/* 作者行：头像 + 名字 + 认证 + 时间 + 更多 */}
                <header className="detail-article__header">
                  <div className="detail-author">
                    {authorAvatar ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img alt="" className="avatar avatar-lg avatar-image detail-author__avatar" src={authorAvatar} />
                    ) : (
                      <span className="detail-author__avatar-wrap"><Avatar label={authorName.slice(0, 1)} size="lg" /></span>
                    )}
                    <div className="detail-author__info">
                      <span className="detail-author__name-row">
                        <strong className="detail-author__name">{authorName}</strong>
                      </span>
                      <time className="detail-author__time">{relativeTime(moment.createdAt)}</time>
                    </div>
                  </div>
                </header>

                {/* 正文内容 */}
                <div className="detail-article__body">
                  {moment.renderedHtml ? (
                    <div className="detail-article__text" dangerouslySetInnerHTML={{ __html: moment.renderedHtml }} />
                  ) : (
                    <p className="detail-article__text">{moment.textContent}</p>
                  )}

                  {/* 外链卡片 */}
                  {moment.linkUrl && (
                    <a className="detail-link-card" href={moment.linkUrl} rel="noreferrer" target="_blank">
                      <ExternalLink size={14} />
                      <span>{moment.linkUrl}</span>
                    </a>
                  )}

                  {/* 关联文章预览 */}
                  {moment.article && moment.article.available && moment.article.canonicalPath && (
                    <Link className="detail-link-card" href={moment.article.canonicalPath}>
                      <BookOpen size={14} />
                      <span>
                        <strong>{moment.article.title}</strong>
                        <small>{moment.article.summary || "查看关联文章"}</small>
                      </span>
                    </Link>
                  )}

                  {/* 转发源 */}
                  {moment.repostSource && moment.repostSource.momentId && moment.repostSource.available && (
                    <Link className="detail-repost-card" href={`/moments/${moment.repostSource.momentId}`}>
                      <strong>@{moment.repostSource.author?.displayName || moment.repostSource.author?.username}</strong>
                      <p>{moment.repostSource.textContent}</p>
                    </Link>
                  )}
                </div>

                {/* 操作栏 */}
                <footer className="detail-actions">
                  <button
                    aria-pressed={moment.liked}
                    className={`detail-action-btn ${moment.liked ? "liked" : ""}`}
                    disabled={busyAction === `like-${moment.momentId}`}
                    onClick={toggleLike}
                    type="button"
                  >
                    <Heart size={17} fill={moment.liked ? "currentColor" : "none"} />
                    <span>点赞</span>
                    <strong>{moment.likeCount}</strong>
                  </button>
                  <button className="detail-action-btn" type="button">
                    <MessageCircle size={17} />
                    <span>评论</span>
                    <strong>{moment.commentCount}</strong>
                  </button>
                  <button
                    aria-pressed={moment.favorited}
                    className={`detail-action-btn ${moment.favorited ? "favorited" : ""}`}
                    disabled={busyAction === `favorite-${moment.momentId}`}
                    onClick={toggleFavorite}
                    type="button"
                  >
                    <Share2 size={17} />
                    <span>收藏</span>
                    <strong>{moment.favoriteCount}</strong>
                  </button>
                  <button className="detail-action-btn" onClick={copyShare} type="button">
                    <Share2 size={17} />
                    <span>分享</span>
                  </button>
                </footer>
              </article>

              {/* ── 评论区 ── */}
              <section className="surface detail-comments">
                <header className="detail-comments__header">
                  <h3>评论区（{moment.commentCount}）</h3>
                  <div className="detail-comments__sort">
                    <button
                      className={`detail-sort-tab ${commentSort === "hot" ? "active" : ""}`}
                      onClick={() => setCommentSort("hot")}
                      type="button"
                    >
                      最热
                    </button>
                    <button
                      className={`detail-sort-tab ${commentSort === "new" ? "active" : ""}`}
                      onClick={() => setCommentSort("new")}
                      type="button"
                    >
                      最新
                    </button>
                  </div>
                </header>

                {/* 评论输入框 */}
                <form className="detail-comment-form" onSubmit={submitComment}>
                  <Avatar label="我" size="sm" />
                  <div className="detail-comment-input-wrap">
                    <input
                      className="field detail-comment-input"
                      onChange={(event) => setCommentText(event.target.value)}
                      placeholder="写下你的评论..."
                      value={commentText}
                    />
                  </div>
                  <button
                    className="primary-button detail-comment-send"
                    disabled={busyAction === "comment" || !commentText.trim()}
                    type="submit"
                  >
                    发送
                  </button>
                </form>

                {/* 评论列表 */}
                <div className="detail-comments__list">
                  {comments.map((thread) => (
                    <CommentRow key={thread.root.commentId} thread={thread} />
                  ))}
                  {!comments.length && (
                    <p className="muted comments-empty">还没有评论，来聊聊你的看法。</p>
                  )}
                </div>
              </section>
            </>
          )}

          {(error || notice) && moment && (
            <div className={`inline-feedback ${error ? "error" : "success"}`} role="status">
              {error ? <AlertTriangle size={16} /> : <Check size={16} />}
              <span>{error || notice}</span>
            </div>
          )}
        </div>

        {/* ════════════════ 右侧边栏 ════════════════ */}
        <aside className="detail-sidebar stack">
          {/* ── 作者资料卡 ── */}
          {moment && (
            <section className="surface detail-author-card">
              <div className="detail-author-card__top">
                {authorAvatar ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img alt="" className="avatar avatar-xl avatar-image detail-author-card__avatar" src={authorAvatar} />
                ) : (
                  <span className="detail-author-card__avatar-wrap"><Avatar label={authorName.slice(0, 1)} size="lg" /></span>
                )}
                <div className="detail-author-card__identity">
                  <span className="detail-author-card__name-row">
                    <strong>{authorName}</strong>
                  </span>
                  <p className="detail-author-card__bio">
                    {moment.blog.name} · 该动态 {moment.likeCount} 次获赞 · {moment.commentCount} 条评论
                  </p>
                </div>
              </div>
              <div className="detail-author-card__stats">
                <div className="detail-stat">
                  <strong>该动态获赞</strong>
                  <span>{moment.likeCount}</span>
                </div>
                <div className="detail-stat">
                  <strong>该动态评论</strong>
                  <span>{moment.commentCount}</span>
                </div>
                <div className="detail-stat">
                  <strong>该动态收藏</strong>
                  <span>{moment.favoriteCount}</span>
                </div>
              </div>
              {moment.blog.slug ? (
                <Link className="detail-author-card__visit" href={`/blogs/${moment.blog.slug}`}>
                  访问博客 <ExternalLink size={13} />
                </Link>
              ) : (
                <span className="muted detail-author-card__visit" style={{ opacity: 0.5 }}>暂无公开博客</span>
              )}
            </section>
          )}

          {/* ── 相关动态（依赖话题体系，暂不展示） ── */}
          {false && (
          <section className="surface detail-side-section">
            <header className="detail-side-section__header">
              <h3>相关动态</h3>
            </header>
            <p className="muted" style={{ padding: "12px 16px", fontSize: 13 }}>
              相关动态推荐即将上线
            </p>
          </section>
          )}

          {/* ── 热门话题（来自平台标签 API） ── */}
          <section className="surface detail-side-section">
            <header className="detail-side-section__header">
              <h3><TrendingUp size={15} /> 热门话题</h3>
            </header>
            {hotTopics.length === 0 ? (
              <p className="muted" style={{ padding: "10px 16px", fontSize: 13 }}>加载中…</p>
            ) : (
              <div className="detail-topics-grid">
                {hotTopics.map((t) => (
                  <a key={t.tagId} className="detail-topic-pill" href={`/articles?tag=${t.slug}`}>
                    <Hash size={12} />
                    {t.name}
                    <span className="detail-topic-pill__count">{formatDetailCount(t.usageCount)}</span>
                  </a>
                ))}
              </div>
            )}
          </section>

          {/* ── 快捷操作 ── */}
          <section className="surface detail-side-section">
            <header className="detail-side-section__header">
              <h3>快捷操作</h3>
            </header>
            <ul className="detail-quick-list">
              {QUICK_ACTIONS.map((action) => (
                <li key={action.label}>
                  <Link className="detail-quick-item" href={action.href}>
                    <action.icon size={17} />
                    <span>
                      <strong>{action.label}</strong>
                      <small>{action.desc}</small>
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        </aside>
      </main>
    </>
  );
}
