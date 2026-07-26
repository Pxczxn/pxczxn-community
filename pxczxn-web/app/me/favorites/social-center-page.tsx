"use client";

import Link from "next/link";
import {
  AlertTriangle,
  Folder,
  FolderPlus,
  LoaderCircle,
  MapPin,
  MoreHorizontal,
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
  SocialCounts,
  SocialProfile,
  communityApi,
  publicFileUrl,
} from "../../lib/community-api";

type Tab = "favorites" | "likes" | "following" | "followers" | "mutual";

export function SocialCenterPage() {
  const [user, setUser] = useState<CurrentCommunityUser | null>(null);
  const [blog, setBlog] = useState<PersonalBlog | null>(null);
  const [moments, setMoments] = useState<MomentPage | null>(null);
  const [counts, setCounts] = useState<SocialCounts>({ following: 0, followers: 0, mutual: 0 });
  const [folders, setFolders] = useState<FavoriteFolder[]>([]);
  const [activeFolderId, setActiveFolderId] = useState("");
  const [favorites, setFavorites] = useState<FavoriteContent[]>([]);
  const [likes, setLikes] = useState<LikedContent[]>([]);
  const [following, setFollowing] = useState<SocialProfile[]>([]);
  const [followers, setFollowers] = useState<SocialProfile[]>([]);
  const [tab, setTab] = useState<Tab>("favorites");
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
        communityApi.myMoments(1, 1),
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
      const folderId = nextFolders[0]?.folderId || "";
      setActiveFolderId(folderId);
      setFavorites(
        folderId
          ? (await communityApi.favoriteItems(folderId, 1, 50)).records
          : [],
      );
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
  const avatar = publicFileUrl(user?.avatarFileId);
  return (
    <>
      <UserTopbar title="个人中心" />
      <main className="profile-page page-shell">
        <section className="surface-lg profile-hero">
          {avatar ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img alt="" className="avatar avatar-lg avatar-image" src={avatar} />
          ) : (
            <Avatar label={displayName.slice(0, 1)} size="lg" />
          )}
          <div className="profile-copy">
            <div className="profile-name-row">
              <h1>{displayName}</h1>
              {user?.verificationStatus === "VERIFIED" && <span className="verified">✓</span>}
              <span className="chip">@{user?.username}</span>
            </div>
            <p>{user?.bio || "这个人很认真，还没来得及写简介。"}</p>
            <span className="profile-location">
              <MapPin size={14} /> 星语社区 · {blog?.name || "个人博客"}
            </span>
          </div>
          <div className="profile-stats">
            {[
              ["文章", blog?.articleCount ?? 0],
              ["动态", moments?.total ?? 0],
              ["收藏", folders.reduce((sum, folder) => sum + folder.itemCount, 0)],
              ["粉丝", counts.followers],
              ["关注", counts.following],
            ].map(([label, value]) => (
              <span key={label}><small>{label}</small><strong>{value}</strong></span>
            ))}
          </div>
          <Link className="ghost-button profile-edit" href="/settings">编辑资料</Link>
        </section>

        <nav className="tabs profile-tabs" aria-label="个人关系和收藏">
          {([
            ["favorites", "收藏夹"],
            ["likes", "喜欢"],
            ["following", "关注"],
            ["followers", "粉丝"],
            ["mutual", "互关"],
          ] as const).map(([key, label]) => (
            <button
              className={`tab ${tab === key ? "active" : ""}`}
              key={key}
              onClick={() => setTab(key)}
              type="button"
            >
              {label}
              {key === "following" && counts.following > 0 ? ` ${counts.following}` : ""}
              {key === "followers" && counts.followers > 0 ? ` ${counts.followers}` : ""}
              {key === "mutual" && counts.mutual > 0 ? ` ${counts.mutual}` : ""}
            </button>
          ))}
        </nav>

        {error && (
          <div className="inline-feedback error" role="status">
            <AlertTriangle size={16} /> {error}
          </div>
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
                    <Folder size={31} />
                    <span><strong>{folder.name}</strong><small>（{folder.itemCount}）</small></span>
                  </button>
                ))}
                <button
                  className="collection-card collection-card--new"
                  onClick={() => setNewFolderOpen((value) => !value)}
                  type="button"
                >
                  <FolderPlus size={31} />
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
              <Users size={18} />
            </div>
            <div className="social-profile-list">
              {relationRecords.map((profile) => (
                <Link className="social-profile-row" href={`/blogs/${profile.blogSlug}`} key={profile.userId}>
                  <Avatar label={(profile.displayName || profile.username).slice(0, 1)} size="md" />
                  <span>
                    <strong>{profile.displayName || profile.username}</strong>
                    <small>@{profile.username} · {profile.blogName}</small>
                    <p>{profile.bio || "暂未填写简介"}</p>
                  </span>
                  {profile.mutual ? (
                    <i className="chip"><UserCheck size={13} />互关</i>
                  ) : (
                    <i className="chip">{profile.following ? "已关注" : "粉丝"}</i>
                  )}
                </Link>
              ))}
              {!relationRecords.length && (
                <EmptyState
                  title="这里还没有人"
                  description="在博客主页关注感兴趣的创作者，关系会实时同步。"
                />
              )}
            </div>
          </section>
        )}
      </main>
    </>
  );
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
      <div>
        {items.map((item, index) => (
          <Link
            className="favorite-article-row"
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
              <strong>{item.title}</strong>
              <small>{item.targetType === "ARTICLE" ? "文章" : "动态"}</small>
              {item.excerpt && <span className="content-excerpt">{item.excerpt}</span>}
            </span>
            <span className="favorite-meta">
              {"favoritedAt" in item
                ? `收藏于 ${formatDate(item.favoritedAt)}`
                : `喜欢于 ${formatDate(item.likedAt)}`}
            </span>
            <MoreHorizontal size={18} />
          </Link>
        ))}
      </div>
      {!items.length && <EmptyState title={emptyTitle} description={emptyDescription} />}
    </section>
  );
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit" }).format(date);
}
