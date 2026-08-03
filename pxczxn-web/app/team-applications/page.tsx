"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { UserTopbar } from "../components/prototype-ui";
import { communityApi, type TeamApplication } from "../lib/community-api";
import {
  CheckCircle2,
  XCircle,
  Clock,
  Ban,
  AlertCircle,
  Loader2,
  Rocket,
  ShieldCheck,
  ArrowRight,
  RotateCcw,
  RefreshCw,
  Feather,
  Target,
  LayoutList,
  Users,
  Info,
  ChevronDown,
} from "lucide-react";

/* ──────────────────────────────────────────
   Constants
────────────────────────────────────────── */
const TEAM_CATEGORIES = [
  { value: "frontend", label: "前端开发", desc: "Web 前端、小程序、跨端技术" },
  { value: "backend", label: "后端开发", desc: "服务端架构、数据库、微服务" },
  { value: "mobile", label: "移动端", desc: "iOS、Android、Flutter、React Native" },
  { value: "ai", label: "AI / 大模型", desc: "LLM 应用、Agent、模型优化" },
  { value: "devops", label: "DevOps", desc: "CI/CD、容器化、基础设施" },
  { value: "opensource", label: "开源项目", desc: "开源库、工具、社区运营" },
  { value: "product", label: "产品设计", desc: "产品策划、UX/UI 设计" },
  { value: "other", label: "其他领域", desc: "不属于以上分类的方向" },
] as const;

const TEAM_SIZE_OPTIONS = [
  { value: "2-5", label: "2–5 人", desc: "小而精的初创团队" },
  { value: "6-10", label: "6–10 人", desc: "有一定规模的协作团队" },
  { value: "11-20", label: "11–20 人", desc: "中型成熟团队" },
  { value: "20+", label: "20 人以上", desc: "大型综合团队" },
] as const;

/* ──────────────────────────────────────────
   Helpers
────────────────────────────────────────── */
type AppStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

const STATUS_META: Record<AppStatus, { icon: typeof Clock; label: string; color: string; bg: string; border: string }> = {
  PENDING:   { icon: Clock,        label: "审核中",  color: "var(--warning)", bg: "color-mix(in srgb, var(--warning) 8%, transparent)",      border: "color-mix(in srgb, var(--warning) 20%, transparent)" },
  APPROVED:  { icon: CheckCircle2, label: "已通过",  color: "var(--success)",  bg: "color-mix(in srgb, var(--success) 8%, transparent)",   border: "color-mix(in srgb, var(--success) 20%, transparent)" },
  REJECTED:  { icon: XCircle,      label: "未通过",  color: "var(--danger)",   bg: "color-mix(in srgb, var(--danger) 8%, transparent)",    border: "color-mix(in srgb, var(--danger) 20%, transparent)" },
  CANCELLED: { icon: Ban,          label: "已撤回",  color: "var(--text-tertiary)", bg: "var(--bg-muted)",                              border: "var(--border-default)" },
};

function StatusPill({ status }: { status: string }) {
  const meta = STATUS_META[status as AppStatus] ?? STATUS_META.PENDING;
  const Icon = meta.icon;
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: "6px", padding: "5px 12px", borderRadius: "100px", fontSize: "13px", fontWeight: 500, background: meta.bg, border: `1px solid ${meta.border}`, color: meta.color }}>
      <Icon size={14} />
      {meta.label}
    </span>
  );
}

function StepIndicator({ current }: { current: number }) {
  const steps = [{ n: 1, label: "填写资料" }, { n: 2, label: "提交申请" }, { n: 3, label: "等待审核" }];
  return (
    <div className="step-indicator" aria-label="申请进度">
      {steps.map((step, i) => {
        const done = step.n < current;
        const active = step.n === current;
        return (
          <div key={step.n} className={`step-indicator__item ${active ? "is-active" : ""} ${done ? "is-done" : ""}`}>
            <div className="step-indicator__dot">{done ? <CheckCircle2 size={14} /> : <span>{step.n}</span>}</div>
            <span className="step-indicator__label">{step.label}</span>
            {i < steps.length - 1 && <div className="step-indicator__line" />}
          </div>
        );
      })}
    </div>
  );
}

function cn(...classes: (string | boolean | undefined | null)[]) {
  return classes.filter(Boolean).join(" ");
}

/* ──────────────────────────────────────────
   Form data types
────────────────────────────────────────── */
interface FormData {
  teamName: string;
  teamSlug: string;
  categories: string[];
  teamGoal: string;
  teamContent: string;
  teamPlan: string;
  teamSize: string;
  otherCategoryDetail: string;
}

function buildDescription(data: FormData): string {
  const catLabels = TEAM_CATEGORIES
    .filter((c) => data.categories.includes(c.value))
    .map((c) => c.label)
    .join("、");
  const parts = [
    `【团队分类】${catLabels || "（未填写）"}`,
    data.categories.includes("other") && data.otherCategoryDetail
      ? `【其他说明】${data.otherCategoryDetail}`
      : null,
    `【团队规模】${TEAM_SIZE_OPTIONS.find((s) => s.value === data.teamSize)?.label || "（未填写）"}`,
    "",
    `【团队目标】${data.teamGoal || "（未填写）"}`,
    "",
    `【内容方向】${data.teamContent || "（未填写）"}`,
    data.teamPlan ? `\n【运营计划】${data.teamPlan}` : null,
  ].filter(Boolean);
  return parts.join("\n");
}

/* ──────────────────────────────────────────
   Page
────────────────────────────────────────── */
export default function TeamApplicationsPage() {
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [application, setApplication] = useState<TeamApplication | null>(null);
  const [fetchError, setFetchError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [showForm, setShowForm] = useState(false);

  const [formData, setFormData] = useState<FormData>({
    teamName: "", teamSlug: "", categories: [], teamGoal: "", teamContent: "", teamPlan: "", teamSize: "", otherCategoryDetail: "",
  });
  const [formErrors, setFormErrors] = useState<Partial<Record<keyof FormData, string>>>({});

  useEffect(() => {
    loadMyApplication();
  }, []);

  async function loadMyApplication() {
    setLoading(true);
    setFetchError("");
    try {
      const data = await communityApi.myTeamApplication();
      setApplication(data);
      setShowForm(!data || data.status === "REJECTED" || data.status === "CANCELLED");
    } catch (err) {
      if (err && typeof err === "object" && "code" in err && (err as { code: number }).code === 401) {
        setFetchError("请先登录后再提交申请");
      } else {
        setFetchError(err instanceof Error ? err.message : "加载失败，请稍后重试");
      }
    } finally {
      setLoading(false);
    }
  }

  function validateForm(): boolean {
    const errors: Partial<Record<keyof FormData, string>> = {};
    if (!formData.teamName.trim()) {
      errors.teamName = "请填写团队名称";
    } else if (formData.teamName.length > 50) {
      errors.teamName = "团队名称不能超过 50 个字符";
    }
    if (!formData.teamSlug.trim()) {
      errors.teamSlug = "请填写团队标识";
    } else if (!/^[a-z0-9]+(?:[-_][a-z0-9]+)*$/.test(formData.teamSlug)) {
      errors.teamSlug = "仅限小写字母、数字、连字符和下划线";
    } else if (formData.teamSlug.length < 3 || formData.teamSlug.length > 50) {
      errors.teamSlug = "标识长度为 3–50 个字符";
    }
    if (formData.categories.length === 0) {
      errors.categories = "请至少选择一个团队分类";
    }
    if (formData.categories.includes("other") && !formData.otherCategoryDetail.trim()) {
      errors.otherCategoryDetail = "请填写其他领域的具体说明";
    } else if (formData.categories.includes("other") && formData.otherCategoryDetail.trim().length < 10) {
      errors.otherCategoryDetail = "其他说明至少需要 10 个字符";
    }
    if (!formData.teamGoal.trim()) {
      errors.teamGoal = "请填写团队目标";
    } else if (formData.teamGoal.trim().length < 20) {
      errors.teamGoal = "团队目标至少需要 20 个字符";
    } else if (formData.teamGoal.length > 300) {
      errors.teamGoal = "团队目标不能超过 300 个字符";
    }
    if (!formData.teamContent.trim()) {
      errors.teamContent = "请填写内容方向";
    } else if (formData.teamContent.trim().length < 20) {
      errors.teamContent = "内容方向至少需要 20 个字符";
    } else if (formData.teamContent.length > 300) {
      errors.teamContent = "内容方向不能超过 300 个字符";
    }
    if (formData.teamPlan.trim() && formData.teamPlan.trim().length < 10) {
      errors.teamPlan = "运营计划至少需要 10 个字符";
    } else if (formData.teamPlan.length > 300) {
      errors.teamPlan = "运营计划不能超过 300 个字符";
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validateForm()) return;
    setSubmitting(true);
    setSubmitError("");
    try {
      const result = await communityApi.submitTeamApplication({
        teamName: formData.teamName.trim(),
        teamSlug: formData.teamSlug.trim(),
        description: buildDescription(formData),
        idempotencyKey: `web-${Date.now()}-${Math.random()}`,
      });
      setApplication(result);
      setShowForm(false);
      setFormData({ teamName: "", teamSlug: "", categories: [], teamGoal: "", teamContent: "", teamPlan: "", teamSize: "", otherCategoryDetail: "" });
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "提交失败，请稍后重试");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancel(id: string) {
    if (!confirm("确定要撤回申请？撤回后可重新提交。")) return;
    setSubmitting(true);
    try {
      await communityApi.cancelTeamApplication(id);
      await loadMyApplication();
    } catch (err) {
      setFetchError(err instanceof Error ? err.message : "撤回失败");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <>
        <UserTopbar title="团队申请" />
        <div className="page-shell team-application-page">
          <div className="app-loading">
            <Loader2 size={28} className="animate-spin" />
            <span>加载中…</span>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <UserTopbar title="团队申请" />
      <div className="page-shell team-application-page">

        {/* ── Hero ── */}
        <section className="ta-hero surface-lg">
          <div className="ta-hero__badge">
            <Rocket size={14} />
            团队空间
          </div>
          <h1>申请建立团队</h1>
          <p>认真填写每一项资料有助于审核人员快速了解你的团队。审核通常在 1–3 个工作日内完成。</p>
          <div className="ta-hero__meta">
            <span><ShieldCheck size={14} /> 审核通过后自动创建团队博客</span>
            <span><Feather size={14} /> 所有字段均为必填</span>
          </div>
        </section>

        {/* ── Global error ── */}
        {fetchError && (
          <div className="app-feedback app-feedback--error" role="alert">
            <AlertCircle size={18} />
            <div>
              <strong>加载失败</strong>
              <p>{fetchError}</p>
            </div>
            <button className="ghost-button" onClick={loadMyApplication}>
              <RefreshCw size={15} /> 重试
            </button>
          </div>
        )}

        {/* ── Application exists ── */}
        {!fetchError && application && (
          <>
            {application.status === "APPROVED" && (
              <section className="ta-card surface-lg">
                <div className="ta-card__header">
                  <div>
                    <h2>审核已通过</h2>
                    <p className="secondary">你的团队已成功创建，可以开始使用了。</p>
                  </div>
                  <StatusPill status={application.status} />
                </div>
                <div className="ta-info-grid">
                  <InfoItem label="团队名称" value={<strong>{application.teamName}</strong>} />
                  <InfoItem label="团队标识" value={<code className="ta-slug">{application.teamSlug}</code>} />
                  {application.reviewComment && <InfoItem label="审核说明" value={application.reviewComment} />}
                </div>
                <div className="ta-success-banner">
                  <CheckCircle2 size={16} />
                  <span>团队「{application.teamName}」已创建完成，快去管理内容吧。</span>
                </div>
                <div className="ta-card__actions">
                  <Link href="/teams" className="primary-button">
                    查看全部团队 <ArrowRight size={15} />
                  </Link>
                </div>
              </section>
            )}

            {application.status === "PENDING" && (
              <section className="ta-card surface-lg">
                <div className="ta-card__header">
                  <div>
                    <h2>申请已提交</h2>
                    <p className="secondary">正在等待平台审核，请耐心等待。</p>
                  </div>
                  <StatusPill status={application.status} />
                </div>
                <StepIndicator current={3} />
                <div className="ta-info-grid">
                  <InfoItem label="团队名称" value={<strong>{application.teamName}</strong>} />
                  <InfoItem label="团队标识" value={<code className="ta-slug">{application.teamSlug}</code>} />
                  {application.description && <InfoItem label="申请说明" value={<ApplicationDescriptionPreview text={application.description} />} />}
                  <InfoItem label="提交时间" value={new Date(application.createdAt).toLocaleString("zh-CN")} />
                </div>
                <div className="ta-card__actions">
                  <button className="ghost-button" onClick={loadMyApplication} disabled={submitting}>
                    <RefreshCw size={15} /> 刷新状态
                  </button>
                  <button className="danger-button" onClick={() => handleCancel(application.id)} disabled={submitting}>
                    {submitting ? "撤回中…" : "撤回申请"}
                  </button>
                </div>
              </section>
            )}

            {(application.status === "REJECTED" || application.status === "CANCELLED") && (
              <>
                <section className="ta-card surface-lg">
                  <div className="ta-card__header">
                    <div>
                      <h2>{application.status === "REJECTED" ? "申请未通过" : "申请已撤回"}</h2>
                      <p className="secondary">
                        {application.status === "REJECTED" ? "审核意见如下，可据此修改后重新提交。" : "你可以重新提交申请。"}
                      </p>
                    </div>
                    <StatusPill status={application.status} />
                  </div>
                  <div className="ta-info-grid">
                    <InfoItem label="团队名称" value={<strong>{application.teamName}</strong>} />
                    <InfoItem label="团队标识" value={<code className="ta-slug">{application.teamSlug}</code>} />
                    {application.description && <InfoItem label="申请说明" value={<ApplicationDescriptionPreview text={application.description} />} />}
                    {application.reviewComment && <InfoItem label="审核说明" value={application.reviewComment} />}
                    {application.reviewedAt && <InfoItem label="审核时间" value={new Date(application.reviewedAt).toLocaleString("zh-CN")} />}
                  </div>
                  <div className="ta-card__actions">
                    <button className="primary-button" onClick={() => setShowForm(true)}>
                      <RotateCcw size={15} /> 重新申请
                    </button>
                  </div>
                </section>
                {showForm && (
                  <ApplicationForm
                    formData={formData}
                    setFormData={setFormData}
                    formErrors={formErrors}
                    handleSubmit={handleSubmit}
                    submitting={submitting}
                    submitError={submitError}
                    setSubmitError={setSubmitError}
                  />
                )}
              </>
            )}
          </>
        )}

        {/* ── No application yet ── */}
        {!fetchError && !application && (
          showForm ? (
            <ApplicationForm
              formData={formData}
              setFormData={setFormData}
              formErrors={formErrors}
              handleSubmit={handleSubmit}
              submitting={submitting}
              submitError={submitError}
              setSubmitError={setSubmitError}
            />
          ) : (
            <section className="ta-empty surface-lg">
              <div className="ta-empty__icon">
                <Rocket size={26} />
              </div>
              <h2>还没有提交过申请</h2>
              <p className="secondary">创建一个属于你的团队博客，与社区成员共同创作内容。</p>
              <button className="primary-button" onClick={() => setShowForm(true)}>
                立即申请 <ArrowRight size={15} />
              </button>
            </section>
          )
        )}
      </div>

      <style jsx>{`
        /* Loading */
        .app-loading {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 12px;
          min-height: 50vh;
          color: var(--text-secondary);
        }

        /* Hero */
        .ta-hero {
          padding: 40px 36px 32px;
          margin-bottom: 24px;
        }
        .ta-hero__badge {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 4px 12px;
          border-radius: 100px;
          background: color-mix(in srgb, var(--primary) 10%, transparent);
          color: var(--primary);
          font-size: 12px;
          font-weight: 600;
          letter-spacing: .06em;
          margin-bottom: 16px;
        }
        .ta-hero h1 {
          font-size: 26px;
          font-weight: 700;
          margin: 0 0 10px;
          letter-spacing: -.02em;
        }
        .ta-hero p {
          color: var(--text-secondary);
          margin: 0 0 16px;
          max-width: 560px;
          line-height: 1.7;
        }
        .ta-hero__meta {
          display: flex;
          flex-wrap: wrap;
          gap: 8px 24px;
        }
        .ta-hero__meta span {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          color: var(--text-tertiary);
          font-size: 13px;
        }

        /* Feedback */
        .app-feedback {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          padding: 14px 16px;
          border-radius: 10px;
          margin-bottom: 20px;
          border: 1px solid;
        }
        .app-feedback--error {
          background: color-mix(in srgb, var(--danger) 5%, transparent);
          border-color: color-mix(in srgb, var(--danger) 20%, transparent);
          color: var(--danger);
        }
        .app-feedback > div { flex: 1; }
        .app-feedback strong { display: block; font-size: 14px; font-weight: 600; margin-bottom: 2px; }
        .app-feedback p { margin: 0; font-size: 13px; opacity: .85; }

        /* Card */
        .ta-card { padding: 28px; margin-bottom: 20px; }
        .ta-card__header {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          gap: 16px;
          margin-bottom: 24px;
        }
        .ta-card__header h2 { font-size: 17px; font-weight: 600; margin: 0 0 4px; }
        .ta-card__header p { margin: 0; font-size: 14px; }
        .ta-card__actions { display: flex; gap: 10px; margin-top: 24px; }

        /* Info grid */
        .ta-info-grid { display: grid; gap: 0; }
        .ta-info-item {
          display: flex;
          align-items: baseline;
          gap: 12px;
          padding: 12px 0;
          border-bottom: 1px solid var(--border-default);
          font-size: 14px;
        }
        .ta-info-item:last-child { border-bottom: none; }
        .ta-info-item__label {
          flex-shrink: 0;
          width: 80px;
          color: var(--text-tertiary);
          font-size: 13px;
        }
        .ta-info-item__value { color: var(--text-primary); flex: 1; }
        .ta-info-item__value strong { color: var(--text-primary); }
        .ta-slug {
          font-family: "SF Mono", Consolas, monospace;
          font-size: 13px;
          background: var(--bg-muted);
          padding: 2px 7px;
          border-radius: 5px;
          color: var(--primary);
        }

        /* Description preview in review card */
        .ta-desc-preview {
          font-size: 13px;
          line-height: 1.8;
          white-space: pre-line;
          color: var(--text-secondary);
          max-height: 240px;
          overflow-y: auto;
        }

        /* Step indicator */
        .step-indicator { display: flex; align-items: center; margin-bottom: 28px; }
        .step-indicator__item { display: flex; align-items: center; }
        .step-indicator__dot {
          width: 28px; height: 28px; border-radius: 50%;
          border: 2px solid var(--border-default);
          background: var(--bg-surface);
          display: flex; align-items: center; justify-content: center;
          font-size: 12px; font-weight: 600;
          color: var(--text-tertiary);
          flex-shrink: 0;
          transition: all 200ms ease;
        }
        .step-indicator__label { font-size: 13px; color: var(--text-tertiary); white-space: nowrap; transition: color 200ms ease; }
        .step-indicator__line { flex: 1; height: 2px; background: var(--border-default); margin: 0 12px; min-width: 40px; transition: background 200ms ease; }
        .step-indicator__item.is-active .step-indicator__dot { border-color: var(--primary); background: var(--primary); color: #fff; }
        .step-indicator__item.is-active .step-indicator__label { color: var(--primary); font-weight: 500; }
        .step-indicator__item.is-done .step-indicator__dot { border-color: var(--success); background: var(--success); color: #fff; }
        .step-indicator__item.is-done + .step-indicator__item .step-indicator__line { background: var(--success); }

        /* Success banner */
        .ta-success-banner {
          display: flex; align-items: center; gap: 10px;
          padding: 14px 16px; border-radius: 10px;
          background: color-mix(in srgb, var(--success) 7%, transparent);
          border: 1px solid color-mix(in srgb, var(--success) 18%, transparent);
          color: var(--success);
          font-size: 14px; margin-top: 20px;
        }

        /* Empty */
        .ta-empty { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 64px 32px; }
        .ta-empty__icon {
          width: 64px; height: 64px; border-radius: 50%;
          background: color-mix(in srgb, var(--primary) 9%, transparent);
          color: var(--primary);
          display: flex; align-items: center; justify-content: center;
          margin-bottom: 20px;
        }
        .ta-empty h2 { font-size: 18px; font-weight: 600; margin: 0 0 8px; }
        .ta-empty p { margin: 0 0 24px; max-width: 380px; }

        /* ── Form ── */
        .ta-form { padding: 28px; }

        /* Section headings */
        .ta-form-section {
          margin-bottom: 24px;
        }
        .ta-form-section + .ta-form-section {
          margin-top: 28px;
          padding-top: 28px;
          border-top: 1px solid var(--border-default);
        }
        .ta-form-section__title {
          display: flex; align-items: center; gap: 8px;
          font-size: 13px; font-weight: 600;
          color: var(--text-tertiary);
          letter-spacing: .06em;
          text-transform: uppercase;
          margin-bottom: 16px;
        }
        .ta-form-section__title::after {
          content: "";
          flex: 1;
          height: 1px;
          background: var(--border-default);
        }

        /* Field */
        .ta-field { margin-bottom: 18px; }
        .ta-field__label {
          display: flex; align-items: center; gap: 4px;
          font-size: 14px; font-weight: 500;
          margin-bottom: 7px;
          color: var(--text-primary);
        }
        .ta-field__label .req { color: var(--danger); font-size: 13px; }
        .ta-field__label .hint {
          font-size: 12px; font-weight: 400;
          color: var(--text-tertiary); margin-left: auto;
        }
        .ta-field__label .hint .char-count { font-variant-numeric: tabular-nums; }
        .ta-field__label .hint.warn { color: var(--warning); }
        .ta-field__label .hint.danger { color: var(--danger); }

        /* Text input */
        .ta-input {
          width: 100%;
          border: 1.5px solid var(--border-default);
          border-radius: 9px;
          background: var(--bg-surface);
          padding: 10px 13px;
          font-size: 14px;
          color: var(--text-primary);
          transition: border-color 140ms ease, box-shadow 140ms ease;
          outline: none;
          line-height: 1.5;
        }
        .ta-input::placeholder { color: var(--text-disabled); }
        .ta-input:focus {
          border-color: var(--primary);
          box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary) 14%, transparent);
        }
        .ta-input.has-error {
          border-color: var(--danger);
        }
        .ta-input.has-error:focus {
          box-shadow: 0 0 0 3px color-mix(in srgb, var(--danger) 14%, transparent);
        }
        .ta-input.slug-input {
          font-family: "SF Mono", Consolas, monospace;
          letter-spacing: .02em;
        }

        /* Textarea */
        .ta-textarea {
          width: 100%;
          border: 1.5px solid var(--border-default);
          border-radius: 9px;
          background: var(--bg-surface);
          padding: 10px 13px;
          font-size: 14px;
          color: var(--text-primary);
          transition: border-color 140ms ease, box-shadow 140ms ease;
          outline: none;
          resize: vertical;
          min-height: 88px;
          line-height: 1.7;
        }
        .ta-textarea::placeholder { color: var(--text-disabled); }
        .ta-textarea:focus {
          border-color: var(--primary);
          box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary) 14%, transparent);
        }
        .ta-textarea.has-error {
          border-color: var(--danger);
        }
        .ta-textarea.has-error:focus {
          box-shadow: 0 0 0 3px color-mix(in srgb, var(--danger) 14%, transparent);
        }

        /* Error message */
        .ta-field__error {
          display: flex; align-items: center; gap: 5px;
          margin-top: 6px;
          font-size: 12px;
          color: var(--danger);
        }

        /* Category grid */
        .ta-category-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
          gap: 8px;
        }
        .ta-category-option {
          display: flex;
          flex-direction: column;
          gap: 3px;
          padding: 10px 12px;
          border: 1.5px solid var(--border-default);
          border-radius: 9px;
          cursor: pointer;
          transition: border-color 140ms ease, background 140ms ease;
          background: var(--bg-surface);
          position: relative;
        }
        .ta-category-option:hover {
          border-color: var(--primary);
          background: color-mix(in srgb, var(--primary) 4%, var(--bg-surface));
        }
        .ta-category-option.selected {
          border-color: var(--primary);
          background: color-mix(in srgb, var(--primary) 7%, var(--bg-surface));
        }
        .ta-category-option__name {
          font-size: 13px; font-weight: 500;
          color: var(--text-primary);
        }
        .ta-category-option.selected .ta-category-option__name {
          color: var(--primary);
        }
        .ta-category-option__desc {
          font-size: 11px;
          color: var(--text-tertiary);
          line-height: 1.4;
        }
        .ta-category-check {
          position: absolute;
          top: 8px; right: 8px;
          color: var(--primary);
        }
        .ta-optional-label {
          font-size: 12px;
          font-weight: 400;
          color: var(--text-tertiary);
          margin-left: 2px;
        }

        /* Size grid */
        .ta-size-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
          gap: 8px;
        }
        .ta-size-option {
          display: flex; flex-direction: column; gap: 3px;
          padding: 10px 12px;
          border: 1.5px solid var(--border-default);
          border-radius: 9px;
          cursor: pointer;
          transition: border-color 140ms ease, background 140ms ease;
          background: var(--bg-surface);
          position: relative;
        }
        .ta-size-option:hover {
          border-color: var(--primary);
          background: color-mix(in srgb, var(--primary) 4%, var(--bg-surface));
        }
        .ta-size-option.selected {
          border-color: var(--primary);
          background: color-mix(in srgb, var(--primary) 7%, var(--bg-surface));
        }
        .ta-size-option__label {
          font-size: 13px; font-weight: 500;
          color: var(--text-primary);
        }
        .ta-size-option.selected .ta-size-option__label { color: var(--primary); }
        .ta-size-option__desc { font-size: 11px; color: var(--text-tertiary); }
        .ta-size-option .ta-radio-dot {
          position: absolute; top: 8px; right: 8px;
          width: 14px; height: 14px; border-radius: 50%;
          border: 1.5px solid var(--border-default);
          background: var(--bg-surface);
          transition: border-color 140ms, background 140ms;
        }
        .ta-size-option.selected .ta-radio-dot {
          border-color: var(--primary);
          background: var(--primary);
          box-shadow: inset 0 0 0 3px var(--bg-surface);
        }

        /* Notice */
        .ta-form__notice {
          padding: 12px 14px;
          border-radius: 9px;
          background: var(--bg-muted);
          border: 1px solid var(--border-default);
          font-size: 13px;
          color: var(--text-secondary);
          margin-bottom: 24px;
          display: flex;
          gap: 8px;
          align-items: flex-start;
          line-height: 1.6;
        }
        .ta-form__notice-icon { flex-shrink: 0; margin-top: 1px; color: var(--info); }

        /* Form actions */
        .ta-form__actions { display: flex; gap: 10px; align-items: center; }

        /* Danger button */
        .danger-button {
          display: inline-flex; min-height: 40px; align-items: center; justify-content: center; gap: 8px;
          border-radius: 8px; padding: 0 16px; cursor: pointer; font-weight: 500; font-size: 14px;
          border: 1px solid var(--border-default); background: transparent; color: var(--danger);
          transition: background 180ms, border-color 180ms;
        }
        .danger-button:hover { background: color-mix(in srgb, var(--danger) 8%, transparent); border-color: var(--danger); }
        .danger-button:disabled { opacity: .5; cursor: not-allowed; }
      `}</style>
    </>
  );
}

/* ──────────────────────────────────────────
   InfoItem helper
────────────────────────────────────────── */
function InfoItem({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="ta-info-item">
      <span className="ta-info-item__label">{label}</span>
      <span className="ta-info-item__value">{value}</span>
    </div>
  );
}

/* ──────────────────────────────────────────
   Description preview (parses structured text)
────────────────────────────────────────── */
function ApplicationDescriptionPreview({ text }: { text: string }) {
  const lines = text.split("\n");
  return (
    <div className="ta-desc-preview">
      {lines.map((line, i) => (
        <div key={i} style={line.startsWith("【") ? { color: "var(--text-tertiary)", marginTop: 8 } : {}}>
          {line || <br />}
        </div>
      ))}
    </div>
  );
}

/* ──────────────────────────────────────────
   Application Form
────────────────────────────────────────── */
function ApplicationForm({
  formData, setFormData, formErrors, handleSubmit, submitting, submitError, setSubmitError,
}: {
  formData: FormData;
  setFormData: (d: FormData) => void;
  formErrors: Partial<Record<keyof FormData, string>>;
  handleSubmit: (e: React.FormEvent) => Promise<void>;
  submitting: boolean;
  submitError: string;
  setSubmitError: (e: string) => void;
}) {
  function set<K extends keyof FormData>(key: K, value: FormData[K]) {
    setFormData({ ...formData, [key]: value });
  }

  function charCount(length: number, min: number, max: number) {
    const remaining = max - length;
    const pct = length / max;
    if (pct >= 1) return { text: `${length}/${max}`, cls: "danger" };
    if (remaining <= 30) return { text: `${length}/${max}`, cls: "warn" };
    return { text: `${length}/${max}`, cls: "" };
  }

  const goalCount = charCount(formData.teamGoal.length, 20, 300);
  const contentCount = charCount(formData.teamContent.length, 20, 300);
  const planCount = charCount(formData.teamPlan.length, 20, 300);

  return (
    <section className="ta-form surface-lg">
      <form onSubmit={handleSubmit} noValidate>

        {/* ── 基础信息 ── */}
        <div className="ta-form-section">
          <div className="ta-form-section__title"><Info size={13} /> 基础信息</div>

          <div className="ta-field">
            <label className="ta-field__label">
              团队名称 <span className="req">*</span>
            </label>
            <input
              type="text"
              value={formData.teamName}
              onChange={(e) => set("teamName", e.target.value)}
              className={cn("ta-input", formErrors.teamName && "has-error")}
              placeholder="例如：前端技术小组、AI 实践圈"
              maxLength={50}
            />
            {formErrors.teamName && (
              <p className="ta-field__error"><AlertCircle size={12} />{formErrors.teamName}</p>
            )}
          </div>

          <div className="ta-field">
            <label className="ta-field__label">
              团队标识 <span className="req">*</span>
            </label>
            <input
              type="text"
              value={formData.teamSlug}
              onChange={(e) => set("teamSlug", e.target.value.toLowerCase())}
              className={cn("ta-input slug-input", formErrors.teamSlug && "has-error")}
              placeholder="frontend-team"
              maxLength={50}
            />
            {formErrors.teamSlug ? (
              <p className="ta-field__error"><AlertCircle size={12} />{formErrors.teamSlug}</p>
            ) : (
              <p className="ta-field__error" style={{ color: "var(--text-tertiary)" }}>
                将作为团队博客 URL 的一部分，例如：pxczxn.com/teams/<strong>frontend-team</strong>
              </p>
            )}
          </div>
        </div>

        {/* ── 团队定位 ── */}
        <div className="ta-form-section">
          <div className="ta-form-section__title"><LayoutList size={13} /> 团队定位</div>

          <div className="ta-field">
            <label className="ta-field__label">
              团队分类 <span className="req">*</span>
            </label>
              <div className="ta-category-grid" role="group" aria-label="团队分类（可多选）">
              {formErrors.categories && (
                <p className="ta-field__error" style={{ gridColumn: "1 / -1" }}>
                  <AlertCircle size={12} />{formErrors.categories}
                </p>
              )}
              {TEAM_CATEGORIES.map((cat) => {
                const selected = formData.categories.includes(cat.value);
                return (
                  <button
                    key={cat.value}
                    type="button"
                    aria-pressed={selected}
                    className={cn("ta-category-option", selected && "selected")}
                    onClick={() => {
                      const next = selected
                        ? formData.categories.filter((c) => c !== cat.value)
                        : [...formData.categories, cat.value];
                      setFormData({ ...formData, categories: next });
                    }}
                  >
                    {selected && (
                      <CheckCircle2 size={14} className="ta-category-check" />
                    )}
                    <span className="ta-category-option__name">{cat.label}</span>
                    <span className="ta-category-option__desc">{cat.desc}</span>
                  </button>
                );
              })}
            </div>

            {formData.categories.includes("other") && (
              <div className="ta-field" style={{ marginTop: 14 }}>
                <label className="ta-field__label">
                  其他领域说明 <span className="req">*</span>
                  <span className={cn("hint", formData.otherCategoryDetail.length > 200 ? "warn" : "")}>
                    <span className="char-count">{formData.otherCategoryDetail.length}/200</span>
                  </span>
                </label>
                <textarea
                  value={formData.otherCategoryDetail}
                  onChange={(e) => setFormData({ ...formData, otherCategoryDetail: e.target.value })}
                  className={cn("ta-textarea", formErrors.otherCategoryDetail && "has-error")}
                  placeholder="请具体说明团队的技术领域方向，例如：游戏开发、音视频技术、区块链等"
                  maxLength={200}
                  rows={2}
                />
                {formErrors.otherCategoryDetail ? (
                  <p className="ta-field__error"><AlertCircle size={12} />{formErrors.otherCategoryDetail}</p>
                ) : (
                  <p className="ta-field__error" style={{ color: "var(--text-tertiary)", fontSize: "11px" }}>
                    至少 10 个字符。请明确说明团队所属的具体技术领域。
                  </p>
                )}
              </div>
            )}
          </div>

          <div className="ta-field">
            <label className="ta-field__label">
              预计规模 <span className="req">*</span>
            </label>
            <div className="ta-size-grid" role="radiogroup" aria-label="团队规模">
              {TEAM_SIZE_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  role="radio"
                  aria-checked={formData.teamSize === opt.value}
                  className={cn("ta-size-option", formData.teamSize === opt.value && "selected")}
                  onClick={() => set("teamSize", opt.value)}
                >
                  <div className="ta-radio-dot" />
                  <span className="ta-size-option__label">{opt.label}</span>
                  <span className="ta-size-option__desc">{opt.desc}</span>
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* ── 申请说明 ── */}
        <div className="ta-form-section">
          <div className="ta-form-section__title"><Feather size={13} /> 申请说明</div>

          <div className="ta-field">
            <label className="ta-field__label">
              团队目标 <span className="req">*</span>
              <span className={cn("hint", goalCount.cls)}>
                <span className="char-count">{goalCount.text}</span>
              </span>
            </label>
            <textarea
              value={formData.teamGoal}
              onChange={(e) => set("teamGoal", e.target.value)}
              className={cn("ta-textarea", formErrors.teamGoal && "has-error")}
              placeholder="例如：凝聚前端技术爱好者，分享工程实践、推动团队成员共同成长，致力于打造高质量的技术沉淀平台"
              maxLength={300}
              rows={3}
            />
            {formErrors.teamGoal ? (
              <p className="ta-field__error"><AlertCircle size={12} />{formErrors.teamGoal}</p>
            ) : (
              <p className="ta-field__error" style={{ color: "var(--text-tertiary)", fontSize: "11px" }}>
                至少 20 字符。说明团队的核心宗旨与长期愿景。
              </p>
            )}
          </div>

          <div className="ta-field">
            <label className="ta-field__label">
              内容方向 <span className="req">*</span>
              <span className={cn("hint", contentCount.cls)}>
                <span className="char-count">{contentCount.text}</span>
              </span>
            </label>
            <textarea
              value={formData.teamContent}
              onChange={(e) => set("teamContent", e.target.value)}
              className={cn("ta-textarea", formErrors.teamContent && "has-error")}
              placeholder="例如：聚焦 React 生态与性能优化，持续输出组件设计、性能监控、工程化方向的深度技术文章"
              maxLength={300}
              rows={3}
            />
            {formErrors.teamContent ? (
              <p className="ta-field__error"><AlertCircle size={12} />{formErrors.teamContent}</p>
            ) : (
              <p className="ta-field__error" style={{ color: "var(--text-tertiary)", fontSize: "11px" }}>
                至少 20 字符。描述团队将持续输出的主题方向与内容类型。
              </p>
            )}
          </div>

          <div className="ta-field">
            <label className="ta-field__label">
              运营计划 <span className="ta-optional-label">（选填）</span>
              <span className={cn("hint", planCount.cls)}>
                <span className="char-count">{planCount.text}</span>
              </span>
            </label>
            <textarea
              value={formData.teamPlan}
              onChange={(e) => set("teamPlan", e.target.value)}
              className={cn("ta-textarea", formErrors.teamPlan && "has-error")}
              placeholder="例如：每周至少发布 2 篇技术文章；每月组织一次线上技术交流；每季度产出一份团队技术报告"
              maxLength={300}
              rows={3}
            />
            {formErrors.teamPlan ? (
              <p className="ta-field__error"><AlertCircle size={12} />{formErrors.teamPlan}</p>
            ) : (
              <p className="ta-field__error" style={{ color: "var(--text-tertiary)", fontSize: "11px" }}>
                至少 20 字符。说明团队内容更新频率与协作机制。
              </p>
            )}
          </div>
        </div>

        {/* ── Notice ── */}
        <div className="ta-form__notice">
          <Info size={15} className="ta-form__notice-icon" />
          <span>
            提交后需等待平台审核，审核通过后将自动创建团队博客、成员关系和默认设置。如审核未通过，可根据意见修改后重新提交。
          </span>
        </div>

        {/* ── Submit error ── */}
        {submitError && (
          <div className="app-feedback app-feedback--error" style={{ marginBottom: 16 }} role="alert">
            <AlertCircle size={16} />
            <div>
              <strong>提交失败</strong>
              <p>{submitError}</p>
            </div>
            <button type="button" className="ghost-button" style={{ minHeight: 32, padding: "0 12px" }} onClick={() => setSubmitError("")}>
              ×
            </button>
          </div>
        )}

        {/* ── Actions ── */}
        <div className="ta-form__actions">
          <button type="submit" disabled={submitting} className="primary-button">
            {submitting ? (
              <><Loader2 size={15} className="animate-spin" /> 提交中…</>
            ) : (
              <>提交申请 <ArrowRight size={15} /></>
            )}
          </button>
        </div>
      </form>
    </section>
  );
}
