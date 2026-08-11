"use client";

import Link from "next/link";
import {
  BookOpen,
  Eye,
  FileText,
  MessageSquare,
  PenLine,
  Send,
  Settings2,
  ThumbsUp,
  UserPlus,
  Users,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Badge, Button, Card, Col, List, Row, Space, Statistic, Typography } from "@/components/ui/community-ui";
import { communityApi, type TeamDashboard } from "../../../lib/community-api";
import {
  activityText,
  formatCount,
  formatDateTime,
  hasTeamPermission,
  PUBLISH_STATUS_LABELS,
  TEAM_PERMISSIONS,
} from "../../team-labels";
import { TeamEmpty, TeamError, TeamLoading } from "../../team-ui";
import { useWorkspace } from "./workspace-context";

const { Text, Title } = Typography;

export default function WorkspaceOverviewPage() {
  const { teamId, teamSlug } = useWorkspace();
  const [dashboard, setDashboard] = useState<TeamDashboard | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!teamId) return;
    let active = true;
    void communityApi.teamDashboard(teamId)
      .then((value) => { if (active) setDashboard(value); })
      .catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "概览加载失败"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [teamId]);

  if (loading || !teamId) return <TeamLoading label="正在加载团队概览…" />;
  if (error || !dashboard) return <TeamError title="概览暂时无法加载" description={error || "没有可用的团队数据"} />;

  const { stats, todos, recentArticles, recentActivities, permissions } = dashboard;
  const hasReviewPermission = hasTeamPermission(permissions, TEAM_PERMISSIONS.MANAGE_SUBMISSIONS);
  const hasSeriesPermission = hasTeamPermission(permissions, TEAM_PERMISSIONS.MANAGE_SERIES);
  const hasMemberPermission = hasTeamPermission(permissions, TEAM_PERMISSIONS.MANAGE_MEMBERS);
  const hasSettingsPermission = hasTeamPermission(permissions, TEAM_PERMISSIONS.MANAGE_TEAM);
  const workspaceHref = (segment: string) => `/teams/${teamSlug}/workspace/${segment}`;
  const todoItems = [
    { label: "待处理投稿", count: todos.pendingSubmissionCount, href: "submissions", visible: hasReviewPermission },
    { label: "需修改投稿", count: todos.revisionRequiredCount, href: "submissions", visible: true },
    { label: "待处理邀请", count: todos.pendingInvitationCount, href: "members", visible: hasMemberPermission },
    { label: "待审核系列", count: todos.pendingSeriesReviewCount, href: "series", visible: hasSeriesPermission },
    { label: "内容风险", count: todos.contentRiskCount, href: "content", visible: true },
  ].filter((item) => item.visible);
  const todoTotal = todoItems.reduce((total, item) => total + item.count, 0);
  const statItems = [
    { label: "文章", value: stats.publishedArticleCount, icon: FileText },
    { label: "系列", value: stats.seriesCount, icon: BookOpen },
    { label: "成员", value: stats.memberCount, icon: Users },
    { label: "互动", value: stats.totalInteractionCount, icon: ThumbsUp },
  ];

  return (
    <div className="workspace-overview">
      <Card className="workspace-quick" bordered={false}>
        <div className="workspace-quick__copy">
          <Text type="secondary">团队工作台</Text>
          <Title level={4}>把内容协作集中在这里</Title>
          <Text type="secondary">管理团队文章、系列、投稿和成员协作。</Text>
        </div>
        <Space wrap className="workspace-quick__actions">
          <Link href={`/editor/new?blogId=${dashboard.team.blogId}`}><Button type="primary" icon={<PenLine size={15} />}>写团队文章</Button></Link>
          <Link href={workspaceHref("submissions")}><Button icon={<Send size={15} />}>向团队投稿</Button></Link>
          {hasSeriesPermission && <Link href={workspaceHref("series")}><Button icon={<BookOpen size={15} />}>创建系列</Button></Link>}
          {hasMemberPermission && <Link href={workspaceHref("members")}><Button icon={<UserPlus size={15} />}>邀请成员</Button></Link>}
          {hasSettingsPermission && <Link href={workspaceHref("settings")}><Button icon={<Settings2 size={15} />}>团队设置</Button></Link>}
        </Space>
      </Card>

      <Row className="workspace-stats" gutter={[16, 16]}>
        {statItems.map((item) => {
          const Icon = item.icon;
          return (
            <Col xs={12} lg={6} key={item.label}>
              <Card className="workspace-stat" bordered={false}>
                <Icon className="workspace-stat__icon" size={20} aria-hidden="true" />
                <Statistic title={item.label} value={item.value} formatter={(value) => formatCount(Number(value))} />
              </Card>
            </Col>
          );
        })}
      </Row>

      <Row className="workspace-overview__columns" gutter={[16, 16]}>
        <Col xs={24} xl={15}>
          <Card
            className="workspace-panel"
            title="最近文章"
            extra={<Link href={workspaceHref("content")}><Button type="link">查看全部</Button></Link>}
            bordered={false}
          >
            {recentArticles.length === 0 ? <TeamEmpty description="团队还没有文章，开始一次创作吧。" /> : (
              <List
                className="workspace-article-list"
                dataSource={recentArticles}
                renderItem={(article) => (
                  <List.Item
                    className="workspace-article-row"
                    actions={[
                      <Space className="workspace-article-row__meta" key="metrics" size={10}>
                        <span><Eye size={13} /> {article.viewCount}</span>
                        <span><ThumbsUp size={13} /> {article.likeCount}</span>
                        <span><MessageSquare size={13} /> {article.commentCount}</span>
                      </Space>,
                    ]}
                  >
                    <List.Item.Meta
                      title={<Link href={article.publishStatus === "PUBLISHED" && article.slug ? `/articles/${article.articleId}` : `/editor/${article.articleId}`}>{article.title}</Link>}
                      description={`${PUBLISH_STATUS_LABELS[article.publishStatus] || article.publishStatus} · ${article.authorDisplayName || "团队成员"} · 更新于 ${formatDateTime(article.updatedAt)}`}
                    />
                  </List.Item>
                )}
              />
            )}
          </Card>
        </Col>
        <Col xs={24} xl={9}>
          <Space className="workspace-overview__side" direction="vertical" size={16} style={{ width: "100%" }}>
            <Card className="workspace-panel" title={<Space>团队待处理 <Badge count={todoTotal} showZero={false} /></Space>} bordered={false}>
              {todoTotal === 0 ? <TeamEmpty description="没有待处理事项，一切就绪。" /> : (
                <List
                  className="workspace-todo-list"
                  dataSource={todoItems}
                  renderItem={(item) => <List.Item><Link href={workspaceHref(item.href)}>{item.label}<Badge count={item.count} showZero color={item.count > 0 ? "#ff4d4f" : "#d9d9d9"} /></Link></List.Item>}
                />
              )}
            </Card>
            <Card className="workspace-panel" title="协作动态" bordered={false}>
              {recentActivities.length === 0 ? <TeamEmpty description="还没有团队活动记录。" /> : (
                <List
                  className="workspace-activity-list"
                  dataSource={recentActivities}
                  renderItem={(activity) => <List.Item><List.Item.Meta title={activityText(activity.eventType, activity.actorDisplayName)} description={formatDateTime(activity.occurredAt)} /></List.Item>}
                />
              )}
            </Card>
          </Space>
        </Col>
      </Row>
    </div>
  );
}
