"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { UserTopbar } from "../../components/prototype-ui";
import { communityApi, type TeamWorkspace } from "../../lib/community-api";

export default function TeamWorkspacePage() {
  const params = useSearchParams();
  const teamId = params.get("teamId");
  const [workspace, setWorkspace] = useState<TeamWorkspace | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!teamId) return;
    let active = true;
    void communityApi.teamWorkspace(teamId)
      .then((value) => { if (active) setWorkspace(value); })
      .catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "Unable to load workspace"); });
    return () => { active = false; };
  }, [teamId]);

  const missingTeam = !teamId;
  return <><UserTopbar title="团队工作台" /><main className="page-shell" style={{ paddingTop: 28 }}>
    {missingTeam ? <p role="alert" style={{ color: "var(--danger)" }}>请选择一个团队工作台。</p>
      : error ? <p role="alert" style={{ color: "var(--danger)" }}>{error}</p>
        : !workspace ? <div className="text-center" style={{ padding: 80 }}><Loader2 className="animate-spin" /></div>
          : <><section className="surface" style={{ padding: 24, marginBottom: 18 }}><h1 style={{ margin: 0 }}>{workspace.team.team.name}</h1><p className="secondary">M3 团队协作创作 · 你的团队角色：{workspace.viewerRole}</p></section><div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(180px,1fr))", gap: 12 }}>{workspace.capabilities.map((capability) => <section className="surface" key={capability} style={{ padding: 18 }}><strong>{label(capability)}</strong><p className="secondary" style={{ marginBottom: 0 }}>此能力由服务端角色权限授予。</p></section>)}</div></>}
  </main></>;
}

function label(value: string) { return ({ OVERVIEW: "概览", ARTICLES: "文章管理", MEMBERS: "成员管理", CATEGORIES: "分类", SETTINGS: "团队设置" }[value] || value); }
