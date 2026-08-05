"use client";

import Link from "next/link";
import { AlertCircle, ImagePlus, Loader2, Save, Trash2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Avatar } from "../../../../components/prototype-ui";
import { communityApi, publicFileUrl } from "../../../../lib/community-api";
import { useWorkspace } from "../workspace-context";

export default function WorkspaceSettingsPage() {
  const { teamId, teamSlug, workspace, reloadWorkspace } = useWorkspace();
  const canManage = Boolean(workspace?.permissions.includes("MANAGE_TEAM"));
  const team = workspace?.team.team;

  const [name, setName] = useState("");
  const [summary, setSummary] = useState("");
  const [avatarFileId, setAvatarFileId] = useState("");
  const [backgroundFileId, setBackgroundFileId] = useState("");
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [backgroundUploading, setBackgroundUploading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const avatarInputRef = useRef<HTMLInputElement>(null);
  const backgroundInputRef = useRef<HTMLInputElement>(null);

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

  async function uploadImage(file: File | undefined, target: "avatar" | "background") {
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setError("请选择图片文件（PNG/JPG/WebP 等）");
      return;
    }
    if (target === "avatar") setAvatarUploading(true);
    else setBackgroundUploading(true);
    setError("");
    setNotice("");
    try {
      const uploaded = await communityApi.uploadFile(file);
      if (target === "avatar") setAvatarFileId(uploaded.fileId);
      else setBackgroundFileId(uploaded.fileId);
      setNotice(target === "avatar" ? "头像已上传，点击「保存资料」生效。" : "背景图已上传，点击「保存资料」生效。");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "图片上传失败，请稍后重试");
    } finally {
      if (target === "avatar") setAvatarUploading(false);
      else setBackgroundUploading(false);
    }
  }

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
      // 让工作台头部（标题/头像）同步最新资料，无需刷新页面。
      reloadWorkspace();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "保存失败，请稍后重试");
    } finally {
      setSaving(false);
    }
  }

  if (!team) {
    return <div className="series-loading surface" aria-live="polite"><Loader2 className="animate-spin" size={22} /> 正在加载团队设置…</div>;
  }

  // 状态已在初始化时复制团队现有文件 ID；清除后为空字符串，不能再回退到团队旧值，
  // 否则“清除”按钮看起来无效。
  const avatarSrc = publicFileUrl(avatarFileId);
  const backgroundSrc = publicFileUrl(backgroundFileId);

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
        <div className="workspace-settings-form__media">
          <div className="workspace-settings-form__avatar">
            <Avatar
              alt={`${team.name}头像`}
              label={team.name.slice(0, 1)}
              size="lg"
              src={avatarSrc}
            />
            {canManage && (
              <div className="workspace-settings-form__media-actions">
                <button
                  type="button"
                  className="secondary-button"
                  disabled={avatarUploading}
                  onClick={() => avatarInputRef.current?.click()}
                >
                  {avatarUploading ? <Loader2 className="animate-spin" size={14} /> : <ImagePlus size={14} />}
                  {avatarFileId ? "更换头像" : "上传头像"}
                </button>
                {avatarFileId && (
                  <button type="button" className="ghost-button" disabled={avatarUploading} onClick={() => setAvatarFileId("")}>
                    <Trash2 size={14} /> 清除
                  </button>
                )}
              </div>
            )}
            <input
              accept="image/*"
              className="settings-avatar-file-input"
              ref={avatarInputRef}
              type="file"
              onChange={(event) => { void uploadImage(event.target.files?.[0], "avatar"); event.target.value = ""; }}
            />
          </div>

          <div className="workspace-settings-form__background">
            {backgroundSrc ? (
              <img className="workspace-settings-form__background-preview" src={backgroundSrc} alt="团队背景图预览" />
            ) : (
              <div className="workspace-settings-form__background-empty">暂无背景图</div>
            )}
            {canManage && (
              <div className="workspace-settings-form__media-actions">
                <button
                  type="button"
                  className="secondary-button"
                  disabled={backgroundUploading}
                  onClick={() => backgroundInputRef.current?.click()}
                >
                  {backgroundUploading ? <Loader2 className="animate-spin" size={14} /> : <ImagePlus size={14} />}
                  {backgroundFileId ? "更换背景图" : "上传背景图"}
                </button>
                {backgroundFileId && (
                  <button type="button" className="ghost-button" disabled={backgroundUploading} onClick={() => setBackgroundFileId("")}>
                    <Trash2 size={14} /> 清除
                  </button>
                )}
              </div>
            )}
            <input
              accept="image/*"
              className="settings-avatar-file-input"
              ref={backgroundInputRef}
              type="file"
              onChange={(event) => { void uploadImage(event.target.files?.[0], "background"); event.target.value = ""; }}
            />
          </div>
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
