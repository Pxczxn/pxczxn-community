"use client";

import Link from "next/link";
import {
  AlertTriangle,
  ArrowUpRight,
  Bookmark,
  Eye,
  Heart,
  Layers,
  LoaderCircle,
  MoreHorizontal,
  PenLine,
  RefreshCw,
  Search,
  Users,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ArticleThumb, EmptyState } from "../../components/prototype-ui";
import {
  PublicArticlePage,
  PublicBlog,
  Series,
  communityApi,
  publicFileUrl,
  readSession,
} from "../../lib/community-api";
import { readingPercent, serializationLabel } from "../../lib/series-labels";

type BlogTab = "articles" | "series" | "about";

const BLOG_TABS: Array<{ key: BlogTab; label: string }> = [
  { key: "articles", label: "文章" },
  { key: "series", label: "系列" },
  { key: "about", label: "关于" },
];

export function PublicBlogPage({ slug }: { slug: string }) {
  const [blog, setBlog] = useState<PublicBlog | null>(null);
  const [articles, setArticles] = useState<PublicArticlePage | null>(null);
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [following, setFollowing] = useState(false);
  const [followBusy, setFollowBusy] = useState(false);
  const [followError, setFollowError] = useState("");
  const [tab, setTab] = useState<BlogTab>("articles");
  const [series, setSeries] = useState<Series[] | null>(null);
  const [seriesError, setSeriesError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [nextBlog, nextArticles] = await Promise.all([
        communityApi.publicBlog(slug),
        communityApi.publicArticles(slug, 1, 20),
      ]);
      setBlog(nextBlog);
      setArticles(nextArticles);
      const session = readSession();
      if (session && session.blogSlug !== nextBlog.slug) {
        try {
          const relationship = await communityApi.blogFollowRelationship(nextBlog.blogId);
          setFollowing(relationship.following);
          setBlog((current) => current
            ? { ...current, followerCount: relationship.followerCount }
            : current);
        } catch {
          setFollowing(false);
        }
      }
      if (nextBlog.themeKey === "light" || nextBlog.themeKey === "dark" || nextBlog.themeKey === "starry") {
        document.documentElement.dataset.theme = nextBlog.themeKey;
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "博客加载失败");
    } finally {
      setLoading(false);
    }
  }, [slug]);

  async function toggleFollow() {
    if (!blog) return;
    if (!readSession()) {
      window.location.assign(`/login?returnTo=${encodeURIComponent(`/${blog.slug}`)}`);
      return;
    }
    setFollowBusy(true);
    setFollowError("");
    try {
      const relationship = await communityApi.setBlogFollow(blog.blogId, !following);
      setFollowing(relationship.following);
      setBlog((current) => current
        ? { ...current, followerCount: relationship.followerCount }
        : current);
    } catch (requestError) {
      setFollowError(requestError instanceof Error ? requestError.message : "关注操作失败");
    } finally {
      setFollowBusy(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  // 系列按需加载：大多数访客只看文章，没必要为了一个可能不会点开的页签多发一次请求。
  useEffect(() => {
    if (tab !== "series" || !blog || series !== null) return;
    let active = true;
    void communityApi
      .blogPublicSeries(blog.blogId)
      .then((value) => {
        if (active) setSeries(value);
      })
      .catch((cause: unknown) => {
        if (!active) return;
        setSeries([]);
        setSeriesError(cause instanceof Error ? cause.message : "系列加载失败");
      });
    return () => {
      active = false;
    };
  }, [tab, blog, series]);

  const records = useMemo(() => {
    const source = articles?.records ?? [];
    const normalized = keyword.trim().toLocaleLowerCase();
    if (!normalized) return source;
    return source.filter((article) =>
      `${article.title} ${article.summary || ""} ${article.tags.map((tag) => tag.name).join(" ")}`
        .toLocaleLowerCase()
        .includes(normalized),
    );
  }, [articles, keyword]);

  if (loading) {
    return (
      <main className="public-blog-state page-shell" aria-busy="true">
        <LoaderCircle className="spin" size={30} />
        <strong>正在加载博客内容…</strong>
        <span>公开资料与文章会在这里呈现</span>
      </main>
    );
  }

  if (error || !blog) {
    return (
      <main className="public-blog-state public-blog-state--error page-shell">
        <AlertTriangle size={34} />
        <h1>暂时无法打开这个博客</h1>
        <p>{error || "博客不存在或暂未公开"}</p>
        <div>
          <button className="primary-button" onClick={load} type="button">
            <RefreshCw size={16} /> 重新加载
          </button>
          <Link className="ghost-button" href="/discover">
            返回发现
          </Link>
        </div>
      </main>
    );
  }

  const background = publicFileUrl(blog.backgroundFileId);
  const avatar = publicFileUrl(blog.avatarFileId || blog.ownerAvatarFileId);
  const session = readSession();
  const isMine = session?.blogSlug === blog.slug;

  return (
    <main className="team-page">
      <div className="team-blog surface-lg shadow-sm">
        <div
          className="team-cover"
          style={background ? { backgroundImage: `url("${background}")` } : undefined}
        >
          <Link className="team-cover__home" href="/login">
            星语社区
          </Link>
        </div>
        <section className="team-identity">
          <div
            className={`team-logo ${avatar ? "team-logo--image" : ""}`}
            style={avatar ? { backgroundImage: `url("${avatar}")` } : undefined}
          >
            {!avatar && blog.name.slice(0, 2)}
          </div>
          <div className="team-identity__copy">
            <div className="team-name-row">
              <h1>{blog.name}</h1>
              <span className="verified">✓</span>
            </div>
            <p>{blog.summary || blog.ownerBio || "这个博客还没有填写简介。"}</p>
            <div className="team-stats-inline">
              <span>文章 <strong>{blog.articleCount}</strong></span>
              <span>类型 <strong>{blog.blogType === "TEAM" ? "团队" : "个人"}</strong></span>
              <span>作者 <strong>@{blog.ownerUsername}</strong></span>
              <span>粉丝 <strong>{blog.followerCount}</strong></span>
            </div>
          </div>
          <div className="team-actions">
            {isMine ? (
              <>
                <Link className="primary-button" href="/editor/new">
                  <PenLine size={17} /> 写文章
                </Link>
                <Link className="icon-button" aria-label="博客设置" href="/settings">
                  <MoreHorizontal size={18} />
                </Link>
              </>
            ) : (
              <>
                <button
                  aria-pressed={following}
                  className={following ? "secondary-button" : "primary-button"}
                  disabled={followBusy}
                  onClick={toggleFollow}
                  type="button"
                >
                  {followBusy ? <LoaderCircle className="spin" size={17} /> : <Users size={17} />}
                  {followBusy ? "处理中" : following ? "已关注" : "关注博客"}
                </button>
                <button aria-label="更多操作" className="icon-button" type="button">
                  <MoreHorizontal size={18} />
                </button>
              </>
            )}
          </div>
        </section>
        {followError && (
          <div className="inline-feedback error team-follow-error" role="alert">
            <AlertTriangle size={16} /> {followError}
          </div>
        )}

        <div className="team-tabs-row">
          <nav className="tabs" aria-label="博客内容">
            {BLOG_TABS.map((item) => (
              <button
                aria-current={tab === item.key ? "page" : undefined}
                className={`tab ${tab === item.key ? "active" : ""}`}
                key={item.key}
                onClick={() => setTab(item.key)}
                type="button"
              >
                {item.label}
              </button>
            ))}
          </nav>
          {tab === "articles" && (
            <label className="team-search">
              <input
                aria-label="搜索博客文章"
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="搜索博客文章"
                value={keyword}
              />
              <Search size={16} />
            </label>
          )}
        </div>

        <section className="team-content">
          <div className="article-list">
            {tab === "series" && (
              <BlogSeriesPanel error={seriesError} isMine={isMine} series={series} />
            )}
            {tab === "about" && (
              <div className="surface side-card">
                <h2 className="card-heading">关于这个博客</h2>
                <p>{blog.summary || "博主还没有填写博客简介。"}</p>
                <p className="muted">
                  作者 @{blog.ownerUsername} · {blog.blogType === "TEAM" ? "团队博客" : "个人博客"} · 主题
                  {themeLabel(blog.themeKey)}
                </p>
                <p>{blog.ownerBio || "作者还没有填写个人简介。"}</p>
              </div>
            )}
            {tab === "articles" && records.map((article, index) => (
              <Link
                className="team-article"
                href={`/${blog.slug}/${article.articleId}`}
                key={article.articleId}
              >
                {article.coverFileId ? (
                  <span
                    aria-hidden="true"
                    className="article-thumb article-thumb--image"
                    style={{
                      backgroundImage: `url("${publicFileUrl(article.coverFileId)}")`,
                    }}
                  />
                ) : (
                  <ArticleThumb variant={index + 1} />
                )}
                <span className="team-article__body">
                  <strong>{article.title}</strong>
                  <span className="article-tags">
                    {article.tags.map((tag) => (
                      <span className="chip" key={tag.tagId}>{tag.name}</span>
                    ))}
                  </span>
                  <small className="muted">
                    {formatDate(article.publishedAt)} · {article.readingTimeMinutes} 分钟阅读
                  </small>
                </span>
                <span className="team-article__meta">
                  <small><Eye size={14} /> {article.viewCount}</small>
                  <small><Heart size={14} /> {article.likeCount}</small>
                  <small><Bookmark size={14} /> {article.favoriteCount}</small>
                </span>
              </Link>
            ))}
            {tab === "articles" && !records.length && (
              <EmptyState
                title={keyword ? "没有匹配的文章" : "还没有公开文章"}
                description={keyword ? "换一个关键词继续搜索。" : "新文章发布后会显示在这里。"}
              />
            )}
          </div>

          <aside className="team-aside stack">
            <div className="surface side-card">
              <h2 className="card-heading">博客数据</h2>
              <dl className="team-data-list">
                <div><dt>文章数</dt><dd>{blog.articleCount}</dd></div>
                <div><dt>粉丝数</dt><dd>{blog.followerCount}</dd></div>
                <div><dt>可见文章</dt><dd>{articles?.total ?? 0}</dd></div>
                <div><dt>主题</dt><dd>{themeLabel(blog.themeKey)}</dd></div>
              </dl>
            </div>
            <div className="surface side-card public-owner-card">
              <h2 className="card-heading">关于作者</h2>
              <strong>{blog.ownerDisplayName || blog.ownerUsername}</strong>
              <span className="muted">@{blog.ownerUsername}</span>
              <p>{blog.ownerBio || "作者还没有填写个人简介。"}</p>
            </div>
          </aside>
        </section>
      </div>
    </main>
  );
}

/**
 * 博客门户的"系列"页签。个人博客与团队博客共用：数据同样来自 blogId，展示口径也一致，
 * 唯一区别是博主本人会额外看到一个去管理页的入口。
 */
function BlogSeriesPanel({
  series,
  error,
  isMine,
}: {
  series: Series[] | null;
  error: string;
  isMine: boolean;
}) {
  if (series === null) {
    return (
      <div className="series-loading surface" aria-busy="true">
        <LoaderCircle className="spin" size={20} /> 正在加载系列…
      </div>
    );
  }

  if (error) {
    return (
      <div className="inline-feedback error" role="alert">
        <AlertTriangle size={16} /> {error}
      </div>
    );
  }

  if (series.length === 0) {
    return (
      <EmptyState
        title="还没有公开系列"
        description={isMine ? "把已发布的文章编排成连载，通过审核后就会展示在这里。" : "作者还没有公开连载系列。"}
      />
    );
  }

  return (
    <div className="team-portal-series-grid">
      {isMine && (
        <Link className="ghost-button" href="/me/series">
          管理我的系列 <ArrowUpRight size={14} />
        </Link>
      )}
      {series.map((item) => {
        const percent = readingPercent(item.viewerReadChapterCount, item.chapterCount);
        return (
          <div className="surface team-portal-series-card" key={item.id}>
            <h3>{item.title}</h3>
            <p className="secondary">{item.summary || "暂无简介"}</p>
            <p className="muted">
              {serializationLabel(item.serializationStatus)} · <Layers size={13} /> {item.chapterCount} 章
              {item.followerCount > 0 && (
                <>
                  {" · "}
                  <Users size={13} /> {item.followerCount} 人追更
                </>
              )}
            </p>
            {percent > 0 && (
              <div
                className="series-progress series-progress--slim"
                role="progressbar"
                aria-valuenow={percent}
                aria-valuemin={0}
                aria-valuemax={100}
              >
                <span style={{ width: `${percent}%` }} />
              </div>
            )}
            {item.chapterCount > 0 && (
              <Link className="ghost-button" href={`/series/${item.id}`}>
                查看系列 <ArrowUpRight size={14} />
              </Link>
            )}
          </div>
        );
      })}
    </div>
  );
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).format(date);
}

function themeLabel(theme: string) {
  return { default: "浅色", light: "浅色", dark: "深色", starry: "星空" }[theme] || theme;
}
