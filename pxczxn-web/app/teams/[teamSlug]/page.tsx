"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import { ArrowUpRight, Sparkles } from "lucide-react";
import { Button, Card, Col, Descriptions, List, Pagination, Row, Space, Statistic, Tabs, Tag, Typography } from "@/components/ui/community-ui";
import { Avatar, UserTopbar } from "../../components/prototype-ui";
import { communityApi, publicFileUrl, readSession, type MyTeam, type PublicArticlePage, type TeamPortal, type Series } from "../../lib/community-api";
import { formatCount, ROLE_LABELS, SERIALIZATION_LABELS } from "../team-labels";
import { TeamEmpty, TeamError, TeamLoading } from "../team-ui";

const { Paragraph, Text, Title } = Typography;
type PortalTab = "home" | "articles" | "series" | "members" | "about";
const TABS: Array<{ key: PortalTab; label: string }> = [
  { key: "home", label: "主页" }, { key: "articles", label: "文章" }, { key: "series", label: "系列" }, { key: "members", label: "成员" }, { key: "about", label: "关于" },
];

function tabFromUrl(): PortalTab {
  if (typeof window === "undefined") return "home";
  const value = new URLSearchParams(window.location.search).get("tab");
  return TABS.some((item) => item.key === value) ? value as PortalTab : "home";
}

export default function TeamDetailPage() {
  const { teamSlug: slug } = useParams<{ teamSlug: string }>();
  const [team, setTeam] = useState<TeamPortal | null>(null);
  const [tab, setTab] = useState<PortalTab>("home");
  const [articles, setArticles] = useState<PublicArticlePage | null>(null);
  const [pageNum, setPageNum] = useState(1);
  const [series, setSeries] = useState<Series[]>([]);
  const [mine, setMine] = useState<MyTeam[] | null>(null);
  const [following, setFollowing] = useState(false);
  const [followBusy, setFollowBusy] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const timer = window.setTimeout(() => setTab(tabFromUrl()), 0);
    return () => window.clearTimeout(timer);
  }, []);
  useEffect(() => {
    let active = true;
    void communityApi.team(slug)
      .then(async (portal) => {
        if (!active) return;
        setTeam(portal);
        const jobs: Array<Promise<unknown>> = [
          communityApi.publicArticles(slug, pageNum, 10).then((value) => { if (active) setArticles(value); }),
          communityApi.teamPublicSeries(portal.team.teamId).then((value) => { if (active) setSeries(value); }),
        ];
        if (readSession()) jobs.push(
          communityApi.myTeams().then((value) => { if (active) setMine(value); }).catch(() => { if (active) setMine(null); }),
          communityApi.blogFollowRelationship(portal.team.blogId).then((value) => { if (active) setFollowing(value.following); }).catch(() => undefined),
        );
        await Promise.all(jobs);
      })
      .catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "加载失败"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [slug, pageNum]);

  const isMember = useMemo(() => Boolean(mine && team && mine.some((item) => item.teamId === team.team.teamId)), [mine, team]);
  function selectTab(next: string) {
    const target = next as PortalTab;
    setTab(target);
    window.history.replaceState(null, "", `/teams/${slug}?tab=${target}`);
  }
  async function toggleFollow() {
    if (!team || !readSession()) {
      window.location.assign(`/login?returnTo=${encodeURIComponent(`/teams/${slug}`)}`);
      return;
    }
    setFollowBusy(true);
    try {
      await communityApi.setBlogFollow(team.team.blogId, !following);
      setFollowing((value) => !value);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "关注操作失败，请稍后重试");
    } finally { setFollowBusy(false); }
  }

  if (loading && !team) return <><UserTopbar title="团队" /><main className="page-shell team-detail-page"><TeamLoading label="正在加载团队…" /></main></>;
  if (error && !team) return <><UserTopbar title="团队" /><main className="page-shell team-detail-page"><TeamError title="团队主页暂时无法打开" description={error} extra={<Link href="/teams"><Button type="primary">返回团队列表</Button></Link>} /></main></>;
  if (!team) return null;

  const publicArticles = articles?.records ?? [];
  const tabContent = {
    home: (
      <Row className="team-detail-layout" gutter={[16, 16]}>
        <Col xs={24} lg={16}><Card className="team-detail-articles" title="最新文章" bordered={false}>
          {publicArticles.length === 0 ? <TeamEmpty description="暂无公开文章。" /> : <List dataSource={publicArticles.slice(0, 5)} renderItem={(article) => <List.Item><List.Item.Meta title={<Link href={`/articles/${article.articleId}`}>{article.title}</Link>} description={article.summary} /></List.Item>} />}
        </Card></Col>
        <Col xs={24} lg={8}><Card className="team-detail-members" title="核心成员" bordered={false}>
          <List dataSource={team.members.slice(0, 8)} renderItem={(member) => { const name = member.displayName || member.username; return <List.Item><List.Item.Meta avatar={<Avatar alt={name} label={name.slice(0, 1)} size="sm" src={publicFileUrl(member.avatarFileId)} />} title={name} description={`@${member.username}`} /><Tag color="blue">{ROLE_LABELS[member.roleCode] || member.roleCode}</Tag></List.Item>; }} />
          {team.members.length > 8 && <Button type="link" onClick={() => selectTab("members")}>查看全部 {team.members.length} 位成员</Button>}
        </Card></Col>
      </Row>
    ),
    articles: (
      <Card className="team-portal-panel" title="团队文章" extra={<Text type="secondary">共 {articles?.total ?? 0} 篇公开文章</Text>} bordered={false}>
        {publicArticles.length === 0 ? <TeamEmpty description="暂无公开文章。" /> : <List dataSource={publicArticles} renderItem={(article) => <List.Item><List.Item.Meta title={<Link href={`/articles/${article.articleId}`}>{article.title}</Link>} description={article.summary} /></List.Item>} />}
        {(articles?.total ?? 0) > (articles?.pageSize ?? 10) && <Pagination className="team-portal-pagination" current={articles?.pageNum ?? 1} pageSize={articles?.pageSize ?? 10} total={articles?.total ?? 0} showSizeChanger={false} onChange={setPageNum} />}
      </Card>
    ),
    series: (
      <Card className="team-portal-panel" title="系列" extra={<Text type="secondary">仅展示公开且通过审核的系列</Text>} bordered={false}>
        {series.length === 0 ? <TeamEmpty description="该团队还没有公开系列。" /> : <Row gutter={[16, 16]}>{series.map((item) => <Col xs={24} md={12} key={item.id}><Card className="team-portal-series-card" size="small" title={item.title} extra={<Tag>{SERIALIZATION_LABELS[item.serializationStatus] || item.serializationStatus}</Tag>}><Paragraph type="secondary">{item.summary || "暂无简介"}</Paragraph><Text type="secondary">{item.chapterCount} 章</Text>{item.chapterCount > 0 && <Link href={`/series/${item.id}`}><Button type="link" icon={<ArrowUpRight size={14} />}>查看系列</Button></Link>}</Card></Col>)}</Row>}
      </Card>
    ),
    members: (
      <Card className="team-portal-panel" title="团队成员" extra={<Text type="secondary">共 {team.members.length} 位成员</Text>} bordered={false}>
        {team.settings.publicMembers ? <List grid={{ gutter: 16, xs: 1, sm: 2, lg: 3 }} dataSource={team.members} renderItem={(member) => { const name = member.displayName || member.username; return <List.Item><Card size="small"><Space><Avatar alt={name} label={name.slice(0, 1)} size="sm" src={publicFileUrl(member.avatarFileId)} /><span><Text strong>{name}</Text><Text type="secondary" style={{ display: "block" }}>@{member.username}</Text></span></Space><Tag color="blue" style={{ float: "right" }}>{ROLE_LABELS[member.roleCode] || member.roleCode}</Tag></Card></List.Item>; }} /> : <TeamEmpty description="该团队未公开成员列表。" />}
      </Card>
    ),
    about: (
      <Row className="team-detail-layout" gutter={[16, 16]}>
        <Col xs={24} lg={16}><Card className="team-portal-panel" title="团队介绍" bordered={false}><Descriptions column={1} items={[{ key: "summary", label: "简介", children: team.team.summary || "这个团队还没有添加简介。" }, ...(team.settings.contentDirection ? [{ key: "direction", label: "内容方向", children: team.settings.contentDirection }] : []), ...(team.settings.submissionGuideline ? [{ key: "guideline", label: "投稿说明", children: team.settings.submissionGuideline }] : []), ...(team.settings.contactInfo ? [{ key: "contact", label: "联系方式", children: team.settings.contactInfo }] : [])]} /></Card></Col>
        <Col xs={24} lg={8}><Card className="team-portal-panel" title="团队数据" bordered={false}><Row gutter={[12, 16]}>{[{ title: "公开文章", value: team.team.articleCount }, { title: "成员", value: team.members.length }, { title: "关注者", value: Number(team.team.followerCount) }, { title: "公开系列", value: series.length }].map((item) => <Col span={12} key={item.title}><Statistic title={item.title} value={item.value} formatter={(value) => formatCount(Number(value))} /></Col>)}</Row><Paragraph type="secondary" style={{ marginTop: 20 }}>{team.settings.allowSubmissions ? "本团队开放外部投稿，欢迎分享你的文章。" : "本团队暂未开放外部投稿，仅团队成员可以投稿。"}</Paragraph></Card></Col>
      </Row>
    ),
  } satisfies Record<PortalTab, ReactNode>;

  return (
    <><UserTopbar title="团队" /><main className="page-shell team-detail-page prototype-team-portal" data-team-theme={team.settings.theme || "default"}>
      <Card className="team-detail-hero" bordered={false}>
        <Avatar alt={`${team.team.name}头像`} label={team.team.name.slice(0, 1)} size="lg" src={publicFileUrl(team.team.avatarFileId)} />
        <div className="team-detail-hero__copy"><Space size={8} wrap><Title level={2}>{team.team.name}</Title>{team.settings.category && <Tag color="blue">{team.settings.category}</Tag>}</Space><Text type="secondary">@{team.team.slug}</Text><Paragraph>{team.team.summary || "这个团队还没有添加简介。"}</Paragraph><Text type="secondary">由 {team.ownerDisplayName || "团队成员"} 维护 · {team.team.articleCount} 篇文章 · {formatCount(Number(team.team.followerCount))} 位关注者</Text></div>
        <Space className="team-detail-hero__actions" wrap>{isMember && <Link href={`/teams/${slug}/workspace`}><Button type="primary" icon={<Sparkles size={15} />}>进入工作台</Button></Link>}<Button loading={followBusy} onClick={() => void toggleFollow()}>{following ? "已关注" : "关注团队"}</Button></Space>
      </Card>
      <Card className="team-portal-tabs" bordered={false} bodyStyle={{ paddingBlock: 0 }}><Tabs activeKey={tab} onChange={selectTab} items={TABS.map((item) => ({ key: item.key, label: item.label }))} /></Card>
      {tabContent[tab]}
    </main></>
  );
}
