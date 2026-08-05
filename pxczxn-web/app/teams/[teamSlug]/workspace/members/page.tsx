"use client";

import { AlertCircle, Crown, Loader2, LogOut, UserMinus, UserPlus } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { Avatar } from "../../../../components/prototype-ui";
import {
  communityApi,
  publicFileUrl,
  type TeamMemberProfile,
  type TeamMemberView,
} from "../../../../lib/community-api";
import { formatDateTime, ROLE_LABELS } from "../../../team-labels";
import { useWorkspace } from "../workspace-context";

const ROLES = ["AUTHOR", "EDITOR", "ADMIN", "OWNER"];

export default function WorkspaceMembersPage() {
  const { teamId, workspace } = useWorkspace();
  const [profiles, setProfiles] = useState<TeamMemberProfile[]>([]);
  const [memberRows, setMemberRows] = useState<TeamMemberView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState<string | null>(null);

  const [inviteUserId, setInviteUserId] = useState("");
  const [inviteRole, setInviteRole] = useState("AUTHOR");
  const [transferUserId, setTransferUserId] = useState("");
  const [disbandConfirm, setDisbandConfirm] = useState("");
  const [confirmOpen, setConfirmOpen] = useState<"transfer" | "disband" | "leave" | null>(null);

  const viewerRole = workspace?.viewerRole ?? "";
  const canInvite = viewerRole === "OWNER" || viewerRole === "ADMIN";
  const isOwner = viewerRole === "OWNER";

  const load = useCallback(() => {
    if (!teamId) return;
    setLoading(true);
    setError("");
    void Promise.all([
      communityApi.teamWorkspace(teamId).then((value) => setProfiles(value.team.members)),
      communityApi.teamMembers(teamId).then(setMemberRows),
    ])
      .catch((cause: unknown) => setError(cause instanceof Error ? cause.message : "成员列表加载失败"))
      .finally(() => setLoading(false));
  }, [teamId]);

  useEffect(() => {
    if (!teamId) return;
    let active = true;
    void Promise.all([
      communityApi.teamWorkspace(teamId).then((value) => { if (active) setProfiles(value.team.members); }),
      communityApi.teamMembers(teamId).then((value) => { if (active) setMemberRows(value); }),
    ])
      .catch((cause: unknown) => { if (active) setError(cause instanceof Error ? cause.message : "成员列表加载失败"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [teamId]);

  const joinedAtByUserId = new Map(memberRows.map((row) => [row.userId, row.joinedAt]));

  async function run(action: () => Promise<unknown>, key: string) {
    setBusy(key);
    setError("");
    setNotice("");
    try {
      await action();
      setNotice("操作成功。");
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "操作失败，请稍后重试");
    } finally {
      setBusy(null);
    }
  }

  async function invite() {
    if (!teamId || !inviteUserId.trim()) return;
    await run(() => communityApi.inviteTeamMember(teamId, { userId: inviteUserId.trim(), roleCode: inviteRole }), `invite-${inviteUserId}`);
    setInviteUserId("");
  }

  function changeRole(member: TeamMemberProfile, roleCode: string) {
    if (!teamId) return;
    void run(() => communityApi.changeTeamMemberRole(teamId, member.userId, roleCode), `role-${member.userId}`);
  }

  function remove(member: TeamMemberProfile) {
    if (!teamId) return;
    void run(() => communityApi.removeTeamMember(teamId, member.userId), `remove-${member.userId}`);
  }

  function transfer() {
    if (!teamId || !transferUserId) return;
    void run(() => communityApi.transferTeamOwnership(teamId, transferUserId), "transfer");
    setConfirmOpen(null);
  }

  function leave() {
    if (!teamId) return;
    void run(() => communityApi.leaveTeam(teamId), "leave");
    setConfirmOpen(null);
  }

  function disband() {
    if (!teamId) return;
    void run(() => communityApi.disbandTeam(teamId), "disband");
    setConfirmOpen(null);
  }

  if (loading || !teamId) {
    return <div className="series-loading surface" aria-live="polite"><Loader2 className="animate-spin" size={22} /> 正在加载团队成员…</div>;
  }
  if (error && profiles.length === 0) {
    return (
      <section className="surface inline-feedback error" role="alert">
        <AlertCircle size={20} />
        <div><strong>成员列表暂时无法加载</strong><p>{error}</p></div>
      </section>
    );
  }

  return (
    <div className="workspace-members-page">
      <header className="workspace-content-page__header">
        <div>
          <span className="eyebrow">团队成员</span>
          <p>查看成员分工，邀请新成员加入团队。</p>
        </div>
      </header>

      {error && <p className="inline-feedback error" role="alert">{error}</p>}
      {notice && <p className="inline-feedback success" role="status">{notice}</p>}

      {canInvite && (
        <section className="surface workspace-invite-form">
          <h2>邀请成员</h2>
          <div className="workspace-invite-form__fields">
            <input
              value={inviteUserId}
              onChange={(event) => setInviteUserId(event.target.value.trim())}
              placeholder="对方用户 ID"
              inputMode="numeric"
              aria-label="被邀请用户 ID"
            />
            <select value={inviteRole} onChange={(event) => setInviteRole(event.target.value)} aria-label="邀请角色">
              {ROLES.filter((role) => role !== "OWNER" && (viewerRole === "OWNER" || role !== "ADMIN")).map((role) => (
                <option key={role} value={role}>{ROLE_LABELS[role]}</option>
              ))}
            </select>
            <button className="primary-button" disabled={busy?.startsWith("invite") || !inviteUserId.trim()} onClick={() => void invite()}>
              {busy?.startsWith("invite") ? <Loader2 className="animate-spin" size={14} /> : <UserPlus size={14} />} 发送邀请
            </button>
          </div>
        </section>
      )}

      <section className="surface workspace-members-list">
        {profiles.length === 0 ? (
          <p className="workspace-panel__empty">团队成员列表为空。</p>
        ) : (
          profiles.map((member) => {
            const name = member.displayName || member.username;
            return (
              <div className="workspace-member-row" key={member.userId}>
                <Avatar
                  alt={name}
                  label={name.slice(0, 1)}
                  size="sm"
                  src={publicFileUrl(member.avatarFileId)}
                />
                <div className="workspace-member-row__copy">
                  <strong>{name}{member.roleCode === "OWNER" && <Crown size={13} className="workspace-member-row__crown" />}</strong>
                  <span>
                    @{member.username} · 加入于 {formatDateTime(joinedAtByUserId.get(member.userId))}
                  </span>
                </div>
                <span className="chip">{ROLE_LABELS[member.roleCode] || member.roleCode}</span>
                <div className="workspace-member-row__actions">
                  {member.roleCode !== "OWNER" && (viewerRole === "OWNER" || (viewerRole === "ADMIN" && ["EDITOR", "AUTHOR"].includes(member.roleCode))) && (
                    <>
                      <select
                        aria-label={`修改 ${name} 的角色`}
                        value={member.roleCode}
                        disabled={busy === `role-${member.userId}`}
                        onChange={(event) => changeRole(member, event.target.value)}
                      >
                        {ROLES.filter((role) => role !== "OWNER" && (viewerRole === "OWNER" || role !== "ADMIN")).map((role) => (
                          <option key={role} value={role}>{ROLE_LABELS[role]}</option>
                        ))}
                      </select>
                      <button
                        type="button"
                        className="icon-button"
                        aria-label={`移出 ${name}`}
                        title="移出团队"
                        disabled={busy === `remove-${member.userId}`}
                        onClick={() => remove(member)}
                      >
                        <UserMinus size={16} />
                      </button>
                    </>
                  )}
                  {isOwner && member.roleCode !== "OWNER" && (
                    <button
                      type="button"
                      className="ghost-button"
                      disabled={busy === "transfer"}
                      onClick={() => { setTransferUserId(member.userId); setConfirmOpen("transfer"); }}
                    >
                      转让所有权
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </section>

      <section className="surface workspace-members-page__danger">
        <h2>退出 / 解散团队</h2>
        <p className="secondary">
          {isOwner
            ? "所有者不能直接退出：需要先转让所有权，或解散团队（已发布文章不会被物理删除，作者归属保留）。"
            : "退出后你将无法访问团队工作台，已发布内容保留。可以随时通过新邀请重新加入。"}
        </p>
        <div className="workspace-members-page__danger-actions">
          {!isOwner && (
            <button type="button" className="danger-button" disabled={busy === "leave"} onClick={() => setConfirmOpen("leave")}>
              <LogOut size={14} /> 退出团队
            </button>
          )}
          {isOwner && (
            <button type="button" className="danger-button" disabled={busy === "disband"} onClick={() => setConfirmOpen("disband")}>
              解散团队
            </button>
          )}
        </div>
      </section>

      {confirmOpen === "transfer" && (
        <ConfirmDialog
          title="转让团队所有权"
          body={`转让后你将自动降级为管理员，且不能再直接退出团队。确认将所有权转让给 ${profiles.find((member) => member.userId === transferUserId)?.displayName || `用户 #${transferUserId}`}？`}
          confirmText="确认转让"
          busy={busy === "transfer"}
          onCancel={() => setConfirmOpen(null)}
          onConfirm={() => void transfer()}
        />
      )}
      {confirmOpen === "leave" && (
        <ConfirmDialog
          title="退出团队"
          body="退出后你将无法访问团队工作台。确定要退出当前团队吗？"
          confirmText="确认退出"
          busy={busy === "leave"}
          onCancel={() => setConfirmOpen(null)}
          onConfirm={() => void leave()}
        />
      )}
      {confirmOpen === "disband" && (
        <ConfirmDialog
          title="解散团队"
          body={`解散后团队公开主页将停止展示，已发布文章不会被物理删除，作者归属保留。请输入团队名称「${workspace?.team.team.name}」以确认。`}
          confirmText="确认解散"
          busy={busy === "disband"}
          requireName={workspace?.team.team.name ?? ""}
          nameValue={disbandConfirm}
          onNameChange={setDisbandConfirm}
          onCancel={() => { setConfirmOpen(null); setDisbandConfirm(""); }}
          onConfirm={() => void disband()}
        />
      )}
    </div>
  );
}

function ConfirmDialog({
  title,
  body,
  confirmText,
  busy,
  requireName,
  nameValue,
  onNameChange,
  onCancel,
  onConfirm,
}: {
  title: string;
  body: string;
  confirmText: string;
  busy: boolean;
  requireName?: string;
  nameValue?: string;
  onNameChange?: (value: string) => void;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  const nameConfirmed = !requireName || nameValue === requireName;
  return (
    <div className="confirm-dialog-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onCancel(); }}>
      <section className="confirm-dialog surface-lg" role="dialog" aria-modal="true" aria-label={title}>
        <h2>{title}</h2>
        <p>{body}</p>
        {requireName && (
          <input
            value={nameValue ?? ""}
            onChange={(event) => onNameChange?.(event.target.value)}
            placeholder={`输入团队名称：${requireName}`}
            aria-label="确认团队名称"
          />
        )}
        <div className="confirm-dialog__actions">
          <button type="button" className="ghost-button" onClick={onCancel}>取消</button>
          <button type="button" className="danger-button" disabled={busy || !nameConfirmed} onClick={onConfirm}>
            {busy ? <Loader2 className="animate-spin" size={14} /> : null} {confirmText}
          </button>
        </div>
      </section>
    </div>
  );
}
