"use client";

import Link from "next/link";
import {
  Check,
  ChevronRight,
  Circle,
  Clock3,
  FileEdit,
  Link2,
  LoaderCircle,
  MessageSquareText,
  RotateCcw,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Avatar, UserTopbar } from "../../components/prototype-ui";
import {
  ArticleEditor,
  ArticleReviewStatus,
  communityApi,
} from "../../lib/community-api";

export function SubmissionDetailPanel() {
  const [articleId, setArticleId] = useState("");
  const [editor, setEditor] = useState<ArticleEditor | null>(null);
  const [status, setStatus] = useState<ArticleReviewStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const id = new URLSearchParams(window.location.search).get("articleId") || "";
      setArticleId(id);
      if (!id) return;
      setLoading(true);
      Promise.all([communityApi.editor(id), communityApi.reviewStatus(id)])
        .then(([nextEditor, nextStatus]) => {
          setEditor(nextEditor);
          setStatus(nextStatus);
        })
        .catch((error) => {
          setMessage(error instanceof Error ? error.message : "审核状态加载失败");
        })
        .finally(() => setLoading(false));
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  async function withdraw() {
    if (!articleId || !status) return;
    setLoading(true);
    setMessage("");
    try {
      const next = await communityApi.withdrawReview(
        articleId,
        status.lockVersion,
        "作者从用户端撤回投稿",
      );
      setStatus(next);
      setEditor(await communityApi.editor(articleId));
      setMessage("投稿已撤回，可以继续编辑");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "撤回失败");
    } finally {
      setLoading(false);
    }
  }

  const isLive = Boolean(editor && status);
  const currentReview = status?.reviewStatus || "PENDING_REVIEW";
  const task = status?.latestTask;
  const submittedAt = task?.submittedAt || "2024-04-12T14:32:00";
  const title = editor?.title || "基于向量数据库的企业知识库构建实践";
  const reason = task?.resultReason
    || (currentReview === "REVISION_REQUIRED"
      ? "请根据审核意见修改后重新提交。"
      : "整体结构清晰，案例丰富。建议补充召回性能的对比数据，并完善章节间的衔接。");
  const canWithdraw = ["PENDING_REVIEW", "AUTO_REVIEWING", "MANUAL_REVIEWING"].includes(currentReview);

  return (
    <>
      <UserTopbar title="投稿中心" />
      <main className="submission-page page-shell">
        <nav className="tabs submission-primary-tabs" aria-label="投稿中心">
          <a className="tab">投稿中心</a>
          <a className="tab active">我的投稿</a>
          <a className="tab">投稿记录</a>
        </nav>

        {loading && (
          <div className="submission-live-notice">
            <LoaderCircle className="spin" size={16} /> 正在同步审核状态…
          </div>
        )}
        {message && <div className="submission-live-notice" role="status">{message}</div>}
        {!articleId && (
          <div className="submission-live-notice submission-live-notice--demo">
            当前展示原型示例；从文章编辑器提交后可查看真实审核进度。
          </div>
        )}

        <section className="submission-grid">
          <div className="stack">
            <article className="surface submission-detail">
              <div className="submission-title-row">
                <div>
                  <h1>{title}</h1>
                  <p>提交于 {formatDateTime(submittedAt)}</p>
                </div>
                <span className={`chip ${statusTone(currentReview)}`}>
                  {statusLabel(currentReview)}
                </span>
              </div>

              <div className="submission-actions">
                <Link
                  className="secondary-button"
                  href={isLive ? `/editor/${articleId}` : "/editor/new"}
                >
                  <FileEdit size={16} /> 编辑投稿
                </Link>
                <button
                  className="ghost-button"
                  disabled={!isLive || !canWithdraw || loading}
                  onClick={withdraw}
                  type="button"
                >
                  <RotateCcw size={16} /> 撤回投稿
                </button>
              </div>

              <div className="tabs submission-sub-tabs">
                <a className="tab active">详情</a>
                <a className="tab">评论（{task?.resultReason ? 1 : 3}）</a>
                <a className="tab">更新记录</a>
              </div>

              <section className="submission-info">
                <h2 className="card-heading">稿件信息</h2>
                <dl>
                  <div><dt>投稿分类</dt><dd>{editor?.categoryId ? "已选择分类" : "未分类"}</dd></div>
                  <div><dt>发布方式</dt><dd>{publishMethodLabel(editor?.publishMethod)}</dd></div>
                  <div><dt>字数统计</dt><dd>{editor?.wordCount ?? 3287} 字</dd></div>
                  <div>
                    <dt>状态</dt>
                    <dd className="article-tags">
                      <span className="chip">{statusLabel(status?.publishStatus || "UNPUBLISHED")}</span>
                      <span className="chip">{editor?.contentMode === "MARKDOWN" ? "Markdown" : "富文本"}</span>
                    </dd>
                  </div>
                </dl>
                <div className="submission-summary">
                  <strong>摘要</strong>
                  <p>
                    {editor?.summary
                      || "本文介绍了基于向量数据库构建企业知识库的完整流程，涵盖文档清洗、切分与向量化、索引构建、检索优化和回答评估。"}
                  </p>
                </div>
              </section>
            </article>

            <section className="surface review-feedback">
              <h2 className="card-heading">审核意见</h2>
              <div className="reviewer-row">
                <Avatar label={task?.reviewType === "AUTO" ? "AI" : "审"} size="sm" />
                <span>
                  <strong>{task?.reviewType === "AUTO" ? "自动审核" : "技术编辑"}</strong>
                  <small>{formatDateTime(task?.completedAt || task?.claimedAt || submittedAt)}</small>
                </span>
              </div>
              <p>{reason}</p>
              {task?.resultCode && <blockquote>结果代码：{task.resultCode}</blockquote>}
              <div className="review-actions">
                <Link className="primary-button" href={isLive ? `/editor/${articleId}` : "/editor/new"}>
                  编辑修改
                </Link>
                <Link className="ghost-button" href="/teams/ai-explorers">
                  返回博客
                </Link>
              </div>
            </section>
          </div>

          <aside className="stack">
            <section className="surface workflow-card">
              <h2 className="card-heading">投稿状态</h2>
              <ReviewWorkflow status={currentReview} task={task ?? null} />
            </section>
            <section className="surface related-help">
              <h2 className="card-heading">相关帮助</h2>
              {[
                [MessageSquareText, "投稿规范"],
                [Link2, "内容要求"],
                [ChevronRight, "常见问题"],
              ].map(([Icon, text]) => {
                const HelpIcon = Icon;
                return (
                  <Link className="link" href="/teams/ai-explorers" key={text as string}>
                    <HelpIcon size={15} /> {text as string}
                  </Link>
                );
              })}
            </section>
          </aside>
        </section>
      </main>
    </>
  );
}

function ReviewWorkflow({
  status,
  task,
}: {
  status: string;
  task: ArticleReviewStatus["latestTask"];
}) {
  const completed = ["APPROVED", "REVISION_REQUIRED", "REJECTED"].includes(status);
  const manual = ["MANUAL_REVIEWING", "REVISION_REQUIRED", "REJECTED"].includes(status)
    || task?.reviewType === "MANUAL";
  return (
    <ol className="workflow">
      <li className="done">
        <span><Check size={13} /></span>
        <div><strong>提交投稿</strong><small>{formatDateTime(task?.submittedAt)}</small></div>
      </li>
      <li className={manual || completed ? "done" : "active"}>
        <span>{manual || completed ? <Check size={13} /> : <Circle size={13} />}</span>
        <div><strong>自动审核</strong><small>{manual || completed ? "已完成" : "进行中"}</small></div>
      </li>
      <li className={completed ? "done" : manual ? "active" : ""}>
        <span>{completed ? <Check size={13} /> : <Circle size={13} />}</span>
        <div><strong>人工复核</strong><small>{completed ? "已完成" : manual ? "进行中" : "按需进入"}</small></div>
      </li>
      <li className={completed ? "done" : ""}>
        <span>{completed ? <Check size={13} /> : <Clock3 size={13} />}</span>
        <div><strong>结果通知</strong><small>{completed ? statusLabel(status) : "等待中"}</small></div>
      </li>
    </ol>
  );
}

function formatDateTime(value?: string | null) {
  if (!value) return "等待中";
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

function statusLabel(value: string) {
  const labels: Record<string, string> = {
    DRAFT: "草稿",
    PENDING_REVIEW: "审核中",
    AUTO_REVIEWING: "自动审核中",
    MANUAL_REVIEWING: "专家审核中",
    APPROVED: "审核通过",
    REVISION_REQUIRED: "需要修改",
    REJECTED: "已驳回",
    UNPUBLISHED: "未发布",
    PUBLISHED: "已发布",
    SCHEDULED: "定时发布",
    PUBLISH_FAILED: "发布失败",
  };
  return labels[value] || value;
}

function statusTone(value: string) {
  if (value === "APPROVED") return "status-success";
  if (["REJECTED", "PUBLISH_FAILED"].includes(value)) return "status-danger";
  return "status-warning";
}

function publishMethodLabel(value?: string) {
  return {
    MANUAL: "审核后手动发布",
    IMMEDIATE: "审核通过后立即发布",
    SCHEDULED: "审核后定时发布",
  }[value || "MANUAL"];
}
