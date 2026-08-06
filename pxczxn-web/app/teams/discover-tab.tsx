"use client";

import Link from "next/link";
import { ArrowUpRight, BookOpen, CheckCircle2, Loader2, Search, Sparkles, Users } from "lucide-react";
import { useState } from "react";
import { Avatar } from "../components/prototype-ui";
import { communityApi, publicFileUrl, type MyTeam, type TeamSummary } from "../lib/community-api";
import { formatCount, TEAM_CATEGORIES } from "./team-labels";

type SortKey = "LATEST" | "ARTICLES" | "FOLLOWERS";

/** “发现团队”Tab：关键词搜索 + 排序 + 公开团队卡片。 */
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
      <section className="discover-teams__toolbar surface">
        <div className="discover-teams__search">
          <Search size={16} className="discover-teams__search-icon" />
          <input
            type="search"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索团队名称、标识或简介…"
            aria-label="搜索团队"
          />
        </div>
        <select
          className="discover-teams__sort"
          value={category}
          onChange={(event) => setCategory(event.target.value)}
          aria-label="团队分类筛选"
        >
          <option value="">全部分类</option>
          {TEAM_CATEGORIES.map((cat) => <option key={cat.value} value={cat.value}>{cat.label}</option>)}
        </select>
        <select
          className="discover-teams__sort"
          value={sort}
          onChange={(event) => setSort(event.target.value as SortKey)}
          aria-label="团队排序"
        >
          <option value="LATEST">最近更新</option>
          <option value="ARTICLES">文章最多</option>
          <option value="FOLLOWERS">关注最多</option>
        </select>
      </section>

      {followError && <p className="inline-feedback error" role="alert">{followError}</p>}

      {loading && (
        <div className="series-loading surface" aria-live="polite">
          <Loader2 className="animate-spin" size={22} /> 正在整理团队列表…
        </div>
      )}

      {!loading && error && (
        <section className="surface inline-feedback error" role="alert">
          <strong>团队目录暂时无法加载</strong>
          <p>{error}</p>
        </section>
      )}

      {!loading && !error && filtered.length === 0 && (
        <section className="series-empty surface">
          <span className="series-empty__icon"><Users size={28} /></span>
          <div>
            <h2>{keyword ? "没有匹配的团队" : "还没有公开团队"}</h2>
            <p>
              {keyword
                ? `没有找到与「${keyword}」匹配的团队，换个关键词试试。`
                : "团队获批后会自动展示在这里。你可以发起团队建立申请。"}
            </p>
          </div>
          <Link href="/team-applications" className="secondary-button" onClick={onRequireLogin}>
            申请建立团队 <ArrowUpRight size={16} />
          </Link>
        </section>
      )}

      {!loading && !error && filtered.length > 0 && (
        <section className="teams-grid" aria-label="公开团队">
          {filtered.map((team) => {
            const isMember = memberTeamIds.has(team.teamId);
            return (
              <article className="team-card surface" key={team.teamId}>
                <div className="team-card__head">
                  <Avatar
                    alt={`${team.name}头像`}
                    label={team.name.slice(0, 1)}
                    size="md"
                    src={publicFileUrl(team.avatarFileId)}
                  />
                  <div className="team-card__identity">
                    <h2>{team.name}</h2>
                    <span className="secondary">@{team.slug}</span>
                  </div>
                  {isMember && <span className="chip team-card__joined"><CheckCircle2 size={13} /> 已加入</span>}
                </div>
                <p className="team-card__summary">{team.summary || "这个团队还没有添加简介。"}</p>
                <div className="team-card__meta">
                  <span><BookOpen size={13} /> {team.articleCount} 篇公开文章</span>
                  <span><Users size={13} /> {formatCount(Number(team.followerCount))} 位关注者</span>
                  {team.category && <span className="chip">{team.category}</span>}
                </div>
                <footer className="team-card__actions">
                  {isMember ? (
                    <Link className="primary-button" href={`/teams/${team.slug}/workspace`}>
                      <Sparkles size={14} /> 进入工作台
                    </Link>
                  ) : (
                    <Link className="secondary-button" href={`/teams/${team.slug}`}>
                      查看团队 <ArrowUpRight size={14} />
                    </Link>
                  )}
                  <button
                    type="button"
                    className="ghost-button"
                    disabled={followBusy === team.teamId}
                    onClick={() => toggleFollow(team)}
                    title={following[team.teamId] ? "取消关注该团队" : "关注该团队"}
                  >
                    {followBusy === team.teamId ? <Loader2 className="animate-spin" size={14} /> : null}
                    {following[team.teamId] ? "已关注" : "关注"}
                  </button>
                </footer>
              </article>
            );
          })}
        </section>
      )}
    </div>
  );
}
