"use client";

import Link from "next/link";
import { ArrowLeft, ArrowRight, BookOpen, Layers, ListTree, Loader2, Play } from "lucide-react";
import { useEffect, useState } from "react";
import { UserTopbar } from "../../components/prototype-ui";
import { communityApi, type TeamSeries } from "../../lib/community-api";

export default function SeriesDetailPage({ params }: { params: Promise<{ seriesId: string }> }) {
  const [series, setSeries] = useState<TeamSeries | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void params
      .then(({ seriesId }) => communityApi.seriesDetail(seriesId))
      .then((value) => {
        if (active) setSeries(value);
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : "无法加载系列详情");
      });
    return () => {
      active = false;
    };
  }, [params]);

  const firstChapter = series?.chapters?.[0];

  return (
    <>
      <UserTopbar title="系列详情" />
      <main className="series-page page-shell">
        <div>
          <Link className="ghost-button" href="/series">
            <ArrowLeft size={16} /> 返回连载书架
          </Link>
        </div>

        {error ? (
          <div className="surface inline-feedback error" role="alert" style={{ padding: 24 }}>
            {error}
          </div>
        ) : !series ? (
          <div className="series-loading surface" aria-busy="true">
            <Loader2 className="animate-spin" size={24} /> 正在加载系列详情…
          </div>
        ) : (
          <>
            <section className="series-detail-hero surface shadow-sm">
              <div className="series-detail-header">
                <span className="series-detail-icon">
                  <BookOpen size={28} />
                </span>
                <div className="series-detail-info">
                  <div className="series-card__topline">
                    <span className={`series-status series-status--${series.serializationStatus.toLowerCase()}`}>
                      {statusText(series.serializationStatus)}
                    </span>
                    <span className="series-card__chapter-count">
                      <Layers size={14} /> 共 {series.chapters.length} 篇章节
                    </span>
                  </div>
                  <h1>{series.title}</h1>
                  <p>{series.summary || "这个系列暂未添加简介。"}</p>

                  {firstChapter && (
                    <div className="series-hero__actions" style={{ marginTop: 12 }}>
                      <Link className="primary-button" href={`/articles/${firstChapter.articleId}`}>
                        <Play size={16} fill="currentColor" /> 开始阅读第 1 章
                      </Link>
                    </div>
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
                    <p>作者团队正在筹备连载章节，公开发布后会在此处依次展示。</p>
                  </div>
                </div>
              ) : (
                <div className="series-chapters-list">
                  {series.chapters.map((chapter) => (
                    <Link
                      className="series-chapter-card surface"
                      href={`/articles/${chapter.articleId}`}
                      key={chapter.articleId}
                    >
                      <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
                        <span className="series-chapter-card__order">
                          {String(chapter.chapterOrder).padStart(2, "0")}
                        </span>
                        <span className="series-chapter-card__title">{chapter.title}</span>
                      </div>
                      <span className="series-card__more" style={{ fontSize: 13 }}>
                        阅读章节 <ArrowRight size={14} />
                      </span>
                    </Link>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </>
  );
}

function statusText(value: TeamSeries["serializationStatus"]) {
  return ({ ONGOING: "连载中", COMPLETED: "已完结", PAUSED: "暂缓更新" } as const)[value] || "系列";
}

