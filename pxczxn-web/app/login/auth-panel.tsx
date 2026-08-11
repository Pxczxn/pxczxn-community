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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
  const [rememberMe, setRememberMe] = useState(false);
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
        setMessage({ tone: "error", text: "密码须为 12-72 位，并包含大写、小写、数字和特殊字符" });
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

      const session = await communityApi.login({ email, password, rememberMe });
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
      <header className="auth-panel-heading">
        <p>{mode === "login" ? "欢迎回来" : "开始你的创作空间"}</p>
        <h1>{mode === "login" ? "登录星语社区" : "创建星语账号"}</h1>
        <span>{mode === "login" ? "继续你的阅读、表达与协作。" : "用一个账号，开始记录与连接。"}</span>
      </header>
      <Tabs className="auth-tabs-container" value={mode} onValueChange={(v) => setMode(v as "login" | "register")}>
        <TabsList className="w-full">
          <TabsTrigger className="flex-1" value="login">登录</TabsTrigger>
          <TabsTrigger className="flex-1" value="register">注册</TabsTrigger>
        </TabsList>

        <TabsContent className="auth-tab-content" value="login">
          <form className="auth-form" onSubmit={submit}>
            <div className="auth-field">
              <label htmlFor="login-email">邮箱</label>
              <Input
                id="login-email"
                autoComplete="email"
                placeholder="请输入邮箱地址"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div className="auth-field">
              <label htmlFor="login-password">密码</label>
              <div className="password-wrapper">
                <Input
                  id="login-password"
                  autoComplete="current-password"
                  placeholder="请输入密码"
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? "隐藏密码" : "显示密码"}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <div className="auth-options">
              <label className="checkbox-row">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                />
                <span>记住我</span>
              </label>
              <button
                type="button"
                className="forgot-link"
                onClick={() => setForgotOpen(true)}
              >
                忘记密码？
              </button>
            </div>

            {message && (
              <div className={`auth-alert auth-alert--${message.tone}`}>
                {message.tone === "error" ? (
                  <AlertCircle size={16} />
                ) : (
                  <CheckCircle2 size={16} />
                )}
                <span>{message.text}</span>
              </div>
            )}

            <Button className="auth-submit-btn" type="submit" disabled={submitting}>
              {submitting && <LoaderCircle className="animate-spin" size={16} />}
              {submitting ? "登录中…" : "登录"}
            </Button>
          </form>
        </TabsContent>

        <TabsContent className="auth-tab-content" value="register">
          <form className="auth-form" onSubmit={submit}>
            <div className="auth-field">
              <label htmlFor="register-name">昵称</label>
              <Input
                id="register-name"
                placeholder="用于公开展示的名称"
                maxLength={80}
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
              />
            </div>

            <div className="auth-field">
              <label htmlFor="register-username">个人空间地址</label>
              <Input
                id="register-username"
                autoComplete="username"
                placeholder="例如 zhangsan"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
              <span className="field-hint">将生成 /{username || "username"} 主页</span>
            </div>

            <div className="auth-field">
              <label htmlFor="register-email">邮箱</label>
              <Input
                id="register-email"
                autoComplete="email"
                placeholder="请输入邮箱地址"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div className="auth-field">
              <label htmlFor="register-password">密码</label>
              <div className="password-wrapper">
                <Input
                  id="register-password"
                  autoComplete="new-password"
                  placeholder="12-72 位，含大小写、数字和特殊字符"
                  type={showPassword ? "text" : "password"}
                  maxLength={72}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? "隐藏密码" : "显示密码"}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <label className="agreement-row">
              <input
                type="checkbox"
                checked={agreed}
                onChange={(e) => setAgreed(e.target.checked)}
              />
              <span>
                我已阅读并同意 <a href="#" className="auth-link">服务协议</a> 与{" "}
                <a href="#" className="auth-link">隐私政策</a>
              </span>
            </label>

            {message && (
              <div className={`auth-alert auth-alert--${message.tone}`}>
                {message.tone === "error" ? (
                  <AlertCircle size={16} />
                ) : (
                  <CheckCircle2 size={16} />
                )}
                <span>{message.text}</span>
              </div>
            )}

            <Button className="auth-submit-btn" type="submit" disabled={submitting}>
              {submitting && <LoaderCircle className="animate-spin" size={16} />}
              {submitting ? "创建中…" : "创建账号"}
            </Button>
          </form>
        </TabsContent>
      </Tabs>

      <p className="auth-demo">
        <Link href="/discover" className="auth-link">暂不登录，先浏览社区内容</Link>
      </p>

      <div className="auth-divider">
        <span>其他登录方式</span>
      </div>

      <div className="social-logins">
        <button type="button" aria-label="使用微信登录">
          <MessageCircle size={19} />
        </button>
        <button type="button" aria-label="使用通用账号登录">
          <UserRound size={19} />
        </button>
        <button type="button" aria-label="使用 GitHub 登录">
          <Github size={19} />
        </button>
      </div>

      <ForgotPasswordDialog
        open={forgotOpen}
        onOpenChange={setForgotOpen}
        onReset={(email) => {
          setForgotOpen(false);
          setEmail(email);
        }}
      />
    </section>
  );
}

function ForgotPasswordDialog({
  open,
  onOpenChange,
  onReset,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
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
    onOpenChange(false);
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
        text: "新密码须为 12-72 位，并包含大写、小写、数字和特殊字符",
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

  if (!open) return null;

  return (
    <div className="dialog-overlay" onClick={close}>
      <div className="dialog-content" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <h2>找回密码</h2>
          <button type="button" className="dialog-close" onClick={close}>×</button>
        </div>

        {step === "email" && (
          <form onSubmit={(e) => { e.preventDefault(); void sendCode(); }} className="dialog-form">
            <p className="dialog-tip">请输入注册时使用的邮箱，我们会向该邮箱发送 6 位数字验证码。</p>
            <div className="auth-field">
              <label htmlFor="forgot-email">邮箱</label>
              <Input
                id="forgot-email"
                autoComplete="email"
                autoFocus
                placeholder="请输入注册邮箱"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            {message && (
              <div className={`auth-alert auth-alert--${message.tone}`}>
                {message.tone === "error" ? <AlertCircle size={16} /> : <CheckCircle2 size={16} />}
                <span>{message.text}</span>
              </div>
            )}
            <Button type="submit" className="w-full" disabled={sendingCode}>
              {sendingCode && <LoaderCircle className="animate-spin" size={16} />}
              {sendingCode ? "发送中…" : "发送验证码"}
            </Button>
          </form>
        )}

        {step === "code" && (
          <form onSubmit={submitReset} className="dialog-form">
            <p className="dialog-tip">
              验证码已发送至 {email}
              {cooldown > 0 ? `（${cooldown} 秒后可重新发送）` : ""}
            </p>
            <div className="auth-field">
              <label htmlFor="reset-code">验证码</label>
              <Input
                id="reset-code"
                autoComplete="one-time-code"
                autoFocus
                inputMode="numeric"
                maxLength={6}
                placeholder="6 位数字验证码"
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
              />
            </div>
            <div className="auth-field">
              <label htmlFor="reset-new-password">新密码</label>
              <div className="password-wrapper">
                <Input
                  id="reset-new-password"
                  autoComplete="new-password"
                  type={showNewPassword ? "text" : "password"}
                  maxLength={72}
                  placeholder="12-72 位，含大小写、数字和特殊字符"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                >
                  {showNewPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>
            <div className="auth-field">
              <label htmlFor="reset-confirm-password">确认新密码</label>
              <div className="password-wrapper">
                <Input
                  id="reset-confirm-password"
                  autoComplete="new-password"
                  type={showConfirmPassword ? "text" : "password"}
                  maxLength={72}
                  placeholder="再次输入新密码"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                >
                  {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>
            {message && (
              <div className={`auth-alert auth-alert--${message.tone}`}>
                {message.tone === "error" ? <AlertCircle size={16} /> : <CheckCircle2 size={16} />}
                <span>{message.text}</span>
              </div>
            )}
            <Button type="submit" className="w-full" disabled={resetting}>
              {resetting && <LoaderCircle className="animate-spin" size={16} />}
              {resetting ? "重置中…" : "重置密码"}
            </Button>
            <button
              type="button"
              className="resend-btn"
              disabled={sendingCode || cooldown > 0}
              onClick={() => void sendCode()}
            >
              {cooldown > 0 ? `${cooldown} 秒后重新发送` : "重新发送验证码"}
            </button>
          </form>
        )}

        {step === "done" && (
          <div className="dialog-done">
            <CheckCircle2 size={48} className="done-icon" />
            <h3>密码已重置</h3>
            <p>请使用新密码重新登录。</p>
            <Button onClick={() => onReset(email)} className="w-full">
              返回登录
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
