"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Compass,
  Flame,
  Hash,
  Layers,
  Loader2,
  MessageCircle,
  Search,
  Sparkles,
  Users,
  Clock,
  ThumbsUp,
  BookOpen,
  Orbit,
  ArrowRight,
} from "lucide-react";
import { UserTopbar, Avatar, EmptyState } from "../components/prototype-ui";
import {
  communityApi,
  type PublicArticleSummary,
  type PlatformTag,
  type Series,
  type Moment,
  type TeamSummary,
} from "../lib/community-api";
import { blogTypeLabel, serializationLabel } from "../lib/series-labels";

/* ─────────────────────────── helpers ─────────────────────────── */

function timeLabel(value: string) {
  const distance = Date.now() - new Date(value).getTime();
  if (distance < 60_000) return "刚刚";
  if (distance < 3_600_000) return `${Math.max(1, Math.floor(distance / 60_000))} 分钟前`;
  if (distance < 86_400_000) return `${Math.floor(distance / 86_400_000)} 天前`;
  return `${Math.floor(distance / 86_400_000)} 天前`;
}

/* ─────────────────────── 1. DiscoverArticleCard ─────────────────────── */

function DiscoverArticleCard({ item, rank }: { item: PublicArticleSummary; rank?: number }) {
  const authorName = item.author.displayName || item.author.username;
  return (
    <article
      className="surface"
      style={{ borderRadius: 10, padding: 16, minWidth: 300, flex: "1 1 300px", display: "flex", flexDirection: "column", gap: 10 }}
    >
      {/* 作者 + 时间 + 排名 */}
      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
        {rank !== undefined && (
          <span style={{ fontSize: 13, fontWeight: 700, color: rank <= 3 ? "var(--primary, #6366f1)" : "var(--text-tertiary)", minWidth: 22 }}>
            #{rank}
          </span>
        )}
        <Avatar label={authorName.slice(0, 1)} size="sm" />
        <span style={{ fontSize: 13, fontWeight: 600 }}>{authorName}</span>
        <span className="muted" style={{ fontSize: 12 }}>· {timeLabel(item.publishedAt)}</span>
      </div>

      {/* 标题 */}
      <Link href={item.canonicalPath} style={{ textDecoration: "none", color: "var(--text-primary)" }}>
        <h3 style={{ margin: 0, fontSize: 15, fontWeight: 700, lineHeight: 1.4 }}>{item.title}</h3>
      </Link>

      {/* 摘要 */}
      {item.summary && (
        <p
          style={{
            margin: 0,
            fontSize: 13,
            color: "var(--text-secondary)",
            lineHeight: 1.6,
            display: "-webkit-box",
            WebkitLineClamp: 2,
            WebkitBoxOrient: "vertical",
            overflow: "hidden",
          }}
        >
          {item.summary}
        </p>
      )}

      {/* 标签 */}
      {item.tags.length > 0 && (
        <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
          {item.tags.map((tag) => (
            <Link
              key={tag.tagId}
              href={`/articles?tag=${encodeURIComponent(tag.slug)}`}
              className="chip"
              style={{ fontSize: 11, padding: "2px 8px", textDecoration: "none", display: "flex", alignItems: "center", gap: 3 }}
            >
              <Hash size={10} /> {tag.name}
            </Link>
          ))}
        </div>
      )}

      {/* 元数据 */}
      <div style={{ display: "flex", alignItems: "center", gap: 14, marginTop: "auto", paddingTop: 8, borderTop: "1px solid var(--border, #e5e7eb)" }}>
        <span style={{ fontSize: 12, color: "var(--text-tertiary)", display: "flex", alignItems: "center", gap: 4 }}>
          <Clock size={12} /> {item.readingTimeMinutes} 分钟
        </span>
        <span style={{ fontSize: 12, color: "var(--text-tertiary)", display: "flex", alignItems: "center", gap: 4 }}>
          <ThumbsUp size={12} /> {item.likeCount}
        </span>
        <span style={{ fontSize: 12, color: "var(--text-tertiary)", display: "flex", alignItems: "center", gap: 4 }}>
          <MessageCircle size={12} /> {item.commentCount}
        </span>
      </div>
    </article>
  );
}

/* ─────────────────────── 2. SeriesExploreCard ─────────────────────── */

function SeriesExploreCard({ item }: { item: Series }) {
  const statusColor =
    item.serializationStatus === "ONGOING"
      ? "var(--color-success, #22c55e)"
      : item.serializationStatus === "COMPLETED"
        ? "var(--color-info, #3b82f6)"
        : "var(--text-tertiary)";

  const statusLabel =
    item.serializationStatus === "ONGOING"
      ? "连载中"
      : item.serializationStatus === "COMPLETED"
        ? "已完结"
        : "暂停中";

  const progress =
    item.chapterCount > 0 && item.viewerReadChapterCount > 0
      ? Math.round((item.viewerReadChapterCount / item.chapterCount) * 100)
      : null;

  return (
    <article
      className="surface"
      style={{ borderRadius: 10, padding: 16, minWidth: 280, flex: "1 1 280px", display: "flex", flexDirection: "column", gap: 8 }}
    >
      {/* 状态标签 */}
      <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
        <span
          style={{
            fontSize: 11,
            fontWeight: 600,
            padding: "2px 8px",
            borderRadius: 4,
            color: statusColor,
            border: `1px solid ${statusColor}`,
          }}
        >
          {statusLabel}
        </span>
      </div>

      {/* 标题 */}
      <Link href={`/series/${item.id}`} style={{ textDecoration: "none", color: "var(--text-primary)" }}>
        <h3 style={{ margin: 0, fontSize: 15, fontWeight: 700, lineHeight: 1.4 }}>{item.title}</h3>
      </Link>

      {/* 摘要 */}
      {item.summary && (
        <p
          style={{
            margin: 0,
            fontSize: 13,
            color: "var(--text-secondary)",
            lineHeight: 1.6,
            display: "-webkit-box",
            WebkitLineClamp: 2,
            WebkitBoxOrient: "vertical",
            overflow: "hidden",
          }}
        >
          {item.summary}
        </p>
      )}

      {/* 章节数 + 来源 */}
      <div style={{ display: "flex", alignItems: "center", gap: 12, fontSize: 12, color: "var(--text-tertiary)" }}>
        <span style={{ display: "flex", alignItems: "center", gap: 4 }}>
          <BookOpen size={12} /> {item.chapterCount} 章
        </span>
        {item.blogName && (
          <span className="muted">{item.blogName} · {blogTypeLabel(item.blogType)}</span>
        )}
      </div>

      {/* 进度条 */}
      {progress !== null && (
        <div style={{ marginTop: 4 }}>
          <div style={{ fontSize: 11, color: "var(--text-tertiary)", marginBottom: 4 }}>
            阅读进度 {progress}%
          </div>
          <div style={{ height: 4, borderRadius: 2, background: "var(--border, #e5e7eb)", overflow: "hidden" }}>
            <div style={{ height: "100%", width: `${progress}%`, borderRadius: 2, background: "var(--primary, #6366f1)" }} />
          </div>
        </div>
      )}
    </article>
  );
}

/* ─────────────────────── 3. CreatorCard ─────────────────────── */

function CreatorCard({
  creator,
  rank,
}: {
  creator: { name: string; username: string; articleCount: number };
  rank?: number;
}) {
  return (
    <Link
      href={`/blogs/${encodeURIComponent(creator.username)}`}
      className="surface"
      style={{
        borderRadius: 10,
        padding: 12,
        display: "flex",
        alignItems: "center",
        gap: 10,
        textDecoration: "none",
        color: "inherit",
      }}
    >
      {rank !== undefined && (
        <span style={{ fontSize: 13, fontWeight: 700, color: rank <= 3 ? "var(--primary, #6366f1)" : "var(--text-tertiary)", minWidth: 22 }}>
          #{rank}
        </span>
      )}
      <Avatar label={creator.name.slice(0, 1)} size="md" />
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
          {creator.name}
        </div>
        <div style={{ fontSize: 12, color: "var(--text-tertiary)" }}>
          @{creator.username} · {creator.articleCount} 篇公开文章
        </div>
      </div>
    </Link>
  );
}

/* ─────────────────────── 4. ActiveTeamCard ─────────────────────── */

function ActiveTeamCard({ team }: { team: TeamSummary }) {
  return (
    <Link
      href={`/teams/${encodeURIComponent(team.slug)}`}
      className="surface"
      style={{
        borderRadius: 10,
        padding: 12,
        display: "flex",
        alignItems: "center",
        gap: 10,
        textDecoration: "none",
        color: "inherit",
      }}
    >
      <Avatar label={team.name.slice(0, 1)} size="md" />
      <div style={{ minWidth: 0, flex: 1 }}>
        <div style={{ fontSize: 14, fontWeight: 600, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
          {team.name}
        </div>
        {team.summary && (
          <div
            style={{
              fontSize: 12,
              color: "var(--text-secondary)",
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis",
            }}
          >
            {team.summary}
          </div>
        )}
        <div style={{ fontSize: 12, color: "var(--text-tertiary)", marginTop: 2 }}>
          {team.articleCount} 篇文章 · {team.followerCount} 关注
        </div>
      </div>
    </Link>
  );
}

/* ─────────────────────── 5. MomentPreviewCard ─────────────────────── */

function MomentPreviewCard({ item }: { item: Moment }) {
  const authorName = item.author.displayName || item.author.username;
  const preview = item.textContent
    ? item.textContent.length > 120
      ? item.textContent.slice(0, 120) + "…"
      : item.textContent
    : "查看这条动态的内容与互动。";

  return (
    <Link
      href={item.canonicalPath}
      className="surface"
      style={{
        borderRadius: 10,
        padding: 16,
        minWidth: 280,
        flex: "1 1 280px",
        display: "flex",
        flexDirection: "column",
        gap: 8,
        textDecoration: "none",
        color: "inherit",
      }}
    >
      {/* 作者 */}
      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
        <Avatar label={authorName.slice(0, 1)} size="sm" />
        <span style={{ fontSize: 13, fontWeight: 600 }}>{authorName}</span>
        <span className="muted" style={{ fontSize: 12 }}>· {timeLabel(item.createdAt)}</span>
      </div>

      {/* 正文摘要 */}
      <p
        style={{
          margin: 0,
          fontSize: 13,
          color: "var(--text-secondary)",
          lineHeight: 1.6,
          display: "-webkit-box",
          WebkitLineClamp: 3,
          WebkitBoxOrient: "vertical",
          overflow: "hidden",
        }}
      >
        {preview}
      </p>

      {/* 互动数据 */}
      <div style={{ display: "flex", alignItems: "center", gap: 14, marginTop: "auto", paddingTop: 8, borderTop: "1px solid var(--border, #e5e7eb)" }}>
        <span style={{ fontSize: 12, color: "var(--text-tertiary)", display: "flex", alignItems: "center", gap: 4 }}>
          <ThumbsUp size={12} /> {item.likeCount}
        </span>
        <span style={{ fontSize: 12, color: "var(--text-tertiary)", display: "flex", alignItems: "center", gap: 4 }}>
          <MessageCircle size={12} /> {item.commentCount}
        </span>
      </div>
    </Link>
  );
}

/* ════════════════════════════════════════════════════════════════════════ */
/*                          DiscoverPage (Explore Hub)                     */
/* ════════════════════════════════════════════════════════════════════════ */

export default function DiscoverPage() {
  const router = useRouter();
  const [searchInput, setSearchInput] = useState("");
  const [articles, setArticles] = useState<PublicArticleSummary[]>([]);
  const [tags, setTags] = useState<PlatformTag[]>([]);
  const [seriesList, setSeriesList] = useState<Series[]>([]);
  const [moments, setMoments] = useState<Moment[]>([]);
  const [teams, setTeams] = useState<TeamSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    Promise.all([
      communityApi.discoverRankedArticles("QUALITY", 1, 5),
      communityApi.tags(),
      communityApi.series().catch(() => [] as Series[]),
      communityApi.moments(1, 5),
      communityApi.teams(),
    ])
      .then(([articlePage, tagRecords, seriesRecords, momentPage, teamRecords]) => {
        if (!active) return;
        setArticles(articlePage.records);
        setTags(tagRecords);
        setSeriesList(seriesRecords);
        setMoments(momentPage.records);
        setTeams(teamRecords);
      })
      .catch((err: unknown) => {
        if (active) setError(err instanceof Error ? err.message : "加载失败");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const handleTopicClick = useCallback(
    (slug: string) => {
      router.push(`/articles?tag=${encodeURIComponent(slug)}`);
    },
    [router],
  );

  const creators = useMemo(() => {
    const seen = new Map<string, { name: string; username: string; articleCount: number }>();
    articles.forEach((article) => {
      const key = article.author.userId;
      const prev = seen.get(key);
      seen.set(key, {
        name: article.author.displayName || article.author.username,
        username: article.author.username,
        articleCount: (prev?.articleCount || 0) + 1,
      });
    });
    if (seen.size < 5) {
      moments.forEach((m) => {
        const key = m.author.userId;
        if (!seen.has(key)) {
          seen.set(key, {
            name: m.author.displayName || m.author.username,
            username: m.author.username,
            articleCount: 1,
          });
        }
      });
    }
    return [...seen.values()].slice(0, 5);
  }, [articles, moments]);

  const handleSearch = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      if (searchInput.trim()) router.push(`/articles?q=${encodeURIComponent(searchInput.trim())}`);
    },
    [searchInput, router],
  );

  const isAllEmpty =
    articles.length === 0 &&
    tags.length === 0 &&
    seriesList.length === 0 &&
    moments.length === 0 &&
    teams.length === 0;

  return (
    <>
      <UserTopbar title="发现" />
      <main className="page-shell discover-editorial-mosaic" style={{ maxWidth: 1100, margin: "0 auto", padding: "24px 20px" }}>
        {/* 紧凑标题 + 搜索 */}
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 24 }}>
          <div>
            <h1 style={{ margin: 0, fontSize: 22, fontWeight: 700 }}>发现</h1>
            <p style={{ margin: "4px 0 0", fontSize: 13, color: "var(--text-secondary)" }}>
              探索社区中的优质内容与创作者
            </p>
          </div>
          <form
            onSubmit={handleSearch}
            className="surface"
            style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 14px", borderRadius: 8, width: 280 }}
          >
            <Search size={15} style={{ color: "var(--text-tertiary)" }} />
            <input
              type="text"
              placeholder="搜索文章..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              style={{ border: "none", background: "transparent", outline: "none", fontSize: 13, flex: 1, color: "var(--text-primary)" }}
            />
          </form>
        </div>

        {/* Loading */}
        {loading && (
          <div className="surface" style={{ padding: 40, textAlign: "center", display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
            <Loader2 className="spin" size={22} /> 加载中...
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="surface" style={{ padding: 16, color: "var(--color-error, #ef4444)" }}>{error}</div>
        )}

        {/* 主体内容 */}
        {!loading && !error && (
          <>
            {isAllEmpty ? (
              <div className="surface" style={{ padding: 40, borderRadius: 12, marginTop: 24, textAlign: "center" }}>
                <EmptyState
                  title="暂无发现内容"
                  description="社区尚未发布公开内容，点击顶部导航去其他频道逛逛吧。"
                />
              </div>
            ) : (
              <>
                {/* ── 热门主题 ── */}
                {tags.length > 0 && (
                  <section style={{ marginBottom: 32 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                      <h2 style={{ margin: 0, fontSize: 17, fontWeight: 700, display: "flex", alignItems: "center", gap: 8 }}>
                        <Flame size={18} style={{ color: "var(--primary)" }} /> 热门主题
                      </h2>
                      <Link href="/tags" className="ghost-button" style={{ fontSize: 12 }}>查看更多 →</Link>
                    </div>
                    <div style={{ display: "flex", gap: 8, overflowX: "auto", paddingBottom: 4 }}>
                      {tags
                        .slice()
                        .sort((a, b) => b.usageCount - a.usageCount)
                        .slice(0, 10)
                        .map((tag) => (
                          <Link
                            key={tag.tagId}
                            href={`/articles?tag=${encodeURIComponent(tag.slug)}`}
                            onClick={() => handleTopicClick(tag.slug)}
                            className="chip"
                            style={{ fontSize: 12, padding: "5px 12px", textDecoration: "none", display: "flex", alignItems: "center", gap: 4, whiteSpace: "nowrap", flexShrink: 0 }}
                          >
                            <Hash size={12} /> {tag.name} <span className="muted" style={{ fontSize: 11 }}>{tag.usageCount}</span>
                          </Link>
                        ))}
                    </div>
                  </section>
                )}

                {/* ── 值得阅读 ── */}
                {articles.length > 0 && (
                  <section style={{ marginBottom: 32 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                      <h2 style={{ margin: 0, fontSize: 17, fontWeight: 700, display: "flex", alignItems: "center", gap: 8 }}>
                        <Sparkles size={18} style={{ color: "var(--primary)" }} /> 值得阅读
                      </h2>
                      <Link href="/articles" className="ghost-button" style={{ fontSize: 12 }}>查看更多 →</Link>
                    </div>
                    <div style={{ display: "flex", gap: 12, overflowX: "auto", paddingBottom: 4 }}>
                      {articles.map((item, idx) => (
                        <DiscoverArticleCard item={item} rank={idx + 1} key={item.articleId} />
                      ))}
                    </div>
                  </section>
                )}

                {/* ── 正在连载 ── */}
                {seriesList.length > 0 && (
                  <section style={{ marginBottom: 32 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                      <h2 style={{ margin: 0, fontSize: 17, fontWeight: 700, display: "flex", alignItems: "center", gap: 8 }}>
                        <Layers size={18} style={{ color: "var(--primary)" }} /> 正在连载
                      </h2>
                      <Link href="/series" className="ghost-button" style={{ fontSize: 12 }}>查看更多 →</Link>
                    </div>
                    <div style={{ display: "flex", gap: 12, overflowX: "auto", paddingBottom: 4 }}>
                      {seriesList.slice(0, 5).map((item) => (
                        <SeriesExploreCard item={item} key={item.id} />
                      ))}
                    </div>
                  </section>
                )}

                {/* ── 创作者 + 团队 并排 ── */}
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 24, marginBottom: 32 }}>
                  {/* 值得关注的创作者 */}
                  {creators.length > 0 && (
                    <section>
                      <h2 style={{ margin: "0 0 12px", fontSize: 17, fontWeight: 700, display: "flex", alignItems: "center", gap: 8 }}>
                        <Users size={18} style={{ color: "var(--primary)" }} /> 值得关注的创作者
                      </h2>
                      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                        {creators.map((creator, idx) => (
                          <CreatorCard creator={creator} rank={idx + 1} key={creator.username} />
                        ))}
                      </div>
                    </section>
                  )}

                  {/* 公开团队 */}
                  {teams.length > 0 && (
                    <section>
                      <h2 style={{ margin: "0 0 12px", fontSize: 17, fontWeight: 700, display: "flex", alignItems: "center", gap: 8 }}>
                        <Users size={18} style={{ color: "var(--primary)" }} /> 公开团队
                      </h2>
                      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                        {teams.slice(0, 5).map((team) => (
                          <ActiveTeamCard team={team} key={team.teamId} />
                        ))}
                      </div>
                    </section>
                  )}
                </div>

                {/* ── 正在发生的动态 ── */}
                {moments.length > 0 && (
                  <section style={{ marginBottom: 32 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                      <h2 style={{ margin: 0, fontSize: 17, fontWeight: 700, display: "flex", alignItems: "center", gap: 8 }}>
                        <Orbit size={18} style={{ color: "var(--primary)" }} /> 正在发生的动态
                      </h2>
                      <Link href="/moments" className="ghost-button" style={{ fontSize: 12 }}>查看更多 →</Link>
                    </div>
                    <div style={{ display: "flex", gap: 12, overflowX: "auto", paddingBottom: 4 }}>
                      {moments.map((item) => (
                        <MomentPreviewCard item={item} key={item.momentId} />
                      ))}
                    </div>
                  </section>
                )}
              </>
            )}
          </>
        )}
      </main>
    </>
  );
}
