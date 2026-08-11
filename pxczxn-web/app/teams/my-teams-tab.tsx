"use client";

import Link from "next/link";
import { Button, Card, Col, Row, Space, Tag, Typography } from "@/components/ui/community-ui";
import { ArrowUpRight, BookOpen, FileText, Inbox, LayoutDashboard, Users } from "lucide-react";
import { Avatar } from "../components/prototype-ui";
import { publicFileUrl, type MyTeam } from "../lib/community-api";
import { formatCount, formatDateTime, ROLE_LABELS } from "./team-labels";

const { Title, Paragraph, Text } = Typography;

/** “我的团队”Tab */
export function MyTeamsTab({ teams }: { teams: MyTeam[] }) {
  return (
    <div className="my-teams">
      <Card style={{ borderRadius: 14, marginBottom: 20 }}>
        <Space align="center" size={8}>
          <Users size={18} style={{ color: "var(--primary, #1677ff)" }} />
          <Text strong style={{ fontSize: 15 }}>
            你参与了 <Text strong style={{ color: "var(--primary, #1677ff)" }}>{teams.length}</Text> 个团队
          </Text>
        </Space>
        <Text type="secondary" style={{ fontSize: 13, display: "block", marginTop: 4 }}>
          进入工作台后可在顶部切换团队；最近访问的团队会被记住。
        </Text>
      </Card>

      <Row gutter={[16, 16]}>
        {teams.map((team) => {
          const hasReview = team.pendingSubmissionCount > 0;
          return (
            <Col xs={24} md={12} key={team.teamId}>
              <Card hoverable style={{ borderRadius: 14, height: "100%", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
                <div>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 12 }}>
                    <Space size={12}>
                      <Avatar
                        alt={`${team.name}头像`}
                        label={team.name.slice(0, 1)}
                        size="md"
                        src={publicFileUrl(team.avatarFileId)}
                      />
                      <div>
                        <Title level={5} style={{ margin: 0, fontSize: 16 }}>{team.name}</Title>
                        <Text type="secondary" style={{ fontSize: 12 }}>@{team.slug}</Text>
                      </div>
                    </Space>
                    <Tag color="blue">{ROLE_LABELS[team.viewerRole] || team.viewerRole}</Tag>
                  </div>

                  <Paragraph type="secondary" style={{ fontSize: 13, margin: "0 0 12px" }} ellipsis={{ rows: 2 }}>
                    {team.summary || "这个团队还没有添加简介。"}
                  </Paragraph>

                  <Space size={12} style={{ fontSize: 12, color: "var(--text-tertiary, #94a3b8)", marginBottom: 12, flexWrap: "wrap" }}>
                    <span><Users size={12} /> {team.memberCount} 成员</span>
                    <span><BookOpen size={12} /> {team.articleCount} 文章</span>
                    <span><FileText size={12} /> {team.seriesCount} 连载</span>
                    <span><Inbox size={12} /> {formatCount(team.followerCount)} 关注</span>
                  </Space>

                  <div style={{ marginBottom: 16 }}>
                    {hasReview && <Tag color="warning">{team.pendingSubmissionCount} 篇投稿待审核</Tag>}
                    {team.revisionRequiredCount > 0 && <Tag color="error">{team.revisionRequiredCount} 篇投稿需修改</Tag>}
                    {!hasReview && team.revisionRequiredCount === 0 && <Tag color="default">暂无待办</Tag>}
                  </div>
                </div>

                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", borderTop: "1px solid var(--color-border, #e2e8f0)", paddingTop: 12 }}>
                  <Space size={8}>
                    <Link href={`/teams/${team.slug}/workspace`}>
                      <Button type="primary" size="small" icon={<LayoutDashboard size={14} />}>
                        工作台
                      </Button>
                    </Link>
                    <Link href={`/teams/${team.slug}`}>
                      <Button size="small" icon={<ArrowUpRight size={13} />}>
                        公开主页
                      </Button>
                    </Link>
                  </Space>
                  <Text type="secondary" style={{ fontSize: 11 }}>
                    更新于 {formatDateTime(team.updatedAt)}
                  </Text>
                </div>
              </Card>
            </Col>
          );
        })}
      </Row>
    </div>
  );
}
