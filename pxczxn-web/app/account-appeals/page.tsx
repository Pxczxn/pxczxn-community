"use client";

import { Loader2, Paperclip, Send, ShieldAlert, X } from "lucide-react";
import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import { UserTopbar } from "../components/prototype-ui";
import { AccountEnforcementCase, CommunityFile, communityApi } from "../lib/community-api";

const measureLabels: Record<string, string> = {
  LONG_FREEZE: "长期冻结",
  DATA_CLEANUP: "数据清理",
  ACCOUNT_DELETE: "账号删除",
};

const statusLabels: Record<string, string> = {
  SUBMITTED: "待审核", UNDER_REVIEW: "审核中", APPROVED: "已批准", ACTIVE: "生效中",
  APPEAL_WINDOW: "申诉期内", APPEALED: "已提交申诉", REJECTED: "已驳回", REVOKED: "已撤销", FINALIZED: "已完成",
};

const reasonLabels: Record<string, string> = {
  POLICY_VIOLATION: "违反社区规范",
  SPAM_OR_BOT: "垃圾信息或异常自动化行为",
  HARASSMENT: "骚扰、辱骂或不当言论",
  COPYRIGHT: "侵犯知识产权",
  FRAUD: "欺诈或误导行为",
  SECURITY_RISK: "账号安全风险",
  OTHER: "其他违规情形",
};

function formatDateTime(value?: string | null) {
  return value ? new Date(value).toLocaleString("zh-CN", { dateStyle: "medium", timeStyle: "short" }) : "以平台通知为准";
}

export default function AccountAppealsPage() {
  const [cases, setCases] = useState<AccountEnforcementCase[]>([]);
  const [selected, setSelected] = useState("");
  const [statement, setStatement] = useState("");
  const [evidenceFiles, setEvidenceFiles] = useState<CommunityFile[]>([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const selectedCase = cases.find((item) => item.id === selected);

  useEffect(() => {
    communityApi.myAccountEnforcements()
      .then(setCases)
      .catch((error) => setMessage(error instanceof Error ? error.message : "加载失败"))
      .finally(() => setLoading(false));
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!selected || !statement.trim()) {
      setMessage("请选择措施并填写申诉说明");
      return;
    }
    setSubmitting(true);
    try {
      await communityApi.appealAccountEnforcement(selected, {
        statement: statement.trim(),
        evidenceFileIds: evidenceFiles.map((file) => file.fileId),
      });
      setMessage("申诉已提交，平台会按权限逐级复核");
      setStatement("");
      setEvidenceFiles([]);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "提交失败");
    } finally {
      setSubmitting(false);
    }
  }

  async function uploadEvidence(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files || []);
    event.target.value = "";
    if (!files.length) return;
    if (evidenceFiles.length + files.length > 10) {
      setMessage("最多上传 10 个申诉附件");
      return;
    }
    setSubmitting(true);
    try {
      const uploaded = await Promise.all(files.map((file) => communityApi.uploadFile(file)));
      setEvidenceFiles((current) => [...current, ...uploaded]);
      setMessage("");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "附件上传失败");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <UserTopbar title="账号措施与申诉" />
      <main className="appeals-container page-shell">
        <p className="inline-feedback error" role="status">
          <ShieldAlert size={16} /> 账号已冻结，暂时无法使用社区功能。您可以在此提交申诉。
        </p>
        <section className="card-surface account-appeals-card">
          <div className="page-header-box">
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <ShieldAlert size={24} style={{ color: "var(--primary)" }} />
              <h1>账号措施与申诉</h1>
            </div>
            <p>您可以查看对本人账号生效的强制措施，并在申诉窗口期内提交说明与补充证据。</p>
          </div>

          {message && <p className="notice" role="status" style={{ marginTop: 8 }}>{message}</p>}

          <div className="account-appeals-workspace">
            <section className="account-appeals-list">
              <h2>可申诉的账号措施</h2>
              {loading ? <p className="secondary"><Loader2 className="spin" size={16} />正在加载数据…</p>
                : cases.length === 0 ? <p className="secondary account-appeals-empty">当前没有可申诉的账号措施。</p>
                  : cases.map((item) => (
                    <button className={`measure-card ${selected === item.id ? "is-selected" : ""}`} key={item.id} onClick={() => setSelected(item.id)} type="button">
                      <span className="measure-card-heading">
                        <span className="measure-type-tag">{measureLabels[item.measureType] || item.measureType}</span>
                        <span className="measure-card-state">{statusLabels[item.status] || item.status}</span>
                      </span>
                      <span className="measure-card-details">
                        <span><b>处罚原因</b>{reasonLabels[item.reasonCode || ""] || item.reasonCode || "平台处置"}</span>
                        <span><b>处理说明</b>{item.userVisibleReason}</span>
                        <span><b>申诉截止</b>{formatDateTime(item.appealDeadlineAt || item.executeAfter || item.expiresAt)}</span>
                      </span>
                    </button>
                  ))}
            </section>
            <form className="account-appeals-form" onSubmit={submit}>
              <h2>提交申诉</h2>
              <p className="secondary">选择一项措施后，填写申诉说明和补充材料。</p>
            <label style={{ display: "grid", gap: 6 }}>
              <span style={{ fontSize: 14, fontWeight: 500 }}>所选措施</span>
              <input className="field" readOnly value={selectedCase ? `${measureLabels[selectedCase.measureType] || selectedCase.measureType}：${reasonLabels[selectedCase.reasonCode || ""] || selectedCase.reasonCode || selectedCase.userVisibleReason}` : "请在左侧选择需要申诉的措施"} />
            </label>

            <label style={{ display: "grid", gap: 6 }}>
              <span style={{ fontSize: 14, fontWeight: 500 }}>申诉说明 *</span>
              <textarea
                className="field textarea"
                maxLength={2000}
                onChange={(event) => setStatement(event.target.value)}
                placeholder="请详细说明您的申诉理由及情况描述"
                required
                value={statement}
              />
            </label>

            <div className="appeal-evidence-upload">
              <span className="appeal-evidence-upload__label">补充证据（可选）</span>
              <label className="appeal-evidence-upload__trigger">
                <Paperclip size={16} />
                <span>上传图片或文件</span>
                <input accept="image/*,.pdf,.doc,.docx,.txt" disabled={submitting} multiple onChange={uploadEvidence} type="file" />
              </label>
              <span className="appeal-evidence-upload__hint">支持图片、PDF、Word 和文本文件，单个文件不超过 20MB。</span>
              {evidenceFiles.length > 0 && <ul className="appeal-evidence-upload__files">
                {evidenceFiles.map((file) => <li key={file.fileId}>
                  <span>{file.originalName}</span>
                  <button aria-label={`移除 ${file.originalName}`} disabled={submitting} onClick={() => setEvidenceFiles((current) => current.filter((item) => item.fileId !== file.fileId))} title="移除附件" type="button"><X size={14} /></button>
                </li>)}
              </ul>}
            </div>

            <button className="primary-button" disabled={submitting || !selected} type="submit">
              {submitting ? <Loader2 className="spin" size={16} /> : <Send size={16} />}
              <span>提交申诉</span>
            </button>
            </form>
          </div>
        </section>
      </main>
    </>
  );
}
