"use client";

import Link from "next/link";
import {
  AlertCircle,
  ArrowUpRight,
  BookOpen,
  Eye,
  Inbox,
  Loader2,
  MessageSquare,
  PenLine,
  Send,
  Settings2,
  ThumbsUp,
  UserPlus,
  Users,
} from "lucide-react";
import { useEffect, useState } from "react";
import { communityApi, type TeamDashboard } from "../../../lib/community-api";
import { activityText, formatCount, formatDateTime, PUBLISH_STATUS_LABELS } from "../../team-labels";
import { useWorkspace } from "./workspace-context";

export default function WorkspaceOverviewPage() {
  const { teamId, teamSlug } = useWorkspace();
  const [dashboard, setDashboard] = useState<TeamDashboard | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!teamId) return;
    let active = true;
    communityApi.teamDashboard(teamId)
      .then((value) => { if (active) setDashboard(value); })
      .catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "概览加载失败"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [teamId]);

  if (loading || !teamId) {
    return <div className="series-loading surface" aria-live="polite"><Loader2 className="animate-spin" size={22} /> 正在加载团队概览…</div>;
  }
  if (error || !dashboard) {
    return (
      <section className="surface inline-feedback error" role="alert">
        <AlertCircle size={20} />
        <div><strong>概览暂时无法加载</strong><p>{error || "没有可用的团队数据"}</p></div>
      </section>
    );
  }

  const { stats, todos, recentArticles, recentActivities, permissions } = dashboard;
  const hasReviewPermission = permissions.includes("MANAGE_SUBMISSIONS");
  const hasSeriesPermission = permissions.includes("MANAGE_SERIES");
  const hasMemberPermission = permissions.includes("MANAGE_MEMBERS");
  const hasSettingsPermission = permissions.includes("MANAGE_TEAM");

  const todoItems: Array<{ label: string; count: number; href: string; show: boolean }> = [
    { label: "待审核投稿", count: todos.pendingSubmissionCount, href: `submissions`, show: hasReviewPermission },
    { label: "待修改投稿", count: todos.revisionRequiredCount, href: `submissions`, show: true },
    { label: "待处理邀请", count: todos.pendingInvitationCount, href: `members`, show: hasMemberPermission },
    { label: "待审核系列", count: todos.pendingSeriesReviewCount, href: `series`, show: hasSeriesPermission },
    { label: "内容审核异常", count: todos.contentRiskCount, href: `content`, show: true },
  ];
  const workspaceHref = (segment: string) => `/teams/${teamSlug}/workspace/${segment}`;
  const visibleTodos = todoItems.filter((item) => item.show);
  const todoTotal = visibleTodos.reduce((sum, item) => sum + item.count, 0);

  return (
    <div className="workspace-overview">
      <section className="workspace-quick surface">
        <div className="workspace-quick__copy">
          <span className="eyebrow">快捷操作</span>
          <p>开始一次团队协作。</p>
        </div>
        <div className="workspace-quick__actions">
          <Link className="secondary-button" href={workspaceHref("submissions")}><Send size={14} /> 向团队投稿</Link>
          {hasSeriesPermission && <Link className="ghost-button" href={workspaceHref("series")}><BookOpen size={14} /> 创建系列</Link>}
          {hasMemberPermission && <Link className="ghost-button" href={workspaceHref("members")}><UserPlus size={14} /> 邀请成员</Link>}
          {hasSettingsPermission && <Link className="ghost-button" href={workspaceHref("settings")}><Settings2 size={14} /> 编辑团队资料</Link>}
        </div>
      </section>

      <section className="workspace-stats" aria-label="团队数据统计">
        {[
          { label: "公开文章", value: stats.publishedArticleCount, icon: BookOpen },
          { label: "草稿", value: stats.draftArticleCount, icon: PenLine },
          { label: "审核中", value: stats.reviewingArticleCount, icon: Inbox },
          { label: "系列", value: stats.seriesCount, icon: BookOpen },
          { label: "成员", value: stats.memberCount, icon: Users },
          { label: "关注者", value: stats.followerCount, icon: Users },
          { label: "累计浏览", value: stats.totalViewCount, icon: Eye },
          { label: "累计互动", value: stats.totalInteractionCount, icon: ThumbsUp },
        ].map((item) => {
          const Icon = item.icon;
          return (
            <div className="surface workspace-stat" key={item.label}>
              <Icon size={16} className="workspace-stat__icon" />
              <strong>{formatCount(item.value)}</strong>
              <span>{item.label}</span>
            </div>
          );
        })}
      </section>

      <div className="workspace-overview__columns">
        <section className="surface workspace-panel">
          <header className="workspace-panel__header">
            <h2>最近文章</h2>
            <Link className="ghost-button" href={workspaceHref("content")}>全部内容 <ArrowUpRight size={14} /></Link>
          </header>
          {recentArticles.length === 0 ? (
            <p className="workspace-panel__empty">团队还没有文章，可以投稿或创建系列开始内容建设。</p>
          ) : (
            <ul className="workspace-article-list">
              {recentArticles.map((article) => (
                <li className="workspace-article-row" key={article.articleId}>
                  <div className="workspace-article-row__copy">
                    <strong>{article.title}</strong>
                    <span>
                      {PUBLISH_STATUS_LABELS[article.publishStatus] || article.publishStatus}
                      {" · "}{article.authorDisplayName || "团队成员"}
                      {" · "}更新于 {formatDateTime(article.updatedAt)}
                    </span>
                  </div>
                  <div className="workspace-article-row__meta">
                    <span title="浏览"><Eye size={12} /> {article.viewCount}</span>
                    <span title="点赞"><ThumbsUp size={12} /> {article.likeCount}</span>
                    <span title="评论"><MessageSquare size={12} /> {article.commentCount}</span>
                    {article.publishStatus === "PUBLISHED" && article.slug ? (
                      <Link className="ghost-button" href={`/articles/${article.articleId}`}>查看</Link>
                    ) : (
                      <Link className="ghost-button" href={`/editor/${article.articleId}`}>编辑</Link>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <aside className="workspace-overview__side">
          <section className="surface workspace-panel">
            <header className="workspace-panel__header">
              <h2>待办事项</h2>
              {todoTotal > 0 && <span className="badge badge--warn">{todoTotal}</span>}
            </header>
            {visibleTodos.every((item) => item.count === 0) ? (
              <p className="workspace-panel__empty">没有待办事项，一切就绪。</p>
            ) : (
              <ul className="workspace-todo-list">
                {visibleTodos.map((item) => (
                  <li key={item.label}>
                    <Link href={workspaceHref(item.href)}>
                      <span className="workspace-todo-list__label">{item.label}</span>
                      <strong>{item.count}</strong>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="surface workspace-panel">
            <header className="workspace-panel__header">
              <h2>最近活动</h2>
            </header>
            {recentActivities.length === 0 ? (
              <p className="workspace-panel__empty">还没有团队活动记录。</p>
            ) : (
              <ul className="workspace-activity-list">
                {recentActivities.map((activity) => (
                  <li key={activity.id}>
                    <p>{activityText(activity.eventType, activity.actorDisplayName)}</p>
                    <span>{formatDateTime(activity.occurredAt)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </aside>
      </div>
    </div>
  );
}
