"use client";

import Link from "next/link";
import {
  AlertTriangle,
  BookOpen,
  Calendar,
  CheckCircle2,
  Edit3,
  Folder,
  FolderHeart,
  FolderPlus,
  Heart,
  Layers3,
  LoaderCircle,
  MapPin,
  MessageCircle,
  MoreHorizontal,
  PenLine,
  Send,
  TrendingUp,
  Plus,
  RefreshCw,
  UserCheck,
  Users,
} from "lucide-react";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { ArticleThumb, Avatar, EmptyState, UserTopbar } from "../../components/prototype-ui";
import {
  CurrentCommunityUser,
  FavoriteContent,
  FavoriteFolder,
  LikedContent,
  MomentPage,
  PersonalBlog,
  PublicArticleSummary,
  SocialCounts,
  SocialProfile,
  communityApi,
  publicFileUrl,
} from "../../lib/community-api";

type Tab = "articles" | "series" | "moments" | "favorites" | "likes" | "following" | "followers" | "mutual";

export function SocialCenterPage() {
  const [user, setUser] = useState<CurrentCommunityUser | null>(null);
  const [blog, setBlog] = useState<PersonalBlog | null>(null);
  const [moments, setMoments] = useState<MomentPage | null>(null);
  const [counts, setCounts] = useState<SocialCounts>({ following: 0, followers: 0, mutual: 0 });
  const [folders, setFolders] = useState<FavoriteFolder[]>([]);
  const [activeFolderId, setActiveFolderId] = useState("");
  const [favorites, setFavorites] = useState<FavoriteContent[]>([]);
  const [activityFavorites, setActivityFavorites] = useState<FavoriteContent[]>([]);
  const [likes, setLikes] = useState<LikedContent[]>([]);
  const [articles, setArticles] = useState<PublicArticleSummary[]>([]);
  const [following, setFollowing] = useState<SocialProfile[]>([]);
  const [followers, setFollowers] = useState<SocialProfile[]>([]);
  const [tab, setTab] = useState<Tab>("moments");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [newFolderOpen, setNewFolderOpen] = useState(false);
  const [newFolderName, setNewFolderName] = useState("");
  const [creatingFolder, setCreatingFolder] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [
        nextUser,
        nextBlog,
        nextMoments,
        nextCounts,
        nextFolders,
        nextLikes,
        nextFollowing,
        nextFollowers,
      ] = await Promise.all([
        communityApi.me(),
        communityApi.myBlog(),
        communityApi.myMoments(1, 20),
        communityApi.socialCounts(),
        communityApi.favoriteFolders(),
        communityApi.myLikes(1, 50),
        communityApi.myFollowing(1, 50),
        communityApi.myFollowers(1, 50),
      ]);
      setUser(nextUser);
      setBlog(nextBlog);
      setMoments(nextMoments);
      setCounts(nextCounts);
      setFolders(nextFolders);
      setLikes(nextLikes.records);
      setFollowing(nextFollowing.records);
      setFollowers(nextFollowers.records);
      setArticles((await communityApi.publicArticles(nextBlog.slug, 1, 20)).records);
      const folderId = nextFolders[0]?.folderId || "";
      setActiveFolderId(folderId);
      const favoritePages = await Promise.all(
        nextFolders.map((folder) => communityApi.favoriteItems(folder.folderId, 1, 50)),
      );
      const nextFavorites = favoritePages.flatMap((page) => page.records);
      setActivityFavorites(nextFavorites);
      setFavorites(folderId ? favoritePages[0]?.records ?? [] : []);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "个人中心加载失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  async function chooseFolder(folderId: string) {
    setActiveFolderId(folderId);
    setError("");
    try {
      setFavorites((await communityApi.favoriteItems(folderId, 1, 50)).records);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "收藏夹加载失败");
    }
  }

  async function createFolder(event: FormEvent) {
    event.preventDefault();
    if (!newFolderName.trim()) return;
    setCreatingFolder(true);
    setError("");
    try {
      const folder = await communityApi.createFavoriteFolder({
        name: newFolderName.trim(),
        visibility: "PRIVATE",
        sortOrder: folders.length + 1,
      });
      setFolders((current) => [...current, folder]);
      setNewFolderName("");
      setNewFolderOpen(false);
      await chooseFolder(folder.folderId);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "收藏夹创建失败");
    } finally {
      setCreatingFolder(false);
    }
  }

  const relationRecords = useMemo(() => {
    if (tab === "following") return following;
    if (tab === "followers") return followers;
    if (tab === "mutual") return following.filter((profile) => profile.mutual);
    return [];
  }, [followers, following, tab]);

  if (loading) {
    return (
      <>
        <UserTopbar title="个人中心" />
        <main className="profile-page page-shell profile-state" aria-busy="true">
          <LoaderCircle className="spin" size={28} />
          <strong>我的文件夹与社交关系正在加载…</strong>
        </main>
      </>
    );
  }

  if (error && !user) {
    return (
      <>
        <UserTopbar title="个人中心" />
        <main className="profile-page page-shell profile-state error">
          <AlertTriangle size={34} />
          <h1>个人中心暂时无法打开</h1>
          <p>{error}</p>
          <div>
            <button className="primary-button" onClick={load} type="button">
              <RefreshCw size={16} /> 重试
            </button>
            <Link className="ghost-button" href="/login">前往登录</Link>
          </div>
        </main>
      </>
    );
  }

  const displayName = user?.displayName || user?.username || "星语用户";
  const avatar = publicFileUrl(blog?.avatarFileId || user?.avatarFileId);
  const background = publicFileUrl(blog?.backgroundFileId);
  return (
    <>
      <UserTopbar title="个人中心" />
      <main className="profile-page page-shell">
        {/* Banner Cover + Hero Card */}
        <section className="profile-hero-wrap">
          <div
            aria-hidden="true"
            className={`profile-cover-banner${background ? " profile-cover-banner--image" : ""}`}
            style={background ? { backgroundImage: `url("${background}")` } : undefined}
          />
          <div className="surface-lg profile-hero">
            <div className="avatar-wrapper">
              {avatar ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img alt="" className="avatar avatar-lg avatar-image" src={avatar} />
              ) : (
                <Avatar label={displayName.slice(0, 1)} size="lg" />
              )}
            </div>

            <div className="profile-copy">
              <div className="profile-name-row">
                <h1>{displayName}</h1>
                {user?.verificationStatus === "VERIFIED" && (
                  <span className="verified-badge" title="认证创作者">
                    <CheckCircle2 size={16} /> 认证创作者
                  </span>
                )}
                <span className="user-handle-chip">@{user?.username}</span>
              </div>
              <p className="profile-bio">{user?.bio || "这个人很认真，还没来得及写简介。"}</p>
              <span className="profile-location">
                <MapPin size={14} /> 星语社区 · {blog?.name || "个人博客"}
              </span>
              <div className="profile-actions">
                <Link className="ghost-button" href="/settings">
                  <Edit3 size={15} /> <span>编辑资料</span>
                </Link>
                <Link className="primary-button" href="/editor/new">
                  <PenLine size={15} /> <span>写文章</span>
                </Link>
              </div>
            </div>

            <div className="profile-stats">
              {[
                ["文章", blog?.articleCount ?? 0],
                ["动态", moments?.total ?? 0],
                ["收藏", folders.reduce((sum, folder) => sum + Number(folder.itemCount || 0), 0)],
                ["粉丝", counts.followers],
                ["关注", counts.following],
              ].map(([label, value]) => (
                <div className="stat-card" key={label}>
                  <small>{label}</small>
                  <strong>{value}</strong>
                </div>
              ))}
            </div>

          </div>
        </section>

        {/* Tab Navigation */}
        <nav className="tabs profile-tabs" aria-label="个人关系和收藏">
          {([
            ["articles", "文章", BookOpen],
            ["series", "连载", Layers3],
            ["moments", "动态", Send],
          ] as const).map(([key, label, Icon]) => (
            <button
              className={`tab ${tab === key ? "active" : ""}`}
              key={key}
              onClick={() => setTab(key)}
              type="button"
            >
              <Icon size={16} />
              <span>{label}</span>
            </button>
          ))}
        </nav>

        {error && (
          <div className="inline-feedback error" role="status">
            <AlertTriangle size={16} /> {error}
          </div>
        )}

        {(tab === "articles" || tab === "series" || tab === "moments") && (
          <CreatorWorkspace
            activeTab={tab}
            articles={articles}
            favorites={activityFavorites}
            likes={likes}
            moments={moments?.records ?? []}
          />
        )}

        {tab === "favorites" && (
          <>
            <section className="surface favorite-section">
              <h2 className="card-heading">我的文件夹</h2>
              <div className="collection-grid">
                {folders.map((folder) => (
                  <button
                    aria-pressed={activeFolderId === folder.folderId}
                    className={`collection-card ${activeFolderId === folder.folderId ? "active" : ""}`}
                    key={folder.folderId}
                    onClick={() => void chooseFolder(folder.folderId)}
                    type="button"
                  >
                    <Folder className="folder-icon" size={28} />
                    <span className="folder-meta">
                      <strong>{folder.name}</strong>
                      <small>（{folder.itemCount} 项）</small>
                    </span>
                  </button>
                ))}
                <button
                  className="collection-card collection-card--new"
                  onClick={() => setNewFolderOpen((value) => !value)}
                  type="button"
                >
                  <FolderPlus size={26} />
                  <span>新建文件夹</span>
                </button>
              </div>
              {newFolderOpen && (
                <form className="new-folder-form" onSubmit={createFolder}>
                  <input
                    autoFocus
                    className="field"
                    maxLength={40}
                    onChange={(event) => setNewFolderName(event.target.value)}
                    placeholder="收藏夹名称"
                    value={newFolderName}
                  />
                  <button className="primary-button" disabled={creatingFolder} type="submit">
                    <Plus size={16} /> {creatingFolder ? "创建中" : "创建"}
                  </button>
                </form>
              )}
            </section>

            <ContentList
              emptyDescription="在文章或动态上点击收藏，它们会出现在这里。"
              emptyTitle="这个收藏夹还是空的"
              items={favorites}
              title={`${folders.find((folder) => folder.folderId === activeFolderId)?.name || "全部收藏"}（${favorites.length}）`}
            />
          </>
        )}

        {tab === "likes" && (
          <ContentList
            emptyDescription="你喜欢过的公开文章和动态会显示在这里。"
            emptyTitle="还没有喜欢的内容"
            items={likes}
            title={`我的喜欢（${likes.length}）`}
          />
        )}

        {(tab === "following" || tab === "followers" || tab === "mutual") && (
          <section className="surface favorite-section">
            <div className="card-title-row">
              <h2 className="card-heading">
                {tab === "following" ? "我的关注" : tab === "followers" ? "我的粉丝" : "互相关注"}
              </h2>
              <Users size={18} style={{ color: "var(--primary)" }} />
            </div>
            <div className="social-profile-grid">
              {relationRecords.map((profile) => (
                <Link className="social-profile-card" href={`/${profile.blogSlug}`} key={profile.userId}>
                  <div className="social-profile-header">
                    <Avatar label={(profile.displayName || profile.username).slice(0, 1)} size="md" />
                    <div className="social-profile-info">
                      <strong>{profile.displayName || profile.username}</strong>
                      <small>@{profile.username}</small>
                    </div>
                    {profile.mutual ? (
                      <i className="chip chip-mutual"><UserCheck size={13} /> 互关</i>
                    ) : (
                      <i className="chip">{profile.following ? "已关注" : "粉丝"}</i>
                    )}
                  </div>
                  <p className="social-profile-bio">{profile.bio || "暂未填写个人简介"}</p>
                  <div className="social-profile-footer">
                    <span><BookOpen size={13} /> {profile.blogName}</span>
                    <span className="view-blog-link">查看博客 &rarr;</span>
                  </div>
                </Link>
              ))}
              {!relationRecords.length && (
                <EmptyState
                  description="在博客主页关注感兴趣的创作者，关系会实时同步。"
                  title="这里还没有人"
                />
              )}
            </div>
          </section>
        )}
      </main>
    </>
  );
}

function CreatorWorkspace({
  activeTab,
  articles,
  moments,
  likes,
  favorites,
}: {
  activeTab: "articles" | "series" | "moments";
  articles: PublicArticleSummary[];
  moments: MomentPage["records"];
  likes: LikedContent[];
  favorites: FavoriteContent[];
}) {
  return (
    <div className="creator-workspace">
      <section className="surface creator-feed">
        {activeTab === "articles" && <ArticleFeed articles={articles} />}
        {activeTab === "series" && <EmptyState title="还没有创建连载" description="将有关联的文章整理为连载，方便读者持续阅读。" />}
        {activeTab === "moments" && <MomentFeed favorites={favorites} likes={likes} moments={moments} />}
      </section>
      <aside className="creator-sidebar">
        <section className="surface creator-side-card">
          <div className="creator-side-heading"><h2>创作数据</h2><span>近 7 天</span></div>
          <div className="creator-metrics">
            <span><TrendingUp size={19} /><small>浏览量</small><strong>-</strong></span>
            <span><Heart size={19} /><small>点赞数</small><strong>-</strong></span>
            <span><FolderHeart size={19} /><small>收藏数</small><strong>-</strong></span>
          </div>
          <Link className="creator-side-link" href="/me/analytics">查看更多数据 →</Link>
        </section>
        <section className="surface creator-side-card">
          <h2>快捷操作</h2>
          <div className="creator-quick-actions">
            <Link href="/editor/new"><PenLine size={16} /> 写文章</Link>
            <Link href="/moments"><MessageCircle size={16} /> 发布动态</Link>
            <Link href="/series"><Layers3 size={16} /> 创建连载</Link>
          </div>
        </section>
        <section className="surface creator-side-card creator-recent-card">
          <div className="creator-side-heading"><h2>最近互动</h2><span>查看全部 →</span></div>
          <p><MessageCircle size={17} /> 暂无互动记录，去参与讨论吧。</p>
        </section>
      </aside>
    </div>
  );
}

function ArticleFeed({ articles }: { articles: PublicArticleSummary[] }) {
  if (!articles.length) return <EmptyState title="还没有发布文章" description="开始写下第一篇文章，它会在这里与读者见面。" />;
  return <div className="creator-list">{articles.map((article) => <Link className="creator-entry" href={article.canonicalPath} key={article.articleId}><BookOpen size={18} /><span><strong>{article.title}</strong><small>{article.summary || `${formatDate(article.publishedAt)} 发布 · ${article.viewCount} 次阅读`}</small></span><time>{formatDate(article.publishedAt)}</time></Link>)}</div>;
}

function MomentFeed({
  moments,
  likes,
  favorites,
}: {
  moments: MomentPage["records"];
  likes: LikedContent[];
  favorites: FavoriteContent[];
}) {
  const activities = [
    ...moments.map((moment) => ({
      id: `moment-${moment.momentId}`,
      at: moment.createdAt,
      href: moment.canonicalPath,
      icon: Send,
      title: "发布了一条动态",
      detail: moment.textContent || moment.article?.title || "查看动态详情",
    })),
    ...likes.map((item) => ({
      id: `like-${item.targetType}-${item.targetId}`,
      at: item.likedAt,
      href: item.canonicalPath,
      icon: Heart,
      title: `点赞了${item.targetType === "ARTICLE" ? "文章" : "动态"}《${item.title}》`,
      detail: item.excerpt || "查看原内容",
    })),
    ...favorites.map((item) => ({
      id: `favorite-${item.targetType}-${item.targetId}`,
      at: item.favoritedAt,
      href: item.canonicalPath,
      icon: FolderHeart,
      title: `收藏了${item.targetType === "ARTICLE" ? "文章" : "动态"}《${item.title}》`,
      detail: item.excerpt || "查看原内容",
    })),
  ].sort((left, right) => new Date(right.at).getTime() - new Date(left.at).getTime());

  if (!activities.length) return <EmptyState title="还没有动态" description="发布、点赞或收藏内容后，记录会按时间显示在这里。" />;
  return <div className="creator-list">{activities.map((activity) => {
    const Icon = activity.icon;
    return <Link className="creator-entry" href={activity.href} key={activity.id}><Icon size={18} /><span><strong>{activity.title}</strong><small>{activity.detail}</small></span><time>{formatDate(activity.at)}</time></Link>;
  })}</div>;
}

function ContentList({
  title,
  items,
  emptyTitle,
  emptyDescription,
}: {
  title: string;
  items: Array<FavoriteContent | LikedContent>;
  emptyTitle: string;
  emptyDescription: string;
}) {
  return (
    <section className="surface favorite-section favorite-articles">
      <div className="card-title-row">
        <h2 className="card-heading">{title}</h2>
        <button aria-label="更多操作" className="icon-button" type="button">
          <MoreHorizontal size={17} />
        </button>
      </div>
      <div className="favorite-article-list">
        {items.map((item, index) => (
          <Link
            className="favorite-article-card"
            href={item.canonicalPath || "#"}
            key={`${item.targetType}-${item.targetId}`}
          >
            {item.coverFileId ? (
              <span
                aria-hidden="true"
                className="article-thumb article-thumb--image"
                style={{ backgroundImage: `url("${publicFileUrl(item.coverFileId)}")` }}
              />
            ) : (
              <ArticleThumb variant={index + 1} />
            )}
            <span className="favorite-article-copy">
              <div className="article-title-row">
                <strong>{item.title}</strong>
                <span className={`badge ${item.targetType === "ARTICLE" ? "badge-primary" : "badge-secondary"}`}>
                  {item.targetType === "ARTICLE" ? "文章" : "动态"}
                </span>
              </div>
              {item.excerpt && <span className="content-excerpt">{item.excerpt}</span>}
              <div className="article-meta-row">
                <span>
                  <Calendar size={13} />{" "}
                  {"favoritedAt" in item
                    ? `收藏于 ${formatDate(item.favoritedAt)}`
                    : `喜欢于 ${formatDate(item.likedAt)}`}
                </span>
              </div>
            </span>
            <span className="icon-action-btn" aria-label="更多选项">
              <MoreHorizontal size={17} />
            </span>
          </Link>
        ))}
      </div>
      {!items.length && <EmptyState description={emptyDescription} title={emptyTitle} />}
    </section>
  );
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit" }).format(date);
}
