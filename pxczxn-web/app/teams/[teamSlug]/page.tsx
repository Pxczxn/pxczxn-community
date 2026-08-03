"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Loader2, Users } from "lucide-react";
import { Avatar, UserTopbar } from "../../components/prototype-ui";
import {
  communityApi,
  publicFileUrl,
  type PublicArticlePage,
  type TeamPortal,
} from "../../lib/community-api";

const roleLabels: Record<string, string> = {
  OWNER: "所有者",
  ADMIN: "管理员",
  EDITOR: "编辑",
  AUTHOR: "作者",
};

export default function TeamDetailPage() {
  const params = useParams<{ teamSlug: string }>();
  const slug = params.teamSlug;
  const [team, setTeam] = useState<TeamPortal | null>(null);
  const [articles, setArticles] = useState<PublicArticlePage | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void Promise.all([
      communityApi.team(slug),
      communityApi.publicArticles(slug, 1, 10),
    ])
      .then(([portal, page]) => {
        if (active) {
          setTeam(portal);
          setArticles(page);
        }
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : "加载失败");
      });
    return () => {
      active = false;
    };
  }, [slug]);

  if (error) {
    return (
      <>
        <UserTopbar title="团队" />
        <main className="page-shell team-detail-page">
          <p className="inline-feedback error" role="alert">{error}</p>
        </main>
      </>
    );
  }

  if (!team) {
    return (
      <>
        <UserTopbar title="团队" />
        <main className="page-shell team-detail-loading">
          <Loader2 aria-label="加载团队" className="animate-spin" />
        </main>
      </>
    );
  }

  const teamAvatar = publicFileUrl(team.team.avatarFileId);

  return (
    <>
      <UserTopbar title="团队" />
      <main className="page-shell team-detail-page">
        <section className="surface team-detail-hero">
          <Avatar
            alt={`${team.team.name}头像`}
            label={team.team.name.slice(0, 1)}
            size="lg"
            src={teamAvatar}
          />
          <div className="team-detail-hero__copy">
            <span className="eyebrow"><Users size={15} /> 团队空间</span>
            <h1>{team.team.name}</h1>
            <p className="secondary">{team.team.summary || "这个团队还没有简介。"}</p>
            <p className="muted">
              由 {team.ownerDisplayName || "团队成员"} 维护 · {team.team.articleCount} 篇文章 · {team.team.followerCount} 位关注者
            </p>
          </div>
          <div className="team-member-stack" aria-label={`共 ${team.members.length} 位团队成员`}>
            {team.members.slice(0, 5).map((member) => (
              <Avatar
                alt={member.displayName || member.username}
                key={member.userId}
                label={(member.displayName || member.username).slice(0, 1)}
                size="sm"
                src={publicFileUrl(member.avatarFileId)}
              />
            ))}
            {team.members.length > 5 && <span className="team-member-stack__more">+{team.members.length - 5}</span>}
          </div>
        </section>

        <div className="team-detail-layout">
          <section className="team-detail-articles">
            <h2>最新文章</h2>
            {articles?.records.map((article) => (
              <Link
                className="surface team-detail-article"
                href={`/articles/${article.articleId}`}
                key={article.articleId}
              >
                <strong>{article.title}</strong>
                <p className="secondary">{article.summary}</p>
              </Link>
            ))}
            {articles?.records.length === 0 && <p className="secondary">暂无公开文章。</p>}
          </section>

          <aside className="surface team-detail-members">
            <h2>团队成员</h2>
            {team.members.map((member) => {
              const name = member.displayName || member.username;
              return (
                <div className="team-member-row" key={member.userId}>
                  <Avatar
                    alt={name}
                    label={name.slice(0, 1)}
                    size="sm"
                    src={publicFileUrl(member.avatarFileId)}
                  />
                  <div className="team-member-copy">
                    <strong>{name}</strong>
                    <span>@{member.username}</span>
                  </div>
                  <span className="chip team-member-role">
                    {roleLabels[member.roleCode] || member.roleCode}
                  </span>
                </div>
              );
            })}
          </aside>
        </div>
      </main>
    </>
  );
}
