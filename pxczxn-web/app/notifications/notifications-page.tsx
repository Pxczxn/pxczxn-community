"use client";

import Link from "next/link";
import {
  AlertTriangle,
  Bell,
  CheckCheck,
  CircleUserRound,
  Heart,
  LoaderCircle,
  MessageCircle,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  UserPlus,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { EmptyState, UserTopbar } from "../components/prototype-ui";
import {
  CommunityNotification,
  NOTIFICATION_EVENT,
  NotificationCategory,
  UnreadNotificationCount,
  communityApi,
} from "../lib/community-api";

const categoryOptions: Array<[NotificationCategory | "ALL", string]> = [
  ["ALL", "全部"],
  ["INTERACTION", "互动"],
  ["COMMENT", "评论"],
  ["FOLLOW", "关注"],
  ["REVIEW", "审核"],
  ["TEAM", "团队"],
  ["SYSTEM", "系统"],
];

export function NotificationsPage() {
  const [records, setRecords] = useState<CommunityNotification[]>([]);
  const [counts, setCounts] = useState<UnreadNotificationCount>({ total: 0, categories: {} });
  const [category, setCategory] = useState<NotificationCategory | "ALL">("ALL");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");
  const [errorTimer, setErrorTimer] = useState<number | null>(null);

  /** 显示操作类错误（自动 4 秒后消失） */
  const showActionError = useCallback((msg: string) => {
    if (errorTimer !== null) window.clearTimeout(errorTimer);
    setError(msg);
    const timer = window.setTimeout(() => setError(""), 4000) as unknown as number;
    setErrorTimer(timer);
  }, [errorTimer]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [page, unread] = await Promise.all([
        communityApi.notifications(category === "ALL" ? undefined : category, undefined, 1, 50),
        communityApi.unreadNotifications(),
      ]);
      setRecords(page.records);
      setCounts(unread);
    } catch {
      // 通知列表非关键路径，加载失败时静默展示空状态，不弹错误
      setRecords([]);
      setCounts({ total: 0, categories: {} });
    } finally {
      setLoading(false);
    }
  }, [category]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    return () => { if (errorTimer !== null) window.clearTimeout(errorTimer); };
  }, [errorTimer]);

  // counts 变化后同步顶部铃铛等全局未读提示
  useEffect(() => {
    window.dispatchEvent(new CustomEvent(NOTIFICATION_EVENT));
  }, [counts.total]);

  async function markRead(notification: CommunityNotification) {
    if (notification.status === "READ") return;
    setBusy(notification.notificationId);
    try {
      const result = await communityApi.readNotification(notification.notificationId);
      setRecords((current) => current.map((item) =>
        item.notificationId === notification.notificationId
          ? { ...item, status: "READ", readAt: result.readAt }
          : item,
      ));
      setCounts((current) => ({
        ...current,
        total: result.unreadCount,
        categories: {
          ...current.categories,
          [notification.category]: Math.max(
            0,
            (current.categories[notification.category] || 0) - 1,
          ),
        },
      }));
      window.dispatchEvent(new CustomEvent(NOTIFICATION_EVENT));
    } catch {
      showActionError("标记已读失败，请稍后重试");
    } finally {
      setBusy("");
    }
  }

  async function markAllRead() {
    setBusy("all");
    setError("");
    try {
      const result = await communityApi.readAllNotifications(
        category === "ALL" ? undefined : category,
      );
      setRecords((current) => current.map((item) => ({ ...item, status: "READ" })));
      setCounts((current) => ({
        total: result.unreadCount,
        categories: category === "ALL"
          ? {}
          : { ...current.categories, [category]: 0 },
      }));
      window.dispatchEvent(new CustomEvent(NOTIFICATION_EVENT));
    } catch {
      showActionError("全部已读操作失败，请稍后重试");
    } finally {
      setBusy("");
    }
  }

  return (
    <>
      <UserTopbar title="通知中心" />
      <main className="notifications-page page-shell">
        <section className="surface-lg notification-shell">
          <header className="notification-heading">
            <div>
              <span className="notification-heading__icon"><Bell size={22} /></span>
              <span>
                <h1>通知中心</h1>
                <p>{counts.total ? `${counts.total} 条未读消息` : "所有消息都已读"}</p>
              </span>
            </div>
            <div>
              <button className="ghost-button" onClick={load} type="button">
                <RefreshCw size={16} /> 刷新
              </button>
              <button
                className="primary-button"
                disabled={!counts.total || busy === "all"}
                onClick={markAllRead}
                type="button"
              >
                {busy === "all"
                  ? <LoaderCircle className="spin" size={16} />
                  : <CheckCheck size={16} />}
                全部已读
              </button>
            </div>
          </header>

          <nav className="tabs notification-tabs" aria-label="通知分类">
            {categoryOptions.map(([key, label]) => {
              const unread = key === "ALL" ? counts.total : counts.categories[key] || 0;
              return (
                <button
                  className={`tab ${category === key ? "active" : ""}`}
                  key={key}
                  onClick={() => setCategory(key)}
                  type="button"
                >
                  {label}
                  {unread > 0 && <i>{unread > 99 ? "99+" : unread}</i>}
                </button>
              );
            })}
          </nav>

          {error && (
            <div className="inline-feedback error" role="alert">
              <AlertTriangle size={16} /> {error}
            </div>
          )}

          {loading ? (
            <div className="notification-state" aria-busy="true">
              <LoaderCircle className="spin" size={26} />
              <strong>正在加载通知…</strong>
            </div>
          ) : (
            <div className="notification-list">
              {records.map((notification) => (
                <NotificationRow
                  busy={busy === notification.notificationId}
                  key={notification.notificationId}
                  notification={notification}
                  onRead={() => void markRead(notification)}
                />
              ))}
              {!records.length && (
                <EmptyState
                  title="暂无通知"
                  description="新的互动、关注、评论和审核进展会出现在这里。"
                />
              )}
            </div>
          )}
        </section>
      </main>
    </>
  );
}

function NotificationRow({
  notification,
  busy,
  onRead,
}: {
  notification: CommunityNotification;
  busy: boolean;
  onRead: () => void;
}) {
  const icon = categoryIcon(notification.category);
  const senderName = notification.sender?.displayName || notification.sender?.username;
  const content = (
    <>
      <span className={`notification-type-icon category-${notification.category.toLowerCase()}`}>
        {icon}
      </span>
      <span className="notification-row__copy">
        <span>
          <strong>{notification.title || "内容暂不可见"}</strong>
          {notification.importance === "HIGH" && <i className="chip status-warning">重要</i>}
          {notification.aggregateCount > 1 && <i className="chip">{notification.aggregateCount} 次</i>}
        </span>
        {senderName && <small>来自 {senderName}</small>}
        <p>{notification.content || "相关内容已不可见，通知信息已安全隐藏。"}</p>
        <time>{formatTime(notification.lastActivityAt)}</time>
      </span>
      {notification.status === "UNREAD" && <i className="notification-unread-dot" />}
      {busy && <LoaderCircle className="spin" size={16} />}
    </>
  );

  const className = `notification-row ${notification.status === "UNREAD" ? "unread" : ""}`;
  if (notification.targetAvailable && notification.canonicalPath) {
    return (
      <Link className={className} href={notification.canonicalPath} onClick={onRead}>
        {content}
      </Link>
    );
  }
  return (
    <button className={className} onClick={onRead} type="button">
      {content}
    </button>
  );
}

function categoryIcon(category: NotificationCategory) {
  if (category === "INTERACTION") return <Heart size={18} />;
  if (category === "COMMENT") return <MessageCircle size={18} />;
  if (category === "FOLLOW") return <UserPlus size={18} />;
  if (category === "REVIEW") return <ShieldCheck size={18} />;
  if (category === "SYSTEM") return <Sparkles size={18} />;
  return <CircleUserRound size={18} />;
}

function formatTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("zh-CN", {
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(date);
}
