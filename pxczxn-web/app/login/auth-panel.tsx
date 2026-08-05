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
import { FormEvent, useEffect, useState } from "react";
import {
  CommunityApiError,
  communityApi,
  saveSession,
} from "../lib/community-api";

const EMAIL_PATTERN = /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{12,72}$/;
const SPACE_ADDRESS_PATTERN = /^[A-Za-z][A-Za-z0-9_-]{0,30}[A-Za-z0-9]$/;
const RESEND_COOLDOWN_SECONDS = 60;

export function AuthPanel() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [showPassword, setShowPassword] = useState(false);
  const [username, setUsername] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [forgotOpen, setForgotOpen] = useState(false);
  const [message, setMessage] = useState<{
    tone: "error" | "success";
    text: string;
  } | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    setMessage(null);

    if (mode === "register") {
      if (!displayName.trim()) {
        setMessage({ tone: "error", text: "请输入姓名或公开显示名称" });
        return;
      }
      if (!SPACE_ADDRESS_PATTERN.test(username)) {
        setMessage({
          tone: "error",
          text: "个人空间地址须为 2–32 位，以字母开头并以字母或数字结尾",
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
          throw new CommunityApiError("该个人空间地址已被使用", 409, 409);
        }
        if (!emailState.available) {
          throw new CommunityApiError("该邮箱已注册", 409, 409);
        }
        const registered = await communityApi.register({
          username,
          displayName: displayName.trim(),
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
        status: me.status,
      });
      setMessage({
        tone: "success",
        text: mode === "register" ? "账号创建成功，正在进入社区…" : "登录成功，正在进入社区…",
      });
      window.setTimeout(() => {
        const redirectTarget = new URLSearchParams(window.location.search).get("returnTo");
        const safeReturnTo = me.status === "FROZEN"
          ? "/account-appeals"
          : session.forcePasswordChange
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
                <span>昵称</span>
                <input
                  className="field"
                  maxLength={80}
                  name="displayName"
                  onChange={(event) => setDisplayName(event.target.value)}
                  placeholder="用于公开展示的名称"
                  required
                  value={displayName}
                />
              </label>
              <label>
                <span>个人空间地址</span>
                <input
                  autoComplete="username"
                  className="field"
                  name="username"
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder="例如 zhangsan，将生成 /zhangsan"
                  required
                  value={username}
                />
                <small className="auth-field-hint">仅限字母、数字、下划线和连字符；以字母开头，以字母或数字结尾。</small>
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
              <button
                className="link text-button"
                onClick={() => setForgotOpen(true)}
                type="button"
              >
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
      <ForgotPasswordDialog
        onClose={() => setForgotOpen(false)}
        onReset={(email) => {
          setForgotOpen(false);
          setMode("login");
          setEmail(email);
        }}
        open={forgotOpen}
      />
    </section>
  );
}

function ForgotPasswordDialog({
  open,
  onClose,
  onReset,
}: {
  open: boolean;
  onClose: () => void;
  onReset: (email: string) => void;
}) {
  const [step, setStep] = useState<"email" | "code" | "done">("email");
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [message, setMessage] = useState<{
    tone: "error" | "success";
    text: string;
  } | null>(null);

  useEffect(() => {
    if (!open) return;
    if (cooldown <= 0) return;
    const timer = window.setInterval(() => {
      setCooldown((value) => {
        if (value <= 1) {
          window.clearInterval(timer);
          return 0;
        }
        return value - 1;
      });
    }, 1000);
    return () => window.clearInterval(timer);
  }, [open, cooldown]);

  function close() {
    if (sendingCode || resetting) return;
    onClose();
    // 下次打开时回到第一步
    window.setTimeout(() => {
      setStep("email");
      setCode("");
      setNewPassword("");
      setConfirmPassword("");
      setMessage(null);
      setCooldown(0);
    }, 200);
  }

  async function sendCode() {
    if (sendingCode || cooldown > 0) return;
    setMessage(null);
    if (!EMAIL_PATTERN.test(email)) {
      setMessage({ tone: "error", text: "请输入有效邮箱，例如 name@example.com" });
      return;
    }
    setSendingCode(true);
    try {
      await communityApi.sendPasswordResetCode(email);
      setStep("code");
      setCooldown(RESEND_COOLDOWN_SECONDS);
      setMessage({
        tone: "success",
        text: "验证码已发送至邮箱，10 分钟内有效",
      });
    } catch (error) {
      setMessage({
        tone: "error",
        text: error instanceof Error ? error.message : "验证码发送失败，请稍后重试",
      });
    } finally {
      setSendingCode(false);
    }
  }

  async function submitReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (resetting) return;
    setMessage(null);
    if (!/^\d{6}$/.test(code.trim())) {
      setMessage({ tone: "error", text: "请输入 6 位数字验证码" });
      return;
    }
    if (!PASSWORD_PATTERN.test(newPassword)) {
      setMessage({
        tone: "error",
        text: "新密码须为 12-72 位，并包含大写、小写、数字和特殊字符，且不能含空格",
      });
      return;
    }
    if (newPassword !== confirmPassword) {
      setMessage({ tone: "error", text: "两次输入的密码不一致" });
      return;
    }
    setResetting(true);
    try {
      await communityApi.resetPassword(email, code.trim(), newPassword);
      setStep("done");
      setMessage(null);
    } catch (error) {
      setMessage({
        tone: "error",
        text: error instanceof Error ? error.message : "密码重置失败，请稍后重试",
      });
    } finally {
      setResetting(false);
    }
  }

  return (
    <div
      aria-hidden={!open}
      className="settings-password-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="forgot-password-title"
      style={{ display: open ? undefined : "none" }}
    >
      <button
        aria-label="关闭找回密码弹窗"
        className="settings-password-backdrop"
        onClick={close}
        type="button"
      />
      <div className="surface settings-password-dialog auth-forgot-dialog">
        <header>
          <h2 id="forgot-password-title">找回密码</h2>
          <button aria-label="关闭" className="icon-button" onClick={close} type="button">×</button>
        </header>

        {step === "email" && (
          <form className="auth-form auth-forgot-form" onSubmit={(event) => { event.preventDefault(); void sendCode(); }}>
            <p className="auth-forgot-tip">
              请输入注册时使用的邮箱，我们会向该邮箱发送 6 位数字验证码。
            </p>
            <label>
              <span>邮箱</span>
              <input
                autoComplete="email"
                autoFocus
                className="field"
                onChange={(event) => setEmail(event.target.value)}
                placeholder="请输入注册邮箱"
                required
                type="email"
                value={email}
              />
            </label>
            {message && (
              <div className={`auth-alert ${message.tone === "success" ? "auth-alert--success" : ""}`} role={message.tone === "error" ? "alert" : "status"}>
                {message.tone === "error"
                  ? <AlertCircle aria-hidden="true" size={17} />
                  : <CheckCircle2 aria-hidden="true" size={17} />}
                <span>{message.text}</span>
              </div>
            )}
            <button
              className="primary-button auth-submit"
              disabled={sendingCode}
              type="submit"
            >
              {sendingCode && <LoaderCircle className="spin" size={17} />}
              {sendingCode ? "发送中…" : "发送验证码"}
            </button>
          </form>
        )}

        {step === "code" && (
          <form className="auth-form auth-forgot-form" onSubmit={submitReset}>
            <p className="auth-forgot-tip">
              验证码已发送至 {email}
              {cooldown > 0 ? `（${cooldown} 秒后可重新发送）` : ""}
            </p>
            <label>
              <span>验证码</span>
              <input
                autoComplete="one-time-code"
                autoFocus
                className="field"
                inputMode="numeric"
                maxLength={6}
                onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))}
                pattern="\d{6}"
                placeholder="6 位数字验证码"
                required
                value={code}
              />
            </label>
            <label>
              <span>新密码</span>
              <span className="password-field">
                <input
                  autoComplete="new-password"
                  className="field"
                  maxLength={72}
                  minLength={12}
                  onChange={(event) => setNewPassword(event.target.value)}
                  placeholder="12-72 位，含大小写、数字和特殊字符"
                  required
                  type={showNewPassword ? "text" : "password"}
                  value={newPassword}
                />
                <button
                  aria-label={showNewPassword ? "隐藏密码" : "显示密码"}
                  onClick={() => setShowNewPassword((value) => !value)}
                  type="button"
                >
                  {showNewPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </span>
            </label>
            <label>
              <span>确认新密码</span>
              <span className="password-field">
                <input
                  autoComplete="new-password"
                  className="field"
                  maxLength={72}
                  minLength={12}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  placeholder="再次输入新密码"
                  required
                  type={showConfirmPassword ? "text" : "password"}
                  value={confirmPassword}
                />
                <button
                  aria-label={showConfirmPassword ? "隐藏密码" : "显示密码"}
                  onClick={() => setShowConfirmPassword((value) => !value)}
                  type="button"
                >
                  {showConfirmPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </span>
            </label>
            <p className="auth-forgot-tip">
              新密码须为 12-72 位，包含大写、小写、数字和特殊字符，且不能含空格。
            </p>
            {message && (
              <div className={`auth-alert ${message.tone === "success" ? "auth-alert--success" : ""}`} role={message.tone === "error" ? "alert" : "status"}>
                {message.tone === "error"
                  ? <AlertCircle aria-hidden="true" size={17} />
                  : <CheckCircle2 aria-hidden="true" size={17} />}
                <span>{message.text}</span>
              </div>
            )}
            <button
              className="primary-button auth-submit"
              disabled={resetting}
              type="submit"
            >
              {resetting && <LoaderCircle className="spin" size={17} />}
              {resetting ? "重置中…" : "重置密码"}
            </button>
            <button
              className="link text-button"
              disabled={sendingCode || cooldown > 0}
              onClick={() => void sendCode()}
              style={{ justifySelf: "center" }}
              type="button"
            >
              {cooldown > 0 ? `${cooldown} 秒后重新发送` : "重新发送验证码"}
            </button>
          </form>
        )}

        {step === "done" && (
          <div className="auth-forgot-done">
            <CheckCircle2 aria-hidden="true" size={44} />
            <h3>密码已重置</h3>
            <p>请使用新密码重新登录。</p>
            <button
              className="primary-button auth-submit"
              onClick={() => onReset(email)}
              type="button"
            >
              返回登录
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
