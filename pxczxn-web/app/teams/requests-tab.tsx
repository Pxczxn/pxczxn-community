"use client";

import Link from "next/link";
import {
  AlertCircle,
  ArrowUpRight,
  CalendarClock,
  Check,
  FileCheck2,
  Inbox,
  Loader2,
  UserPlus,
  X,
} from "lucide-react";
import { useState } from "react";
import { Avatar } from "../components/prototype-ui";
import {
  communityApi,
  publicFileUrl,
  type MyTeam,
  type TeamApplication,
  type TeamInvitation,
  type TeamSummary,
} from "../lib/community-api";
import {
  APPLICATION_STATUS_LABELS,
  formatDateTime,
  ROLE_LABELS,
} from "./team-labels";

/** “邀请与申请”Tab：收到的邀请 + 团队建立申请。 */
export function RequestsTab({
  invitations,
  application,
  teams,
  mine,
  loading,
  error,
  onRequireLogin,
  onInvitationChanged,
  onApplicationChanged,
}: {
  invitations: TeamInvitation[];
  application: TeamApplication | null;
  teams: TeamSummary[];
  mine: MyTeam[] | null;
  loading: boolean;
  error: string;
  onRequireLogin: () => void;
  onInvitationChanged: () => void;
  onApplicationChanged: () => void;
}) {
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState("");

  const teamNameById = new Map<string, string>();
  for (const team of teams) teamNameById.set(team.teamId, team.name);
  for (const team of mine ?? []) teamNameById.set(team.teamId, team.name);
  const myTeamByTeamId = new Map((mine ?? []).map((team) => [team.teamId, team]));

  async function respond(invitationId: string, action: "accept" | "reject") {
    if (!mine) {
      onRequireLogin();
      return;
    }
    setBusy(invitationId);
    setActionError("");
    try {
      if (action === "accept") await communityApi.acceptTeamInvitation(invitationId);
      else await communityApi.rejectTeamInvitation(invitationId);
      onInvitationChanged();
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : "处理邀请失败，请稍后重试");
    } finally {
      setBusy(null);
    }
  }

  async function cancelApplication() {
    if (!application) return;
    setBusy(`application-${application.id}`);
    setActionError("");
    try {
      await communityApi.cancelTeamApplication(application.id);
      onApplicationChanged();
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : "撤回申请失败，请稍后重试");
    } finally {
      setBusy(null);
    }
  }

  const approvedTeam = application?.status === "APPROVED" && application.teamSlug
    ? myTeamByTeamId.get(
        (mine ?? []).find((team) => team.slug === application.teamSlug)?.teamId ?? "",
      )
    : undefined;

  return (
    <div className="requests-tab">
      {actionError && <p className="inline-feedback error" role="alert">{actionError}</p>}
      {loading && (
        <div className="series-loading surface" aria-live="polite">
          <Loader2 className="animate-spin" size={22} /> 正在加载邀请与申请…
        </div>
      )}
      {!loading && error && (
        <section className="surface inline-feedback error" role="alert">
          <AlertCircle size={20} />
          <div>
            <strong>邀请与申请暂时无法加载</strong>
            <p>{error}</p>
          </div>
        </section>
      )}

      {!loading && !error && (
        <div className="requests-tab__columns">
          <section className="surface requests-panel">
            <header className="requests-panel__header">
              <span className="eyebrow"><Inbox size={15} /> 收到的邀请</span>
              {invitations.length > 0 && <span className="badge badge--warn">{invitations.length} 条待处理</span>}
            </header>

            {invitations.length === 0 ? (
              <p className="requests-panel__empty">暂时没有待处理的团队邀请。</p>
            ) : (
              <ul className="requests-panel__list">
                {invitations.map((invitation) => {
                  const name = teamNameById.get(invitation.teamId);
                  const member = myTeamByTeamId.get(invitation.teamId);
                  return (
                    <li className="request-row" key={invitation.id}>
                      <Avatar
                        alt={`${name ?? "团队"}头像`}
                        label={(name ?? "团").slice(0, 1)}
                        size="sm"
                        src={member ? publicFileUrl(member.avatarFileId) : null}
                      />
                      <div className="request-row__copy">
                        <strong>{name ?? `团队 #${invitation.teamId}`}</strong>
                        <span>
                          邀请你以「{ROLE_LABELS[invitation.roleCode] || invitation.roleCode}」身份加入
                          · 有效期至 {formatDateTime(invitation.expiresAt)}
                        </span>
                      </div>
                      <div className="request-row__actions">
                        <button
                          type="button"
                          className="primary-button"
                          disabled={busy === invitation.id}
                          onClick={() => respond(invitation.id, "accept")}
                        >
                          {busy === invitation.id ? <Loader2 className="animate-spin" size={14} /> : <Check size={14} />}
                          接受
                        </button>
                        <button
                          type="button"
                          className="ghost-button"
                          disabled={busy === invitation.id}
                          onClick={() => respond(invitation.id, "reject")}
                        >
                          <X size={14} /> 拒绝
                        </button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
          </section>

          <section className="surface requests-panel">
            <header className="requests-panel__header">
              <span className="eyebrow"><FileCheck2 size={15} /> 团队建立申请</span>
              <Link href="/team-applications" className="ghost-button">
                查看详情 <ArrowUpRight size={14} />
              </Link>
            </header>

            {!application ? (
              <div className="requests-panel__empty">
                <p>你还没有提交过团队建立申请。</p>
                <Link href="/team-applications" className="secondary-button" onClick={onRequireLogin}>
                  <UserPlus size={15} /> 申请建立团队
                </Link>
              </div>
            ) : (
              <div className="application-card">
                <div className="application-card__identity">
                  <h3>{application.teamName}</h3>
                  <span className="secondary">@{application.teamSlug}</span>
                  <span className={`chip application-card__status is-${application.status.toLowerCase()}`}>
                    {APPLICATION_STATUS_LABELS[application.status] || application.status}
                  </span>
                </div>
                <p className="application-card__meta">
                  <CalendarClock size={13} /> 提交于 {formatDateTime(application.createdAt)}
                </p>
                {application.reviewComment && (
                  <p className="application-card__review">
                    审核意见：{application.reviewComment}
                  </p>
                )}
                <div className="application-card__actions">
                  {application.status === "PENDING" && (
                    <button
                      type="button"
                      className="ghost-button"
                      disabled={busy === `application-${application.id}`}
                      onClick={cancelApplication}
                    >
                      撤回申请
                    </button>
                  )}
                  {application.status === "REJECTED" && (
                    <Link href="/team-applications" className="secondary-button">
                      修改后重新提交
                    </Link>
                  )}
                  {application.status === "APPROVED" && (
                    <>
                      <Link className="primary-button" href={approvedTeam ? `/teams/${approvedTeam.slug}/workspace` : "/teams"}>
                        进入团队工作台
                      </Link>
                      <Link className="ghost-button" href={`/teams/${application.teamSlug}`}>
                        访问团队主页 <ArrowUpRight size={14} />
                      </Link>
                    </>
                  )}
                </div>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
