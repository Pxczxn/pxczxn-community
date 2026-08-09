"use client";

import Link from "next/link";
import {
  ArrowLeft,
  ArrowRight,
  BellOff,
  BellRing,
  BookOpen,
  Check,
  Layers,
  ListTree,
  Loader2,
  Play,
  Users,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { UserTopbar } from "../../components/prototype-ui";
import { communityApi, publicFileUrl, readSession, type Series } from "../../lib/community-api";
import { blogTypeLabel, readingPercent, serializationLabel } from "../../lib/series-labels";

export default function SeriesDetailPage({ params }: { params: Promise<{ seriesId: string }> }) {
  const [series, setSeries] = useState<Series | null>(null);
  const [error, setError] = useState("");
  const [signedIn] = useState(() => Boolean(readSession()));
  const [following, setFollowing] = useState(false);
  const [followerCount, setFollowerCount] = useState(0);
  const [readCount, setReadCount] = useState(0);
  const [lastReadArticleId, setLastReadArticleId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    void params
      .then(({ seriesId }) => communityApi.seriesDetail(seriesId))
      .then((value) => {
        if (!active) return;
        setSeries(value);
        setFollowing(value.viewerFollowing);
        setFollowerCount(value.followerCount);
        setReadCount(value.viewerReadChapterCount);
        setLastReadArticleId(value.viewerLastReadArticleId);
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : "无法加载系列详情");
      });
    return () => {
      active = false;
    };
  }, [params]);

  const toggleFollow = useCallback(async () => {
    if (!series || busy) return;
    setBusy(true);
    try {
      const next = following
        ? await communityApi.unfollowSeries(series.id)
        : await communityApi.followSeries(series.id);
      setFollowing(next.following);
      setFollowerCount(next.followerCount);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "操作失败");
    } finally {
      setBusy(false);
    }
  }, [busy, following, series]);

  if (error && !series) {
    return (
      <>
        <UserTopbar title="系列详情" />
        <main className="series-page page-shell">
          <BackLink />
          <div className="surface inline-feedback error" role="alert" style={{ padding: 24 }}>
            {error}
          </div>
        </main>
      </>
    );
  }

  if (!series) {
    return (
      <>
        <UserTopbar title="系列详情" />
        <main className="series-page page-shell">
          <BackLink />
          <div className="series-loading surface" aria-busy="true">
            <Loader2 className="animate-spin" size={24} /> 正在加载系列详情…
          </div>
        </main>
      </>
    );
  }

  const percent = readingPercent(readCount, series.chapterCount);
  // 上次读到哪一章决定"继续阅读"落点；没读过就从第一章开始。
  const resumeChapter =
    series.chapters.find((chapter) => chapter.articleId === lastReadArticleId) ?? series.chapters[0];
  const resumed = Boolean(lastReadArticleId) && readCount > 0;
  const blogHref = series.blogSlug ? `/${encodeURIComponent(series.blogSlug)}` : null;
  const creatorAvatar = publicFileUrl(series.creatorAvatarFileId);

  return (
    <>
      <UserTopbar title="系列详情" />
      <main className="series-page page-shell">
        <BackLink />

        <section className="series-detail-hero surface shadow-sm">
          <div className="series-detail-header">
            <span className="series-detail-icon">
              <BookOpen size={28} />
            </span>
            <div className="series-detail-info">
              <div className="series-card__topline">
                <span className={`series-status series-status--${series.serializationStatus.toLowerCase()}`}>
                  {serializationLabel(series.serializationStatus)}
                </span>
                <span className="series-card__chapter-count">
                  <Layers size={14} /> 共 {series.chapterCount} 章
                </span>
                <span className="series-card__chapter-count">
                  <Users size={14} /> {followerCount} 人追更
                </span>
              </div>
              <h1>{series.title}</h1>
              <p>{series.summary || "这个系列暂未添加简介。"}</p>

              <div className="series-detail-owner">
                {creatorAvatar ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img alt="" className="avatar avatar-sm avatar-image" src={creatorAvatar} />
                ) : null}
                <span>
                  由 <strong>{series.creatorDisplayName || series.creatorUsername || "匿名作者"}</strong> 维护
                  {series.blogName && (
                    <>
                      {" · "}
                      {blogHref ? <Link href={blogHref}>{series.blogName}</Link> : series.blogName}
                      <small className="muted"> （{blogTypeLabel(series.blogType)}）</small>
                    </>
                  )}
                </span>
              </div>

              {signedIn && percent > 0 && (
                <div className="series-detail-progress">
                  <div
                    className="series-progress"
                    role="progressbar"
                    aria-valuenow={percent}
                    aria-valuemin={0}
                    aria-valuemax={100}
                  >
                    <span style={{ width: `${percent}%` }} />
                  </div>
                  <span className="muted">
                    已读 {readCount}/{series.chapterCount} 章（{percent}%）
                  </span>
                </div>
              )}

              <div className="series-hero__actions" style={{ marginTop: 12 }}>
                {resumeChapter && (
                  <Link className="primary-button" href={`/articles/${resumeChapter.articleId}`}>
                    <Play size={16} fill="currentColor" />{" "}
                    {resumed ? `继续读第 ${resumeChapter.chapterOrder} 章` : "开始阅读第 1 章"}
                  </Link>
                )}
                {signedIn ? (
                  <button className={following ? "ghost-button" : "secondary-button"} disabled={busy} onClick={() => void toggleFollow()} type="button">
                    {following ? (
                      <>
                        <BellOff size={16} /> 取消追更
                      </>
                    ) : (
                      <>
                        <BellRing size={16} /> 追更这个系列
                      </>
                    )}
                  </button>
                ) : (
                  <Link className="ghost-button" href="/login">
                    <BellRing size={16} /> 登录后追更
                  </Link>
                )}
              </div>
              {error && (
                <p className="inline-feedback error" role="alert">
                  {error}
                </p>
              )}
            </div>
          </div>
        </section>

        <section className="series-chapters-section">
          <div className="series-chapters-header">
            <h2>
              <ListTree size={20} /> 章节目录
            </h2>
            <span className="series-toolbar__count">按发布顺序阅读</span>
          </div>

          {series.chapters.length === 0 ? (
            <div className="series-empty surface">
              <span className="series-empty__icon">
                <BookOpen size={24} />
              </span>
              <div>
                <h2>暂时没有公开章节</h2>
                <p>作者正在筹备连载章节，公开发布后会在此处依次展示。</p>
              </div>
            </div>
          ) : (
            <div className="series-chapters-list">
              {series.chapters.map((chapter) => {
                // 最远进度是"读过到第几章"，因此序号不大于它的章节都算已读。
                const read = signedIn && chapter.chapterOrder <= readCount;
                const current = chapter.articleId === lastReadArticleId;
                return (
                  <Link
                    className={`series-chapter-card surface${read ? " series-chapter-card--read" : ""}${current ? " series-chapter-card--current" : ""}`}
                    href={`/articles/${chapter.articleId}`}
                    key={chapter.articleId}
                  >
                    <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
                      <span className="series-chapter-card__order">
                        {read ? <Check size={16} /> : String(chapter.chapterOrder).padStart(2, "0")}
                      </span>
                      <span className="series-chapter-card__title">
                        {chapter.title}
                        {current && <small className="muted"> · 上次读到这里</small>}
                      </span>
                    </div>
                    <span className="series-card__more" style={{ fontSize: 13 }}>
                      阅读章节 <ArrowRight size={14} />
                    </span>
                  </Link>
                );
              })}
            </div>
          )}
        </section>
      </main>
    </>
  );
}

function BackLink() {
  return (
    <div>
      <Link className="ghost-button" href="/series">
        <ArrowLeft size={16} /> 返回连载系列
      </Link>
    </div>
  );
}
