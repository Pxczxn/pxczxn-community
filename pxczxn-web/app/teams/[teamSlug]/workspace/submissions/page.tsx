"use client";

import Link from "next/link";
import { AlertCircle, Check, Loader2, RefreshCw, Send, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { communityApi, type SubmittableArticle, type TeamSubmission } from "../../../../lib/community-api";
import {
  formatDateTime,
  hasTeamPermission,
  PUBLISH_STATUS_LABELS,
  SUBMISSION_STATUS_LABELS,
  TEAM_PERMISSIONS,
} from "../../../team-labels";
import { useWorkspace } from "../workspace-context";

type SubmissionTab = "MINE" | "QUEUE";

/** 这些状态下投稿仍在流转，同一篇文章不能再次投给同一团队（后端有唯一约束）。 */
const IN_FLIGHT_STATUSES = new Set(["TEAM_PENDING", "PLATFORM_PENDING", "PLATFORM_PUBLISHING"]);

export default function WorkspaceSubmissionsPage() {
  const { teamId, workspace } = useWorkspace();
  const [tab, setTab] = useState<SubmissionTab>("MINE");
  const [mine, setMine] = useState<TeamSubmission[]>([]);
  const [queue, setQueue] = useState<TeamSubmission[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState<string | null>(null);
  const [comment, setComment] = useState<Record<string, string>>({});
  const [candidates, setCandidates] = useState<SubmittableArticle[]>([]);
  const [sourceArticleId, setSourceArticleId] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState("");

  const canReview = hasTeamPermission(workspace?.permissions, TEAM_PERMISSIONS.MANAGE_SUBMISSIONS);

  const load = useCallback(() => {
    if (!teamId) return;
    setLoading(true);
    setError("");
    const jobs: Array<Promise<unknown>> = [
      communityApi.myTeamSubmissions().then(setMine),
      communityApi.submittableArticles().then(setCandidates),
    ];
    if (canReview) jobs.push(communityApi.teamSubmissions(teamId).then(setQueue));
    void Promise.all(jobs)
      .catch((cause: unknown) => setError(cause instanceof Error ? cause.message : "投稿记录加载失败"))
      .finally(() => setLoading(false));
  }, [teamId, canReview]);

  useEffect(() => {
    if (!teamId) return;
    let active = true;
    const jobs: Array<Promise<unknown>> = [
      communityApi.myTeamSubmissions().then((value) => { if (active) setMine(value); }),
      communityApi.submittableArticles().then((value) => { if (active) setCandidates(value); }),
    ];
    if (canReview) jobs.push(communityApi.teamSubmissions(teamId).then((value) => { if (active) setQueue(value); }));
    void Promise.all(jobs)
      .catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "投稿记录加载失败"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [teamId, canReview]);

  async function decide(submission: TeamSubmission, action: "approve" | "revision" | "reject") {
    if (!canReview) return;
    const reviewComment = comment[submission.id]?.trim() ?? "";
    setBusy(submission.id);
    setError("");
    try {
      await communityApi.decideTeamSubmission(submission.id, action, submission.lockVersion, reviewComment);
      setComment((current) => ({ ...current, [submission.id]: "" }));
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "审核操作失败，请稍后重试");
    } finally {
      setBusy(null);
    }
  }

  async function resubmit(submission: TeamSubmission) {
    if (!teamId) return;
    setBusy(submission.id);
    setError("");
    setNotice("");
    try {
      await communityApi.createTeamSubmission({
        sourceArticleId: submission.sourceArticleId,
        targetTeamId: teamId,
        supersedesSubmissionId: submission.id,
      });
      setNotice("已重新提交，当前版本已固定并进入团队审核。");
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "重新提交失败，请稍后重试");
    } finally {
      setBusy(null);
    }
  }

  async function createSubmission() {
    if (!teamId || !sourceArticleId) return;
    setSubmitting(true);
    setError("");
    setNotice("");
    try {
      await communityApi.createTeamSubmission({
        sourceArticleId,
        targetTeamId: teamId,
      });
      setNotice("投稿已创建，当前版本已固定并进入团队审核。");
      setSourceArticleId("");
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "投稿失败");
    } finally {
      setSubmitting(false);
    }
  }

  const mySubmissions = useMemo(
    () => mine.filter((submission) => submission.targetTeamId === teamId),
    [mine, teamId],
  );
  const pendingQueue = queue.filter((submission) => submission.status === "TEAM_PENDING");
  /** 已经在流转中的文章不能重复投稿，列表里直接置灰，省得点了才报 409。 */
  const inFlightArticleIds = useMemo(
    () => new Set(mySubmissions.filter((item) => IN_FLIGHT_STATUSES.has(item.status)).map((item) => item.sourceArticleId)),
    [mySubmissions],
  );
  const selectedCandidate = candidates.find((item) => item.articleId === sourceArticleId) ?? null;

  if (loading || !teamId) {
    return <div className="series-loading surface" aria-live="polite"><Loader2 className="animate-spin" size={22} /> 正在加载投稿记录…</div>;
  }
  if (error && mine.length === 0 && queue.length === 0) {
    return (
      <section className="surface inline-feedback error" role="alert">
        <AlertCircle size={20} />
        <div><strong>投稿记录暂时无法加载</strong><p>{error}</p></div>
      </section>
    );
  }

  return (
    <div className="workspace-submissions-page">
      <header className="workspace-content-page__header">
        <div>
          <span className="eyebrow">团队投稿</span>
          <p>投稿会固定当前文章版本；团队审核通过后进入平台审核并发布为团队文章。</p>
        </div>
        <button className="icon-button" onClick={() => void load()} aria-label="刷新投稿记录" title="刷新"><RefreshCw size={17} /></button>
      </header>

      {error && <p className="inline-feedback error" role="alert">{error}</p>}
      {notice && <p className="inline-feedback success" role="status">{notice}</p>}

      <section className="surface workspace-submit-form">
        <h2>向本团队投稿</h2>
        <p className="secondary">从你的个人文章中选择一篇；投稿会固定文章的当前版本，之后再改动个人文章不影响本次投稿。</p>
        {candidates.length === 0 ? (
          <p className="workspace-panel__empty">你的个人博客还没有可投稿的文章，先去写一篇再回来。</p>
        ) : (
          <div className="workspace-submit-form__fields">
            <select
              value={sourceArticleId}
              onChange={(event) => setSourceArticleId(event.target.value)}
              aria-label="选择要投稿的个人文章"
            >
              <option value="">选择一篇我的文章…</option>
              {candidates.map((article) => {
                const status = PUBLISH_STATUS_LABELS[article.publishStatus] || article.publishStatus;
                const blocked = inFlightArticleIds.has(article.articleId);
                return (
                  <option key={article.articleId} value={article.articleId} disabled={blocked}>
                    {article.title || `文章 #${article.articleId}`}（{status}）{blocked ? " · 已在投稿流程中" : ""}
                  </option>
                );
              })}
            </select>
            <button className="primary-button" disabled={submitting || !sourceArticleId} onClick={() => void createSubmission()}>
              {submitting ? <Loader2 className="animate-spin" size={14} /> : <Send size={14} />} 提交投稿
            </button>
          </div>
        )}
        {selectedCandidate && (
          <p className="secondary workspace-submit-form__hint">
            将固定《{selectedCandidate.title}》当前版本 · 最近更新 {formatDateTime(selectedCandidate.updatedAt)}
          </p>
        )}
      </section>

      <div className="workspace-tabs" role="tablist" aria-label="投稿视角">
        <button type="button" role="tab" aria-selected={tab === "MINE"} className={tab === "MINE" ? "active" : ""} onClick={() => setTab("MINE")}>
          我提交的 <span className="workspace-tabs__count">{mySubmissions.length}</span>
        </button>
        {canReview && (
          <button type="button" role="tab" aria-selected={tab === "QUEUE"} className={tab === "QUEUE" ? "active" : ""} onClick={() => setTab("QUEUE")}>
            团队收到的
            {pendingQueue.length > 0 && <span className="badge badge--warn">{pendingQueue.length}</span>}
          </button>
        )}
      </div>

      {tab === "MINE" && (
        mySubmissions.length === 0 ? (
          <section className="series-empty surface">
            <div>
              <h2>还没有投稿记录</h2>
              <p>把你的个人文章投稿给团队，审核通过后会展示在团队博客中。</p>
            </div>
          </section>
        ) : (
          <section className="surface workspace-submission-list">
            {mySubmissions.map((submission) => (
              <div className="workspace-submission-row" key={submission.id}>
                <div className="workspace-submission-row__copy">
                  <strong>{submission.sourceArticleTitle || `文章 #${submission.sourceArticleId}`}</strong>
                  <span>
                    固定版本 #{submission.fixedSourceVersionId}
                    {" · "}提交于 {formatDateTime(submission.createdAt)}
                    {submission.teamReviewComment ? ` · 团队意见：${submission.teamReviewComment}` : ""}
                    {submission.platformReviewComment ? ` · 平台意见：${submission.platformReviewComment}` : ""}
                  </span>
                </div>
                <div className="workspace-submission-row__meta">
                  <span className={`chip submission-status is-${submission.status.toLowerCase()}`}>
                    {SUBMISSION_STATUS_LABELS[submission.status] || submission.status}
                  </span>
                  {submission.publishedTeamArticleId && (
                    <Link className="ghost-button" href={`/articles/${submission.publishedTeamArticleId}`}>查看团队文章</Link>
                  )}
                  {["TEAM_REVISION_REQUIRED", "TEAM_REJECTED", "PLATFORM_REVISION_REQUIRED", "PLATFORM_REJECTED"].includes(submission.status) && (
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={busy === submission.id}
                      onClick={() => void resubmit(submission)}
                    >
                      {busy === submission.id ? <Loader2 className="animate-spin" size={14} /> : <RefreshCw size={14} />}
                      重新提交
                    </button>
                  )}
                </div>
              </div>
            ))}
          </section>
        )
      )}

      {tab === "QUEUE" && canReview && (
        queue.length === 0 ? (
          <section className="series-empty surface">
            <div>
              <h2>团队还没有收到投稿</h2>
              <p>成员提交的投稿会出现在这里，审核通过后将进入平台审核。</p>
            </div>
          </section>
        ) : (
          <div className="workspace-submission-list">
            {queue.map((submission) => {
              const isPending = submission.status === "TEAM_PENDING";
              return (
                <section className="surface workspace-submission-row workspace-submission-row--review" key={submission.id}>
                  <div className="workspace-submission-row__copy">
                    <strong>{submission.sourceArticleTitle || `文章 #${submission.sourceArticleId}`}</strong>
                    <span>
                      固定版本 #{submission.fixedSourceVersionId}
                      {" · "}提交于 {formatDateTime(submission.createdAt)}
                      {" · "}提交人 #{submission.submittedByUserId}
                      {submission.teamReviewComment ? ` · 团队意见：${submission.teamReviewComment}` : ""}
                    </span>
                  </div>
                  <div className="workspace-submission-row__meta">
                    <span className={`chip submission-status is-${submission.status.toLowerCase()}`}>
                      {SUBMISSION_STATUS_LABELS[submission.status] || submission.status}
                    </span>
                  </div>
                  {isPending && (
                    <div className="workspace-submission-review">
                      <textarea
                        value={comment[submission.id] ?? ""}
                        onChange={(event) => setComment((current) => ({ ...current, [submission.id]: event.target.value }))}
                        placeholder="填写审核意见（必填）"
                        rows={2}
                        maxLength={500}
                        aria-label="审核意见"
                      />
                      <div className="workspace-submission-review__actions">
                        <button type="button" className="primary-button" disabled={busy === submission.id} onClick={() => void decide(submission, "approve")}>
                          {busy === submission.id ? <Loader2 className="animate-spin" size={14} /> : <Check size={14} />} 通过
                        </button>
                        <button type="button" className="secondary-button" disabled={busy === submission.id} onClick={() => void decide(submission, "revision")}>
                          要求修改
                        </button>
                        <button type="button" className="danger-button" disabled={busy === submission.id} onClick={() => void decide(submission, "reject")}>
                          <X size={14} /> 拒绝
                        </button>
                      </div>
                    </div>
                  )}
                  {!isPending && submission.teamReviewedAt && (
                    <p className="secondary">团队已于 {formatDateTime(submission.teamReviewedAt)} 处理。</p>
                  )}
                </section>
              );
            })}
          </div>
        )
      )}
    </div>
  );
}
