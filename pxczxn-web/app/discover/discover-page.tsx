"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ArrowRight, BookOpen, Clock, Compass, Flame, Layers, LibraryBig, LoaderCircle, MessageSquare, Orbit, PenLine, Sparkles, Tag as TagIcon, ThumbsUp, Users } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Avatar, EmptyState, UserTopbar } from "../components/prototype-ui";
import { type Moment, type PlatformTag, type PublicArticleSummary, type TeamSeries, communityApi, readSession } from "../lib/community-api";

type FeedTab = "recommended" | "latest" | "popular" | "following";
type FeedItem =
  | { kind: "article"; value: PublicArticleSummary; occurredAt: string; heat: number }
  | { kind: "moment"; value: Moment; occurredAt: string; heat: number };

const tabs: Array<{ key: FeedTab; label: string; Icon: typeof Sparkles; description: string }> = [
  { key: "recommended", label: "推荐", Icon: Sparkles, description: "编辑精选与近期公开内容，不使用智能推荐算法" },
  { key: "latest", label: "最新", Icon: Clock, description: "按公开发布时间排序" },
  { key: "popular", label: "热门", Icon: Flame, description: "按公开互动热度排序" },
  { key: "following", label: "关注", Icon: Users, description: "已关注博客的最新公开文章" },
];

function timeLabel(value: string) {
  const distance = Date.now() - new Date(value).getTime();
  if (distance < 60_000) return "刚刚";
  if (distance < 3_600_000) return `${Math.max(1, Math.floor(distance / 60_000))} 分钟前`;
  if (distance < 86_400_000) return `${Math.floor(distance / 3_600_000)} 小时前`;
  return `${Math.floor(distance / 86_400_000)} 天前`;
}

export function DiscoverPage({ articlesOnly = false }: { articlesOnly?: boolean }) {
  const pathname = usePathname();
  const isHomePage = pathname === "/" && !articlesOnly;

  const [tab, setTab] = useState<FeedTab>(articlesOnly ? "latest" : "recommended");
  const [articles, setArticles] = useState<PublicArticleSummary[]>([]);
  const [followingArticles, setFollowingArticles] = useState<PublicArticleSummary[]>([]);
  const [moments, setMoments] = useState<Moment[]>([]);
  const [tags, setTags] = useState<PlatformTag[]>([]);
  const [seriesList, setSeriesList] = useState<TeamSeries[]>([]);
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
      communityApi.discoverRankedArticles(
        tab === "recommended" ? "QUALITY" : tab === "popular" ? "LIKES" : "LATEST",
        1,
        20,
      ),
      articlesOnly ? Promise.resolve({ records: [] as Moment[] }) : communityApi.moments(1, 20),
      communityApi.tags(),
      followingRequest,
      communityApi.series().catch(() => [] as TeamSeries[]),
    ])
      .then(([articlePage, momentPage, tagRecords, nextFollowingArticles, seriesRecords]) => {
        if (!active) return;
        setArticles(articlePage.records);
        setMoments(momentPage.records);
        setTags(tagRecords);
        setFollowingArticles(nextFollowingArticles);
        setSeriesList(seriesRecords);
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestError instanceof Error ? requestError.message : "内容加载失败");
      })
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [articlesOnly, tab]);

  const feed = useMemo(() => {
    const values: FeedItem[] = [
      ...(tab === "following" ? followingArticles : articles).map((value) => ({
        kind: "article" as const,
        value,
        occurredAt: value.publishedAt,
        heat: value.likeCount + value.favoriteCount * 2 + value.commentCount * 3,
      })),
      ...(tab === "latest" ? moments : []).map((value) => ({
        kind: "moment" as const,
        value,
        occurredAt: value.createdAt,
        heat: value.likeCount + value.favoriteCount * 2 + value.commentCount * 3 + value.repostCount * 2,
      })),
    ];
    if (tab === "following") return values.sort((left, right) => +new Date(right.occurredAt) - +new Date(left.occurredAt));
    return values;
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

  if (isHomePage) {
    return (
      <>
        <UserTopbar title="首页" />
        <main className="discover-page page-shell">
          {/* Home Portal Hero */}
          <section className="home-hero-card shadow-sm">
            <div style={{ display: "grid", gap: 12, maxWidth: 760 }}>
              <span className="eyebrow"><Sparkles size={15} /> 星语社区 Portal</span>
              <h1 style={{ margin: 0, fontSize: "clamp(30px, 4.5vw, 48px)", fontWeight: 800, lineHeight: 1.15 }}>
                注册即拥有个人博客
              </h1>
              <p style={{ margin: 0, fontSize: 16, color: "var(--text-secondary)", lineHeight: 1.6 }}>
                专为技术与思想创作者设计的公开社区。写作、连载、组建团队专栏，这里是你沉淀长远价值的精神家园。
              </p>
              <div className="discover-hero__actions" style={{ marginTop: 6 }}>
                <Link className="primary-button" href="/editor/new"><PenLine size={17} /> 开始创作</Link>
                <Link className="secondary-button" href="/discover"><Compass size={17} /> 探索内容</Link>
                <Link className="ghost-button" href="/series"><LibraryBig size={17} /> 连载系列</Link>
              </div>
            </div>

            {/* Feature Highlights Grid */}
            <div className="home-features-grid">
              <Link href="/editor/new" className="home-feature-card">
                <span className="home-feature-icon"><BookOpen size={22} /></span>
                <h3>个人博客与创作中心</h3>
                <p>注册即可拥有独立博客二级域名与专栏，支持 Markdown 与沉浸式编辑。</p>
              </Link>
              <Link href="/series" className="home-feature-card">
                <span className="home-feature-icon"><Layers size={22} /></span>
                <h3>团队协作与连载专栏</h3>
                <p>按章节顺序搭建深度专栏与技术书架，方便读者循序渐进地阅读。</p>
              </Link>
              <Link href="/moments" className="home-feature-card">
                <span className="home-feature-icon"><Orbit size={22} /></span>
                <h3>极简动态与同频互动</h3>
                <p>随手发布想法碎片、技术链接与微动态，与全站创作者交流。</p>
              </Link>
            </div>
          </section>

          <section className="discover-layout">
            <div className="stack">
              <div className="discover-tabs surface">
                {tabs.filter((entry) => entry.key !== "following").map((entry) => {
                  const Icon = entry.Icon;
                  return (
                    <button className={tab === entry.key ? "active" : ""} key={entry.key} onClick={() => setTab(entry.key)} type="button">
                      <Icon size={15} />
                      <span>{entry.label}</span>
                    </button>
                  );
                })}
              </div>
              <p className="discover-sort-note">{tabs.find((entry) => entry.key === tab)?.description}</p>
              {loading && <div className="surface feed-loading"><LoaderCircle className="spin" size={22} /> 正在加载最新内容…</div>}
              {error && <div className="inline-feedback error">{error}</div>}
              {!loading && !error && !feed.length && (
                <div className="surface"><EmptyState title="还没有公开内容" description="第一篇文章或第一条动态会出现在这里。" /></div>
              )}
              {!loading && feed.map((item) => (
                <DiscoverFeedCard item={item} key={`${item.kind}-${item.kind === "article" ? item.value.articleId : item.value.momentId}`} />
              ))}
            </div>

            <aside className="discover-aside stack">
              <section className="home-guide-card">
                <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, display: "flex", alignItems: "center", gap: 8 }}>
                  <Sparkles size={17} style={{ color: "var(--primary)" }} /> 社区创作指南
                </h3>
                <p style={{ margin: 0, fontSize: 13, color: "var(--text-secondary)", lineHeight: 1.6 }}>
                  所有发布的内容均公开透明，不使用智能推荐算法拦截，按公开发布时间与真实互动热度呈现。
                </p>
                <Link className="secondary-button" href="/editor/new" style={{ fontSize: 13, padding: "6px 12px", width: "fit-content" }}>
                  写第一篇文章 <ArrowRight size={14} />
                </Link>
              </section>

              <section className="surface discover-aside-card">
                <h2><Flame size={17} /> 热门标签</h2>
                <div className="discover-tag-chips">
                  {tags.slice().sort((left, right) => right.usageCount - left.usageCount).slice(0, 8).map((tag) => (
                    <Link className="discover-tag-chip" href={`/tags?tag=${encodeURIComponent(tag.slug)}`} key={tag.tagId}>
                      <TagIcon size={13} />
                      <span>{tag.name}</span>
                      <small>{tag.usageCount}</small>
                    </Link>
                  ))}
                </div>
                {!tags.length && !loading && <p className="muted">暂无可展示标签</p>}
              </section>

              <section className="surface discover-aside-card">
                <h2><Users size={17} /> 推荐创作者</h2>
                {creators.map((creator) => (
                  <Link className="discover-creator" href={`/blogs/${encodeURIComponent(creator.username)}`} key={creator.username}>
                    <Avatar label={creator.name.slice(0, 1)} size="sm" />
                    <span>
                      <strong>{creator.name}</strong>
                      <small>@{creator.username} · {creator.articleCount} 篇公开文章</small>
                    </span>
                  </Link>
                ))}
                {!creators.length && !loading && <p className="muted">公开创作者会在有内容后出现</p>}
              </section>
            </aside>
          </section>
        </main>
      </>
    );
  }

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
            <Link className="secondary-button" href="/moments"><Orbit size={17} /> 浏览动态</Link>
            <Link className="ghost-button" href="/series"><LibraryBig size={17} /> 连载系列</Link>
          </div>
        </section>

        <section className="discover-layout">
          <div className="stack">
            <div className="discover-tabs surface">
              {tabs.filter((entry) => !articlesOnly || entry.key !== "following").map((entry) => {
                const Icon = entry.Icon;
                return (
                  <button className={tab === entry.key ? "active" : ""} key={entry.key} onClick={() => setTab(entry.key)} type="button">
                    <Icon size={15} />
                    <span>{entry.label}</span>
                  </button>
                );
              })}
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
              <div className="discover-tag-chips">
                {tags.slice().sort((left, right) => right.usageCount - left.usageCount).slice(0, 8).map((tag) => (
                  <Link className="discover-tag-chip" href={`/tags?tag=${encodeURIComponent(tag.slug)}`} key={tag.tagId}>
                    <TagIcon size={13} />
                    <span>{tag.name}</span>
                    <small>{tag.usageCount}</small>
                  </Link>
                ))}
              </div>
              {!tags.length && !loading && <p className="muted">暂无可展示标签</p>}
            </section>
            <section className="surface discover-aside-card">
              <h2><Users size={17} /> 推荐创作者</h2>
              {creators.map((creator) => (
                <Link className="discover-creator" href={`/blogs/${encodeURIComponent(creator.username)}`} key={creator.username}>
                  <Avatar label={creator.name.slice(0, 1)} size="sm" />
                  <span>
                    <strong>{creator.name}</strong>
                    <small>@{creator.username} · {creator.articleCount} 篇公开文章</small>
                  </span>
                </Link>
              ))}
              {!creators.length && !loading && <p className="muted">公开创作者会在有内容后出现</p>}
            </section>
            <section className="surface discover-aside-card">
              <h2><LibraryBig size={17} /> 热门系列</h2>
              {seriesList.length > 0 ? (
                seriesList.slice(0, 3).map((item) => (
                  <Link className="discover-series-preview" href={`/series/${item.id}`} key={item.id}>
                    <span className="discover-series-preview__title">{item.title}</span>
                    <span className="discover-series-preview__meta">
                      <span>{item.chapters.length} 篇章节</span>
                      <span>{item.serializationStatus === "COMPLETED" ? "已完结" : item.serializationStatus === "PAUSED" ? "暂缓更新" : "连载中"}</span>
                    </span>
                  </Link>
                ))
              ) : (
                <div className="discover-planned-card">
                  <p>浏览社区公开发布的专栏与连载系列，按章节循序渐进。</p>
                  <Link className="secondary-button" href="/series" style={{ marginTop: 8, fontSize: 13, padding: "6px 12px" }}>去连载系列</Link>
                </div>
              )}
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
    const authorName = article.author.displayName || article.author.username;
    return (
      <article className="surface discover-feed-card">
        <div className="discover-feed-card__header">
          <div className="discover-feed-card__author">
            <Avatar label={authorName.slice(0, 1)} size="sm" />
            <span className="discover-feed-card__author-name">{authorName}</span>
            <span className="discover-feed-card__time">· {timeLabel(article.publishedAt)}</span>
          </div>
          <span className="discover-feed-card__kind"><BookOpen size={13} /> 文章</span>
        </div>
        <Link href={article.canonicalPath}>
          <h2>{article.title}</h2>
        </Link>
        <p>{article.summary || "作者暂未填写摘要，打开文章阅读全文。"}</p>
        <div className="discover-feed-card__footer">
          <div className="discover-feed-card__meta">
            <span className="discover-feed-card__meta-item"><Clock size={13} /> {article.readingTimeMinutes} 分钟阅读</span>
            <span className="discover-feed-card__meta-item"><ThumbsUp size={13} /> {article.likeCount} 赞</span>
            <span className="discover-feed-card__meta-item"><MessageSquare size={13} /> {article.commentCount} 评</span>
          </div>
          {article.tags.length > 0 && (
            <div className="discover-feed-card__tags">
              {article.tags.map((tag) => (
                <Link href={`/tags?tag=${encodeURIComponent(tag.slug)}`} key={tag.tagId}>
                  <TagIcon size={11} />
                  <span>{tag.name}</span>
                </Link>
              ))}
            </div>
          )}
        </div>
      </article>
    );
  }
  const moment = item.value;
  const authorName = moment.author.displayName || moment.author.username;
  return (
    <article className="surface discover-feed-card">
      <div className="discover-feed-card__header">
        <div className="discover-feed-card__author">
          <Avatar label={authorName.slice(0, 1)} size="sm" />
          <span className="discover-feed-card__author-name">{authorName}</span>
          <span className="discover-feed-card__time">· {timeLabel(moment.createdAt)}</span>
        </div>
        <span className="discover-feed-card__kind"><Orbit size={13} /> 动态</span>
      </div>
      <Link href={`/moments/${moment.momentId}`}>
        <h2>{authorName} 的动态</h2>
      </Link>
      <p>{moment.textContent || "查看这条动态的内容与互动。"}</p>
      <div className="discover-feed-card__footer">
        <div className="discover-feed-card__meta">
          <span>来自 {moment.blog.name}</span>
          <span className="discover-feed-card__meta-item"><ThumbsUp size={13} /> {moment.likeCount} 赞</span>
          <span className="discover-feed-card__meta-item"><MessageSquare size={13} /> {moment.commentCount} 评</span>
        </div>
      </div>
    </article>
  );
}


