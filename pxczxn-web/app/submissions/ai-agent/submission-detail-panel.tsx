"use client";

import Link from "next/link";
import { LoaderCircle, RotateCcw } from "lucide-react";
import { useEffect, useState } from "react";
import { UserTopbar } from "../../components/prototype-ui";
import {
  communityApi,
  type ArticleEditor,
  type ArticleReviewStatus,
} from "../../lib/community-api";

const copy = {
  title: "\u6295\u7a3f\u4e2d\u5fc3",
  selectTitle: "\u8bf7\u9009\u62e9\u6295\u7a3f",
  selectDescription: "\u4ece\u6587\u7ae0\u7f16\u8f91\u5668\u63d0\u4ea4\u540e\uff0c\u53ef\u643a\u5e26 articleId \u8bbf\u95ee\u672c\u9875\u67e5\u770b\u771f\u5b9e\u5ba1\u6838\u8fdb\u5ea6\u3002",
  openEditor: "\u6253\u5f00\u7f16\u8f91\u5668",
  loadFailed: "\u5ba1\u6838\u72b6\u6001\u52a0\u8f7d\u5931\u8d25",
  withdrawFailed: "\u64a4\u56de\u5931\u8d25",
  withdrawn: "\u6295\u7a3f\u5df2\u64a4\u56de\u3002",
  untitled: "\u672a\u547d\u540d\u6587\u7ae0",
  status: "\u5ba1\u6838\u72b6\u6001",
  submittedAt: "\u63d0\u4ea4\u65f6\u95f4",
  noSubmission: "\u6682\u65e0\u63d0\u4ea4\u8bb0\u5f55",
  reviewNotes: "\u5ba1\u6838\u610f\u89c1",
  noReviewNotes: "\u6682\u65e0\u5ba1\u6838\u610f\u89c1",
  summary: "\u6458\u8981",
  noSummary: "\u6682\u65e0\u6458\u8981",
  edit: "\u7f16\u8f91\u6587\u7ae0",
  withdraw: "\u64a4\u56de\u6295\u7a3f",
  notFound: "\u672a\u627e\u5230\u8be5\u6295\u7a3f\u6216\u65e0\u6743\u8bbf\u95ee\u3002",
};

export function SubmissionDetailPanel() {
  const [articleId, setArticleId] = useState<string | null>(null);
  const [editor, setEditor] = useState<ArticleEditor | null>(null);
  const [status, setStatus] = useState<ArticleReviewStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const id = new URLSearchParams(window.location.search).get("articleId");
    setArticleId(id);
    if (!id) return;

    let active = true;
    setLoading(true);
    void Promise.all([communityApi.editor(id), communityApi.reviewStatus(id)])
      .then(([nextEditor, nextStatus]) => {
        if (!active) return;
        setEditor(nextEditor);
        setStatus(nextStatus);
      })
      .catch((cause: unknown) => {
        if (active) setMessage(cause instanceof Error ? cause.message : copy.loadFailed);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  async function withdraw() {
    if (!articleId || !status) return;
    setLoading(true);
    setMessage("");
    try {
      const next = await communityApi.withdrawReview(
        articleId,
        status.lockVersion,
        copy.withdraw,
      );
      setStatus(next);
      setEditor(await communityApi.editor(articleId));
      setMessage(copy.withdrawn);
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : copy.withdrawFailed);
    } finally {
      setLoading(false);
    }
  }

  if (articleId === null) {
    return (
      <>
        <UserTopbar title={copy.title} />
        <main className="page-shell" style={{ paddingTop: 32 }}>
          <section className="surface stack" style={{ padding: 24 }}>
            <h1>{copy.selectTitle}</h1>
            <p className="secondary">{copy.selectDescription}</p>
            <Link className="primary-button" href="/editor/new">
              {copy.openEditor}
            </Link>
          </section>
        </main>
      </>
    );
  }

  const canWithdraw = Boolean(
    status && ["PENDING_REVIEW", "AUTO_REVIEWING", "MANUAL_REVIEWING"].includes(status.reviewStatus),
  );

  return (
    <>
      <UserTopbar title={copy.title} />
      <main className="page-shell stack" style={{ paddingTop: 32, paddingBottom: 48 }}>
        {loading && <LoaderCircle className="spin" />}
        {message && <p role="status" className="inline-feedback">{message}</p>}
        {editor && status && (
          <>
            <header>
              <h1>{editor.title || copy.untitled}</h1>
              <p className="secondary">{copy.status}: {status.reviewStatus}</p>
            </header>
            <section className="surface stack" style={{ padding: 20 }}>
              <p>{copy.submittedAt}: {status.latestTask?.submittedAt || copy.noSubmission}</p>
              <p>{copy.reviewNotes}: {status.latestTask?.resultReason || copy.noReviewNotes}</p>
              <p>{copy.summary}: {editor.summary || copy.noSummary}</p>
              <div>
                <Link className="secondary-button" href={`/editor/${articleId}`}>
                  {copy.edit}
                </Link>
                <button
                  className="ghost-button"
                  disabled={loading || !canWithdraw}
                  onClick={() => void withdraw()}
                  type="button"
                >
                  <RotateCcw size={16} /> {copy.withdraw}
                </button>
              </div>
            </section>
          </>
        )}
        {!loading && !editor && (
          <p role="alert" className="inline-feedback error">
            {message || copy.notFound}
          </p>
        )}
      </main>
    </>
  );
}
