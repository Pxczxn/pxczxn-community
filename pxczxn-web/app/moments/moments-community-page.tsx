"use client";

import {
  AlertTriangle,
  Bookmark,
  Check,
  ChevronDown,
  Code2,
  FileText,
  Filter,
  Globe2,
  Hash,
  Image as ImageIcon,
  Link2,
  Lock,
  LoaderCircle,
  Maximize2,
  MessageCircle,
  Minimize2,
  Quote,
  RefreshCw,
  Repeat2,
  Rocket,
  Send,
  Share2,
  Smile,
  Star,
  TrendingUp,
  Users,
  UserPlus,
  Video,
  X,
} from "lucide-react";
import {
  FormEvent,
  MouseEvent as ReactMouseEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { Avatar, EmptyState, UserTopbar } from "../components/prototype-ui";
import {
  CommentThread,
  Moment,
  MomentFeedFilter,
  PlatformTag,
  communityApi,
  publicFileUrl,
} from "../lib/community-api";
import {
  MomentActions,
  MomentBody,
  messageOf,
  relativeTime,
} from "./moment-parts";

/* ─── 左侧导航数据 ─── */
const NAV_ITEMS: { key: string; label: string; Icon: React.ComponentType<{ size?: number }>; dot?: boolean }[] = [
  { key: "recommended", label: "推荐", Icon: Star },
  { key: "following", label: "关注", Icon: Users },
  { key: "latest", label: "最新", Icon: RefreshCw },
  { key: "team", label: "团队动态", Icon: Users },
  { key: "mine", label: "我的互动", Icon: MessageCircle, dot: true },
];

/* ─── 热门话题与创作者从 API 动态加载 ─── */

/** 格式化数字：≥1000 显示为 1.2k，否则原样 */
function formatCount(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}k`;
  return String(n);
}

/** 从动态列表聚合作者（去重，按活跃度排序） */
function deriveCreators(list: Moment[]) {
  const map = new Map<string, {
    blogId: string;
    name: string;
    avatar: string | null;
    momentCount: number;
    totalLikes: number;
  }>();
  for (const m of list) {
    const key = m.blog.blogId;
    const existing = map.get(key);
    const name = m.author.displayName || m.author.username;
    const avatar = publicFileUrl(m.author.avatarFileId);
    if (existing) {
      existing.momentCount += 1;
      existing.totalLikes += m.likeCount;
    } else {
      map.set(key, { blogId: key, name, avatar, momentCount: 1, totalLikes: m.likeCount });
    }
  }
  return Array.from(map.values())
    .sort((a, b) => b.totalLikes - a.totalLikes)
    .slice(0, 5)
    .map((c) => ({
      ...c,
      desc: `${c.momentCount} 条动态 · ${c.totalLikes} 次互动`,
    }));
}

export function MomentsCommunityPage({ initialMomentId }: { initialMomentId?: string }) {
  const [moments, setMoments] = useState<Moment[]>([]);
  const [selected, setSelected] = useState<Moment | null>(null);
  const [comments, setComments] = useState<CommentThread[]>([]);
  const [text, setText] = useState("");
  const [linkUrl, setLinkUrl] = useState("");
  const [visibility, setVisibility] = useState("PUBLIC");
  const [visibilityOpen, setVisibilityOpen] = useState(false);
  const [commentText, setCommentText] = useState("");
  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] = useState(false);
  const [busyAction, setBusyAction] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [activeNav, setActiveNav] = useState<string>("recommended");
  const [activeTab, setActiveTab] = useState<string>("recommended");
  const [composerFullscreen, setComposerFullscreen] = useState(false);
  const [hotTopics, setHotTopics] = useState<PlatformTag[]>([]);
  const [followingBlogIds, setFollowingBlogIds] = useState<Set<string>>(new Set());
  // 公共流筛选态（仅 recommended / latest 生效；following / mine 不应用）
  const [momentTypeFilter, setMomentTypeFilter] = useState<string[]>([]);
  const [filterDraft, setFilterDraft] = useState<string[]>([]);
  const [filterOpen, setFilterOpen] = useState(false);
  const filterContainerRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const publicTab = activeTab === "recommended" || activeTab === "latest";

  const loadComments = useCallback(async (momentId: string) => {
    try {
      const page = await communityApi.comments("MOMENT", momentId, 1, 20);
      setComments(page.records);
    } catch {
      setComments([]);
    }
  }, []);

  /** 加载热门话题（平台标签，按使用量倒序） */
  const loadHotTags = useCallback(async () => {
    try {
      const tags = await communityApi.tags();
      setHotTopics(tags.slice(0, 8));
    } catch {
      // 静默降级，不阻塞主流程
    }
  }, []);

  /** 公共流筛选项：仅在 recommended / latest 时下发到后端 */
  const publicFeedFilter = useMemo<MomentFeedFilter | undefined>(() => {
    if (!publicTab || momentTypeFilter.length === 0) return undefined;
    return { momentTypes: momentTypeFilter };
  }, [publicTab, momentTypeFilter]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      let page: { records: Moment[]; total: number };
      switch (activeTab) {
        case "mine":
          page = await communityApi.myMoments(1, 20);
          break;
        case "recommended": {
          // 推荐模式：拉更多条，按热度公式本地排序（受 momentTypeFilter 约束）
          const raw = await communityApi.moments(1, 40, publicFeedFilter);
          const records = publicFeedFilter
            ? raw.records
            : raw.records
                .map((m) => ({
                  moment: m,
                  score: m.likeCount + m.favoriteCount * 2 + m.commentCount * 3,
                }))
                .sort((a, b) => b.score - a.score)
                .map((s) => s.moment);
          page = { records: records.slice(0, 20), total: raw.total };
          break;
        }
        case "following": {
          // 关注流：取 followingFeed 中 MOMENT 类型的 targetId，批量拉详情
          const feed = await communityApi.followingFeed(1, 20);
          const momentIds = feed.records
            .filter((item) => item.itemType === "MOMENT")
            .map((item) => item.targetId);
          if (momentIds.length === 0) {
            page = { records: [], total: 0 };
          } else {
            const detailResults = await Promise.all(
              momentIds.map((id) => communityApi.moment(id).catch(() => null))
            );
            page = {
              records: detailResults.filter((m): m is Moment => m !== null),
              total: feed.total,
            };
          }
          break;
        }
        default:
          page = await communityApi.moments(1, 20, publicFeedFilter);
          break;
      }
      let nextSelected = page.records[0] ?? null;
      if (initialMomentId) {
        nextSelected =
          page.records.find((item) => item.momentId === initialMomentId)
          ?? await communityApi.moment(initialMomentId);
      }
      setMoments(page.records);
      setSelected(nextSelected);
      if (nextSelected) await loadComments(nextSelected.momentId);
    } catch (requestError) {
      setError(messageOf(requestError, "动态加载失败"));
    } finally {
      setLoading(false);
    }
  }, [initialMomentId, loadComments, activeTab, publicFeedFilter]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadHotTags(), 0);
    return () => window.clearTimeout(timer);
  }, [loadHotTags]);

  useEffect(() => {
    if (!visibilityOpen) return;
    const handler = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      if (!target.closest(".visibility-dropdown")) setVisibilityOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [visibilityOpen]);

  useEffect(() => {
    if (!filterOpen) return;
    const handler = (event: MouseEvent) => {
      const container = filterContainerRef.current;
      if (container && !container.contains(event.target as Node)) {
        setFilterOpen(false);
      }
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setFilterOpen(false);
    };
    document.addEventListener("mousedown", handler);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", handler);
      document.removeEventListener("keydown", onKey);
    };
  }, [filterOpen]);

  useEffect(() => {
    if (!composerFullscreen) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setComposerFullscreen(false);
    };
    document.addEventListener("keydown", onKey);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const focusTimer = window.setTimeout(() => textareaRef.current?.focus(), 60);
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = previousOverflow;
      window.clearTimeout(focusTimer);
    };
  }, [composerFullscreen, textareaRef]);

  async function publish(event: FormEvent) {
    event.preventDefault();
    if (!text.trim() && !linkUrl.trim()) {
      setError("请先写下想分享的内容，或添加一个链接。");
      return;
    }
    setPublishing(true);
    setError("");
    setNotice("");
    try {
      const result = await communityApi.publishMoment({
        momentType: linkUrl.trim() ? "LINK" : "TEXT",
        textContent: text.trim() || undefined,
        linkUrl: linkUrl.trim() || undefined,
        visibility,
      });
      setMoments((current) => [result.moment, ...current]);
      setSelected(result.moment);
      setComments([]);
      setText("");
      setLinkUrl("");
      setNotice(result.moderationWarning ? "动态已发布，部分内容经过安全处理。" : "动态发布成功。");
      setComposerFullscreen(false);
    } catch (requestError) {
      setError(messageOf(requestError, "动态发布失败"));
    } finally {
      setPublishing(false);
    }
  }

  async function chooseMoment(moment: Moment) {
    setSelected(moment);
    setComments([]);
    await loadComments(moment.momentId);
  }

  async function toggleLike(moment: Moment) {
    const action = `like-${moment.momentId}`;
    setBusyAction(action);
    setError("");
    try {
      const relationship = await communityApi.setLike("MOMENT", moment.momentId, !moment.liked);
      updateMoment(moment.momentId, {
        liked: relationship.liked,
        likeCount: relationship.likeCount,
      });
    } catch (requestError) {
      setError(messageOf(requestError, "点赞失败"));
    } finally {
      setBusyAction("");
    }
  }

  async function toggleFavorite(moment: Moment) {
    const action = `favorite-${moment.momentId}`;
    setBusyAction(action);
    setError("");
    try {
      const relationship = await communityApi.setFavorite(
        "MOMENT",
        moment.momentId,
        !moment.favorited,
      );
      updateMoment(moment.momentId, {
        favorited: relationship.favorited,
        favoriteCount: relationship.favoriteCount,
      });
    } catch (requestError) {
      setError(messageOf(requestError, "收藏失败"));
    } finally {
      setBusyAction("");
    }
  }

  async function copyShare(moment: Moment) {
    const url = new URL(`/moments/${moment.momentId}`, window.location.origin).toString();
    await navigator.clipboard.writeText(url);
    setNotice("动态链接已复制。");
  }

  async function toggleCreatorFollow(c: { blogId: string; followed: boolean }) {
    try {
      await communityApi.setBlogFollow(c.blogId, !c.followed);
      setFollowingBlogIds((prev) => {
        const next = new Set(prev);
        if (c.followed) next.delete(c.blogId); else next.add(c.blogId);
        return next;
      });
    } catch {
      // 静默失败，不弹错误
    }
  }

  async function submitComment(event: FormEvent) {
    event.preventDefault();
    if (!selected || !commentText.trim()) return;
    setBusyAction("comment");
    setError("");
    try {
      await communityApi.createComment("MOMENT", selected.momentId, commentText.trim());
      setCommentText("");
      await loadComments(selected.momentId);
      updateMoment(selected.momentId, { commentCount: selected.commentCount + 1 });
    } catch (requestError) {
      setError(messageOf(requestError, "评论发布失败"));
    } finally {
      setBusyAction("");
    }
  }

  function updateMoment(momentId: string, patch: Partial<Moment>) {
    setMoments((current) =>
      current.map((item) => item.momentId === momentId ? { ...item, ...patch } : item),
    );
    setSelected((current) =>
      current?.momentId === momentId ? { ...current, ...patch } : current,
    );
  }

  return (
    <>
      <UserTopbar title="动态" />
      <main className="moments-page page-shell">
        {/* ─── 左侧导航栏 ─── */}
        <aside className="moments-sidebar">
          <nav className="moments-nav">
            <h3 className="moments-nav__title">动态导航</h3>
            <ul className="moments-nav__list">
              {NAV_ITEMS.map(({ key, label, Icon, dot }) => (
                <li key={key}>
                  <button
                    className={`moments-nav__item ${activeNav === key ? "active" : ""}`}
                    onClick={() => { setActiveNav(key); setActiveTab(key); }}
                    type="button"
                  >
                    <Icon size={17} />
                    <span>{label}</span>
                    {dot && <span className="moments-nav__dot" />}
                  </button>
                </li>
              ))}
            </ul>
          </nav>

          <section className="moments-sidebar__topics">
            <div className="moments-sidebar__topics-header">
              <h4>
                <TrendingUp size={15} />
                热门话题
              </h4>
              <button type="button">换一换</button>
            </div>
            <ul className="moments-sidebar__topic-list">
              {hotTopics.slice(0, 5).map((t) => (
                <li key={t.tagId}>
                  <button type="button">
                    <Hash size={14} />
                    <span className="topic-tag">{t.name}</span>
                    <span className="topic-count">{formatCount(t.usageCount)}讨论</span>
                  </button>
                </li>
              ))}
            </ul>
            <button className="moments-sidebar__more-btn" type="button">查看全部话题</button>
          </section>
        </aside>

        {/* ─── 中间信息流 ─── */}
        <section className="moments-feed stack">
          {/* 发布框 */}
          <ComposerForm
            variant="inline"
            text={text}
            linkUrl={linkUrl}
            visibility={visibility}
            visibilityOpen={visibilityOpen}
            publishing={publishing}
            onTextChange={setText}
            onLinkChange={setLinkUrl}
            onVisibilityChange={setVisibility}
            onVisibilityOpenChange={setVisibilityOpen}
            onSubmit={publish}
            onRequestFullscreen={() => setComposerFullscreen(true)}
          />

          {(error || notice) && (
            <div className={`inline-feedback ${error ? "error" : "success"}`} role="status">
              {error ? <AlertTriangle size={16} /> : <Check size={16} />}
              <span>{error || notice}</span>
            </div>
          )}

          {/* 标签切换栏 */}
          <div className="moments-feed__tabs">
            <div className="moments-feed__tab-bar">
              {[
                { key: "recommended", label: "推荐" },
                { key: "latest", label: "最新" },
                { key: "following", label: "关注" },
              ].map((tab) => (
                <button
                  key={tab.key}
                  className={`moments-feed__tab ${activeTab === tab.key ? "active" : ""}`}
                  onClick={() => setActiveTab(tab.key)}
                  type="button"
                >
                  {tab.label}
                </button>
              ))}
            </div>
            <div ref={filterContainerRef} className="moments-feed__filter-wrapper">
              <button
                aria-controls="moments-filter-popover"
                aria-expanded={filterOpen}
                aria-haspopup="dialog"
                aria-label="筛选动态"
                className="moments-feed__filter"
                disabled={!publicTab}
                onClick={(event: ReactMouseEvent<HTMLButtonElement>) => {
                  event.stopPropagation();
                  if (!publicTab) {
                    setNotice("");
                    setError("仅在「推荐」或「最新」页面可应用动态筛选。");
                    return;
                  }
                  setFilterDraft(momentTypeFilter);
                  setFilterOpen((open) => !open);
                }}
                title={publicTab ? "筛选动态" : "当前页面不支持筛选"}
                type="button"
              >
                <Filter size={14} />
                <span>筛选</span>
                {momentTypeFilter.length > 0 && (
                  <span className="moments-feed__filter-badge">{momentTypeFilter.length}</span>
                )}
              </button>
              {filterOpen && publicTab && (
                <FilterPopover
                  draft={filterDraft}
                  onApply={() => {
                    setMomentTypeFilter(filterDraft);
                    setFilterOpen(false);
                  }}
                  onClear={() => {
                    setFilterDraft([]);
                    setMomentTypeFilter([]);
                    setFilterOpen(false);
                  }}
                  onDraftChange={setFilterDraft}
                />
              )}
            </div>
          </div>

          <h1 className="moments-feed__title">动态广场</h1>

          {loading && (
            <div className="surface feed-loading" aria-busy="true">
              <LoaderCircle className="spin" size={23} /> 正在获取最新动态…
            </div>
          )}
          {!loading && !moments.length && (
            <div className="surface">
              <EmptyState title="动态广场还是空的" description="成为第一个分享想法的人吧。" />
            </div>
          )}
          {moments.map((moment) => (
            <MomentCard
              active={selected?.momentId === moment.momentId}
              busyAction={busyAction}
              key={moment.momentId}
              moment={moment}
              onChoose={() => void chooseMoment(moment)}
              onFavorite={() => void toggleFavorite(moment)}
              onLike={() => void toggleLike(moment)}
              onShare={() => void copyShare(moment)}
            />
          ))}
        </section>

        {/* ─── 右侧面板 ─── */}
        <aside className="moments-right-panel stack">
          {/* 动态详情卡片 */}
          <section className="surface moments-detail-card">
            <header className="moments-detail-card__header">
              <h2>动态详情</h2>
              <a href="#" className="moments-detail-card__view-all">查看全部 →</a>
            </header>
            {!selected ? (
              <div className="moments-detail-card__empty">
                <p>点击左侧动态查看详情和评论。</p>
              </div>
            ) : (
              <div className="moments-detail-card__body">
                <div className="moment-author-row">
                  {(() => {
                    const avatar = publicFileUrl(selected.author.avatarFileId);
                    const authorName = selected.author.displayName || selected.author.username;
                    return avatar ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img alt="" className="avatar avatar-md avatar-image" src={avatar} />
                    ) : (
                      <Avatar label={authorName.slice(0, 1)} size="md" />
                    );
                  })()}
                  <span>
                    <strong>{selected.author.displayName || selected.author.username}</strong>
                    <small>{relativeTime(selected.createdAt)}</small>
                  </span>
                </div>
                {selected.renderedHtml ? (
                  <div className="moment-copy" dangerouslySetInnerHTML={{ __html: selected.renderedHtml }} />
                ) : (
                  <p className="moment-copy">{selected.textContent}</p>
                )}
                <div className="moments-detail-card__stats">
                  <span><MessageCircle size={14} /> {selected.likeCount}</span>
                  <span><MessageCircle size={14} /> {selected.commentCount}</span>
                  <span><Share2 size={14} /> 分享</span>
                  <span><Bookmark size={14} /> {selected.favoriteCount}</span>
                </div>
                <form className="moments-detail-card__comment-form" onSubmit={submitComment}>
                  <Avatar label="我" size="sm" />
                  <input
                    className="field"
                    onChange={(event) => setCommentText(event.target.value)}
                    placeholder="写下你的评论..."
                    value={commentText}
                  />
                  <button aria-label="发送评论" className="icon-button" disabled={busyAction === "comment"} type="submit">
                    <Send size={16} />
                  </button>
                </form>
                {comments.length > 0 && (
                  <div className="moments-detail-card__comments-preview">
                    {comments.slice(0, 3).map((thread) => (
                      <CommentRowCompact key={thread.root.commentId} thread={thread} />
                    ))}
                    {comments.length > 3 && (
                      <a href="#" className="moments-detail-card__more-comments">
                        查看全部 {selected.commentCount} 条评论 ›
                      </a>
                    )}
                  </div>
                )}
              </div>
            )}
          </section>

          {/* 热门话题 */}
          <section className="surface moments-right-card">
            <header className="moments-right-card__header">
              <h3>
                <TrendingUp size={15} />
                热门话题
              </h3>
              <a href="#">更多 ›</a>
            </header>
            <div className="moments-topics-grid">
              {hotTopics.slice(0, 4).map((t) => (
                <a key={t.tagId} href="#" className="moments-topic-pill">
                  <Hash size={12} />
                  {t.name}
                  <span>{formatCount(t.usageCount)}讨论</span>
                </a>
              ))}
            </div>
          </section>

          {/* 创作者推荐 — 从当前列表聚合作者 */}
          <section className="surface moments-right-card">
            <header className="moments-right-card__header">
              <h3>创作者推荐</h3>
              <a href="#">更多 ›</a>
            </header>
            {moments.length === 0 ? (
              <p className="muted" style={{ padding: "8px 12px", fontSize: 13 }}>加载动态后显示活跃作者</p>
            ) : (
              <ul className="moments-creator-list">
                {deriveCreators(moments).map((c) => ({
                  ...c,
                  followed: followingBlogIds.has(c.blogId),
                })).map((c) => (
                  <li key={c.blogId} className="moments-creator-item">
                    {c.avatar ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img alt="" className="avatar avatar-sm avatar-image" src={c.avatar} />
                    ) : (
                      <Avatar label={c.name.slice(0, 1)} size="sm" />
                    )}
                    <div className="moments-creator-info">
                      <strong>{c.name}</strong>
                      <small>{c.desc}</small>
                    </div>
                    <button
                      className={`${c.followed ? "secondary-button" : "primary-button"} moments-creator-follow`}
                      onClick={() => void toggleCreatorFollow(c)}
                      type="button"
                    >
                      {c.followed ? "已关注" : <><UserPlus size={13} /> 关注</>}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </aside>
      </main>

      {/* 全屏编辑模态 */}
      {composerFullscreen && (
        <div
          className="composer-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="全屏编辑动态"
          onClick={(event) => { if (event.target === event.currentTarget) setComposerFullscreen(false); }}
        >
          <ComposerForm
            variant="fullscreen"
            text={text}
            linkUrl={linkUrl}
            visibility={visibility}
            visibilityOpen={visibilityOpen}
            publishing={publishing}
            onTextChange={setText}
            onLinkChange={setLinkUrl}
            onVisibilityChange={setVisibility}
            onVisibilityOpenChange={setVisibilityOpen}
            onSubmit={publish}
            onRequestClose={() => setComposerFullscreen(false)}
            textareaRef={textareaRef}
          />
        </div>
      )}
    </>
  );
}

/* ─── 发布框（普通 / 全屏两种形态复用） ─── */
interface ComposerFormProps {
  variant: "inline" | "fullscreen";
  text: string;
  linkUrl: string;
  visibility: string;
  visibilityOpen: boolean;
  publishing: boolean;
  onTextChange: (value: string) => void;
  onLinkChange: (value: string) => void;
  onVisibilityChange: (value: string) => void;
  onVisibilityOpenChange: (open: boolean) => void;
  onSubmit: (event: FormEvent) => void;
  onRequestFullscreen?: () => void;
  onRequestClose?: () => void;
  textareaRef?: React.RefObject<HTMLTextAreaElement | null>;
}

function ComposerForm(props: ComposerFormProps) {
  const {
    variant,
    text,
    linkUrl,
    visibility,
    visibilityOpen,
    publishing,
    onTextChange,
    onLinkChange,
    onVisibilityChange,
    onVisibilityOpenChange,
    onSubmit,
    onRequestFullscreen,
    onRequestClose,
    textareaRef,
  } = props;
  const isFull = variant === "fullscreen";
  return (
    <form
      className={`surface composer ${isFull ? "composer--fullscreen" : ""}`}
      onSubmit={onSubmit}
    >
      <div className="composer__input-row">
        <Avatar label="我" size={isFull ? "lg" : "md"} />
        <textarea
          ref={textareaRef}
          aria-label="分享你的想法"
          onChange={(event) => onTextChange(event.target.value)}
          placeholder={isFull ? "在这里写下你的想法，全屏模式下更专注…" : "分享你的想法..."}
          value={text}
        />
      </div>
      {linkUrl !== "" && (
        <input
          aria-label="动态链接"
          className="field composer-link-field"
          onChange={(event) => onLinkChange(event.target.value)}
          placeholder="https://example.com"
          type="url"
          value={linkUrl}
        />
      )}
      <div className="composer-toolbar">
        <div className="composer-toolbar__tools">
          <button disabled type="button"><ImageIcon size={16} /> 图片</button>
          <button onClick={() => onLinkChange(linkUrl ? "" : "https://")} type="button">
            <Link2 size={16} /> 链接
          </button>
          <button disabled type="button"><MessageCircle size={16} /> 投票</button>
          <button disabled type="button"><Smile size={16} /> 话题</button>
        </div>
        <div className="composer-toolbar__actions">
          {!isFull && (
            <button
              aria-label="全屏编辑"
              className="composer__fullscreen-btn"
              onClick={onRequestFullscreen}
              title="全屏编辑"
              type="button"
            >
              <Maximize2 size={16} />
            </button>
          )}
          <div className="visibility-dropdown">
            <button
              aria-expanded={visibilityOpen}
              aria-haspopup="listbox"
              className="visibility-dropdown__trigger"
              onClick={() => onVisibilityOpenChange(!visibilityOpen)}
              type="button"
            >
              {visibility === "PUBLIC" ? <Globe2 size={15} /> : visibility === "FOLLOWERS_ONLY" ? <Users size={15} /> : <Lock size={15} />}
              <span>{visibility === "PUBLIC" ? "公开" : visibility === "FOLLOWERS_ONLY" ? "仅粉丝" : "仅自己"}</span>
              <ChevronDown size={14} className={visibilityOpen ? "rotate-180" : ""} />
            </button>
            {visibilityOpen && (
              <div className="visibility-dropdown__menu" role="listbox">
                <button
                  className={`visibility-dropdown__item ${visibility === "PUBLIC" ? "active" : ""}`}
                  onClick={() => { onVisibilityChange("PUBLIC"); onVisibilityOpenChange(false); }}
                  type="button"
                >
                  <Globe2 size={16} />
                  <span>
                    <strong>公开</strong>
                    <small>所有人可见</small>
                  </span>
                  {visibility === "PUBLIC" && <Check size={14} />}
                </button>
                <button
                  className={`visibility-dropdown__item ${visibility === "FOLLOWERS_ONLY" ? "active" : ""}`}
                  onClick={() => { onVisibilityChange("FOLLOWERS_ONLY"); onVisibilityOpenChange(false); }}
                  type="button"
                >
                  <Users size={16} />
                  <span>
                    <strong>仅粉丝</strong>
                    <small>只有粉丝可看</small>
                  </span>
                  {visibility === "FOLLOWERS_ONLY" && <Check size={14} />}
                </button>
                <button
                  className={`visibility-dropdown__item ${visibility === "PRIVATE" ? "active" : ""}`}
                  onClick={() => { onVisibilityChange("PRIVATE"); onVisibilityOpenChange(false); }}
                  type="button"
                >
                  <Lock size={16} />
                  <span>
                    <strong>仅自己</strong>
                    <small>私密，仅自己可见</small>
                  </span>
                  {visibility === "PRIVATE" && <Check size={14} />}
                </button>
              </div>
            )}
          </div>
          {isFull && onRequestClose && (
            <button className="ghost-button composer__collapse-btn" onClick={onRequestClose} type="button">
              <Minimize2 size={16} /> 收起
            </button>
          )}
          <button className="primary-button composer__publish-btn" disabled={publishing} type="submit">
            {publishing ? <LoaderCircle className="spin" size={16} /> : <Send size={16} />}
            {publishing ? "发布中" : "发布"}
          </button>
        </div>
      </div>
    </form>
  );
}

/* ─── 紧凑型评论行（右侧详情预览用） ─── */
function CommentRowCompact({ thread }: { thread: CommentThread }) {
  const comment = thread.root;
  const authorName = comment.author?.displayName || comment.author?.username || "用户";
  return (
    <div className="comment-row-compact">
      <Avatar label={authorName.slice(0, 1)} size="sm" />
      <div>
        <strong>{authorName}</strong>
        <p>{comment.renderedHtml?.replace(/<[^>]*>/g, "").slice(0, 80) || "发表了评论…"}</p>
        <small>{relativeTime(comment.createdAt)}</small>
      </div>
    </div>
  );
}

/** 动态卡片 */
function MomentCard({
  moment,
  active,
  busyAction,
  onChoose,
  onLike,
  onFavorite,
  onShare,
}: {
  moment: Moment;
  active: boolean;
  busyAction: string;
  onChoose: () => void;
  onLike: () => void;
  onFavorite: () => void;
  onShare: () => void;
}) {
  return (
    <article className={`surface moment-card ${active ? "moment-card--active" : ""}`}>
      <div className="moment-card__inner">
        <button className="moment-card__select" onClick={onChoose} type="button">
          <MomentBody moment={moment} />
        </button>
        <MomentActions
          busyAction={busyAction}
          moment={moment}
          onFavorite={onFavorite}
          onLike={onLike}
          onShare={onShare}
        />
      </div>
    </article>
  );
}


/* ─── 筛选弹层 ─── */
interface MomentTypeOption {
  value: string;
  label: string;
  Icon: React.ComponentType<{ size?: number }>;
}

const MOMENT_TYPE_OPTIONS: MomentTypeOption[] = [
  { value: "TEXT", label: "图文", Icon: FileText },
  { value: "LINK", label: "链接", Icon: Link2 },
  { value: "ARTICLE_SHARE", label: "文章", Icon: Bookmark },
  { value: "PROJECT_UPDATE", label: "项目更新", Icon: Rocket },
  { value: "CODE", label: "代码片段", Icon: Code2 },
  { value: "REPOST", label: "转发", Icon: Repeat2 },
  { value: "QUOTE", label: "引用", Icon: Quote },
  { value: "VIDEO_LINK", label: "视频", Icon: Video },
];

interface FilterPopoverProps {
  draft: string[];
  onDraftChange: (next: string[]) => void;
  onApply: () => void;
  onClear: () => void;
}

function FilterPopover({
  draft,
  onDraftChange,
  onApply,
  onClear,
}: FilterPopoverProps) {
  const toggle = (value: string, checked: boolean) => {
    const set = new Set(draft);
    if (checked) set.add(value); else set.delete(value);
    onDraftChange(Array.from(set));
  };
  return (
    <div
      aria-label="动态筛选"
      className="moments-filter-popover"
      id="moments-filter-popover"
      role="dialog"
    >
      <h4 className="moments-filter-popover__title">动态类型</h4>
      <ul className="moments-filter-popover__list">
        {MOMENT_TYPE_OPTIONS.map(({ value, label, Icon }) => {
          const checked = draft.includes(value);
          return (
            <li key={value}>
              <label
                className={`moments-filter-popover__item ${checked ? "moments-filter-popover__item--checked" : ""}`}
              >
                <input
                  checked={checked}
                  onChange={(event) => toggle(value, event.target.checked)}
                  type="checkbox"
                />
                <Icon size={14} />
                <span>{label}</span>
              </label>
            </li>
          );
        })}
      </ul>
      <div className="moments-filter-popover__actions">
        <button
          className="moments-filter-popover__clear"
          onClick={onClear}
          type="button"
        >
          <X size={12} /> 清空
        </button>
        <button
          className="moments-filter-popover__apply"
          onClick={onApply}
          type="button"
        >
          <Check size={12} /> 应用
        </button>
      </div>
    </div>
  );
}

