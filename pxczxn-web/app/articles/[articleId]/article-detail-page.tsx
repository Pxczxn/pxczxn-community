"use client";

/* Dynamic community file URLs are authorized by the backend at runtime. */
/* eslint-disable @next/next/no-img-element */

import Link from "next/link";
import {
  AlertTriangle,
  Bookmark,
  Check,
  ChevronLeft,
  Clock3,
  Copy,
  Heart,
  LoaderCircle,
  MessageCircle,
  RefreshCw,
  Share2,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Avatar, UserTopbar } from "../../components/prototype-ui";
import {
  PublicArticleDetail,
  communityApi,
  publicFileUrl,
  readSession,
} from "../../lib/community-api";

interface TocEntry {
  id?: string;
  text?: string;
  title?: string;
  level?: number;
}

export function ArticleDetailPage({ articleId }: { articleId: string }) {
  const [article, setArticle] = useState<PublicArticleDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [liked, setLiked] = useState(false);
  const [saved, setSaved] = useState(false);
  const [copied, setCopied] = useState(false);
  const [interactionBusy, setInteractionBusy] = useState("");
  const [interactionError, setInteractionError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const nextArticle = await communityApi.publicArticle(articleId);
      setArticle(nextArticle);
      if (readSession()) {
        try {
          const [like, favorite] = await Promise.all([
            communityApi.likeRelationship("ARTICLE", articleId),
            communityApi.favoriteRelationship("ARTICLE", articleId),
          ]);
          setLiked(like.liked);
          setSaved(favorite.favorited);
          setArticle((current) => current
            ? {
              ...current,
              likeCount: like.likeCount,
              favoriteCount: favorite.favoriteCount,
            }
            : current);
        } catch {
          // Public reading remains available when optional relationship state fails.
        }
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "文章加载失败");
    } finally {
      setLoading(false);
    }
  }, [articleId]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const toc = useMemo<TocEntry[]>(() => {
    if (!article?.tocJson) return [];
    try {
      const parsed = JSON.parse(article.tocJson);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }, [article]);

  async function copyLink() {
    await navigator.clipboard.writeText(window.location.href);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  }

  async function toggleLike() {
    if (!article) return;
    setInteractionBusy("like");
    setInteractionError("");
    try {
      const relationship = await communityApi.setLike("ARTICLE", articleId, !liked);
      setLiked(relationship.liked);
      setArticle((current) => current
        ? { ...current, likeCount: relationship.likeCount }
        : current);
    } catch (requestError) {
      setInteractionError(requestError instanceof Error ? requestError.message : "点赞失败");
    } finally {
      setInteractionBusy("");
    }
  }

  async function toggleFavorite() {
    if (!article) return;
    setInteractionBusy("favorite");
    setInteractionError("");
    try {
      const relationship = await communityApi.setFavorite("ARTICLE", articleId, !saved);
      setSaved(relationship.favorited);
      setArticle((current) => current
        ? { ...current, favoriteCount: relationship.favoriteCount }
        : current);
    } catch (requestError) {
      setInteractionError(requestError instanceof Error ? requestError.message : "收藏失败");
    } finally {
      setInteractionBusy("");
    }
  }

  if (loading) {
    return (
      <>
        <UserTopbar title="文章" />
        <main className="article-loading page-shell" aria-busy="true">
          <LoaderCircle className="spin" size={30} />
          <strong>正在准备阅读内容…</strong>
        </main>
      </>
    );
  }

  if (error || !article) {
    return (
      <>
        <UserTopbar title="文章" />
        <main className="article-error page-shell">
          <AlertTriangle size={38} />
          <h1>文章暂时无法阅读</h1>
          <p>{error || "文章不存在、未公开或已经下架。"}</p>
          <div>
            <button className="primary-button" onClick={load} type="button">
              <RefreshCw size={16} /> 重新加载
            </button>
            <Link className="ghost-button" href="/teams/ai-explorers">
              返回社区
            </Link>
          </div>
        </main>
      </>
    );
  }

  const cover = publicFileUrl(article.coverFileId);
  return (
    <>
      <UserTopbar title={article.blog.name} />
      <main className="article-page page-shell">
        <Link className="article-back link" href={`/blogs/${article.blog.slug}`}>
          <ChevronLeft size={17} /> 返回博客
        </Link>

        <div className="article-layout">
          <aside className="article-toc surface">
            <strong>目录</strong>
            {toc.length ? (
              <nav aria-label="文章目录">
                {toc.map((entry, index) => (
                  <a
                    className={`toc-level-${entry.level || 1}`}
                    href={`#${encodeURIComponent(entry.id || "")}`}
                    key={`${entry.id}-${index}`}
                  >
                    {entry.text || entry.title || `章节 ${index + 1}`}
                  </a>
                ))}
              </nav>
            ) : (
              <span className="muted">本文暂无目录</span>
            )}
          </aside>

          <article className="article-paper surface-lg">
            <header className="article-header">
              <div className="article-tags">
                {article.category && <span className="chip">{article.category.name}</span>}
                {article.tags.map((tag) => (
                  <span className="chip" key={tag.tagId}>{tag.name}</span>
                ))}
              </div>
              <h1>{article.title}</h1>
              {article.summary && <p className="article-summary">{article.summary}</p>}
              <div className="article-byline">
                <Avatar
                  label={(article.author.displayName || article.author.username).slice(0, 1)}
                  size="md"
                />
                <span>
                  <strong>{article.author.displayName || article.author.username}</strong>
                  <small>
                    {formatDateTime(article.publishedAt)}
                    <i>·</i>
                    <Clock3 size={13} /> {article.readingTimeMinutes} 分钟阅读
                    <i>·</i>
                    {article.wordCount} 字
                  </small>
                </span>
              </div>
            </header>

            {cover && (
              <img
                alt={article.title}
                className="article-cover"
                src={cover}
              />
            )}
            <div
              className="article-content"
              dangerouslySetInnerHTML={{ __html: article.renderedHtml }}
            />

            <footer className="article-footer">
              <div>
                <Link className="article-author-card" href={`/blogs/${article.blog.slug}`}>
                  <Avatar
                    label={(article.author.displayName || article.author.username).slice(0, 1)}
                    size="lg"
                  />
                  <span>
                    <strong>{article.author.displayName || article.author.username}</strong>
                    <small>@{article.author.username} · {article.blog.name}</small>
                    <p>{article.author.bio || article.blog.summary}</p>
                  </span>
                </Link>
              </div>
            </footer>
          </article>

          <aside className="article-tools">
            {interactionError && (
              <div className="inline-feedback error article-interaction-error">
                <AlertTriangle size={15} /> {interactionError}
              </div>
            )}
            <div className="surface article-tool-card">
              <button
                aria-pressed={liked}
                className={liked ? "active" : ""}
                disabled={interactionBusy === "like"}
                onClick={toggleLike}
                type="button"
              >
                <Heart fill={liked ? "currentColor" : "none"} size={19} />
                <span>{article.likeCount}</span>
                <small>喜欢</small>
              </button>
              <button
                aria-pressed={saved}
                className={saved ? "active" : ""}
                disabled={interactionBusy === "favorite"}
                onClick={toggleFavorite}
                type="button"
              >
                <Bookmark fill={saved ? "currentColor" : "none"} size={19} />
                <span>{article.favoriteCount}</span>
                <small>收藏</small>
              </button>
              <button type="button">
                <MessageCircle size={19} />
                <span>{article.commentCount}</span>
                <small>评论</small>
              </button>
              <button onClick={copyLink} type="button">
                {copied ? <Check size={19} /> : <Share2 size={19} />}
                <span>{copied ? "已复制" : "分享"}</span>
                <small>{copied ? "链接已复制" : "复制链接"}</small>
              </button>
            </div>
            <button className="ghost-button article-copy" onClick={copyLink} type="button">
              <Copy size={15} /> 复制文章链接
            </button>
          </aside>
        </div>
      </main>
    </>
  );
}

function formatDateTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(date);
}
