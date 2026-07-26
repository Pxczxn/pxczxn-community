"use client";

import Link from "next/link";
import {
  AlertTriangle,
  Bookmark,
  Check,
  ChevronDown,
  Globe2,
  Heart,
  Image as ImageIcon,
  Link2,
  LoaderCircle,
  MessageCircle,
  MoreHorizontal,
  RefreshCw,
  Send,
  Share2,
  Smile,
} from "lucide-react";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { ArticleThumb, Avatar, EmptyState, UserTopbar } from "../components/prototype-ui";
import {
  CommentThread,
  CommunityApiError,
  Moment,
  communityApi,
  publicFileUrl,
} from "../lib/community-api";

export function MomentsCommunityPage({ initialMomentId }: { initialMomentId?: string }) {
  const [moments, setMoments] = useState<Moment[]>([]);
  const [selected, setSelected] = useState<Moment | null>(null);
  const [comments, setComments] = useState<CommentThread[]>([]);
  const [text, setText] = useState("");
  const [linkUrl, setLinkUrl] = useState("");
  const [visibility, setVisibility] = useState("PUBLIC");
  const [commentText, setCommentText] = useState("");
  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] = useState(false);
  const [busyAction, setBusyAction] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadComments = useCallback(async (momentId: string) => {
    try {
      const page = await communityApi.comments("MOMENT", momentId, 1, 20);
      setComments(page.records);
    } catch {
      setComments([]);
    }
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const page = await communityApi.moments(1, 20);
      let nextSelected = page.records[0] ?? null;
      if (initialMomentId) {
        nextSelected =
          page.records.find((item) => item.momentId === initialMomentId)
          ?? await communityApi.moment(initialMomentId);
      }
      setMoments(page.records);
      setSelected(nextSelected);
      if (nextSelected) await loadComments(nextSelected.momentId);
    } catch (requestError) {
      setError(messageOf(requestError, "动态加载失败"));
    } finally {
      setLoading(false);
    }
  }, [initialMomentId, loadComments]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  async function publish(event: FormEvent) {
    event.preventDefault();
    if (!text.trim() && !linkUrl.trim()) {
      setError("请先写下想分享的内容，或添加一个链接。");
      return;
    }
    setPublishing(true);
    setError("");
    setNotice("");
    try {
      const result = await communityApi.publishMoment({
        momentType: linkUrl.trim() ? "LINK" : "TEXT",
        textContent: text.trim() || undefined,
        linkUrl: linkUrl.trim() || undefined,
        visibility,
      });
      setMoments((current) => [result.moment, ...current]);
      setSelected(result.moment);
      setComments([]);
      setText("");
      setLinkUrl("");
      setNotice(result.moderationWarning ? "动态已发布，部分内容经过安全处理。" : "动态发布成功。");
    } catch (requestError) {
      setError(messageOf(requestError, "动态发布失败"));
    } finally {
      setPublishing(false);
    }
  }

  async function chooseMoment(moment: Moment) {
    setSelected(moment);
    setComments([]);
    await loadComments(moment.momentId);
  }

  async function toggleLike(moment: Moment) {
    const action = `like-${moment.momentId}`;
    setBusyAction(action);
    setError("");
    try {
      const relationship = await communityApi.setLike("MOMENT", moment.momentId, !moment.liked);
      updateMoment(moment.momentId, {
        liked: relationship.liked,
        likeCount: relationship.likeCount,
      });
    } catch (requestError) {
      setError(messageOf(requestError, "点赞失败"));
    } finally {
      setBusyAction("");
    }
  }

  async function toggleFavorite(moment: Moment) {
    const action = `favorite-${moment.momentId}`;
    setBusyAction(action);
    setError("");
    try {
      const relationship = await communityApi.setFavorite(
        "MOMENT",
        moment.momentId,
        !moment.favorited,
      );
      updateMoment(moment.momentId, {
        favorited: relationship.favorited,
        favoriteCount: relationship.favoriteCount,
      });
    } catch (requestError) {
      setError(messageOf(requestError, "收藏失败"));
    } finally {
      setBusyAction("");
    }
  }

  async function copyShare(moment: Moment) {
    const url = new URL(`/moments/${moment.momentId}`, window.location.origin).toString();
    await navigator.clipboard.writeText(url);
    setNotice("动态链接已复制。");
  }

  async function submitComment(event: FormEvent) {
    event.preventDefault();
    if (!selected || !commentText.trim()) return;
    setBusyAction("comment");
    setError("");
    try {
      await communityApi.createComment("MOMENT", selected.momentId, commentText.trim());
      setCommentText("");
      await loadComments(selected.momentId);
      updateMoment(selected.momentId, { commentCount: selected.commentCount + 1 });
    } catch (requestError) {
      setError(messageOf(requestError, "评论发布失败"));
    } finally {
      setBusyAction("");
    }
  }

  function updateMoment(momentId: string, patch: Partial<Moment>) {
    setMoments((current) =>
      current.map((item) => item.momentId === momentId ? { ...item, ...patch } : item),
    );
    setSelected((current) =>
      current?.momentId === momentId ? { ...current, ...patch } : current,
    );
  }

  return (
    <>
      <UserTopbar title="动态" />
      <main className="moments-page page-shell">
        <section className="moments-feed stack">
          <form className="surface composer" onSubmit={publish}>
            <textarea
              aria-label="分享你的想法"
              onChange={(event) => setText(event.target.value)}
              placeholder="分享你的想法..."
              value={text}
            />
            {linkUrl !== "" && (
              <input
                aria-label="动态链接"
                className="field composer-link-field"
                onChange={(event) => setLinkUrl(event.target.value)}
                placeholder="https://example.com"
                type="url"
                value={linkUrl}
              />
            )}
            <div className="composer-toolbar">
              <div>
                <button disabled type="button"><ImageIcon size={16} /> 图片</button>
                <button onClick={() => setLinkUrl((value) => value ? "" : "https://")} type="button">
                  <Link2 size={16} /> 链接
                </button>
                <button disabled type="button"><MessageCircle size={16} /> 投票</button>
                <button disabled type="button"><Smile size={16} /> 话题</button>
              </div>
              <div>
                <label className="visibility-button">
                  <Globe2 size={15} />
                  <select
                    aria-label="动态可见范围"
                    onChange={(event) => setVisibility(event.target.value)}
                    value={visibility}
                  >
                    <option value="PUBLIC">公开</option>
                    <option value="FOLLOWERS_ONLY">仅粉丝</option>
                    <option value="PRIVATE">仅自己</option>
                  </select>
                  <ChevronDown size={14} />
                </label>
                <button className="primary-button" disabled={publishing} type="submit">
                  {publishing ? <LoaderCircle className="spin" size={16} /> : <Send size={16} />}
                  {publishing ? "发布中" : "发布"}
                </button>
              </div>
            </div>
          </form>

          {(error || notice) && (
            <div className={`inline-feedback ${error ? "error" : "success"}`} role="status">
              {error ? <AlertTriangle size={16} /> : <Check size={16} />}
              <span>{error || notice}</span>
            </div>
          )}

          <div className="card-title-row">
            <h1 className="moments-title">动态广场</h1>
            <button aria-label="刷新动态" className="icon-button" onClick={load} type="button">
              <RefreshCw size={16} />
            </button>
          </div>

          {loading && (
            <div className="surface feed-loading" aria-busy="true">
              <LoaderCircle className="spin" size={23} /> 正在获取最新动态…
            </div>
          )}
          {!loading && !moments.length && (
            <div className="surface">
              <EmptyState title="动态广场还是空的" description="成为第一个分享想法的人吧。" />
            </div>
          )}
          {moments.map((moment) => (
            <MomentCard
              active={selected?.momentId === moment.momentId}
              busyAction={busyAction}
              key={moment.momentId}
              moment={moment}
              onChoose={() => void chooseMoment(moment)}
              onFavorite={() => void toggleFavorite(moment)}
              onLike={() => void toggleLike(moment)}
              onShare={() => void copyShare(moment)}
            />
          ))}
        </section>

        <aside className="surface moment-detail">
          <header>
            <h2 className="card-heading">动态详情</h2>
            {selected && (
              <Link aria-label="打开独立详情页" href={`/moments/${selected.momentId}`}>
                <MoreHorizontal size={18} />
              </Link>
            )}
          </header>
          {!selected ? (
            <EmptyState title="选择一条动态" description="点击左侧动态查看详情和评论。" />
          ) : (
            <>
              <MomentBody moment={selected} />
              <MomentActions
                busyAction={busyAction}
                moment={selected}
                onFavorite={() => void toggleFavorite(selected)}
                onLike={() => void toggleLike(selected)}
                onShare={() => void copyShare(selected)}
              />
              <section className="comments">
                <h3>评论（{selected.commentCount}）</h3>
                <form className="moment-comment-form" onSubmit={submitComment}>
                  <Avatar label="我" size="sm" />
                  <input
                    className="field"
                    onChange={(event) => setCommentText(event.target.value)}
                    placeholder="写下你的评论..."
                    value={commentText}
                  />
                  <button
                    aria-label="发布评论"
                    className="icon-button"
                    disabled={busyAction === "comment"}
                    type="submit"
                  >
                    <Send size={16} />
                  </button>
                </form>
                {comments.map((thread) => (
                  <CommentRow key={thread.root.commentId} thread={thread} />
                ))}
                {!comments.length && (
                  <p className="muted comments-empty">还没有评论，来聊聊你的看法。</p>
                )}
              </section>
            </>
          )}
        </aside>
      </main>
    </>
  );
}

function MomentCard({
  moment,
  active,
  busyAction,
  onChoose,
  onLike,
  onFavorite,
  onShare,
}: {
  moment: Moment;
  active: boolean;
  busyAction: string;
  onChoose: () => void;
  onLike: () => void;
  onFavorite: () => void;
  onShare: () => void;
}) {
  return (
    <article className={`surface moment-card ${active ? "moment-card--active" : ""}`}>
      <button className="moment-card__select" onClick={onChoose} type="button">
        <MomentBody moment={moment} />
      </button>
      <MomentActions
        busyAction={busyAction}
        moment={moment}
        onFavorite={onFavorite}
        onLike={onLike}
        onShare={onShare}
      />
    </article>
  );
}

function MomentBody({ moment }: { moment: Moment }) {
  const avatar = publicFileUrl(moment.author.avatarFileId);
  const authorName = moment.author.displayName || moment.author.username;
  return (
    <>
      <div className="moment-author-row">
        {avatar ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img alt="" className="avatar avatar-md avatar-image" src={avatar} />
        ) : (
          <Avatar label={authorName.slice(0, 1)} size="md" />
        )}
        <span>
          <strong>{authorName}</strong>
          <small>来自 {moment.blog.name} · {relativeTime(moment.createdAt)}</small>
        </span>
      </div>
      {moment.renderedHtml ? (
        <div className="moment-copy" dangerouslySetInnerHTML={{ __html: moment.renderedHtml }} />
      ) : (
        <p className="moment-copy">{moment.textContent}</p>
      )}
      {moment.linkUrl && (
        <a className="moment-link" href={moment.linkUrl} rel="noreferrer" target="_blank">
          {moment.linkUrl}
        </a>
      )}
      {moment.article && (
        moment.article.available && moment.article.canonicalPath ? (
          <Link className="link-preview" href={moment.article.canonicalPath}>
            <ArticleThumb variant={1} />
            <span>
              <strong>{moment.article.title}</strong>
              <small>{moment.article.summary || "查看关联文章"}</small>
            </span>
          </Link>
        ) : (
          <div className="link-preview compact muted">关联文章暂不可见</div>
        )
      )}
      {moment.repostSource && (
        <div className="repost-preview">
          {moment.repostSource.available ? (
            <>
              <strong>
                @{moment.repostSource.author?.displayName || moment.repostSource.author?.username}
              </strong>
              <p>{moment.repostSource.textContent}</p>
            </>
          ) : (
            <span className="muted">原动态已不可见</span>
          )}
        </div>
      )}
    </>
  );
}

function MomentActions({
  moment,
  busyAction,
  onLike,
  onFavorite,
  onShare,
}: {
  moment: Moment;
  busyAction: string;
  onLike: () => void;
  onFavorite: () => void;
  onShare: () => void;
}) {
  return (
    <footer className="moment-actions">
      <button
        aria-pressed={moment.liked}
        className={moment.liked ? "active" : ""}
        disabled={busyAction === `like-${moment.momentId}`}
        onClick={onLike}
        type="button"
      >
        <Heart fill={moment.liked ? "currentColor" : "none"} size={16} /> {moment.likeCount}
      </button>
      <button type="button"><MessageCircle size={16} /> {moment.commentCount}</button>
      <button onClick={onShare} type="button"><Share2 size={16} /> 分享</button>
      <button
        aria-pressed={moment.favorited}
        className={moment.favorited ? "active" : ""}
        disabled={busyAction === `favorite-${moment.momentId}`}
        onClick={onFavorite}
        type="button"
      >
        <Bookmark fill={moment.favorited ? "currentColor" : "none"} size={16} />
        {moment.favoriteCount || "收藏"}
      </button>
    </footer>
  );
}

function CommentRow({ thread }: { thread: CommentThread }) {
  const comment = thread.root;
  const authorName = comment.author?.displayName || comment.author?.username || "用户";
  return (
    <div className="comment-row">
      <Avatar label={authorName.slice(0, 1)} size="sm" />
      <span>
        <strong>{authorName}</strong>
        <div dangerouslySetInnerHTML={{ __html: comment.renderedHtml }} />
        <small>
          {relativeTime(comment.createdAt)}
          {thread.replyCount > 0 ? ` · ${thread.replyCount} 条回复` : ""}
        </small>
      </span>
    </div>
  );
}

function relativeTime(value: string) {
  const timestamp = new Date(/[zZ]|[+-]\d\d:\d\d$/.test(value) ? value : `${value}Z`).getTime();
  if (!Number.isFinite(timestamp)) return value;
  const seconds = Math.max(0, Math.floor((Date.now() - timestamp) / 1000));
  if (seconds < 60) return "刚刚";
  if (seconds < 3600) return `${Math.floor(seconds / 60)} 分钟前`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} 小时前`;
  if (seconds < 86400 * 7) return `${Math.floor(seconds / 86400)} 天前`;
  return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit" })
    .format(new Date(timestamp));
}

function messageOf(error: unknown, fallback: string) {
  if (error instanceof CommunityApiError && error.code === 401) {
    return "请先登录星语社区，再完成这项操作。";
  }
  return error instanceof Error ? error.message : fallback;
}
