"use client";

import Link from "next/link";
import {
  Bell,
  LockKeyhole,
  Monitor,
  Moon,
  Palette,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Sun,
  LoaderCircle,
  UserRound,
} from "lucide-react";
import { ChangeEvent, useEffect, useRef, useState } from "react";
import { Avatar } from "../components/prototype-ui";
import { setTheme, ThemeMode } from "../components/theme-bootstrap";
import {
  CurrentCommunityUser,
  PersonalBlog,
  communityApi,
  publicFileUrl,
  readSession,
} from "../lib/community-api";

const navItems = [
  ["account", UserRound, "账号设置"],
  ["security", LockKeyhole, "安全设置"],
  ["notifications", Bell, "通知设置"],
  ["privacy", ShieldCheck, "隐私设置"],
  ["theme", Palette, "主题设置"],
  ["preferences", SlidersHorizontal, "偏好设置"],
  ["blocks", Monitor, "屏蔽设置"],
] as const;

const inPageSections = [
  "account",
  "security",
  "notifications",
  "privacy",
  "theme",
  "preferences",
] as const;

export function SettingsPanel() {
  const [theme, updateTheme] = useState<ThemeMode>(() =>
    typeof window === "undefined"
      ? "light"
      : normalizeTheme(window.localStorage.getItem("pxczxn-theme")),
  );
  const [fontSize, setFontSize] = useState("标准");
  const [density, setDensity] = useState("标准");
  const [currentUser, setCurrentUser] = useState<CurrentCommunityUser | null>(null);
  const [blog, setBlog] = useState<PersonalBlog | null>(null);
  const [name, setName] = useState("程序员小明");
  const [summary, setSummary] = useState("热爱技术，持续学习，不断成长。");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [avatarPreviewOpen, setAvatarPreviewOpen] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [activeSection, setActiveSection] = useState("account");
  const avatarInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const updateActiveSection = () => {
      const topOffset = 140;
      const sections = inPageSections.map((id) => {
        const el = document.getElementById(`settings-${id}`);
        if (!el) return { id, top: Infinity, bottom: -Infinity };
        const rect = el.getBoundingClientRect();
        return { id, top: rect.top, bottom: rect.bottom };
      });

      const isAtBottom =
        window.innerHeight + window.scrollY >=
        document.documentElement.scrollHeight - 60;

      if (isAtBottom) {
        setActiveSection("preferences");
        return;
      }

      let currentActive: string = sections[0].id;
      for (const sec of sections) {
        if (sec.top <= topOffset) {
          currentActive = sec.id;
        }
      }
      setActiveSection(currentActive);
    };

    window.addEventListener("scroll", updateActiveSection, { passive: true });
    updateActiveSection();
    return () => window.removeEventListener("scroll", updateActiveSection);
  }, []);

  function selectSection(section: string) {
    if (section === "blocks") {
      window.location.assign("/blocks");
      return;
    }
    setActiveSection(section);
    const el = document.getElementById(`settings-${section}`);
    if (el) {
      const topOffset = 100;
      const elementPosition = el.getBoundingClientRect().top + window.scrollY;
      const offsetPosition = elementPosition - topOffset;
      window.scrollTo({
        top: offsetPosition,
        behavior: "smooth",
      });
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (!readSession()) return;
      setLoading(true);
      communityApi.me()
        .then((me) => {
          setCurrentUser(me);
          return communityApi.myBlog().then((myBlog) => ({ me, myBlog }));
        })
        .then(({ myBlog }) => {
          const themeMode = normalizeTheme(myBlog.settings.themeKey);
          setBlog(myBlog);
          setName(myBlog.name);
          setSummary(myBlog.summary || "");
          updateTheme(themeMode);
          setTheme(themeMode);
        })
        .catch((error) => {
          setNotice(error instanceof Error ? error.message : "账号资料加载失败");
        })
        .finally(() => setLoading(false));
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  function chooseTheme(nextTheme: ThemeMode) {
    updateTheme(nextTheme);
    setTheme(nextTheme);
    if (blog) {
      communityApi.updateBlogSettings({ themeKey: nextTheme })
        .then((settings) => {
          setBlog((current) => current ? { ...current, settings } : current);
          setNotice("主题已同步到个人博客");
        })
        .catch((error) => {
          setNotice(error instanceof Error ? error.message : "主题同步失败");
        });
    }
  }

  async function saveProfile() {
    if (!blog) {
      setNotice("登录后可以保存真实博客资料；当前为原型预览。");
      return;
    }
    setSaving(true);
    setNotice("");
    try {
      const next = await communityApi.updateMyBlog({
        name: name.trim(),
        summary: summary.trim(),
      });
      setBlog(next);
      setNotice("博客资料已保存");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "保存失败");
    } finally {
      setSaving(false);
    }
  }

  async function changeAvatar(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setNotice("请选择图片文件作为头像");
      return;
    }
    if (!blog) {
      setNotice("请先登录后再更换头像");
      return;
    }
    setAvatarUploading(true);
    setNotice("");
    try {
      const uploaded = await communityApi.uploadFile(file);
      const updatedBlog = await communityApi.updateMyBlog({ avatarFileId: uploaded.fileId });
      setBlog(updatedBlog);
      setCurrentUser((current) => current ? { ...current, avatarFileId: uploaded.fileId } : current);
      setNotice("头像已更新");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "头像上传失败");
    } finally {
      setAvatarUploading(false);
    }
  }

  async function changePassword() {
    if (newPassword !== confirmPassword) {
      setNotice("两次输入的新密码不一致");
      return;
    }
    setChangingPassword(true);
    setNotice("");
    try {
      await communityApi.changePassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setCurrentUser((user) => user ? { ...user, forcePasswordChange: false } : user);
      setNotice("密码已修改，请妥善保管新密码");
      setPasswordModalOpen(false);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "密码修改失败");
    } finally {
      setChangingPassword(false);
    }
  }

  return (
    <main className="settings-page page-shell">
      <aside className="surface settings-nav">
        <div className="settings-nav-header">
          <Settings className="settings-nav-icon" size={20} />
          <h1>设置中心</h1>
        </div>
        <nav aria-label="设置导航">
          {navItems.map(([id, Icon, label]) => (
            <button
              className={`settings-nav-item ${activeSection === id ? "active" : ""}`}
              key={id}
              onClick={() => selectSection(id)}
              type="button"
            >
              <span className="nav-indicator" />
              <Icon size={18} />
              <span>{label}</span>
            </button>
          ))}
        </nav>
      </aside>

      <div className="settings-content">
        {/* 1. 账号与个人资料 */}
        <section className="surface settings-card" id="settings-account">
          <div className="card-header">
            <div className="card-title-group">
              <UserRound className="header-icon" size={22} />
              <div>
                <h2>账号设置</h2>
                <p className="card-subtitle">管理您的个人资料、博客名称与公开描述</p>
              </div>
            </div>
          </div>

          {loading && (
            <div className="settings-loading">
              <LoaderCircle className="spin" size={17} /> 正在同步账号资料
            </div>
          )}
          {notice && <div className="settings-notice" role="status">{notice}</div>}

          <div className="settings-profile-row">
            <button aria-label="预览头像" className="settings-avatar-preview-button" onClick={() => setAvatarPreviewOpen(true)} type="button">
              <Avatar alt="当前头像" label={(currentUser?.displayName || currentUser?.username || name).slice(0, 1)} size="lg" src={publicFileUrl(blog?.avatarFileId || currentUser?.avatarFileId)} />
            </button>
            <div className="profile-info">
              <strong>{currentUser?.displayName || name}</strong>
              <span className="username-handle">@{currentUser?.username || "xiaoming"}</span>
            </div>
            <input accept="image/*" className="settings-avatar-file-input" onChange={changeAvatar} ref={avatarInputRef} type="file" />
            <button className="secondary-button change-avatar-btn" disabled={avatarUploading} onClick={() => avatarInputRef.current?.click()} type="button">{avatarUploading ? "上传中…" : "更换头像"}</button>
          </div>

          <div className="form-group">
            <label>
              <span>博客名称</span>
              <input
                className="field"
                maxLength={80}
                onChange={(event) => setName(event.target.value)}
                value={name}
              />
            </label>
            <label>
              <span>博客简介</span>
              <textarea
                className="field textarea"
                maxLength={500}
                onChange={(event) => setSummary(event.target.value)}
                value={summary}
              />
            </label>
          </div>

          <div className="card-footer">
            <button
              className="primary-button settings-save"
              disabled={saving}
              onClick={saveProfile}
              type="button"
            >
              {saving && <LoaderCircle className="spin" size={16} />}
              {saving ? "保存中…" : "保存修改"}
            </button>
          </div>
        </section>

        {/* 2. 安全设置 */}
        <section className="surface settings-card" id="settings-security">
          <div className="card-header">
            <div className="card-title-group">
              <LockKeyhole className="header-icon" size={22} />
              <div>
                <h2>安全设置</h2>
                <p className="card-subtitle">管理账号密码、登录安全与账号风控措施</p>
              </div>
            </div>
          </div>

          <dl className="security-list">
            <div className="security-item">
              <dt>账号状态</dt>
              <dd><span className="status-badge success-badge">{accountStatusLabel(currentUser?.status)}</span></dd>
            </div>
            <div className="security-item">
              <dt>绑定邮箱</dt>
              <dd>{currentUser?.email || "xiaoming.dev@example.com"}</dd>
            </div>
            <div className="security-item">
              <dt>用户名</dt>
              <dd>@{currentUser?.username || "xiaoming"}</dd>
            </div>
            <div className="security-item">
              <dt>两步验证 (2FA)</dt>
              <dd className="secondary">未开启</dd>
            </div>
          </dl>

          <p className="secondary security-hint">当前账号使用密码登录。两步验证功能暂未开放。</p>

          <div className="security-actions">
            <Link className="secondary-button settings-appeal-link" href="/account-appeals">账号措施与申诉</Link>
            <button className="secondary-button" onClick={() => setPasswordModalOpen(true)} type="button">修改密码</button>
          </div>

          {currentUser?.forcePasswordChange && (
            <p className="settings-notice alert-notice" role="alert">管理员已重置密码，请立即设置新密码后继续使用账号。</p>
          )}
        </section>

        {/* 3. 通知设置 */}
        <section className="surface settings-card" id="settings-notifications">
          <div className="card-header">
            <div className="card-title-group">
              <Bell className="header-icon" size={22} />
              <div>
                <h2>通知设置</h2>
                <p className="card-subtitle">控制提醒与消息接收偏好</p>
              </div>
            </div>
          </div>

          <div className="switch-group">
            {[
              ["系统消息通知", true],
              ["评论和回复通知", true],
              ["关注与粉丝通知", true],
              ["私信通知", false],
            ].map(([label, checked]) => (
              <label className="switch-row" key={label as string}>
                <span>{label as string}</span>
                <input defaultChecked={checked as boolean} type="checkbox" />
              </label>
            ))}
          </div>
        </section>

        {/* 4. 隐私设置 */}
        <section className="surface settings-card" id="settings-privacy">
          <div className="card-header">
            <div className="card-title-group">
              <ShieldCheck className="header-icon" size={22} />
              <div>
                <h2>隐私设置</h2>
                <p className="card-subtitle">管理您的公开信息可见性</p>
              </div>
            </div>
          </div>

          <div className="switch-group">
            <label className="switch-row"><span>公开展示个人博客</span><input defaultChecked type="checkbox" /></label>
            <label className="switch-row"><span>允许他人查看关注列表</span><input type="checkbox" /></label>
          </div>
        </section>

        {/* 5. 主题与偏好设置 */}
        <section className="surface settings-card" id="settings-theme">
          <div className="card-header">
            <div className="card-title-group">
              <Palette className="header-icon" size={22} />
              <div>
                <h2>主题设置</h2>
                <p className="card-subtitle">选择界面色彩主题与视觉风格</p>
              </div>
            </div>
          </div>

          <div className="theme-options">
            <ThemeCard
              active={theme === "light"}
              description="明亮清爽，适合常规光线"
              label="浅色模式"
              mode="light"
              onSelect={chooseTheme}
            />
            <ThemeCard
              active={theme === "dark"}
              description="沉浸防眩，低光环境推荐"
              label="深色模式"
              mode="dark"
              onSelect={chooseTheme}
            />
            <ThemeCard
              active={theme === "starry"}
              description="深空蓝调，克制深邃的星空氛围"
              label="星空模式"
              mode="starry"
              onSelect={chooseTheme}
            />
          </div>

          <div className="theme-preferences-box" id="settings-preferences">
            <div className="theme-control">
              <span className="control-label">字体大小</span>
              <Segmented
                onChange={setFontSize}
                options={["A-", "标准", "A+"]}
                value={fontSize}
              />
            </div>
            <div className="theme-control">
              <span className="control-label">界面密度</span>
              <Segmented
                onChange={setDensity}
                options={["舒适", "标准", "紧凑"]}
                value={density}
              />
            </div>
          </div>

          <div className="theme-note">
            <Settings size={16} />
            <span>主题设置会自动保存在当前设备，并实时同步</span>
          </div>
        </section>
      </div>

      {passwordModalOpen && (
        <div className="settings-password-overlay" role="dialog" aria-modal="true" aria-labelledby="password-dialog-title">
          <button aria-label="关闭修改密码弹窗" className="settings-password-backdrop" onClick={() => setPasswordModalOpen(false)} type="button" />
          <form className="surface settings-password-dialog" onSubmit={(event) => { event.preventDefault(); void changePassword(); }}>
            <header><h2 id="password-dialog-title">修改密码</h2><button aria-label="关闭" className="icon-button" onClick={() => setPasswordModalOpen(false)} type="button">×</button></header>
            <label><span>当前密码</span><input autoComplete="current-password" className="field" onChange={(event) => setCurrentPassword(event.target.value)} required type="password" value={currentPassword} /></label>
            <label><span>新密码</span><input autoComplete="new-password" className="field" onChange={(event) => setNewPassword(event.target.value)} required type="password" value={newPassword} /></label>
            <label><span>确认新密码</span><input autoComplete="new-password" className="field" onChange={(event) => setConfirmPassword(event.target.value)} required type="password" value={confirmPassword} /></label>
            <p>新密码须为 12-72 位，包含大写、小写、数字和特殊字符，且不能含空格。</p>
            <footer><button className="secondary-button" onClick={() => setPasswordModalOpen(false)} type="button">取消</button><button className="primary-button" disabled={changingPassword} type="submit">{changingPassword ? "修改中…" : "确认修改"}</button></footer>
          </form>
        </div>
      )}
      {avatarPreviewOpen && (
        <div className="settings-password-overlay" role="dialog" aria-modal="true" aria-labelledby="avatar-preview-title">
          <button aria-label="关闭头像预览" className="settings-password-backdrop" onClick={() => setAvatarPreviewOpen(false)} type="button" />
          <section className="surface settings-avatar-preview-dialog">
            <header><h2 id="avatar-preview-title">头像预览</h2><button aria-label="关闭" className="icon-button" onClick={() => setAvatarPreviewOpen(false)} type="button">×</button></header>
            {blog?.avatarFileId || currentUser?.avatarFileId ? (
    // eslint-disable-next-line @next/next/no-img-element -- 头像预览,尺寸由 CSS 控制
    <img alt="当前头像预览" src={publicFileUrl(blog?.avatarFileId || currentUser?.avatarFileId) || undefined} />
  ) : <Avatar label={(currentUser?.displayName || currentUser?.username || name).slice(0, 1)} size="lg" />}
          </section>
        </div>
      )}
    </main>
  );
}

function normalizeTheme(value?: string | null): ThemeMode {
  return value === "dark" || value === "starry" ? value : "light";
}

function accountStatusLabel(value?: string | null): string {
  const labels: Record<string, string> = {
    NORMAL: "正常",
    FROZEN: "已冻结",
    DEACTIVATED: "已停用",
    DISABLED: "已禁用",
    BANNED: "已封禁",
    DELETED: "已删除",
  };
  return value ? labels[value] || value : "正常登录";
}

function ThemeCard({
  mode,
  label,
  description,
  active,
  onSelect,
}: {
  mode: ThemeMode;
  label: string;
  description: string;
  active: boolean;
  onSelect: (theme: ThemeMode) => void;
}) {
  return (
    <button
      aria-pressed={active}
      className={`theme-card theme-card--${mode} ${active ? "active" : ""}`}
      onClick={() => onSelect(mode)}
      type="button"
    >
      <div className="theme-card-header">
        <div className="theme-title-box">
          <span className="theme-icon-wrap">
            {mode === "light" && <Sun size={18} />}
            {mode === "dark" && <Moon size={18} />}
            {mode === "starry" && <Sparkles size={18} />}
          </span>
          <div>
            <strong>{label}</strong>
            <span className="theme-desc">{description}</span>
          </div>
        </div>
        <span className="theme-badge">{active ? "已选择" : "切换"}</span>
      </div>

      <div className="theme-preview-window">
        <div className="preview-window-bar">
          <span className="window-dot dot-red" />
          <span className="window-dot dot-yellow" />
          <span className="window-dot dot-green" />
        </div>
        <div className="preview-window-body">
          <div className="preview-sidebar">
            <span className="sidebar-line" />
            <span className="sidebar-line" />
          </div>
          <div className="preview-content">
            <span className="content-bar bar-title" />
            <span className="content-bar bar-text" />
            <span className="content-bar bar-short" />
          </div>
        </div>
      </div>
    </button>
  );
}

function Segmented({
  options,
  value,
  onChange,
}: {
  options: string[];
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="segmented">
      {options.map((option) => (
        <button
          className={option === value ? "active" : ""}
          key={option}
          onClick={() => onChange(option)}
          type="button"
        >
          {option}
        </button>
      ))}
    </div>
  );
}
