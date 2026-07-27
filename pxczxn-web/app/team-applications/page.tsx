"use client";

import { useState, useEffect } from "react";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, type TeamApplication, type SubmitTeamApplicationInput } from "../lib/community-api";
import { CheckCircle2, XCircle, Clock, Ban, AlertCircle, Loader2 } from "lucide-react";

export default function TeamApplicationsPage() {
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [application, setApplication] = useState<TeamApplication | null>(null);
  const [error, setError] = useState<string>("");
  const [showForm, setShowForm] = useState(false);

  const [formData, setFormData] = useState({
    teamName: "",
    teamSlug: "",
    description: "",
  });
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    loadMyApplication();
  }, []);

  async function loadMyApplication() {
    setLoading(true);
    setError("");
    try {
      const data = await communityApi.myTeamApplication();
      setApplication(data);
      setShowForm(!data || data.status === "REJECTED" || data.status === "CANCELLED");
    } catch (err) {
      if (err && typeof err === "object" && "code" in err && err.code === 401) {
        setError("请先登录");
      } else {
        setError(err instanceof Error ? err.message : "加载失败");
      }
    } finally {
      setLoading(false);
    }
  }

  function validateForm(): boolean {
    const errors: Record<string, string> = {};

    if (!formData.teamName.trim()) {
      errors.teamName = "团队名称不能为空";
    } else if (formData.teamName.length > 50) {
      errors.teamName = "团队名称不能超过50个字符";
    }

    if (!formData.teamSlug.trim()) {
      errors.teamSlug = "团队标识不能为空";
    } else if (!/^[a-z0-9]+(?:[-_][a-z0-9]+)*$/.test(formData.teamSlug)) {
      errors.teamSlug = "只能包含小写字母、数字、连字符和下划线";
    } else if (formData.teamSlug.length < 3 || formData.teamSlug.length > 50) {
      errors.teamSlug = "团队标识长度为3-50个字符";
    }

    if (formData.description && formData.description.length > 500) {
      errors.description = "说明不能超过500个字符";
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      const input: SubmitTeamApplicationInput = {
        teamName: formData.teamName.trim(),
        teamSlug: formData.teamSlug.trim(),
        description: formData.description.trim() || undefined,
        idempotencyKey: `web-${Date.now()}-${Math.random()}`,
      };

      const result = await communityApi.submitTeamApplication(input);
      setApplication(result);
      setShowForm(false);
      setFormData({ teamName: "", teamSlug: "", description: "" });
    } catch (err) {
      setError(err instanceof Error ? err.message : "提交失败");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancel(applicationId: number) {
    if (!confirm("确定要撤销申请吗？撤销后可以重新提交。")) {
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      await communityApi.cancelTeamApplication(applicationId);
      await loadMyApplication();
    } catch (err) {
      setError(err instanceof Error ? err.message : "撤销失败");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <>
        <UserTopbar title="团队申请" />
        <div className="page-shell">
          <div className="flex items-center justify-center" style={{ minHeight: "60vh" }}>
            <div className="text-center">
              <Loader2 className="animate-spin mx-auto mb-3" size={32} style={{ color: "var(--primary)" }} />
              <p className="secondary">加载中...</p>
            </div>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <UserTopbar title="团队申请" />
      <div className="page-shell" style={{ maxWidth: "800px", marginInline: "auto" }}>
        <div className="mb-8">
          <h1 style={{ fontSize: "28px", fontWeight: 600, marginBottom: "8px" }}>团队申请</h1>
          <p className="secondary" style={{ fontSize: "15px" }}>
            创建团队博客需要提交申请，平台审核通过后将自动创建团队博客和相关资源。
          </p>
        </div>

        {error && (
          <div
            className="surface-lg mb-6"
            style={{
              padding: "16px",
              borderColor: "var(--danger)",
              background: "color-mix(in srgb, var(--danger) 6%, var(--bg-surface))",
            }}
          >
            <div className="flex items-start gap-3">
              <AlertCircle size={20} style={{ color: "var(--danger)", flexShrink: 0, marginTop: "2px" }} />
              <div className="flex-1">
                <p style={{ color: "var(--danger)", margin: 0 }}>{error}</p>
              </div>
              <button
                onClick={() => setError("")}
                className="ghost-button"
                style={{ padding: "4px", minWidth: "auto" }}
              >
                ×
              </button>
            </div>
          </div>
        )}

        {/* Existing Application */}
        {application && application.status === "PENDING" && (
          <div className="surface-lg shadow-sm mb-6" style={{ padding: "24px" }}>
            <div className="flex items-start justify-between mb-5">
              <h2 style={{ fontSize: "18px", fontWeight: 600, margin: 0 }}>我的申请</h2>
              <StatusBadge status={application.status} />
            </div>

            <div className="space-y-4">
              <InfoRow label="团队名称" value={application.teamName} />
              <InfoRow label="团队标识" value={<code className="code-inline">{application.teamSlug}</code>} />
              {application.description && <InfoRow label="申请说明" value={application.description} />}
              <InfoRow label="提交时间" value={new Date(application.createdAt).toLocaleString("zh-CN")} />
            </div>

            <div className="flex gap-3 mt-6">
              <button
                onClick={() => handleCancel(application.id)}
                disabled={submitting}
                className="secondary-button"
              >
                {submitting ? "撤销中..." : "撤销申请"}
              </button>
              <button onClick={loadMyApplication} disabled={submitting} className="ghost-button">
                刷新状态
              </button>
            </div>
          </div>
        )}

        {application && (application.status === "APPROVED" || application.status === "REJECTED") && (
          <div
            className="surface-lg shadow-sm mb-6"
            style={{
              padding: "24px",
              borderColor:
                application.status === "APPROVED" ? "var(--success)" : "var(--danger)",
            }}
          >
            <div className="flex items-start justify-between mb-5">
              <h2 style={{ fontSize: "18px", fontWeight: 600, margin: 0 }}>审核结果</h2>
              <StatusBadge status={application.status} />
            </div>

            <div className="space-y-4">
              <InfoRow label="团队名称" value={application.teamName} />
              <InfoRow label="团队标识" value={<code className="code-inline">{application.teamSlug}</code>} />
              {application.reviewComment && <InfoRow label="审核说明" value={application.reviewComment} />}
              <InfoRow
                label="审核时间"
                value={
                  application.reviewedAt
                    ? new Date(application.reviewedAt).toLocaleString("zh-CN")
                    : "-"
                }
              />
            </div>

            {application.status === "APPROVED" && (
              <div
                className="mt-5"
                style={{
                  padding: "16px",
                  background: "color-mix(in srgb, var(--success) 6%, var(--bg-surface))",
                  borderRadius: "8px",
                  border: "1px solid color-mix(in srgb, var(--success) 20%, transparent)",
                }}
              >
                <p style={{ margin: 0, color: "var(--success)", fontSize: "14px" }}>
                  🎉 恭喜！你的团队已创建成功。你可以在团队博客页面管理内容和成员。
                </p>
              </div>
            )}

            {application.status === "REJECTED" && (
              <div className="mt-6">
                <button onClick={() => setShowForm(true)} className="primary-button">
                  重新申请
                </button>
              </div>
            )}
          </div>
        )}

        {/* Application Form */}
        {showForm && (
          <div className="surface-lg shadow-sm" style={{ padding: "24px" }}>
            <h2 style={{ fontSize: "18px", fontWeight: 600, marginBottom: "20px" }}>
              提交团队申请
            </h2>

            <form onSubmit={handleSubmit} className="space-y-5">
              <FormField
                label="团队名称"
                required
                error={formErrors.teamName}
                help="团队博客的显示名称"
              >
                <input
                  type="text"
                  value={formData.teamName}
                  onChange={(e) => setFormData({ ...formData, teamName: e.target.value })}
                  className="input"
                  placeholder="例如：前端技术小组"
                  maxLength={50}
                />
              </FormField>

              <FormField
                label="团队标识"
                required
                error={formErrors.teamSlug}
                help="3-50个字符，只能包含小写字母、数字、连字符和下划线，将作为团队博客URL的一部分"
              >
                <input
                  type="text"
                  value={formData.teamSlug}
                  onChange={(e) =>
                    setFormData({ ...formData, teamSlug: e.target.value.toLowerCase() })
                  }
                  className="input"
                  style={{ fontFamily: "monospace" }}
                  placeholder="frontend-team"
                  pattern="[a-z0-9]+(?:[-_][a-z0-9]+)*"
                  maxLength={50}
                />
              </FormField>

              <FormField label="申请说明" error={formErrors.description}>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="input"
                  rows={4}
                  placeholder="简要说明团队的目的和计划..."
                  maxLength={500}
                  style={{ resize: "vertical" }}
                />
                <div className="mt-1 text-right muted" style={{ fontSize: "13px" }}>
                  {formData.description.length}/500
                </div>
              </FormField>

              <div
                style={{
                  padding: "16px",
                  background: "var(--bg-subtle)",
                  borderRadius: "8px",
                  border: "1px solid var(--border-default)",
                }}
              >
                <p className="secondary" style={{ margin: 0, fontSize: "13px" }}>
                  ℹ️ 提交后需要等待平台审核。审核通过后将自动创建团队博客、成员关系和默认设置。
                </p>
              </div>

              <div className="flex gap-3">
                <button type="submit" disabled={submitting} className="primary-button">
                  {submitting ? (
                    <>
                      <Loader2 className="animate-spin" size={16} />
                      提交中...
                    </>
                  ) : (
                    "提交申请"
                  )}
                </button>
                {application && (
                  <button
                    type="button"
                    onClick={() => setShowForm(false)}
                    className="secondary-button"
                  >
                    取消
                  </button>
                )}
              </div>
            </form>
          </div>
        )}

        {!application && !showForm && !loading && (
          <div
            className="surface-lg shadow-sm text-center"
            style={{ padding: "60px 24px" }}
          >
            <div
              style={{
                width: "64px",
                height: "64px",
                borderRadius: "50%",
                background: "var(--bg-muted)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                margin: "0 auto 16px",
              }}
            >
              <svg
                width="32"
                height="32"
                viewBox="0 0 24 24"
                fill="none"
                stroke="var(--text-tertiary)"
                strokeWidth="2"
              >
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </div>
            <h3 style={{ fontSize: "16px", fontWeight: 600, marginBottom: "8px" }}>
              暂无申请
            </h3>
            <p className="secondary" style={{ marginBottom: "24px" }}>
              开始创建你的团队博客吧
            </p>
            <button onClick={() => setShowForm(true)} className="primary-button">
              提交团队申请
            </button>
          </div>
        )}
      </div>

      <style jsx>{`
        .space-y-4 > * + * {
          margin-top: 16px;
        }
        .space-y-5 > * + * {
          margin-top: 20px;
        }
        .code-inline {
          padding: 2px 6px;
          background: var(--bg-muted);
          border-radius: 4px;
          font-family: "SF Mono", "Consolas", "Monaco", monospace;
          font-size: 13px;
        }
      `}</style>
    </>
  );
}

function StatusBadge({ status }: { status: string }) {
  type StatusConfig = { icon: typeof Clock; text: string; color: string; bg: string };
  const configs: Record<string, StatusConfig> = {
    PENDING: { icon: Clock, text: "待审核", color: "var(--warning)", bg: "color-mix(in srgb, var(--warning) 8%, transparent)" },
    APPROVED: { icon: CheckCircle2, text: "已通过", color: "var(--success)", bg: "color-mix(in srgb, var(--success) 8%, transparent)" },
    REJECTED: { icon: XCircle, text: "已拒绝", color: "var(--danger)", bg: "color-mix(in srgb, var(--danger) 8%, transparent)" },
    CANCELLED: { icon: Ban, text: "已取消", color: "var(--text-tertiary)", bg: "var(--bg-muted)" },
  };

  const config = configs[status] || { icon: Clock, text: status, color: "var(--text-tertiary)", bg: "var(--bg-muted)" };

  const Icon = config.icon;

  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: "6px",
        padding: "6px 12px",
        borderRadius: "6px",
        fontSize: "13px",
        fontWeight: 500,
        background: config.bg,
        color: config.color,
      }}
    >
      <Icon size={14} />
      {config.text}
    </span>
  );
}

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <dt className="muted" style={{ fontSize: "13px", marginBottom: "6px" }}>
        {label}
      </dt>
      <dd style={{ margin: 0, fontSize: "14px" }}>{value}</dd>
    </div>
  );
}

function FormField({
  label,
  required,
  error,
  help,
  children,
}: {
  label: string;
  required?: boolean;
  error?: string;
  help?: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label className="form-label">
        {label}
        {required && <span style={{ color: "var(--danger)", marginLeft: "4px" }}>*</span>}
      </label>
      {children}
      {help && !error && (
        <p className="muted" style={{ fontSize: "13px", marginTop: "6px", marginBottom: 0 }}>
          {help}
        </p>
      )}
      {error && (
        <p style={{ color: "var(--danger)", fontSize: "13px", marginTop: "6px", marginBottom: 0 }}>
          {error}
        </p>
      )}
    </div>
  );
}
