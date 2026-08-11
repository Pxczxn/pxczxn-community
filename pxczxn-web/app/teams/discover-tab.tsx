"use client";

import Link from "next/link";
import { Button, Card, Col, Empty, Input, Row, Select, Space, Spin, Tag, Typography } from "@/components/ui/community-ui";
import { ArrowUpRight, BookOpen, CheckCircle2, Search, Sparkles, Users } from "lucide-react";
import { useState } from "react";
import { Avatar } from "../components/prototype-ui";
import { communityApi, publicFileUrl, type MyTeam, type TeamSummary } from "../lib/community-api";
import { formatCount, TEAM_CATEGORIES } from "./team-labels";

const { Title, Paragraph, Text } = Typography;

type SortKey = "LATEST" | "ARTICLES" | "FOLLOWERS";

export function DiscoverTeamsTab({
  teams,
  mine,
  loading,
  error,
  onRequireLogin,
}: {
  teams: TeamSummary[];
  mine: MyTeam[] | null;
  loading: boolean;
  error: string;
  onRequireLogin: () => void;
}) {
  const [keyword, setKeyword] = useState("");
  const [sort, setSort] = useState<SortKey>("LATEST");
  const [category, setCategory] = useState("");
  const [followBusy, setFollowBusy] = useState<string | null>(null);
  const [followError, setFollowError] = useState("");
  const [following, setFollowing] = useState<Record<string, boolean>>({});

  const memberTeamIds = new Set((mine ?? []).map((team) => team.teamId));

  const filtered = teams
    .filter((team) => {
      const query = keyword.trim().toLowerCase();
      if (query && !(
        team.name.toLowerCase().includes(query)
        || team.slug.toLowerCase().includes(query)
        || (team.summary ?? "").toLowerCase().includes(query)
      )) return false;
      if (category && (team.category ?? "") !== category) return false;
      return true;
    })
    .sort((a, b) => {
      if (sort === "ARTICLES") return Number(b.articleCount) - Number(a.articleCount);
      if (sort === "FOLLOWERS") return Number(b.followerCount) - Number(a.followerCount);
      return 0;
    });

  async function toggleFollow(team: TeamSummary) {
    if (!mine) {
      onRequireLogin();
      return;
    }
    setFollowBusy(team.teamId);
    setFollowError("");
    try {
      const relationship = await communityApi.blogFollowRelationship(team.blogId);
      const next = !relationship.following;
      await communityApi.setBlogFollow(team.blogId, next);
      setFollowing((current) => ({ ...current, [team.teamId]: next }));
    } catch (cause) {
      setFollowError(cause instanceof Error ? cause.message : "关注操作失败，请稍后重试");
    } finally {
      setFollowBusy(null);
    }
  }

  return (
    <div className="discover-teams">
      {/* Toolbar */}
      <Card size="small" style={{ borderRadius: 12, marginBottom: 20 }}>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 12, alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ minWidth: 240, flex: "1 1 280px" }}>
            <Input
              prefix={<Search size={15} style={{ color: "var(--text-tertiary)" }} />}
              placeholder="搜索团队名称、标识或简介…"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              allowClear
            />
          </div>
          <Space wrap size="middle">
            <Select
              value={category}
              onChange={setCategory}
              options={[
                { value: "", label: "全部分类" },
                ...TEAM_CATEGORIES.map((cat) => ({ value: cat.value, label: cat.label })),
              ]}
              style={{ width: 130 }}
            />
            <Select
              value={sort}
              onChange={(val) => setSort(val as SortKey)}
              options={[
                { value: "LATEST", label: "最近更新" },
                { value: "ARTICLES", label: "文章最多" },
                { value: "FOLLOWERS", label: "关注最多" },
              ]}
              style={{ width: 120 }}
            />
          </Space>
        </div>
      </Card>

      {followError && (
        <Card style={{ borderRadius: 12, borderColor: "#ff4d4f", marginBottom: 16 }}>
          <Text type="danger">{followError}</Text>
        </Card>
      )}

      {loading && (
        <div style={{ textAlign: "center", padding: 60 }}>
          <Spin tip="正在整理团队列表…" />
        </div>
      )}

      {!loading && error && (
        <Card style={{ borderRadius: 12, borderColor: "#ff4d4f", marginBottom: 16 }}>
          <Text type="danger">{error}</Text>
        </Card>
      )}

      {!loading && !error && filtered.length === 0 && (
        <Empty description={keyword ? `没有找到与「${keyword}」匹配的团队` : "还没有公开团队"} style={{ margin: "40px 0" }}>
          <Link href="/team-applications">
            <Button type="primary" onClick={onRequireLogin} icon={<ArrowUpRight size={15} />}>
              申请建立团队
            </Button>
          </Link>
        </Empty>
      )}

      {!loading && !error && filtered.length > 0 && (
        <Row gutter={[16, 16]}>
          {filtered.map((team) => {
            const isMember = memberTeamIds.has(team.teamId);
            return (
              <Col xs={24} sm={12} lg={8} key={team.teamId}>
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
                      {isMember && <Tag color="green" icon={<CheckCircle2 size={12} />}>已加入</Tag>}
                    </div>
                    <Paragraph type="secondary" style={{ fontSize: 13, margin: "0 0 12px" }} ellipsis={{ rows: 2 }}>
                      {team.summary || "这个团队还没有添加简介。"}
                    </Paragraph>
                    <Space size={12} style={{ fontSize: 12, color: "var(--text-tertiary, #94a3b8)", marginBottom: 16 }}>
                      <span><BookOpen size={12} /> {team.articleCount} 文章</span>
                      <span><Users size={12} /> {formatCount(Number(team.followerCount))} 关注</span>
                      {team.category && <Tag style={{ margin: 0, fontSize: 11 }}>{team.category}</Tag>}
                    </Space>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", borderTop: "1px solid var(--color-border, #e2e8f0)", paddingTop: 12 }}>
                    {isMember ? (
                      <Link href={`/teams/${team.slug}/workspace`}>
                        <Button type="primary" size="small" icon={<Sparkles size={13} />}>
                          工作台
                        </Button>
                      </Link>
                    ) : (
                      <Link href={`/teams/${team.slug}`}>
                        <Button size="small" icon={<ArrowUpRight size={13} />}>
                          查看团队
                        </Button>
                      </Link>
                    )}
                    <Button
                      size="small"
                      type={following[team.teamId] ? "default" : "dashed"}
                      loading={followBusy === team.teamId}
                      onClick={() => toggleFollow(team)}
                    >
                      {following[team.teamId] ? "已关注" : "关注"}
                    </Button>
                  </div>
                </Card>
              </Col>
            );
          })}
        </Row>
      )}
    </div>
  );
}
