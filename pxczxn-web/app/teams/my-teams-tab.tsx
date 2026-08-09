"use client";

import Link from "next/link";
import { ArrowUpRight, BookOpen, FileText, Inbox, LayoutDashboard, Users } from "lucide-react";
import { Avatar } from "../components/prototype-ui";
import { publicFileUrl, type MyTeam } from "../lib/community-api";
import { formatCount, formatDateTime, ROLE_LABELS } from "./team-labels";

/** “我的团队”Tab：展示用户加入的全部团队卡片。 */
export function MyTeamsTab({ teams }: { teams: MyTeam[] }) {
  return (
    <div className="my-teams">
      <section className="my-teams__summary surface">
        <div className="my-teams__summary-copy">
          <span className="eyebrow"><Users size={15} /> 我的团队</span>
          <p>
            你参与了 <strong>{teams.length}</strong> 个团队。进入工作台后可在顶部切换团队；最近访问的团队会被记住。
          </p>
        </div>
      </section>

      <div className="my-teams__grid">
        {teams.map((team) => {
          const hasReview = team.pendingSubmissionCount > 0;
          return (
            <article className="surface my-team-card" key={team.teamId}>
              <header className="my-team-card__header">
                <Avatar
                  alt={`${team.name}头像`}
                  label={team.name.slice(0, 1)}
                  size="md"
                  src={publicFileUrl(team.avatarFileId)}
                />
                <div className="my-team-card__identity">
                  <h3>{team.name}</h3>
                  <span className="secondary">@{team.slug}</span>
                </div>
                <span className="chip my-team-card__role">{ROLE_LABELS[team.viewerRole] || team.viewerRole}</span>
              </header>

              <p className="my-team-card__summary">{team.summary || "这个团队还没有添加简介。"}</p>

              <div className="my-team-card__stats" aria-label="团队数据">
                <span><Users size={13} /> {team.memberCount} 成员</span>
                <span><BookOpen size={13} /> {team.articleCount} 文章</span>
                <span><FileText size={13} /> {team.seriesCount} 连载</span>
                <span><Inbox size={13} /> {formatCount(team.followerCount)} 关注</span>
              </div>

              <div className="my-team-card__todos" aria-label="待办事项">
                {hasReview && <span className="badge badge--warn">{team.pendingSubmissionCount} 篇投稿待审核</span>}
                {team.revisionRequiredCount > 0 && (
                  <span className="badge badge--danger">{team.revisionRequiredCount} 篇投稿需修改</span>
                )}
                {!hasReview && team.revisionRequiredCount === 0 && (
                  <span className="badge badge--info">暂无待办</span>
                )}
              </div>

              <footer className="my-team-card__footer">
                <Link className="primary-button" href={`/teams/${team.slug}/workspace`}>
                  <LayoutDashboard size={15} /> 进入工作台
                </Link>
                <Link className="ghost-button" href={`/teams/${team.slug}`}>
                  公开主页 <ArrowUpRight size={14} />
                </Link>
                <span className="my-team-card__updated" title={team.updatedAt || undefined}>
                  更新于 {formatDateTime(team.updatedAt)}
                </span>
              </footer>
            </article>
          );
        })}
      </div>
    </div>
  );
}
