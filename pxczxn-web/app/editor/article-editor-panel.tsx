"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  AlertCircle,
  Bold,
  CheckCircle2,
  ChevronLeft,
  Clock3,
  Code2,
  Eye,
  FileText,
  Heading1,
  Heading2,
  Italic,
  Link2,
  List,
  ListOrdered,
  LoaderCircle,
  Quote,
  Save,
  Send,
  Sparkles,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { UserTopbar } from "../components/prototype-ui";
import {
  ArticleEditor,
  BlogCategory,
  PlatformTag,
  communityApi,
  readSession,
} from "../lib/community-api";

type ContentMode = "RICH_TEXT" | "MARKDOWN";

interface EditorForm {
  title: string;
  slug: string;
  summary: string;
  contentMode: ContentMode;
  content: string;
  categoryId: string;
  visibility: string;
  publishMethod: string;
  tagIds: string[];
}

const emptyForm: EditorForm = {
  title: "",
  slug: "",
  summary: "",
  contentMode: "RICH_TEXT",
  content: "",
  categoryId: "",
  visibility: "PUBLIC",
  publishMethod: "MANUAL",
  tagIds: [],
};

export function ArticleEditorPanel({ articleId: initialId }: { articleId?: string }) {
  const [articleId, setArticleId] = useState(initialId || "");
  const [editor, setEditor] = useState<ArticleEditor | null>(null);
  const [form, setForm] = useState<EditorForm>(emptyForm);
  const [categories, setCategories] = useState<BlogCategory[]>([]);
  const [tags, setTags] = useState<PlatformTag[]>([]);
  const [loading, setLoading] = useState(Boolean(initialId));
  const [saving, setSaving] = useState(false);
  const [reviewing, setReviewing] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [preview, setPreview] = useState(false);
  const contentRef = useRef<HTMLTextAreaElement>(null);
  const [dirty, setDirty] = useState(false);
  const [notice, setNotice] = useState<{
    tone: "error" | "success" | "info";
    text: string;
  } | null>(null);
  const formRef = useRef(form);
  const editorRef = useRef(editor);
  const router = useRouter();

  useEffect(() => {
    formRef.current = form;
  }, [form]);
  useEffect(() => {
    editorRef.current = editor;
  }, [editor]);

  // 返回按钮：优先回"上次进来的地方"，仅在首次直接打开编辑器（history.length ≤ 1）时兜底跳个人后台。
  const goBack = useCallback(() => {
    if (typeof window !== "undefined" && window.history.length > 1) {
      router.back();
      return;
    }
    router.push("/me/blog");
  }, [router]);

  const hydrate = useCallback((next: ArticleEditor) => {
    setEditor(next);
    setArticleId(next.articleId);
    setForm({
      title: next.title,
      slug: next.slug,
      summary: next.summary || "",
      contentMode: next.contentMode,
      content: next.contentMode === "MARKDOWN"
        ? next.markdownContent || ""
        : richTextToPlainText(next.richTextJson),
      categoryId: next.categoryId || "",
      visibility: next.visibility,
      publishMethod: next.publishMethod,
      tagIds: next.tagIds,
    });
    setDirty(false);
  }, []);

  useEffect(() => {
    let cancelled = false;
    let timer = 0;
    async function bootstrap() {
      if (!readSession()) {
        setNotice({ tone: "error", text: "请先登录，再开始创作。" });
        setLoading(false);
        return;
      }
      try {
        const [nextCategories, nextTags, existing] = await Promise.all([
          communityApi.categories(),
          communityApi.tags(),
          initialId ? communityApi.editor(initialId) : Promise.resolve(null),
        ]);
        if (cancelled) return;
        setCategories(nextCategories);
        setTags(nextTags);
        if (existing) hydrate(existing);
      } catch (error) {
        if (!cancelled) {
          setNotice({
            tone: "error",
            text: error instanceof Error ? error.message : "编辑器初始化失败",
          });
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    timer = window.setTimeout(() => void bootstrap(), 0);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [hydrate, initialId]);

  function change<K extends keyof EditorForm>(key: K, value: EditorForm[K]) {
    setForm((current) => ({ ...current, [key]: value }));
    setDirty(true);
  }

  function switchMode(nextMode: ContentMode) {
    if (nextMode === form.contentMode) return;
    if (form.content.trim() && !window.confirm("切换编辑器模式可能改变排版，确认继续吗？")) {
      return;
    }
    change("contentMode", nextMode);
  }

  function applyMarkdown(kind: "h1" | "h2" | "bold" | "italic" | "link" | "quote" | "code" | "list" | "ordered-list") {
    const input = contentRef.current;
    if (!input) return;
    const start = input.selectionStart;
    const end = input.selectionEnd;
    const selected = form.content.slice(start, end);
    let replacement = selected || "文字";
    let replaceStart = start;
    let replaceEnd = end;
    let selectionStart = start;

    if (["h1", "h2", "quote", "list", "ordered-list"].includes(kind)) {
      const lineStart = form.content.lastIndexOf("\n", start - 1) + 1;
      const lineEnd = form.content.indexOf("\n", end);
      replaceStart = lineStart;
      replaceEnd = lineEnd === -1 ? form.content.length : lineEnd;
      const block = form.content.slice(replaceStart, replaceEnd) || "文字";
      const prefix = kind === "h1" ? "# " : kind === "h2" ? "## " : kind === "quote" ? "> " : kind === "list" ? "- " : "1. ";
      replacement = block.split("\n").map((line, index) => `${kind === "ordered-list" ? `${index + 1}. ` : prefix}${line}`).join("\n");
      selectionStart = replaceStart + prefix.length;
    } else if (kind === "bold") {
      replacement = `**${replacement}**`;
      selectionStart += 2;
    } else if (kind === "italic") {
      replacement = `*${replacement}*`;
      selectionStart += 1;
    } else if (kind === "code") {
      replacement = `\`${replacement}\``;
      selectionStart += 1;
    } else {
      replacement = `[${selected || "链接文字"}](https://)`;
      selectionStart += 1;
    }

    setForm((current) => ({ ...current, content: `${form.content.slice(0, replaceStart)}${replacement}${form.content.slice(replaceEnd)}` }));
    setDirty(true);
    requestAnimationFrame(() => {
      input.focus();
      input.setSelectionRange(selectionStart, selectionStart + (selected || "文字").length);
    });
  }

  function toggleTag(tagId: string) {
    setForm((current) => {
      const selected = current.tagIds.includes(tagId);
      if (!selected && current.tagIds.length >= 5) {
        setNotice({ tone: "error", text: "每篇文章最多选择 5 个标签" });
        return current;
      }
      setDirty(true);
      return {
        ...current,
        tagIds: selected
          ? current.tagIds.filter((value) => value !== tagId)
          : [...current.tagIds, tagId],
      };
    });
  }

  const [teamBlogId, setTeamBlogId] = useState<string | null>(null);
  useEffect(() => {
    // 团队工作台“写团队文章”入口会携带 ?blogId=；创建时把文章写入团队博客。
    const timer = window.setTimeout(() => {
      setTeamBlogId(new URLSearchParams(window.location.search).get("blogId"));
    }, 0);
    return () => window.clearTimeout(timer);
  }, []);

  const buildPayload = useCallback((current: EditorForm, lockVersion?: number, creating = false) => {
    return {
      title: current.title.trim(),
      slug: current.slug.trim(),
      summary: current.summary.trim() || null,
      categoryId: current.categoryId || null,
      coverFileId: null,
      clearCoverFile: false,
      contentMode: current.contentMode,
      richTextJson: current.contentMode === "RICH_TEXT"
        ? plainTextToRichText(current.content)
        : null,
      markdownContent: current.contentMode === "MARKDOWN" ? current.content : null,
      visibility: current.visibility,
      publishMethod: current.publishMethod,
      tagIds: current.tagIds,
      contentFileIds: [],
      ...(creating && teamBlogId ? { blogId: teamBlogId } : {}),
      expectedLockVersion: lockVersion,
    };
  }, [teamBlogId]);

  function validate(current: EditorForm) {
    if (!current.title.trim()) return "请输入文章标题";
    if (!/^[a-z0-9]+(?:[-_][a-z0-9]+)*$/.test(current.slug.trim())) {
      return "文章地址仅支持小写字母、数字、中划线或下划线";
    }
    if (!current.content.trim()) return "正文不能为空";
    return null;
  }

  const persist = useCallback(async (autosave = false) => {
    const current = formRef.current;
    const validation = validate(current);
    if (validation) {
      if (!autosave) setNotice({ tone: "error", text: validation });
      return null;
    }
    if (saving) return editorRef.current;
    setSaving(true);
    if (!autosave) setNotice({ tone: "info", text: "正在保存文章…" });
    try {
      const currentEditor = editorRef.current;
      const next = currentEditor
        ? await communityApi.saveArticle(
          currentEditor.articleId,
          buildPayload(current, currentEditor.lockVersion),
          autosave,
        )
        : await communityApi.createArticle(buildPayload(current, undefined, true));
      hydrate(next);
      if (!currentEditor) {
        window.history.replaceState(null, "", `/editor/${next.articleId}`);
      }
      setNotice({
        tone: "success",
        text: autosave ? "已自动保存" : `已保存 · 版本 ${next.currentVersionId || "新草稿"}`,
      });
      return next;
    } catch (error) {
      setNotice({
        tone: "error",
        text: error instanceof Error ? error.message : "保存失败",
      });
      return null;
    } finally {
      setSaving(false);
    }
  }, [hydrate, saving, buildPayload]);

  useEffect(() => {
    if (!dirty || !editor || saving || reviewing || publishing) return;
    const timer = window.setTimeout(() => {
      void persist(true);
    }, 8000);
    return () => window.clearTimeout(timer);
  }, [dirty, editor, form, persist, publishing, reviewing, saving]);

  async function showPreview() {
    const saved = dirty || !editor ? await persist(false) : editor;
    if (saved) setPreview(true);
  }

  async function submitReview() {
    setReviewing(true);
    try {
      const saved = dirty || !editor ? await persist(false) : editor;
      if (!saved) return;
      const status = await communityApi.submitReview(saved.articleId, saved.lockVersion);
      const refreshed = await communityApi.editor(saved.articleId);
      hydrate(refreshed);
      setNotice({
        tone: "success",
        text: status.reviewStatus === "APPROVED"
          ? "自动审核已通过"
          : status.reviewStatus === "REJECTED"
            ? "自动审核未通过，请查看原因"
            : "已提交审核，可在投稿中心查看进度",
      });
    } catch (error) {
      setNotice({
        tone: "error",
        text: error instanceof Error ? error.message : "提交审核失败",
      });
    } finally {
      setReviewing(false);
    }
  }

  async function publish() {
    if (!editor) return;
    setPublishing(true);
    try {
      const result = await communityApi.publish(editor.articleId, editor.lockVersion);
      const refreshed = await communityApi.editor(editor.articleId);
      hydrate(refreshed);
      setNotice({ tone: "success", text: "文章已发布，正在打开公开页面…" });
      window.setTimeout(() => {
        window.location.assign(`/articles/${result.articleId}`);
      }, 500);
    } catch (error) {
      setNotice({
        tone: "error",
        text: error instanceof Error ? error.message : "发布失败",
      });
    } finally {
      setPublishing(false);
    }
  }

  const editable = !editor || !["PENDING_REVIEW", "AUTO_REVIEWING", "MANUAL_REVIEWING"].includes(editor.reviewStatus);
  const canPublish = editor?.reviewStatus === "APPROVED";
  const statusText = useMemo(() => {
    if (!editor) return "尚未创建";
    return `审核：${reviewStatusLabel(editor.reviewStatus)} · 发布：${publishStatusLabel(editor.publishStatus)}`;
  }, [editor]);

  return (
    <>
      <UserTopbar title="创作中心" />
      <main className="editor-page">
        <header className="editor-toolbar">
          <div>
            <button type="button" className="icon-button" aria-label="返回" onClick={goBack}>
              <ChevronLeft size={18} />
            </button>
            <span>
              <strong>{articleId ? "编辑文章" : "新建文章"}</strong>
              <small>
                {saving ? "保存中…" : dirty ? "有未保存修改" : statusText}
              </small>
            </span>
          </div>
          <div className="editor-actions">
            <button className="ghost-button" onClick={() => persist(false)} type="button">
              {saving ? <LoaderCircle className="spin" size={16} /> : <Save size={16} />}
              保存草稿
            </button>
            <button className="secondary-button" onClick={showPreview} type="button">
              <Eye size={16} /> 预览
            </button>
            {canPublish ? (
              <button
                className="primary-button"
                disabled={publishing}
                onClick={publish}
                type="button"
              >
                {publishing ? <LoaderCircle className="spin" size={16} /> : <Sparkles size={16} />}
                发布
              </button>
            ) : (
              <button
                className="primary-button"
                disabled={!editable || reviewing}
                onClick={submitReview}
                type="button"
              >
                {reviewing ? <LoaderCircle className="spin" size={16} /> : <Send size={16} />}
                提交审核
              </button>
            )}
          </div>
        </header>

        {notice && (
          <div className={`editor-notice editor-notice--${notice.tone}`} role="status">
            {notice.tone === "error"
              ? <AlertCircle size={17} />
              : <CheckCircle2 size={17} />}
            <span>{notice.text}</span>
            {notice.tone === "error" && !readSession() && (
              <Link className="link" href="/login">去登录</Link>
            )}
          </div>
        )}

        {loading ? (
          <section className="editor-loading" aria-busy="true">
            <LoaderCircle className="spin" size={30} />
            <strong>正在加载创作空间…</strong>
          </section>
        ) : (
          <div className="editor-layout">
            <section className="surface editor-canvas">
              <input
                aria-label="文章标题"
                className="editor-title"
                disabled={!editable}
                maxLength={120}
                onChange={(event) => change("title", event.target.value)}
                placeholder="请输入文章标题"
                value={form.title}
              />
              <textarea
                aria-label="文章摘要"
                className="editor-summary"
                disabled={!editable}
                maxLength={500}
                onChange={(event) => change("summary", event.target.value)}
                placeholder="用一两句话概括文章内容（可选）"
                value={form.summary}
              />
              <div className="editor-mode-tabs" role="tablist">
                <button
                  aria-selected={form.contentMode === "RICH_TEXT"}
                  className={form.contentMode === "RICH_TEXT" ? "active" : ""}
                  onClick={() => switchMode("RICH_TEXT")}
                  role="tab"
                  type="button"
                >
                  <FileText size={15} /> 富文本
                </button>
                <button
                  aria-selected={form.contentMode === "MARKDOWN"}
                  className={form.contentMode === "MARKDOWN" ? "active" : ""}
                  onClick={() => switchMode("MARKDOWN")}
                  role="tab"
                  type="button"
                >
                  <span className="code-symbol">M↓</span> Markdown
                </button>
              </div>
              {form.contentMode === "MARKDOWN" && (
                <div className="markdown-toolbar" aria-label="Markdown 格式工具">
                  <button aria-label="一级标题" disabled={!editable} onClick={() => applyMarkdown("h1")} title="一级标题" type="button"><Heading1 size={17} /></button>
                  <button aria-label="二级标题" disabled={!editable} onClick={() => applyMarkdown("h2")} title="二级标题" type="button"><Heading2 size={17} /></button>
                  <span className="markdown-toolbar-divider" />
                  <button aria-label="加粗" disabled={!editable} onClick={() => applyMarkdown("bold")} title="加粗" type="button"><Bold size={16} /></button>
                  <button aria-label="斜体" disabled={!editable} onClick={() => applyMarkdown("italic")} title="斜体" type="button"><Italic size={16} /></button>
                  <button aria-label="行内代码" disabled={!editable} onClick={() => applyMarkdown("code")} title="行内代码" type="button"><Code2 size={16} /></button>
                  <button aria-label="链接" disabled={!editable} onClick={() => applyMarkdown("link")} title="链接" type="button"><Link2 size={16} /></button>
                  <span className="markdown-toolbar-divider" />
                  <button aria-label="引用" disabled={!editable} onClick={() => applyMarkdown("quote")} title="引用" type="button"><Quote size={16} /></button>
                  <button aria-label="无序列表" disabled={!editable} onClick={() => applyMarkdown("list")} title="无序列表" type="button"><List size={17} /></button>
                  <button aria-label="有序列表" disabled={!editable} onClick={() => applyMarkdown("ordered-list")} title="有序列表" type="button"><ListOrdered size={17} /></button>
                </div>
              )}
              <textarea
                aria-label={form.contentMode === "MARKDOWN" ? "Markdown 正文" : "富文本正文"}
                className={`editor-body editor-body--${form.contentMode.toLowerCase()}`}
                disabled={!editable}
                ref={contentRef}
                onChange={(event) => change("content", event.target.value)}
                placeholder={
                  form.contentMode === "MARKDOWN"
                    ? "# 从这里开始写作\n\n支持 Markdown 语法。"
                    : "从这里开始写作。段落之间使用换行分隔。"
                }
                value={form.content}
              />
              <footer className="editor-canvas-footer">
                <span>{form.content.trim().length} 字符</span>
                <span><Clock3 size={13} /> {editor?.readingTimeMinutes || 1} 分钟阅读</span>
              </footer>
            </section>

            <aside className="surface editor-settings">
              <h2>发布设置</h2>
              <label>
                <span>文章地址</span>
                <input
                  className="field"
                  disabled={!editable}
                  onChange={(event) => change("slug", event.target.value.toLowerCase())}
                  placeholder="article-url-slug"
                  value={form.slug}
                />
                <small>小写字母、数字、中划线或下划线</small>
              </label>
              <label>
                <span>博客分类</span>
                <select
                  className="field"
                  disabled={!editable}
                  onChange={(event) => change("categoryId", event.target.value)}
                  value={form.categoryId}
                >
                  <option value="">使用默认分类</option>
                  {categories.map((category) => (
                    <option key={category.categoryId} value={category.categoryId}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                <span>可见范围</span>
                <select
                  className="field"
                  disabled={!editable}
                  onChange={(event) => change("visibility", event.target.value)}
                  value={form.visibility}
                >
                  <option value="PUBLIC">公开</option>
                  <option value="UNLISTED">不列出</option>
                  <option value="PRIVATE">私密</option>
                </select>
              </label>
              <label>
                <span>发布方式</span>
                <select
                  className="field"
                  disabled={!editable}
                  onChange={(event) => change("publishMethod", event.target.value)}
                  value={form.publishMethod}
                >
                  <option value="MANUAL">审核后手动发布</option>
                  <option value="IMMEDIATE">审核通过后立即发布</option>
                  <option value="SCHEDULED">审核后定时发布</option>
                </select>
              </label>
              <fieldset className="editor-tags" disabled={!editable}>
                <legend>平台标签 <small>最多 5 个</small></legend>
                <div>
                  {tags.length ? tags.map((tag) => (
                    <label
                      className={form.tagIds.includes(tag.tagId) ? "selected" : ""}
                      key={tag.tagId}
                    >
                      <input
                        checked={form.tagIds.includes(tag.tagId)}
                        onChange={() => toggleTag(tag.tagId)}
                        type="checkbox"
                      />
                      {tag.name}
                    </label>
                  )) : <span className="muted">暂无可用平台标签</span>}
                </div>
              </fieldset>
              {editor && (
                <div className="editor-status-card">
                  <span>当前状态</span>
                  <strong>{statusText}</strong>
                  <small>锁版本 {editor.lockVersion}</small>
                  {!["DRAFT", "NOT_SUBMITTED"].includes(editor.reviewStatus) && (
                    <Link className="link" href={`/submissions/ai-agent?articleId=${editor.articleId}`}>
                      查看审核进度
                    </Link>
                  )}
                </div>
              )}
            </aside>
          </div>
        )}
      </main>

      {preview && editor && (
        <div className="editor-preview-overlay" role="dialog" aria-modal="true">
          <button
            aria-label="关闭预览"
            className="editor-preview-backdrop"
            onClick={() => setPreview(false)}
            type="button"
          />
          <article className="surface-lg editor-preview">
            <header>
              <span>文章预览</span>
              <button className="ghost-button" onClick={() => setPreview(false)} type="button">
                关闭
              </button>
            </header>
            <h1>{editor.title}</h1>
            {editor.summary && <p className="article-summary">{editor.summary}</p>}
            <div
              className="article-content"
              dangerouslySetInnerHTML={{ __html: editor.renderedHtml }}
            />
          </article>
        </div>
      )}
    </>
  );
}

function plainTextToRichText(value: string) {
  const content = value.split(/\n{2,}/).map((paragraph) => ({
    type: "paragraph",
    content: paragraph
      ? [{ type: "text", text: paragraph.replace(/\n/g, " ") }]
      : [],
  }));
  return JSON.stringify({ type: "doc", content });
}

function richTextToPlainText(raw?: string | null) {
  if (!raw) return "";
  try {
    const root = JSON.parse(raw);
    const paragraphs: string[] = [];
    function visit(node: unknown): string {
      if (!node || typeof node !== "object") return "";
      const candidate = node as {
        type?: string;
        text?: string;
        content?: unknown[];
      };
      if (candidate.type === "text") return candidate.text || "";
      const text: string = (candidate.content || []).map(visit).join("");
      if (["paragraph", "heading", "blockquote"].includes(candidate.type || "")) {
        paragraphs.push(text);
        return "";
      }
      return text;
    }
    visit(root);
    return paragraphs.filter(Boolean).join("\n\n");
  } catch {
    return "";
  }
}

function reviewStatusLabel(value: string) {
  const labels: Record<string, string> = {
    DRAFT: "草稿",
    NOT_SUBMITTED: "未投稿",
    PENDING_REVIEW: "待审核",
    AUTO_REVIEWING: "自动审核中",
    MANUAL_REVIEWING: "人工审核中",
    APPROVED: "审核通过",
    REVISION_REQUIRED: "待修改",
    REJECTED: "已驳回",
    WITHDRAWN: "已撤回",
  };
  return labels[value] || value;
}

function publishStatusLabel(value: string) {
  const labels: Record<string, string> = {
    DRAFT: "草稿",
    APPROVED: "待发布",
    UNPUBLISHED: "未发布",
    SCHEDULED: "定时发布",
    PUBLISHED: "已发布",
    PUBLISH_FAILED: "发布失败",
  };
  return labels[value] || value;
}
