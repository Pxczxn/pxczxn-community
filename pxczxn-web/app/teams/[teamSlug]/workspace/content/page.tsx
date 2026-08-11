"use client";

import Link from "next/link";
import { Eye, MessageSquare, ThumbsUp } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Badge, Button, Card, List, Space, Tabs, Tag, Typography } from "@/components/ui/community-ui";
import { communityApi, type TeamArticleBrief } from "../../../../lib/community-api";
import { formatDateTime, PUBLISH_STATUS_LABELS } from "../../../team-labels";
import { TeamEmpty, TeamError, TeamLoading } from "../../../team-ui";
import { useWorkspace } from "../workspace-context";

const { Text, Title } = Typography;
type ContentTab = "ALL" | "DRAFT" | "REVIEWING" | "PUBLISHED" | "REJECTED";
const CONTENT_TABS: Array<{ key: ContentTab; label: string }> = [
  { key: "ALL", label: "全部" }, { key: "DRAFT", label: "草稿" }, { key: "REVIEWING", label: "审核中" }, { key: "PUBLISHED", label: "已发布" }, { key: "REJECTED", label: "已退回" },
];
function matchesTab(article: TeamArticleBrief, tab: ContentTab): boolean {
  if (tab === "ALL") return true;
  if (tab === "DRAFT") return article.publishStatus === "DRAFT";
  if (tab === "REVIEWING") return ["PENDING_REVIEW", "APPROVED", "SCHEDULED"].includes(article.publishStatus);
  if (tab === "PUBLISHED") return ["PUBLISHED", "HIDDEN"].includes(article.publishStatus);
  return ["TAKEN_DOWN", "PUBLISH_FAILED"].includes(article.publishStatus) || ["REJECTED", "REVISION_REQUIRED"].includes(article.reviewStatus);
}

export default function WorkspaceContentPage() {
  const { teamId, teamSlug, workspace } = useWorkspace();
  const [articles, setArticles] = useState<TeamArticleBrief[]>([]);
  const [tab, setTab] = useState<ContentTab>("ALL");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    if (!teamId) return;
    let active = true;
    void communityApi.teamArticles(teamId).then((value) => { if (active) setArticles(value); }).catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "内容列表加载失败"); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [teamId]);
  const counts = useMemo(() => Object.fromEntries(CONTENT_TABS.map((item) => [item.key, articles.filter((article) => matchesTab(article, item.key)).length])) as Record<ContentTab, number>, [articles]);
  const visible = articles.filter((article) => matchesTab(article, tab));
  if (loading || !teamId) return <TeamLoading label="正在加载团队内容…" />;
  if (error) return <TeamError title="内容列表暂时无法加载" description={error} />;
  return <div className="workspace-content-page">
    <Card className="workspace-content-page__header" bordered={false}>
      <div><Text type="secondary">内容管理</Text><Title level={4}>团队内容</Title><Text type="secondary">管理团队博客下的全部文章，包括草稿与投稿发布结果。</Text></div>
      <Space wrap><Link href={`/editor/new?blogId=${workspace?.team.team.blogId ?? ""}`}><Button type="primary">写团队文章</Button></Link><Link href={`/teams/${teamSlug}/workspace/submissions`}><Button>向团队投稿</Button></Link></Space>
    </Card>
    <Card className="workspace-tabs" bordered={false} bodyStyle={{ paddingBlock: 0 }}><Tabs activeKey={tab} onChange={(key) => setTab(key as ContentTab)} items={CONTENT_TABS.map((item) => ({ key: item.key, label: <Space size={5}>{item.label}<Badge count={counts[item.key]} showZero color="#d9d9d9" /></Space> }))} /></Card>
    <Card className="workspace-content-table" bordered={false}>
      {visible.length === 0 ? <TeamEmpty description={tab === "ALL" ? "团队还没有文章。" : "这个状态下没有内容。"} /> : <List dataSource={visible} renderItem={(article) => <List.Item className="workspace-content-row" actions={[<Space className="workspace-content-row__meta" key="meta" wrap><span><Eye size={13} /> {article.viewCount}</span><span><ThumbsUp size={13} /> {article.likeCount}</span><span><MessageSquare size={13} /> {article.commentCount}</span>{article.seriesTitle && <Tag>{article.seriesTitle}</Tag>}<Tag color={article.publishStatus === "PUBLISHED" ? "success" : "default"}>{PUBLISH_STATUS_LABELS[article.publishStatus] || article.publishStatus}</Tag><Link href={article.publishStatus === "PUBLISHED" && article.slug ? `/articles/${article.articleId}` : `/editor/${article.articleId}`}><Button size="small">{article.publishStatus === "PUBLISHED" ? "查看" : "编辑"}</Button></Link></Space>]}><List.Item.Meta title={article.title} description={`${article.authorDisplayName || "团队成员"} · 更新于 ${formatDateTime(article.updatedAt)}`} /></List.Item>} />}
    </Card>
  </div>;
}
