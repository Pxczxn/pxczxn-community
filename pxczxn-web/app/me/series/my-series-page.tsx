"use client";

import Link from "next/link";
import { AlertTriangle, LibraryBig, LoaderCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { SeriesManager } from "../../components/series-manager";
import { UserTopbar } from "../../components/prototype-ui";
import { communityApi, type PersonalBlog } from "../../lib/community-api";

/**
 * 个人中心的"我的系列"。
 *
 * 个人博客和团队博客在系列上是完全对等的：这里只负责拿到当前用户的 personalBlogId，
 * 剩下的创建、编排、提交审核全部复用与团队工作台同一个 SeriesManager。
 */
export function MySeriesPage() {
  const [blog, setBlog] = useState<PersonalBlog | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void communityApi
      .myBlog()
      .then((value) => {
        if (active) setBlog(value);
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : "无法加载个人博客");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading) {
    return (
      <>
        <UserTopbar title="我的连载" />
        <main className="profile-page page-shell profile-state" aria-busy="true">
          <LoaderCircle className="spin" size={28} />
          <strong>正在加载你的连载…</strong>
        </main>
      </>
    );
  }

  if (error || !blog) {
    return (
      <>
        <UserTopbar title="我的连载" />
        <main className="profile-page page-shell profile-state error">
          <AlertTriangle size={34} />
          <h1>连载管理暂时无法打开</h1>
          <p>{error || "没有找到你的个人博客。"}</p>
          <Link className="ghost-button" href="/login">
            前往登录
          </Link>
        </main>
      </>
    );
  }

  return (
    <>
      <UserTopbar title="我的连载" />
      <main className="series-page page-shell">
        <header className="workspace-content-page__header">
          <div>
            <span className="eyebrow">
              <LibraryBig size={15} /> 我的连载
            </span>
            <h1>把零散的文章，串成一条读得下去的线</h1>
            <p>
              连载属于你的个人博客「{blog.name}」。编排好章节并通过审核后，读者可以在
              <Link href={`/${encodeURIComponent(blog.slug)}`}> 你的博客主页 </Link>
              和公开的连载书架上按顺序阅读。
            </p>
          </div>
        </header>

        <SeriesManager
          blogId={blog.blogId}
          publicHref={`/${encodeURIComponent(blog.slug)}?tab=series`}
          scopeNoun="你"
          articleSourceHint="可编排的是你个人博客下已发布的文章；投稿到团队的文章归团队博客管理。"
        />
      </main>
    </>
  );
}
