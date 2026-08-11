"use client";

import Link from "next/link";
import { Check, RefreshCw, Send, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert, Badge, Button, Card, Input, List, Select, Space, Tabs, Tag, Typography } from "@/components/ui/community-ui";
import { communityApi, type SubmittableArticle, type TeamSubmission } from "../../../../lib/community-api";
import { formatDateTime, hasTeamPermission, PUBLISH_STATUS_LABELS, SUBMISSION_STATUS_LABELS, TEAM_PERMISSIONS } from "../../../team-labels";
import { TeamEmpty, TeamError, TeamLoading } from "../../../team-ui";
import { useWorkspace } from "../workspace-context";

const { Text, Title } = Typography;
type SubmissionTab = "MINE" | "QUEUE";
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
    if (!teamId) return Promise.resolve();
    setLoading(true); setError("");
    const jobs: Array<Promise<unknown>> = [communityApi.myTeamSubmissions().then(setMine), communityApi.submittableArticles().then(setCandidates)];
    if (canReview) jobs.push(communityApi.teamSubmissions(teamId).then(setQueue));
    return Promise.all(jobs).catch((cause: unknown) => setError(cause instanceof Error ? cause.message : "投稿记录加载失败")).finally(() => setLoading(false));
  }, [teamId, canReview]);
  useEffect(() => {
    const timer = window.setTimeout(() => { void load(); }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);
  async function decide(submission: TeamSubmission, action: "approve" | "revision" | "reject") {
    if (!canReview) return;
    setBusy(submission.id); setError("");
    try { await communityApi.decideTeamSubmission(submission.id, action, submission.lockVersion, comment[submission.id]?.trim() ?? ""); setComment((value) => ({ ...value, [submission.id]: "" })); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "审核操作失败，请稍后重试"); } finally { setBusy(null); }
  }
  async function resubmit(submission: TeamSubmission) {
    if (!teamId) return;
    setBusy(submission.id); setError(""); setNotice("");
    try { await communityApi.createTeamSubmission({ sourceArticleId: submission.sourceArticleId, targetTeamId: teamId, supersedesSubmissionId: submission.id }); setNotice("已重新提交，当前版本已固定并进入团队审核。"); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "重新提交失败，请稍后重试"); } finally { setBusy(null); }
  }
  async function createSubmission() {
    if (!teamId || !sourceArticleId) return;
    setSubmitting(true); setError(""); setNotice("");
    try { await communityApi.createTeamSubmission({ sourceArticleId, targetTeamId: teamId }); setNotice("投稿已创建，当前版本已固定并进入团队审核。"); setSourceArticleId(""); await load(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "投稿失败"); } finally { setSubmitting(false); }
  }
  const mySubmissions = useMemo(() => mine.filter((submission) => submission.targetTeamId === teamId), [mine, teamId]);
  const pendingQueue = queue.filter((submission) => submission.status === "TEAM_PENDING");
  const inFlightArticleIds = useMemo(() => new Set(mySubmissions.filter((item) => IN_FLIGHT_STATUSES.has(item.status)).map((item) => item.sourceArticleId)), [mySubmissions]);
  const selectedCandidate = candidates.find((item) => item.articleId === sourceArticleId);
  if (loading || !teamId) return <TeamLoading label="正在加载投稿记录…" />;
  if (error && mine.length === 0 && queue.length === 0) return <TeamError title="投稿记录暂时无法加载" description={error} />;
  const submissionList = (items: TeamSubmission[], review = false) => items.length === 0 ? <TeamEmpty description={review ? "团队还没有收到投稿。" : "还没有投稿记录。"} /> : <List className="workspace-submission-list" dataSource={items} renderItem={(submission) => {
    const needsResubmit = ["TEAM_REVISION_REQUIRED", "TEAM_REJECTED", "PLATFORM_REVISION_REQUIRED", "PLATFORM_REJECTED"].includes(submission.status);
    const pending = review && submission.status === "TEAM_PENDING";
    return <List.Item className="workspace-submission-row"><List.Item.Meta title={submission.sourceArticleTitle || `文章 #${submission.sourceArticleId}`} description={<Space direction="vertical" size={3}><Text type="secondary">固定版本 #{submission.fixedSourceVersionId} · 提交于 {formatDateTime(submission.createdAt)}</Text>{submission.teamReviewComment && <Text type="secondary">团队意见：{submission.teamReviewComment}</Text>}{submission.platformReviewComment && <Text type="secondary">平台意见：{submission.platformReviewComment}</Text>}{pending && <Input.TextArea value={comment[submission.id] ?? ""} onChange={(event) => setComment((value) => ({ ...value, [submission.id]: event.target.value }))} placeholder="填写审核意见（必填）" maxLength={500} autoSize={{ minRows: 2, maxRows: 4 }} />}</Space>} /><Space className="workspace-submission-row__meta" wrap><Tag color={submission.status.includes("REJECTED") ? "error" : submission.status.includes("PENDING") ? "warning" : "blue"}>{SUBMISSION_STATUS_LABELS[submission.status] || submission.status}</Tag>{submission.publishedTeamArticleId && <Link href={`/articles/${submission.publishedTeamArticleId}`}><Button size="small">查看团队文章</Button></Link>}{needsResubmit && <Button size="small" loading={busy === submission.id} onClick={() => void resubmit(submission)} icon={<RefreshCw size={13} />}>重新提交</Button>}{pending && <Space><Button type="primary" size="small" loading={busy === submission.id} onClick={() => void decide(submission, "approve")} icon={<Check size={13} />}>通过</Button><Button size="small" loading={busy === submission.id} onClick={() => void decide(submission, "revision")}>要求修改</Button><Button danger size="small" loading={busy === submission.id} onClick={() => void decide(submission, "reject")} icon={<X size={13} />}>拒绝</Button></Space>}</Space></List.Item>;
  }} />;
  return <div className="workspace-submissions-page">
    <Card className="workspace-content-page__header" bordered={false}><div><Text type="secondary">团队投稿</Text><Title level={4}>投稿与审核</Title><Text type="secondary">投稿会固定当前文章版本；团队审核通过后进入平台审核并发布为团队文章。</Text></div><Button aria-label="刷新投稿记录" icon={<RefreshCw size={16} />} onClick={() => void load()}>刷新</Button></Card>
    {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}{notice && <Alert type="success" showIcon message={notice} style={{ marginBottom: 16 }} />}
    <Card className="workspace-submit-form" title="向本团队投稿" bordered={false}><Text type="secondary">从你的个人文章中选择一篇；投稿后个人文章的后续修改不会影响本次投稿。</Text>{candidates.length === 0 ? <TeamEmpty description="个人博客还没有可投稿的文章。" /> : <Space direction="vertical" size={12} style={{ width: "100%", marginTop: 16 }}><Select aria-label="选择要投稿的个人文章" value={sourceArticleId || undefined} placeholder="选择一篇我的文章" onChange={setSourceArticleId} options={candidates.map((article) => ({ value: article.articleId, disabled: inFlightArticleIds.has(article.articleId), label: `${article.title || `文章 #${article.articleId}`}（${PUBLISH_STATUS_LABELS[article.publishStatus] || article.publishStatus}${inFlightArticleIds.has(article.articleId) ? " · 已在投稿流程中" : ""}）` }))} /><Button type="primary" loading={submitting} disabled={!sourceArticleId} onClick={() => void createSubmission()} icon={<Send size={14} />}>提交投稿</Button>{selectedCandidate && <Text type="secondary">将固定《{selectedCandidate.title}》当前版本 · 最近更新 {formatDateTime(selectedCandidate.updatedAt)}</Text>}</Space>}</Card>
    <Card className="workspace-tabs" bordered={false} bodyStyle={{ paddingBlock: 0 }}><Tabs activeKey={tab} onChange={(key) => setTab(key as SubmissionTab)} items={[{ key: "MINE", label: <Space size={5}>我提交的 <Badge count={mySubmissions.length} showZero color="#d9d9d9" /></Space> }, ...(canReview ? [{ key: "QUEUE", label: <Space size={5}>团队收到的 <Badge count={pendingQueue.length} showZero color={pendingQueue.length ? "#ff4d4f" : "#d9d9d9"} /></Space> }] : [])]} /></Card>
    <Card bordered={false}>{tab === "MINE" ? submissionList(mySubmissions) : submissionList(queue, true)}</Card>
  </div>;
}
