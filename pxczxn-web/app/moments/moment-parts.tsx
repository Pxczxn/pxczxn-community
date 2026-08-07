"use client";

import Link from "next/link";
import { Bookmark, Heart, MessageCircle, Share2 } from "lucide-react";
import { ArticleThumb, Avatar } from "../components/prototype-ui";
import {
  CommentThread,
  CommunityApiError,
  Moment,
  publicFileUrl,
} from "../lib/community-api";

/** 动态正文：作者行 + 文本/外链/关联文章/转发源。列表页与详情页共用。 */
export function MomentBody({ moment }: { moment: Moment }) {
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
        moment.repostSource.momentId && moment.repostSource.available ? (
          <Link className="repost-preview" href={`/moments/${moment.repostSource.momentId}`}>
            <strong>
              @{moment.repostSource.author?.displayName || moment.repostSource.author?.username}
            </strong>
            <p>{moment.repostSource.textContent}</p>
          </Link>
        ) : (
          <div className="repost-preview muted">
            <span>原动态已不可见</span>
          </div>
        )
      )}
    </>
  );
}

/** 动态操作栏：点赞 / 评论数 / 分享 / 收藏。 */
export function MomentActions({
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

/** 评论行（含楼中楼预览）。 */
export function CommentRow({ thread }: { thread: CommentThread }) {
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
        {thread.replyPreview.length > 0 && (
          <div className="comment-replies">
            {thread.replyPreview.map((reply) => {
              const replyName = reply.author?.displayName || reply.author?.username || "用户";
              return (
                <div className="comment-reply" key={reply.commentId}>
                  <strong>{replyName}</strong>
                  <span dangerouslySetInnerHTML={{ __html: reply.renderedHtml }} />
                </div>
              );
            })}
          </div>
        )}
      </span>
    </div>
  );
}

export function relativeTime(value: string) {
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

export function messageOf(error: unknown, fallback: string) {
  if (error instanceof CommunityApiError && error.code === 401) {
    return "请先登录星语社区，再完成这项操作。";
  }
  return error instanceof Error ? error.message : fallback;
}
