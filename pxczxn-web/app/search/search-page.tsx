"use client";

import Link from "next/link";
import { BookOpen, LoaderCircle, Orbit, Rss, Search, Tag, Users } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";
import { UserTopbar } from "../components/prototype-ui";
import { type UnifiedSearchPage, type UnifiedSearchResult, type UnifiedSearchType, communityApi } from "../lib/community-api";

const types: Array<{ value: UnifiedSearchType; label: string; Icon: typeof Search }> = [
  { value: "ALL", label: "全部", Icon: Search },
  { value: "ARTICLE", label: "文章", Icon: BookOpen },
  { value: "MOMENT", label: "动态", Icon: Orbit },
  { value: "BLOG", label: "博客", Icon: Users },
  { value: "SERIES", label: "系列", Icon: Rss },
  { value: "TAG", label: "标签", Icon: Tag },
  { value: "USER", label: "用户", Icon: Users },
];

const labels: Record<UnifiedSearchResult["type"], string> = {
  ARTICLE: "文章", MOMENT: "动态", BLOG: "博客", SERIES: "系列", TAG: "标签", USER: "用户",
};

export function SearchPage() {
  const [keyword, setKeyword] = useState(() => initialKeyword());
  const [submittedKeyword, setSubmittedKeyword] = useState(() => initialKeyword());
  const [type, setType] = useState<UnifiedSearchType>("ALL");
  const [page, setPage] = useState<UnifiedSearchPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const loading = Boolean(submittedKeyword) && page === null && error === null;

  useEffect(() => {
    if (!submittedKeyword) return;
    let active = true;
    communityApi.search(submittedKeyword, type)
      .then((value) => active && setPage(value))
      .catch((requestError: unknown) => active && setError(requestError instanceof Error ? requestError.message : "搜索失败"))
    return () => { active = false; };
  }, [submittedKeyword, type]);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const next = keyword.trim();
    if (!next) return;
    window.history.replaceState(null, "", `/search?q=${encodeURIComponent(next)}`);
    setPage(null); setError(null);
    setSubmittedKeyword(next);
  }

  return (
    <>
      <UserTopbar title="搜索" />
      <main className="page-shell search-page">
        <section className="search-hero-card shadow-sm">
          <span className="eyebrow"><Search size={15} /> 全站搜索</span>
          <h1>找到你关心的内容</h1>
          <form className="search-input-group" onSubmit={submit}>
            <Search size={20} style={{ color: "var(--text-tertiary)" }} />
            <input aria-label="搜索关键词" maxLength={80} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索文章、动态、博客、系列、标签或用户..." value={keyword} />
            <button className="primary-button" type="submit" style={{ padding: "8px 20px" }}>搜索</button>
          </form>
          <p className="secondary" style={{ fontSize: 13, margin: 0 }}>只展示公开、仍可访问的内容；搜索结果中的匹配文本均经过安全处理。</p>
        </section>

        <section className="search-layout" style={{ marginTop: 24 }}>
          <div className="discover-tabs surface">
            {types.map((item) => {
              const Icon = item.Icon;
              return (
                <button className={type === item.value ? "active" : ""} key={item.value} onClick={() => { setPage(null); setError(null); setType(item.value); }} type="button">
                  <Icon size={14} />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </div>

          {loading && <div className="surface feed-loading"><LoaderCircle className="spin" size={22} /> 正在搜索…</div>}
          {error && <div className="inline-feedback error">{error}</div>}
          {!loading && page && <p className="muted" style={{ margin: "12px 0 4px" }}>“<strong>{submittedKeyword}</strong>” 共找到 {page.total} 条公开结果</p>}
          {!loading && page?.records.map((result) => <SearchCard key={`${result.type}-${result.targetId}`} result={result} />)}
          {!loading && page && page.records.length === 0 && (
            <div className="surface empty-state" style={{ padding: 48, textAlign: "center", borderRadius: 16, marginTop: 12 }}>
              <h2>没有匹配的公开内容</h2>
              <p className="secondary">试试更短的关键词或切换其他分类搜索类型。</p>
            </div>
          )}
        </section>
      </main>
    </>
  );
}

function initialKeyword() {
  if (typeof window === "undefined") return "";
  return new URLSearchParams(window.location.search).get("q")?.trim() || "";
}

function SearchCard({ result }: { result: UnifiedSearchResult }) {
  return (
    <article className="surface discover-feed-card" style={{ marginTop: 12 }}>
      <div className="discover-feed-card__header">
        <span className="discover-feed-card__kind">{labels[result.type]}</span>
        {result.occurredAt && <span className="secondary" style={{ fontSize: 12 }}>{new Date(result.occurredAt).toLocaleDateString("zh-CN")}</span>}
      </div>
      <Link href={result.canonicalPath}>
        <h2 dangerouslySetInnerHTML={{ __html: result.titleHighlightHtml }} />
      </Link>
      {result.excerptHighlightHtml && <p dangerouslySetInnerHTML={{ __html: result.excerptHighlightHtml }} />}
      <div className="discover-feed-card__footer">
        <div className="discover-feed-card__meta">
          {result.authorName && <span>{result.authorName}</span>}
          {result.blogName && <span>来自 {result.blogName}</span>}
        </div>
      </div>
    </article>
  );
}

