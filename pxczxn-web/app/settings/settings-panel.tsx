"use client";

import {
  Bell,
  LockKeyhole,
  Monitor,
  Palette,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  LoaderCircle,
  UserRound,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Avatar } from "../components/prototype-ui";
import { setTheme, ThemeMode } from "../components/theme-bootstrap";
import {
  CurrentCommunityUser,
  PersonalBlog,
  communityApi,
  readSession,
} from "../lib/community-api";

const navItems = [
  [UserRound, "账号设置"],
  [LockKeyhole, "安全设置"],
  [Bell, "通知设置"],
  [ShieldCheck, "隐私设置"],
  [Palette, "主题设置"],
  [SlidersHorizontal, "偏好设置"],
  [Monitor, "屏蔽设置"],
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
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "密码修改失败");
    } finally {
      setChangingPassword(false);
    }
  }

  return (
    <main className="settings-page page-shell">
      <aside className="surface settings-nav">
        <h1>设置中心</h1>
        <nav aria-label="设置导航">
          {navItems.map(([Icon, label], index) => (
            <button className={index === 0 || index === 4 ? "active" : ""} key={label} type="button">
              <Icon size={17} /> {label}
            </button>
          ))}
        </nav>
      </aside>

      <section className="surface settings-account">
        <h2>账号设置</h2>
        {loading && (
          <div className="settings-loading">
            <LoaderCircle className="spin" size={17} /> 正在同步账号资料
          </div>
        )}
        {notice && <div className="settings-notice" role="status">{notice}</div>}
        <div className="settings-profile-row">
          <Avatar label={(currentUser?.displayName || currentUser?.username || name).slice(0, 1)} size="lg" />
          <button className="link text-button" type="button">更换头像</button>
        </div>
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
        <button
          className="primary-button settings-save"
          disabled={saving}
          onClick={saveProfile}
          type="button"
        >
          {saving && <LoaderCircle className="spin" size={16} />}
          {saving ? "保存中…" : "保存"}
        </button>

        <section className="settings-subsection">
          <h3>安全设置</h3>
          <a className="link" href="/account-appeals">查看账号措施与提交申诉</a>
          <dl className="security-list">
            <div><dt>账号状态</dt><dd className="success-text">{currentUser?.status || "演示账号"}</dd></div>
            <div><dt>邮箱</dt><dd>{currentUser?.email || "xiaoming.dev@example.com"}</dd></div>
            <div><dt>用户名</dt><dd>@{currentUser?.username || "xiaoming"}</dd></div>
            <div><dt>未开启</dt><dd><button className="secondary-button" type="button">开启</button></dd></div>
          </dl>
          {currentUser?.forcePasswordChange && (
            <p className="settings-notice" role="alert">管理员已重置密码，请立即设置新密码后继续使用账号。</p>
          )}
          <div className="settings-password-form">
            <label><span>当前密码</span><input className="field" onChange={(event) => setCurrentPassword(event.target.value)} type="password" value={currentPassword} /></label>
            <label><span>新密码</span><input className="field" onChange={(event) => setNewPassword(event.target.value)} type="password" value={newPassword} /></label>
            <label><span>确认新密码</span><input className="field" onChange={(event) => setConfirmPassword(event.target.value)} type="password" value={confirmPassword} /></label>
            <p>新密码须为 12-72 位，包含大写、小写、数字和特殊字符，且不能含空格。</p>
            <button className="secondary-button" disabled={changingPassword} onClick={changePassword} type="button">{changingPassword ? "修改中…" : "修改密码"}</button>
          </div>
        </section>

        <section className="settings-subsection">
          <h3>通知设置</h3>
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
        </section>
      </section>

      <section className="surface settings-theme">
        <div className="theme-heading">
          <Palette size={19} />
          <h2>主题设置</h2>
        </div>
        <p>选择主题风格</p>
        <div className="theme-options">
          <ThemeCard
            active={theme === "light"}
            label="浅色"
            mode="light"
            onSelect={chooseTheme}
          />
          <ThemeCard
            active={theme === "dark"}
            label="深色"
            mode="dark"
            onSelect={chooseTheme}
          />
          <ThemeCard
            active={theme === "starry"}
            label="星空"
            mode="starry"
            onSelect={chooseTheme}
          />
        </div>

        <div className="theme-control">
          <span>字体大小</span>
          <Segmented
            onChange={setFontSize}
            options={["A-", "标准", "A+"]}
            value={fontSize}
          />
        </div>
        <div className="theme-control">
          <span>界面密度</span>
          <Segmented
            onChange={setDensity}
            options={["舒适", "标准", "紧凑"]}
            value={density}
          />
        </div>
        <div className="theme-note">
          <Settings size={17} />
          <span>主题会自动保存在当前设备</span>
        </div>
      </section>
    </main>
  );
}

function normalizeTheme(value?: string | null): ThemeMode {
  return value === "dark" || value === "starry" ? value : "light";
}

function ThemeCard({
  mode,
  label,
  active,
  onSelect,
}: {
  mode: ThemeMode;
  label: string;
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
      <span className="theme-check">{active ? "✓" : ""}</span>
      <strong>{label}</strong>
      <span className="theme-preview">
        <i />
        <i />
        <i />
      </span>
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
