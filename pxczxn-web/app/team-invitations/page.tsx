"use client";

import { useEffect, useState } from "react";
import { Check, Clock3, Inbox, Loader2, X } from "lucide-react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, type TeamInvitation } from "../lib/community-api";

const roleLabels: Record<TeamInvitation["roleCode"], string> = {
  OWNER: "Owner",
  ADMIN: "Admin",
  EDITOR: "Editor",
  AUTHOR: "Author",
};

function formatExpiry(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function TeamInvitationsPage() {
  const [invitations, setInvitations] = useState<TeamInvitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState<string | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void communityApi.myTeamInvitations()
      .then((result) => {
        if (active) setInvitations(result);
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : "加载团队邀请失败");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  async function decide(invitation: TeamInvitation, decision: "accept" | "reject") {
    setProcessingId(invitation.id);
    setError("");
    try {
      if (decision === "accept") {
        await communityApi.acceptTeamInvitation(invitation.id);
      } else {
        await communityApi.rejectTeamInvitation(invitation.id);
      }
      setInvitations((current) => current.filter((item) => item.id !== invitation.id));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "处理团队邀请失败");
    } finally {
      setProcessingId(null);
    }
  }

  return (
    <>
      <UserTopbar title="团队邀请" />
      <main className="page-shell" style={{ maxWidth: 840, paddingTop: 32, paddingBottom: 48 }}>
        <section className="surface" style={{ padding: 24 }} aria-labelledby="team-invitations-title">
          <div className="flex items-center gap-3" style={{ marginBottom: 20 }}>
            <Inbox size={22} aria-hidden="true" style={{ color: "var(--primary)" }} />
            <div>
              <h1 id="team-invitations-title" style={{ margin: 0, fontSize: 22 }}>团队邀请</h1>
              <p className="secondary" style={{ margin: "4px 0 0" }}>接受后将以对应角色加入团队。</p>
            </div>
          </div>

          {error && <p role="alert" style={{ color: "var(--danger)", marginBottom: 16 }}>{error}</p>}

          {loading ? (
            <div className="flex items-center justify-center" style={{ minHeight: 180 }}>
              <Loader2 className="animate-spin" size={28} aria-label="加载中" />
            </div>
          ) : invitations.length === 0 ? (
            <div className="text-center secondary" style={{ padding: "48px 16px" }}>
              <Inbox size={32} aria-hidden="true" style={{ marginBottom: 12, opacity: 0.65 }} />
              <p style={{ margin: 0 }}>暂无待处理的团队邀请</p>
            </div>
          ) : (
            <ul style={{ display: "grid", gap: 12, padding: 0, margin: 0, listStyle: "none" }}>
              {invitations.map((invitation) => {
                const processing = processingId === invitation.id;
                return (
                  <li key={invitation.id} className="surface" style={{ padding: 18, border: "1px solid var(--border)" }}>
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <strong>团队 #{invitation.teamId}</strong>
                        <p className="secondary" style={{ margin: "6px 0 0" }}>邀请角色：{roleLabels[invitation.roleCode]}</p>
                        <p className="muted flex items-center gap-1" style={{ margin: "8px 0 0", fontSize: 13 }}>
                          <Clock3 size={14} aria-hidden="true" /> 有效期至 {formatExpiry(invitation.expiresAt)}
                        </p>
                      </div>
                      <div className="flex gap-2" aria-label="邀请操作">
                        <button className="button-primary" type="button" disabled={processing}
                          onClick={() => void decide(invitation, "accept")}>
                          <Check size={16} aria-hidden="true" /> 接受
                        </button>
                        <button className="button-secondary" type="button" disabled={processing}
                          onClick={() => void decide(invitation, "reject")}>
                          <X size={16} aria-hidden="true" /> 拒绝
                        </button>
                      </div>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </main>
    </>
  );
}
