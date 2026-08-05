"use client";

import Link from "next/link";
import { AlertCircle, Eye, Loader2, MessageSquare, ThumbsUp } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { communityApi, type TeamArticleBrief } from "../../../../lib/community-api";
import { formatDateTime, PUBLISH_STATUS_LABELS } from "../../../team-labels";
import { useWorkspace } from "../workspace-context";

type ContentTab = "ALL" | "DRAFT" | "REVIEWING" | "PUBLISHED" | "REJECTED";

const CONTENT_TABS: Array<{ key: ContentTab; label: string }> = [
  { key: "ALL", label: "全部" },
  { key: "DRAFT", label: "草稿" },
  { key: "REVIEWING", label: "审核中" },
  { key: "PUBLISHED", label: "已发布" },
  { key: "REJECTED", label: "已退回" },
];

function matchesTab(article: TeamArticleBrief, tab: ContentTab): boolean {
  switch (tab) {
    case "ALL":
      return true;
    case "DRAFT":
      return article.publishStatus === "DRAFT";
    case "REVIEWING":
      return ["PENDING_REVIEW", "APPROVED", "SCHEDULED"].includes(article.publishStatus);
    case "PUBLISHED":
      return article.publishStatus === "PUBLISHED" || article.publishStatus === "HIDDEN";
    case "REJECTED":
      return (
        ["TAKEN_DOWN", "PUBLISH_FAILED"].includes(article.publishStatus)
        || ["REJECTED", "REVISION_REQUIRED"].includes(article.reviewStatus)
      );
  }
}

export default function WorkspaceContentPage() {
  const { teamId, teamSlug } = useWorkspace();
  const [articles, setArticles] = useState<TeamArticleBrief[]>([]);
  const [tab, setTab] = useState<ContentTab>("ALL");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!teamId) return;
    let active = true;
    communityApi.teamArticles(teamId)
      .then((value) => { if (active) setArticles(value); })
      .catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "内容列表加载失败"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [teamId]);

  const counts = useMemo(() => {
    const result: Record<ContentTab, number> = { ALL: articles.length, DRAFT: 0, REVIEWING: 0, PUBLISHED: 0, REJECTED: 0 };
    for (const article of articles) {
      for (const key of CONTENT_TABS) {
        if (key.key !== "ALL" && matchesTab(article, key.key)) result[key.key] += 1;
      }
    }
    return result;
  }, [articles]);

  const visible = articles.filter((article) => matchesTab(article, tab));

  if (loading || !teamId) {
    return <div className="series-loading surface" aria-live="polite"><Loader2 className="animate-spin" size={22} /> 正在加载团队内容…</div>;
  }
  if (error) {
    return (
      <section className="surface inline-feedback error" role="alert">
        <AlertCircle size={20} />
        <div><strong>内容列表暂时无法加载</strong><p>{error}</p></div>
      </section>
    );
  }

  return (
    <div className="workspace-content-page">
      <section className="workspace-content-page__header">
        <div>
          <span className="eyebrow">内容管理</span>
          <p>团队博客下的全部文章，包含草稿与投稿发布结果。</p>
        </div>
        <Link className="secondary-button" href={`/teams/${teamSlug}/workspace/submissions`}>向团队投稿</Link>
      </section>

      <div className="workspace-tabs" role="tablist" aria-label="内容状态筛选">
        {CONTENT_TABS.map((item) => (
          <button
            type="button"
            role="tab"
            key={item.key}
            aria-selected={tab === item.key}
            className={tab === item.key ? "active" : ""}
            onClick={() => setTab(item.key)}
          >
            {item.label}
            <span className="workspace-tabs__count">{counts[item.key]}</span>
          </button>
        ))}
      </div>

      {visible.length === 0 ? (
        <section className="series-empty surface">
          <div>
            <h2>{tab === "ALL" ? "团队还没有文章" : "这个状态下没有内容"}</h2>
            <p>{tab === "ALL" ? "可以通过投稿把个人文章发布到团队，或等待成员投稿。": "切换其他状态查看，或等待内容流转到该状态。"}</p>
          </div>
        </section>
      ) : (
        <section className="surface workspace-content-table">
          {visible.map((article) => (
            <div className="workspace-content-row" key={article.articleId}>
              <div className="workspace-content-row__copy">
                <strong>{article.title}</strong>
                <span>
                  {article.authorDisplayName || "团队成员"}
                  {" · "}更新于 {formatDateTime(article.updatedAt)}
                </span>
              </div>
              <div className="workspace-content-row__meta">
                <span title="浏览"><Eye size={12} /> {article.viewCount}</span>
                <span title="点赞"><ThumbsUp size={12} /> {article.likeCount}</span>
                <span title="评论"><MessageSquare size={12} /> {article.commentCount}</span>
                <span className={`chip content-status is-${article.publishStatus.toLowerCase()}`}>
                  {PUBLISH_STATUS_LABELS[article.publishStatus] || article.publishStatus}
                </span>
                {article.publishStatus === "PUBLISHED" && article.slug ? (
                  <Link className="ghost-button" href={`/articles/${article.articleId}`}>查看</Link>
                ) : (
                  <Link className="ghost-button" href={`/editor/${article.articleId}`}>编辑</Link>
                )}
              </div>
            </div>
          ))}
        </section>
      )}
    </div>
  );
}
