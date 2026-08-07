"use client";

import { AlertCircle, Crown, Loader2, Search, UserMinus, UserPlus, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Avatar } from "../../../../components/prototype-ui";
import {
  communityApi,
  publicFileUrl,
  type TeamInvitation,
  type TeamMemberView,
  type UnifiedSearchResult,
} from "../../../../lib/community-api";
import {
  formatDateTime,
  hasTeamPermission,
  INVITATION_STATUS_LABELS,
  ROLE_LABELS,
  TEAM_PERMISSIONS,
} from "../../../team-labels";
import { useWorkspace } from "../workspace-context";

/** OWNER 不可直接授予，只能通过转让所有权产生。 */
const ASSIGNABLE_ROLES = ["AUTHOR", "EDITOR", "ADMIN"];
const SEARCH_DEBOUNCE_MS = 300;
const SEARCH_PAGE_SIZE = 8;

interface PickedUser {
  userId: string;
  name: string;
}

export default function WorkspaceMembersPage() {
  const { teamId, workspace } = useWorkspace();
  const [members, setMembers] = useState<TeamMemberView[]>([]);
  const [invitations, setInvitations] = useState<TeamInvitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState<string | null>(null);

  const [inviteQuery, setInviteQuery] = useState("");
  const [inviteResults, setInviteResults] = useState<UnifiedSearchResult[]>([]);
  const [invitePicked, setInvitePicked] = useState<PickedUser | null>(null);
  const [searching, setSearching] = useState(false);
  const [searchHint, setSearchHint] = useState("");
  const [inviteRole, setInviteRole] = useState("AUTHOR");

  const [transferTarget, setTransferTarget] = useState<TeamMemberView | null>(null);
  const [confirmOpen, setConfirmOpen] = useState<"transfer" | null>(null);

  const viewerRole = workspace?.viewerRole ?? "";
  const permissions = workspace?.permissions;
  const canManageMembers = hasTeamPermission(permissions, TEAM_PERMISSIONS.MANAGE_MEMBERS);
  const canTransfer = hasTeamPermission(permissions, TEAM_PERMISSIONS.TRANSFER_OWNERSHIP);
  const isOwner = viewerRole === "OWNER";
  const assignableRoles = ASSIGNABLE_ROLES.filter((role) => isOwner || role !== "ADMIN");

  const load = useCallback(async () => {
    if (!teamId) return;
    const [rows, invites] = await Promise.all([
      communityApi.teamMembers(teamId),
      canManageMembers ? communityApi.teamInvitations(teamId) : Promise.resolve<TeamInvitation[]>([]),
    ]);
    setMembers(rows);
    setInvitations(invites);
  }, [teamId, canManageMembers]);

  useEffect(() => {
    if (!teamId) return;
    let active = true;
    // 推迟一帧再进入 loading，避免在 effect 体内同步 setState 造成级联渲染。
    const timer = window.setTimeout(() => {
      setLoading(true);
      void load()
        .catch((cause: unknown) => {
          if (active) setError(cause instanceof Error ? cause.message : "成员列表加载失败");
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }, 0);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [teamId, load]);

  // 搜索用户：输入停顿后再发请求，选中之后不再继续搜索。
  // 清空结果由 pickUser / clearPickedUser 负责，effect 内不做同步 setState。
  useEffect(() => {
    const keyword = inviteQuery.trim();
    if (invitePicked || keyword.length === 0) return;
    let active = true;
    const timer = window.setTimeout(() => {
      if (!active) return;
      setSearching(true);
      void communityApi
        .search(keyword, "USER", 1, SEARCH_PAGE_SIZE)
        .then((page) => {
          if (!active) return;
          setInviteResults(page.records);
          setSearchHint(page.records.length === 0 ? "没有匹配的用户，换个昵称或用户名试试。" : "");
        })
        .catch((cause: unknown) => {
          if (!active) return;
          setInviteResults([]);
          setSearchHint(cause instanceof Error ? cause.message : "用户搜索失败");
        })
        .finally(() => {
          if (active) setSearching(false);
        });
    }, SEARCH_DEBOUNCE_MS);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [inviteQuery, invitePicked]);

  const memberIds = useMemo(() => new Set(members.map((member) => member.userId)), [members]);
  const pendingInviteeIds = useMemo(
    () => new Set(invitations.filter((item) => item.status === "PENDING").map((item) => item.inviteeUserId)),
    [invitations],
  );
  const pendingInvitations = useMemo(() => invitations.filter((item) => item.status === "PENDING"), [invitations]);
  const handledInvitations = useMemo(() => invitations.filter((item) => item.status !== "PENDING").slice(0, 10), [invitations]);

  async function run(action: () => Promise<unknown>, key: string): Promise<boolean> {
    setBusy(key);
    setError("");
    setNotice("");
    let ok = false;
    try {
      await action();
      ok = true;
      setNotice("操作成功。");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "操作失败，请稍后重试");
    }
    // 刷新失败不应把已成功的操作报成失败，因此单独兜底。
    try {
      await load();
    } catch {
      setError((current) => current || "列表刷新失败，请手动重试。");
    }
    setBusy(null);
    return ok;
  }

  function pickUser(result: UnifiedSearchResult) {
    setInvitePicked({ userId: result.targetId, name: result.title });
    setInviteQuery(result.title);
    setInviteResults([]);
    setSearchHint("");
    setSearching(false);
  }

  function clearPickedUser() {
    setInvitePicked(null);
    setInviteQuery("");
    setInviteResults([]);
    setSearchHint("");
    setSearching(false);
  }

  async function invite() {
    if (!teamId || !invitePicked) return;
    const ok = await run(
      () => communityApi.inviteTeamMember(teamId, { userId: invitePicked.userId, roleCode: inviteRole }),
      `invite-${invitePicked.userId}`,
    );
    if (ok) clearPickedUser();
  }

  function revokeInvitation(invitation: TeamInvitation) {
    if (!teamId) return;
    void run(() => communityApi.revokeTeamInvitation(teamId, invitation.id), `revoke-${invitation.id}`);
  }

  function changeRole(member: TeamMemberView, roleCode: string) {
    if (!teamId) return;
    void run(() => communityApi.changeTeamMemberRole(teamId, member.userId, roleCode), `role-${member.userId}`);
  }

  function remove(member: TeamMemberView) {
    if (!teamId) return;
    void run(() => communityApi.removeTeamMember(teamId, member.userId), `remove-${member.userId}`);
  }

  function transfer() {
    if (!teamId || !transferTarget) return;
    void run(() => communityApi.transferTeamOwnership(teamId, transferTarget.userId), "transfer");
    setConfirmOpen(null);
  }

  if (loading || !teamId) {
    return (
      <div className="series-loading surface" aria-live="polite">
        <Loader2 className="animate-spin" size={22} /> 正在加载团队成员…
      </div>
    );
  }
  if (error && members.length === 0) {
    return (
      <section className="surface inline-feedback error" role="alert">
        <AlertCircle size={20} />
        <div>
          <strong>成员列表暂时无法加载</strong>
          <p>{error}</p>
        </div>
      </section>
    );
  }

  return (
    <div className="workspace-members-page">
      <header className="workspace-content-page__header">
        <div>
          <span className="eyebrow">团队成员</span>
          <p>查看成员分工与产出，搜索用户发出邀请，并跟进邀请状态。</p>
        </div>
      </header>

      {error && <p className="inline-feedback error" role="alert">{error}</p>}
      {notice && <p className="inline-feedback success" role="status">{notice}</p>}

      {canManageMembers && (
        <section className="surface workspace-invite-form">
          <h2>邀请成员</h2>
          <div className="workspace-invite-form__fields">
            <div className="workspace-invite-picker">
              <div className="workspace-invite-picker__field">
                {invitePicked ? <UserPlus size={15} aria-hidden /> : <Search size={15} aria-hidden />}
                <input
                  value={inviteQuery}
                  onChange={(event) => {
                    setInviteQuery(event.target.value);
                    if (invitePicked) setInvitePicked(null);
                  }}
                  placeholder="搜索昵称或用户名"
                  aria-label="搜索要邀请的用户"
                  autoComplete="off"
                />
                {searching && !invitePicked && inviteQuery.trim().length > 0 && (
                  <Loader2 className="animate-spin" size={14} aria-hidden />
                )}
                {inviteQuery && (
                  <button type="button" className="icon-button" aria-label="清空搜索" onClick={clearPickedUser}>
                    <X size={14} />
                  </button>
                )}
              </div>
              {!invitePicked && inviteResults.length > 0 && (
                <ul className="workspace-invite-picker__results" role="listbox" aria-label="用户搜索结果">
                  {inviteResults.map((result) => {
                    const joined = memberIds.has(result.targetId);
                    const invited = pendingInviteeIds.has(result.targetId);
                    const disabled = joined || invited;
                    return (
                      <li key={result.targetId}>
                        <button
                          type="button"
                          role="option"
                          aria-selected={false}
                          className="workspace-invite-picker__option"
                          disabled={disabled}
                          onClick={() => pickUser(result)}
                        >
                          <Avatar alt={result.title} label={result.title.slice(0, 1)} size="sm" />
                          <span className="workspace-invite-picker__option-copy">
                            <strong>{result.title}</strong>
                            {result.blogName && <span>{result.blogName}</span>}
                          </span>
                          {joined && <span className="chip">已在团队</span>}
                          {!joined && invited && <span className="chip">已邀请</span>}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
              {searchHint && <p className="workspace-invite-picker__hint">{searchHint}</p>}
              {invitePicked && (
                <p className="workspace-invite-picker__hint">
                  已选择：{invitePicked.name}
                </p>
              )}
            </div>
            <select value={inviteRole} onChange={(event) => setInviteRole(event.target.value)} aria-label="邀请角色">
              {assignableRoles.map((role) => (
                <option key={role} value={role}>{ROLE_LABELS[role]}</option>
              ))}
            </select>
            <button
              className="primary-button"
              disabled={busy?.startsWith("invite") || !invitePicked}
              onClick={() => void invite()}
            >
              {busy?.startsWith("invite") ? <Loader2 className="animate-spin" size={14} /> : <UserPlus size={14} />} 发送邀请
            </button>
          </div>
        </section>
      )}

      {canManageMembers && (pendingInvitations.length > 0 || handledInvitations.length > 0) && (
        <section className="surface workspace-members-list">
          <h2 className="workspace-invitation-title">
            邀请记录
            {pendingInvitations.length > 0 && <span className="chip">{pendingInvitations.length} 条待处理</span>}
          </h2>
          {pendingInvitations.map((invitation) => {
            const name = invitation.inviteeDisplayName || invitation.inviteeUsername || `用户 #${invitation.inviteeUserId}`;
            return (
              <div className="workspace-member-row" key={invitation.id}>
                <Avatar alt={name} label={name.slice(0, 1)} size="sm" src={publicFileUrl(invitation.inviteeAvatarFileId)} />
                <div className="workspace-member-row__copy">
                  <strong>{name}</strong>
                  <span>
                    {invitation.inviterDisplayName || invitation.inviterUsername || "成员"} 邀请于{" "}
                    {formatDateTime(invitation.createdAt)} · {formatDateTime(invitation.expiresAt)} 过期
                  </span>
                </div>
                <span className="chip">{ROLE_LABELS[invitation.roleCode] || invitation.roleCode}</span>
                <span className="chip">{INVITATION_STATUS_LABELS[invitation.status] || invitation.status}</span>
                <div className="workspace-member-row__actions">
                  <button
                    type="button"
                    className="ghost-button"
                    disabled={busy === `revoke-${invitation.id}`}
                    onClick={() => revokeInvitation(invitation)}
                  >
                    {busy === `revoke-${invitation.id}` ? <Loader2 className="animate-spin" size={14} /> : null} 撤销邀请
                  </button>
                </div>
              </div>
            );
          })}
          {handledInvitations.map((invitation) => {
            const name = invitation.inviteeDisplayName || invitation.inviteeUsername || `用户 #${invitation.inviteeUserId}`;
            return (
              <div className="workspace-member-row workspace-member-row--muted" key={invitation.id}>
                <Avatar alt={name} label={name.slice(0, 1)} size="sm" src={publicFileUrl(invitation.inviteeAvatarFileId)} />
                <div className="workspace-member-row__copy">
                  <strong>{name}</strong>
                  <span>邀请于 {formatDateTime(invitation.createdAt)}</span>
                </div>
                <span className="chip">{ROLE_LABELS[invitation.roleCode] || invitation.roleCode}</span>
                <span className="chip">{INVITATION_STATUS_LABELS[invitation.status] || invitation.status}</span>
              </div>
            );
          })}
        </section>
      )}

      <section className="surface workspace-members-list">
        {members.length === 0 ? (
          <p className="workspace-panel__empty">团队成员列表为空。</p>
        ) : (
          members.map((member) => {
            const name = member.displayName || member.username || `用户 #${member.userId}`;
            const canEditThisMember =
              canManageMembers &&
              member.roleCode !== "OWNER" &&
              (isOwner || ["EDITOR", "AUTHOR"].includes(member.roleCode));
            return (
              <div className="workspace-member-row" key={member.userId}>
                <Avatar alt={name} label={name.slice(0, 1)} size="sm" src={publicFileUrl(member.avatarFileId)} />
                <div className="workspace-member-row__copy">
                  <strong>
                    {name}
                    {member.roleCode === "OWNER" && <Crown size={13} className="workspace-member-row__crown" />}
                  </strong>
                  <span>
                    {member.username ? `@${member.username} · ` : ""}加入于 {formatDateTime(member.joinedAt)} · 贡献{" "}
                    {member.contributionCount} 篇
                    {member.lastActiveAt ? ` · 最近产出 ${formatDateTime(member.lastActiveAt)}` : ""}
                  </span>
                </div>
                <span className="chip">{ROLE_LABELS[member.roleCode] || member.roleCode}</span>
                <div className="workspace-member-row__actions">
                  {canEditThisMember && (
                    <>
                      <select
                        aria-label={`修改 ${name} 的角色`}
                        value={member.roleCode}
                        disabled={busy === `role-${member.userId}`}
                        onChange={(event) => changeRole(member, event.target.value)}
                      >
                        {assignableRoles.map((role) => (
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
                  {canTransfer && member.roleCode !== "OWNER" && (
                    <button
                      type="button"
                      className="ghost-button"
                      disabled={busy === "transfer"}
                      onClick={() => {
                        setTransferTarget(member);
                        setConfirmOpen("transfer");
                      }}
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

      {confirmOpen === "transfer" && (
        <ConfirmDialog
          title="转让团队所有权"
          body={`转让后你将自动降级为管理员，且不能再直接退出团队。确认将所有权转让给 ${
            transferTarget?.displayName || transferTarget?.username || `用户 #${transferTarget?.userId ?? ""}`
          }？`}
          confirmText="确认转让"
          busy={busy === "transfer"}
          onCancel={() => setConfirmOpen(null)}
          onConfirm={() => void transfer()}
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
  onCancel,
  onConfirm,
}: {
  title: string;
  body: string;
  confirmText: string;
  busy: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <div className="confirm-dialog-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onCancel(); }}>
      <section className="confirm-dialog surface-lg" role="dialog" aria-modal="true" aria-label={title}>
        <h2>{title}</h2>
        <p>{body}</p>
        <div className="confirm-dialog__actions">
          <button type="button" className="ghost-button" onClick={onCancel}>取消</button>
          <button type="button" className="danger-button" disabled={busy} onClick={onConfirm}>
            {busy ? <Loader2 className="animate-spin" size={14} /> : null} {confirmText}
          </button>
        </div>
      </section>
    </div>
  );
}
