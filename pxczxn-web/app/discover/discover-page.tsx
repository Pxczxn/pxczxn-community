"use client";

import Link from "next/link";
import { BookOpen, Compass, Flame, LoaderCircle, Rss, Sparkles, Users } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { EmptyState, UserTopbar } from "../components/prototype-ui";
import { type Moment, type PlatformTag, type PublicArticleSummary, communityApi, readSession } from "../lib/community-api";

type FeedTab = "recommended" | "latest" | "popular" | "following";
type FeedItem =
  | { kind: "article"; value: PublicArticleSummary; occurredAt: string; heat: number }
  | { kind: "moment"; value: Moment; occurredAt: string; heat: number };

const tabs: Array<{ key: FeedTab; label: string; description: string }> = [
  { key: "recommended", label: "推荐", description: "编辑精选与近期公开内容，不使用智能推荐算法" },
  { key: "latest", label: "最新", description: "按公开发布时间排序" },
  { key: "popular", label: "热门", description: "按公开互动热度排序" },
  { key: "following", label: "关注", description: "已关注博客的最新公开文章" },
];

function timeLabel(value: string) {
  const distance = Date.now() - new Date(value).getTime();
  if (distance < 60_000) return "刚刚";
  if (distance < 3_600_000) return `${Math.max(1, Math.floor(distance / 60_000))} 分钟前`;
  if (distance < 86_400_000) return `${Math.floor(distance / 3_600_000)} 小时前`;
  return `${Math.floor(distance / 86_400_000)} 天前`;
}

export function DiscoverPage({ articlesOnly = false }: { articlesOnly?: boolean }) {
  const [tab, setTab] = useState<FeedTab>(articlesOnly ? "latest" : "recommended");
  const [articles, setArticles] = useState<PublicArticleSummary[]>([]);
  const [followingArticles, setFollowingArticles] = useState<PublicArticleSummary[]>([]);
  const [moments, setMoments] = useState<Moment[]>([]);
  const [tags, setTags] = useState<PlatformTag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const followingRequest = readSession()
      ? communityApi.myFollowing(1, 20)
        .then((following) => Promise.all(
          following.records.slice(0, 20).map((profile) => communityApi.publicArticles(profile.blogSlug, 1, 10)),
        ))
        .then((pages) => pages.flatMap((page) => page.records))
        .catch(() => [] as PublicArticleSummary[])
      : Promise.resolve([] as PublicArticleSummary[]);
    Promise.all([
      communityApi.discoverArticles(1, 20),
      articlesOnly ? Promise.resolve({ records: [] as Moment[] }) : communityApi.moments(1, 20),
      communityApi.tags(),
      followingRequest,
    ])
      .then(([articlePage, momentPage, tagRecords, nextFollowingArticles]) => {
        if (!active) return;
        setArticles(articlePage.records);
        setMoments(momentPage.records);
        setTags(tagRecords);
        setFollowingArticles(nextFollowingArticles);
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestError instanceof Error ? requestError.message : "内容加载失败");
      })
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [articlesOnly]);

  const feed = useMemo(() => {
    const values: FeedItem[] = [
      ...(tab === "following" ? followingArticles : articles).map((value) => ({
        kind: "article" as const,
        value,
        occurredAt: value.publishedAt,
        heat: value.likeCount + value.favoriteCount * 2 + value.commentCount * 3,
      })),
      ...(tab === "following" ? [] : moments).map((value) => ({
        kind: "moment" as const,
        value,
        occurredAt: value.createdAt,
        heat: value.likeCount + value.favoriteCount * 2 + value.commentCount * 3 + value.repostCount * 2,
      })),
    ];
    if (tab === "popular") return values.sort((left, right) => right.heat - left.heat || +new Date(right.occurredAt) - +new Date(left.occurredAt));
    return values.sort((left, right) => +new Date(right.occurredAt) - +new Date(left.occurredAt));
  }, [articles, followingArticles, moments, tab]);

  const creators = useMemo(() => {
    const seen = new Map<string, { name: string; username: string; articleCount: number }>();
    articles.forEach((article) => {
      const key = article.author.userId;
      const previous = seen.get(key);
      seen.set(key, {
        name: article.author.displayName || article.author.username,
        username: article.author.username,
        articleCount: (previous?.articleCount || 0) + 1,
      });
    });
    return [...seen.values()].sort((left, right) => right.articleCount - left.articleCount).slice(0, 5);
  }, [articles]);

  return (
    <>
      <UserTopbar title={articlesOnly ? "文章" : "发现"} />
      <main className="discover-page page-shell">
        <section className="discover-hero surface-lg shadow-sm">
          <span className="eyebrow"><Compass size={15} /> 星语社区</span>
          <h1>{articlesOnly ? "文章" : "发现值得阅读的内容"}</h1>
          <p>{articlesOnly ? "按公开时间浏览社区文章。" : "文章、动态和创作者在这里汇集；所有排序来源都会明确说明。"}</p>
          <div className="discover-hero__actions">
            <Link className="primary-button" href="/editor/new"><BookOpen size={17} /> 写文章</Link>
            <Link className="secondary-button" href="/moments">浏览动态</Link>
          </div>
        </section>

        <section className="discover-layout">
          <div className="stack">
            <div className="discover-tabs surface">
              {tabs.filter((entry) => !articlesOnly || entry.key !== "following").map((entry) => (
                <button className={tab === entry.key ? "active" : ""} key={entry.key} onClick={() => setTab(entry.key)} type="button">
                  {entry.label}
                </button>
              ))}
            </div>
            <p className="discover-sort-note">{tabs.find((entry) => entry.key === tab)?.description}</p>
            {loading && <div className="surface feed-loading"><LoaderCircle className="spin" size={22} /> 正在加载公开内容…</div>}
            {error && <div className="inline-feedback error">{error}</div>}
            {!loading && !error && !feed.length && (
              <div className="surface"><EmptyState title={tab === "following" ? "还没有关注内容" : "还没有公开内容"} description={tab === "following" ? "登录后关注博客，它们最新公开的文章会出现在这里。" : "第一篇文章或第一条动态会出现在这里。"} /></div>
            )}
            {!loading && feed.map((item) => (
              <DiscoverFeedCard item={item} key={`${item.kind}-${item.kind === "article" ? item.value.articleId : item.value.momentId}`} />
            ))}
          </div>

          <aside className="discover-aside stack">
            <section className="surface discover-aside-card">
              <h2><Flame size={17} /> 热门标签</h2>
              {tags.slice().sort((left, right) => right.usageCount - left.usageCount).slice(0, 8).map((tag) => (
                <Link className="discover-tag" href={`/tags?tag=${encodeURIComponent(tag.slug)}`} key={tag.tagId}>#{tag.name}<small>{tag.usageCount}</small></Link>
              ))}
              {!tags.length && !loading && <p className="muted">暂无可展示标签</p>}
            </section>
            <section className="surface discover-aside-card">
              <h2><Users size={17} /> 推荐创作者</h2>
              {creators.map((creator) => <Link className="discover-creator" href={`/blogs/${encodeURIComponent(creator.username)}`} key={creator.username}><span className="avatar avatar-sm">{creator.name.slice(0, 1)}</span><span><strong>{creator.name}</strong><small>@{creator.username} · {creator.articleCount} 篇公开文章</small></span></Link>)}
              {!creators.length && !loading && <p className="muted">公开创作者会在有内容后出现</p>}
            </section>
            <section className="surface discover-aside-card discover-planned-card">
              <h2><Rss size={17} /> 热门系列</h2>
              <p>系列与连载属于 M3 团队与协作创作阶段，尚未开放，不以示例数据伪装。</p>
            </section>
            <section className="surface discover-aside-card discover-planned-card">
              <h2><Sparkles size={17} /> 编辑精选</h2>
              <p>编辑精选入口已预留；运营配置能力将在 M5 建立。当前内容流按公开时间或互动热度展示。</p>
            </section>
          </aside>
        </section>
      </main>
    </>
  );
}

function DiscoverFeedCard({ item }: { item: FeedItem }) {
  if (item.kind === "article") {
    const article = item.value;
    return <article className="surface discover-feed-card">
      <span className="discover-feed-card__kind">文章</span>
      <Link href={article.canonicalPath}><h2>{article.title}</h2></Link>
      <p>{article.summary || "作者暂未填写摘要，打开文章阅读全文。"}</p>
      <div className="discover-feed-card__meta"><span>{article.author.displayName || article.author.username}</span><span>{timeLabel(article.publishedAt)}</span><span>{article.readingTimeMinutes} 分钟阅读</span><span>{article.likeCount} 赞 · {article.commentCount} 评</span></div>
      <div className="discover-feed-card__tags">{article.tags.map((tag) => <Link href={`/tags?tag=${encodeURIComponent(tag.slug)}`} key={tag.tagId}>#{tag.name}</Link>)}</div>
    </article>;
  }
  const moment = item.value;
  return <article className="surface discover-feed-card">
    <span className="discover-feed-card__kind">动态</span>
    <Link href={`/moments/${moment.momentId}`}><h2>{moment.author.displayName || moment.author.username} 的动态</h2></Link>
    <p>{moment.textContent || "查看这条动态的内容与互动。"}</p>
    <div className="discover-feed-card__meta"><span>来自 {moment.blog.name}</span><span>{timeLabel(moment.createdAt)}</span><span>{moment.likeCount} 赞 · {moment.commentCount} 评</span></div>
  </article>;
}
