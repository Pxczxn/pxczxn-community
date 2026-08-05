"use client";

import Link from "next/link";
import { AlertCircle, Loader2, Save } from "lucide-react";
import { useEffect, useState } from "react";
import { Avatar } from "../../../../components/prototype-ui";
import { communityApi, publicFileUrl } from "../../../../lib/community-api";
import { useWorkspace } from "../workspace-context";

export default function WorkspaceSettingsPage() {
  const { teamId, teamSlug, workspace } = useWorkspace();
  const canManage = Boolean(workspace?.permissions.includes("MANAGE_TEAM"));
  const team = workspace?.team.team;

  const [name, setName] = useState("");
  const [summary, setSummary] = useState("");
  const [avatarFileId, setAvatarFileId] = useState("");
  const [backgroundFileId, setBackgroundFileId] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (!team) return;
    const timer = window.setTimeout(() => {
      setName(team.name);
      setSummary(team.summary ?? "");
      setAvatarFileId(team.avatarFileId ?? "");
      setBackgroundFileId(team.backgroundFileId ?? "");
    }, 0);
    return () => window.clearTimeout(timer);
  }, [team]);

  async function save() {
    if (!teamId || !name.trim()) return;
    setSaving(true);
    setError("");
    setNotice("");
    try {
      await communityApi.updateTeamSettings(teamId, {
        name: name.trim(),
        summary: summary.trim() || null,
        avatarFileId: avatarFileId.trim() || null,
        backgroundFileId: backgroundFileId.trim() || null,
      });
      setNotice("团队资料已保存。");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "保存失败，请稍后重试");
    } finally {
      setSaving(false);
    }
  }

  if (!team) {
    return <div className="series-loading surface" aria-live="polite"><Loader2 className="animate-spin" size={22} /> 正在加载团队设置…</div>;
  }

  return (
    <div className="workspace-settings-page">
      <header className="workspace-content-page__header">
        <div>
          <span className="eyebrow">团队设置</span>
          <p>管理团队基础资料。团队标识（@slug）在 V1 暂不支持修改，避免旧链接失效。</p>
        </div>
      </header>

      {!canManage && (
        <section className="surface inline-feedback" role="status">
          <AlertCircle size={20} />
          <div>
            <strong>当前角色没有设置权限</strong>
            <p>仅所有者和管理员可以修改团队资料。你可以查看以下只读信息。</p>
          </div>
        </section>
      )}

      {error && <p className="inline-feedback error" role="alert">{error}</p>}
      {notice && <p className="inline-feedback success" role="status">{notice}</p>}

      <section className="surface workspace-settings-form">
        <div className="workspace-settings-form__avatar">
          <Avatar
            alt={`${team.name}头像`}
            label={team.name.slice(0, 1)}
            size="lg"
            src={publicFileUrl(team.avatarFileId)}
          />
        </div>
        <label>
          团队名称
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={120}
            disabled={!canManage}
            aria-label="团队名称"
          />
        </label>
        <label>
          团队标识
          <input value={`@${team.slug}`} disabled aria-label="团队标识" />
          <span className="muted">创建后不可修改；旧链接始终有效。</span>
        </label>
        <label>
          简介
          <textarea
            value={summary}
            onChange={(event) => setSummary(event.target.value)}
            maxLength={500}
            rows={3}
            disabled={!canManage}
            aria-label="团队简介"
          />
        </label>
        <label>
          头像文件 ID（可选）
          <input
            value={avatarFileId}
            onChange={(event) => setAvatarFileId(event.target.value.trim())}
            placeholder="上传头像后填入文件 ID"
            disabled={!canManage}
            aria-label="头像文件 ID"
          />
        </label>
        <label>
          背景图文件 ID（可选）
          <input
            value={backgroundFileId}
            onChange={(event) => setBackgroundFileId(event.target.value.trim())}
            placeholder="上传背景图后填入文件 ID"
            disabled={!canManage}
            aria-label="背景图文件 ID"
          />
        </label>
        {canManage && (
          <button className="primary-button" disabled={saving || !name.trim()} onClick={() => void save()}>
            {saving ? <Loader2 className="animate-spin" size={14} /> : <Save size={14} />} 保存资料
          </button>
        )}
      </section>

      <section className="surface workspace-settings-danger">
        <h2>危险操作</h2>
        <p className="secondary">
          退出团队、转让所有权与解散团队均需要二次确认，操作后不可撤销。
        </p>
        <Link className="danger-button" href={`/teams/${teamSlug}/workspace/members`}>
          前往成员页处理退出 / 转让 / 解散
        </Link>
      </section>
    </div>
  );
}
