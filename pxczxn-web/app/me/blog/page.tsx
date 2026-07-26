"use client";

import { useEffect, useState } from "react";
import { LoaderCircle } from "lucide-react";
import { UserTopbar } from "../../components/prototype-ui";
import { communityApi, type PersonalBlog } from "../../lib/community-api";

export default function MyBlogPage() {
  const [blog, setBlog] = useState<PersonalBlog | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { void communityApi.myBlog().then((result) => { setBlog(result); window.location.replace(`/blogs/${encodeURIComponent(result.slug)}`); }).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "无法加载我的博客")); }, []);
  return <><UserTopbar title="我的博客" /><main className="page-shell community-placeholder-page"><section className="surface-lg community-placeholder-card">{error ? <p className="inline-feedback error">{error}</p> : <><LoaderCircle className="spin" size={24}/><p>{blog ? "正在进入我的博客…" : "正在加载我的博客…"}</p></>}</section></main></>;
}
