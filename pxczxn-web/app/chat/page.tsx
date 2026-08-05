"use client";

import {
  FormEvent,
  useCallback,
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
} from "react";
import {
  LoaderCircle,
  MessageSquare,
  Send,
  UserCheck,
  Users,
  Wifi,
  WifiOff,
} from "lucide-react";
import { Avatar, UserTopbar } from "../components/prototype-ui";
import {
  COMMUNITY_API_BASE_URL,
  SESSION_EVENT,
  communityApi,
  readSession,
  type CommunityChatMessage,
  type SocialProfile,
} from "../lib/community-api";

type ConnectionState = "connecting" | "online" | "offline";

function mergeMessage(
  messages: CommunityChatMessage[],
  next: CommunityChatMessage,
) {
  if (messages.some((message) => message.id === next.id)) return messages;
  return [...messages, next].sort((left, right) =>
    left.createdAt.localeCompare(right.createdAt),
  );
}

function socketUrl(ticket: string) {
  const endpoint = new URL(COMMUNITY_API_BASE_URL);
  endpoint.protocol = endpoint.protocol === "https:" ? "wss:" : "ws:";
  endpoint.pathname = "/ws/community-chat";
  endpoint.search = new URLSearchParams({ ticket }).toString();
  return endpoint.toString();
}

function subscribeToSession(onStoreChange: () => void) {
  window.addEventListener(SESSION_EVENT, onStoreChange);
  window.addEventListener("storage", onStoreChange);
  return () => {
    window.removeEventListener(SESSION_EVENT, onStoreChange);
    window.removeEventListener("storage", onStoreChange);
  };
}

let cachedSessionRaw: string | null | undefined;
let cachedSession: ReturnType<typeof readSession> = null;

function readSessionSnapshot() {
  const raw = typeof window === "undefined" ? null : window.localStorage.getItem("pxczxn-community-session");
  if (raw !== cachedSessionRaw) {
    cachedSessionRaw = raw;
    cachedSession = readSession();
  }
  return cachedSession;
}

function readChatEvent(value: string): CommunityChatMessage | null {
  try {
    const payload = JSON.parse(value) as Partial<CommunityChatMessage> & {
      type?: string;
    };
    if (
      payload.type !== "chat"
      || !payload.id
      || !payload.senderUserId
      || !payload.recipientUserId
      || !payload.contentText
      || !payload.status
      || !payload.createdAt
    ) return null;
    return {
      id: payload.id,
      senderUserId: payload.senderUserId,
      recipientUserId: payload.recipientUserId,
      contentText: payload.contentText,
      status: payload.status,
      readAt: payload.readAt || null,
      createdAt: payload.createdAt,
    };
  } catch {
    return null;
  }
}

export default function ChatPage() {
  const session = useSyncExternalStore(
    subscribeToSession,
    readSessionSnapshot,
    () => null,
  );
  const [contacts, setContacts] = useState<SocialProfile[]>([]);
  const [peerId, setPeerId] = useState<string | null>(null);
  const [messages, setMessages] = useState<CommunityChatMessage[]>([]);
  const [content, setContent] = useState("");
  const [loadingContacts, setLoadingContacts] = useState(false);
  const [loadingConversation, setLoadingConversation] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const [connection, setConnection] = useState<ConnectionState>("offline");
  const activePeerRef = useRef<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  const loadConversation = useCallback(async (targetPeerId: string) => {
    setLoadingConversation(true);
    try {
      const history = await communityApi.chatHistory(targetPeerId);
      setMessages(history);
      await communityApi.markChatRead(targetPeerId);
      setError("");
    } catch (cause) {
      setMessages([]);
      setError(cause instanceof Error ? cause.message : "无法加载会话");
    } finally {
      setLoadingConversation(false);
    }
  }, []);

  useEffect(() => {
    if (!session) return;

    let cancelled = false;
    void Promise.resolve().then(async () => {
      setLoadingContacts(true);
      try {
        const result = await communityApi.myFollowing(1, 100);
        if (cancelled) return;
        const mutualContacts = result.records.filter((profile) => profile.mutual);
        setContacts(mutualContacts);
        if (mutualContacts.length > 0) {
          setPeerId(mutualContacts[0].userId);
        }
      } catch (cause) {
        if (!cancelled) {
          setError(cause instanceof Error ? cause.message : "无法加载联系人");
        }
      } finally {
        if (!cancelled) setLoadingContacts(false);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [session]);

  useEffect(() => {
    activePeerRef.current = peerId;
    if (peerId) {
      void Promise.resolve().then(() => loadConversation(peerId));
    }
  }, [loadConversation, peerId]);

  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages]);

  useEffect(() => {
    if (!session || !COMMUNITY_API_BASE_URL) return;
    let disposed = false;
    let retryId: number | undefined;
    let socket: WebSocket | undefined;

    const connect = async () => {
      setConnection("connecting");
      try {
        const { ticket } = await communityApi.chatTicket();
        if (disposed) return;
        socket = new WebSocket(socketUrl(ticket));
        socket.onopen = () => setConnection("online");
        socket.onmessage = (event) => {
          const message = readChatEvent(event.data);
          if (!message) return;
          const otherUserId = message.senderUserId === session.userId
            ? message.recipientUserId
            : message.senderUserId;
          if (otherUserId !== activePeerRef.current) return;
          setMessages((current) => mergeMessage(current, message));
        };
        socket.onclose = () => {
          if (disposed) return;
          setConnection("offline");
          retryId = window.setTimeout(() => void connect(), 2000);
        };
        socket.onerror = () => socket?.close();
      } catch {
        if (disposed) return;
        setConnection("offline");
        retryId = window.setTimeout(() => void connect(), 2000);
      }
    };

    void connect();
    return () => {
      disposed = true;
      if (retryId) window.clearTimeout(retryId);
      socket?.close();
    };
  }, [session]);

  async function send(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!peerId || !content.trim()) return;
    setSending(true);
    try {
      const message = await communityApi.sendChatMessage(peerId, content);
      setMessages((current) => mergeMessage(current, message));
      setContent("");
      setError("");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "发送失败");
    } finally {
      setSending(false);
    }
  }

  const peer = contacts.find((contact) => contact.userId === peerId) ?? null;

  return (
    <>
      <UserTopbar title="即时聊天" />
      <main className="chat-page-container page-shell">
        <header className="chat-page-header">
          <div className="chat-header-title-box">
            <MessageSquare className="chat-header-icon" size={24} />
            <div>
              <h1>即时聊天</h1>
              <p className="chat-header-desc">与互相关注的社区创作者进行私密即时交流</p>
            </div>
          </div>
          <span className={`chat-conn-status conn-${connection}`}>
            {connection === "online" ? <Wifi size={14} /> : <WifiOff size={14} />}
            <span>{connection === "online" ? "实时就绪" : connection === "connecting" ? "连接中…" : "离线"}</span>
          </span>
        </header>

        {error && <p className="inline-feedback error" role="alert">{error}</p>}
        {!session && <p className="inline-feedback error">请先登录后继续查看对话。</p>}

        <section className="surface community-chat-layout">
          <aside className="community-chat-contacts">
            <div className="contacts-header">
              <Users size={16} />
              <span>互关联系人 ({contacts.length})</span>
            </div>
            <div className="contacts-list">
              {loadingContacts && (
                <div className="contacts-loading">
                  <LoaderCircle className="spin" size={18} />
                  <span>加载联系人…</span>
                </div>
              )}
              {!loadingContacts && contacts.length === 0 && (
                <p className="contacts-empty">暂无可聊天的互关联系人</p>
              )}
              {contacts.map((contact) => (
                <button
                  className={`contact-item ${contact.userId === peerId ? "active" : ""}`}
                  key={contact.userId}
                  onClick={() => setPeerId(contact.userId)}
                  type="button"
                >
                  <Avatar label={(contact.displayName || contact.username).slice(0, 1)} size="md" />
                  <div className="contact-info">
                    <strong>{contact.displayName || contact.username}</strong>
                    <small>@{contact.username}</small>
                  </div>
                  <UserCheck className="contact-mutual-icon" size={14} aria-label="互相关注" />
                </button>
              ))}
            </div>
          </aside>

          <section className="community-chat-conversation">
            <header className="conversation-header">
              {peer ? (
                <div className="conversation-peer-info">
                  <Avatar label={(peer.displayName || peer.username).slice(0, 1)} size="md" />
                  <div>
                    <strong>{peer.displayName || peer.username}</strong>
                    <span className="peer-handle">@{peer.username} · {peer.blogName}</span>
                  </div>
                </div>
              ) : (
                <strong>请在左侧选择联系人</strong>
              )}
              <span className={`status-pill pill-${connection}`}>
                {connection === "online" ? "在线" : connection === "connecting" ? "连接中" : "离线"}
              </span>
            </header>

            <div aria-live="polite" className="community-chat-messages">
              {loadingConversation && (
                <div className="messages-loading">
                  <LoaderCircle className="spin" size={20} />
                  <span>正在同步聊天记录…</span>
                </div>
              )}
              {!loadingConversation && peer && messages.length === 0 && (
                <div className="messages-empty">
                  <MessageSquare size={32} />
                  <p>还没有消息，输入下方文本开启对话吧。</p>
                </div>
              )}
              {!loadingConversation &&
                messages.map((message) => {
                  const isOwn = message.senderUserId === session?.userId;
                  return (
                    <article
                      className={`community-chat-bubble ${isOwn ? "bubble-own" : "bubble-peer"}`}
                      data-own-message={isOwn}
                      key={message.id}
                    >
                      {!isOwn && (
                        <Avatar label={(peer?.displayName || peer?.username || "?").slice(0, 1)} size="sm" />
                      )}
                      <div className="bubble-content-wrap">
                        <div className="bubble-body">
                          <p>{message.contentText}</p>
                        </div>
                        <span className="bubble-time">{formatTime(message.createdAt)}</span>
                      </div>
                    </article>
                  );
                })}
              <div ref={messagesEndRef} />
            </div>

            <form className="chat-composer-form" onSubmit={send}>
              <input
                className="chat-input"
                disabled={!peer || sending}
                maxLength={2000}
                onChange={(event) => setContent(event.target.value)}
                placeholder={peer ? `发消息给 ${peer.displayName || peer.username}…` : "请先选择联系人"}
                value={content}
              />
              <button
                className="primary-button chat-send-btn"
                disabled={!peer || !content.trim() || sending}
                type="submit"
              >
                {sending ? <LoaderCircle className="spin" size={16} /> : <Send size={16} />}
                <span>发送</span>
              </button>
            </form>
          </section>
        </section>

        <p className="chat-footer-note">
          <MessageSquare size={14} /> 消息已通过端到端与加密通道建立安全连接。
        </p>
      </main>
    </>
  );
}

function formatTime(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit" }).format(date);
}
