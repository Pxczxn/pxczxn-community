"use client";

import Link from "next/link";
import {
  AlertCircle,
  CheckCircle2,
  Eye,
  EyeOff,
  Github,
  LoaderCircle,
  MessageCircle,
  UserRound,
} from "lucide-react";
import { FormEvent, useState } from "react";
import {
  CommunityApiError,
  communityApi,
  saveSession,
} from "../lib/community-api";

const EMAIL_PATTERN = /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{12,72}$/;

export function AuthPanel() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [showPassword, setShowPassword] = useState(false);
  const [username, setUsername] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{
    tone: "error" | "success";
    text: string;
  } | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    setMessage(null);

    if (mode === "register") {
      if (!/^[A-Za-z0-9][A-Za-z0-9_-]{2,31}$/.test(username)) {
        setMessage({
          tone: "error",
          text: "用户名须为 3–32 位字母、数字、下划线或连字符",
        });
        return;
      }
      if (!agreed) {
        setMessage({ tone: "error", text: "请先阅读并同意服务协议与隐私政策" });
        return;
      }
      if (!EMAIL_PATTERN.test(email)) {
        setMessage({ tone: "error", text: "请输入有效邮箱，例如 name@example.com" });
        return;
      }
      if (!PASSWORD_PATTERN.test(password)) {
        setMessage({ tone: "error", text: "密码须为 12-72 位，并包含大写、小写、数字和特殊字符，且不能含空格" });
        return;
      }
    }
    if (password.length < (mode === "register" ? 8 : 1)) {
      setMessage({
        tone: "error",
        text: mode === "register" ? "密码至少需要 8 个字符" : "请输入密码",
      });
      return;
    }

    setSubmitting(true);
    try {
      let registeredBlogSlug: string | null = null;
      if (mode === "register") {
        const [usernameState, emailState] = await Promise.all([
          communityApi.checkUsername(username),
          communityApi.checkEmail(email),
        ]);
        if (!usernameState.available) {
          throw new CommunityApiError("该用户名已被使用", 409, 409);
        }
        if (!emailState.available) {
          throw new CommunityApiError("该邮箱已注册", 409, 409);
        }
        const registered = await communityApi.register({
          username,
          displayName: displayName.trim() || username,
          email,
          password,
        });
        registeredBlogSlug = registered.blogSlug;
      }

      const session = await communityApi.login({ email, password });
      saveSession({ ...session, blogSlug: registeredBlogSlug });
      const me = await communityApi.me();
      saveSession({
        ...session,
        displayName: me.displayName,
        blogSlug: me.blogSlug,
      });
      setMessage({
        tone: "success",
        text: mode === "register" ? "账号创建成功，正在进入社区…" : "登录成功，正在进入社区…",
      });
      window.setTimeout(() => {
        const redirectTarget = new URLSearchParams(window.location.search).get("returnTo");
        const safeReturnTo = session.forcePasswordChange
          ? "/settings"
          : redirectTarget
          && redirectTarget.startsWith("/")
          && !redirectTarget.startsWith("//")
          ? redirectTarget
          : "/discover";
        window.location.assign(safeReturnTo);
      }, 350);
    } catch (error) {
      setMessage({
        tone: "error",
        text: error instanceof Error ? error.message : "登录失败，请稍后重试",
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-panel">
      <div className="auth-card surface-lg shadow-sm">
        <div className="auth-tabs" role="tablist">
          <button
            aria-selected={mode === "login"}
            className={mode === "login" ? "active" : ""}
            onClick={() => setMode("login")}
            role="tab"
            type="button"
          >
            登录
          </button>
          <button
            aria-selected={mode === "register"}
            className={mode === "register" ? "active" : ""}
            onClick={() => setMode("register")}
            role="tab"
            type="button"
          >
            注册
          </button>
        </div>

        <form className="auth-form" onSubmit={submit}>
          {mode === "register" && (
            <>
              <label>
                <span>用户名</span>
                <input
                  autoComplete="username"
                  className="field"
                  name="username"
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder="3-32 位字母、数字、下划线或连字符"
                  required
                  value={username}
                />
              </label>
              <label>
                <span>显示名称</span>
                <input
                  className="field"
                  maxLength={80}
                  name="displayName"
                  onChange={(event) => setDisplayName(event.target.value)}
                  placeholder="公开展示的名称，可稍后修改"
                  value={displayName}
                />
              </label>
            </>
          )}
          <label>
            <span>邮箱</span>
            <input
              autoComplete="email"
              className="field"
              name="email"
              onChange={(event) => setEmail(event.target.value)}
              placeholder="请输入邮箱地址"
              required
              type="email"
              value={email}
            />
          </label>
          <label>
            <span>密码</span>
            <span className="password-field">
              <input
                autoComplete={mode === "login" ? "current-password" : "new-password"}
                className="field"
                name="password"
                onChange={(event) => setPassword(event.target.value)}
                minLength={mode === "register" ? 12 : 1}
                maxLength={72}
                placeholder={mode === "login" ? "请输入密码" : "12-72 位，含大小写、数字和特殊字符"}
                required
                type={showPassword ? "text" : "password"}
                value={password}
              />
              <button
                aria-label={showPassword ? "隐藏密码" : "显示密码"}
                onClick={() => setShowPassword((value) => !value)}
                type="button"
              >
                {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>
            </span>
          </label>

          {mode === "login" ? (
            <div className="auth-options">
              <label className="checkbox-row">
                <input type="checkbox" />
                <span>记住我</span>
              </label>
              <button className="link text-button" type="button">
                忘记密码？
              </button>
            </div>
          ) : (
            <label className="checkbox-row auth-agreement">
              <input
                checked={agreed}
                onChange={(event) => setAgreed(event.target.checked)}
                type="checkbox"
              />
              <span>
                我已阅读并同意 <a className="link">服务协议</a> 与{" "}
                <a className="link">隐私政策</a>
              </span>
            </label>
          )}

          {message && (
            <div
              className={`auth-alert auth-alert--${message.tone}`}
              role={message.tone === "error" ? "alert" : "status"}
            >
              {message.tone === "error"
                ? <AlertCircle aria-hidden="true" size={17} />
                : <CheckCircle2 aria-hidden="true" size={17} />}
              <span>{message.text}</span>
            </div>
          )}

          <button
            className="primary-button auth-submit"
            disabled={submitting}
            type="submit"
          >
            {submitting && <LoaderCircle className="spin" size={17} />}
            {submitting
              ? mode === "login" ? "登录中…" : "创建中…"
              : mode === "login" ? "登录" : "创建账号"}
          </button>
        </form>

        <p className="auth-switch">
          {mode === "login" ? "还没有账号？" : "已有账号？"}
          <button
            className="link text-button"
            onClick={() => setMode(mode === "login" ? "register" : "login")}
            type="button"
          >
            {mode === "login" ? "立即注册" : "返回登录"}
          </button>
        </p>
        <p className="auth-demo"><Link className="link" href="/discover">暂不登录，先浏览社区内容</Link></p>

        <div className="auth-divider">
          <span>其他登录方式</span>
        </div>
        <div className="social-logins">
          <button aria-label="使用微信登录" type="button">
            <MessageCircle size={19} />
          </button>
          <button aria-label="使用通用账号登录" type="button">
            <UserRound size={19} />
          </button>
          <button aria-label="使用 GitHub 登录" type="button">
            <Github size={19} />
          </button>
        </div>
        <p className="auth-legal">
          登录即代表同意《用户协议》和《隐私政策》
        </p>
      </div>
    </section>
  );
}
