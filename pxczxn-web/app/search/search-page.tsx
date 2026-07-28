"use client";

import Link from "next/link";
import { LoaderCircle, Search } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";
import { UserTopbar } from "../components/prototype-ui";
import { type UnifiedSearchPage, type UnifiedSearchResult, type UnifiedSearchType, communityApi } from "../lib/community-api";

const types: Array<{ value: UnifiedSearchType; label: string }> = [
  { value: "ALL", label: "全部" }, { value: "ARTICLE", label: "文章" }, { value: "MOMENT", label: "动态" },
  { value: "BLOG", label: "博客" }, { value: "SERIES", label: "系列" }, { value: "TAG", label: "标签" }, { value: "USER", label: "用户" },
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

  return <>
    <UserTopbar title="统一搜索" />
    <main className="page-shell search-page">
      <section className="surface-lg shadow-sm search-hero">
        <span className="eyebrow"><Search size={15} /> 公开内容搜索</span>
        <h1>找到你关心的内容</h1>
        <form className="search-form" onSubmit={submit}>
          <input aria-label="搜索关键词" maxLength={80} onChange={(event) => setKeyword(event.target.value)} placeholder="文章、动态、博客、系列、标签或用户" value={keyword} />
          <button className="primary-button" type="submit"><Search size={17} /> 搜索</button>
        </form>
        <p>只展示公开、仍可访问的内容；搜索结果中的高亮文本经过转义处理。</p>
      </section>
      <section className="search-layout">
        <div className="search-types surface">
          {types.map((item) => <button className={type === item.value ? "active" : ""} key={item.value} onClick={() => { setPage(null); setError(null); setType(item.value); }} type="button">{item.label}</button>)}
        </div>
        {loading && <div className="surface feed-loading"><LoaderCircle className="spin" size={22} /> 正在搜索…</div>}
        {error && <div className="inline-feedback error">{error}</div>}
        {!loading && page && <p className="muted">“{submittedKeyword}” 共找到 {page.total} 条公开结果</p>}
        {!loading && page?.records.map((result) => <SearchCard key={`${result.type}-${result.targetId}`} result={result} />)}
        {!loading && page && page.records.length === 0 && <div className="surface empty-state"><h2>没有匹配的公开内容</h2><p>试试更短的关键词或切换搜索类型。</p></div>}
      </section>
    </main>
  </>;
}

function initialKeyword() {
  if (typeof window === "undefined") return "";
  return new URLSearchParams(window.location.search).get("q")?.trim() || "";
}

function SearchCard({ result }: { result: UnifiedSearchResult }) {
  return <article className="surface search-card">
    <span className="search-card__type">{labels[result.type]}</span>
    <Link href={result.canonicalPath}><h2 dangerouslySetInnerHTML={{ __html: result.titleHighlightHtml }} /></Link>
    {result.excerptHighlightHtml && <p dangerouslySetInnerHTML={{ __html: result.excerptHighlightHtml }} />}
    <div className="discover-feed-card__meta">
      {result.authorName && <span>{result.authorName}</span>}
      {result.blogName && <span>来自 {result.blogName}</span>}
      {result.occurredAt && <span>{new Date(result.occurredAt).toLocaleDateString("zh-CN")}</span>}
    </div>
  </article>;
}
