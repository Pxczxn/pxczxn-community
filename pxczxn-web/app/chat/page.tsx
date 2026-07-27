"use client";

import {
  FormEvent,
  useCallback,
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
} from "react";
import { LoaderCircle, MessageCircle, Send } from "lucide-react";
import { UserTopbar } from "../components/prototype-ui";
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
    readSession,
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

  return <>
    <UserTopbar title="即时聊天" />
    <main className="page-shell" style={{ paddingTop: 28, paddingBottom: 48 }}>
      <header>
        <h1>即时聊天</h1>
        <p className="secondary">与互相关注的社区创作者交流</p>
      </header>
      {error && <p role="alert" className="inline-feedback error">{error}</p>}
      <section className="surface community-chat-layout">
        <aside className="community-chat-contacts">
          <strong>联系人</strong>
          <div style={{ display: "grid", gap: 6, marginTop: 14 }}>
            {loadingContacts && <LoaderCircle className="spin" size={20} />}
            {!loadingContacts && contacts.length === 0 && <p className="secondary">暂无可聊天的联系人</p>}
            {contacts.map((contact) => <button
              className={contact.userId === peerId ? "secondary-button" : "ghost-button"}
              key={contact.userId}
              onClick={() => setPeerId(contact.userId)}
              type="button"
            >
              {contact.displayName || contact.username}
            </button>)}
          </div>
        </aside>
        <section className="community-chat-conversation">
          <header style={{ alignItems: "center", display: "flex", justifyContent: "space-between" }}>
            <strong>{peer ? (peer.displayName || peer.username) : "选择联系人"}</strong>
            <span className="secondary">{connection === "online" ? "已连接" : connection === "connecting" ? "连接中" : "离线"}</span>
          </header>
          <div aria-live="polite" className="community-chat-messages">
            {loadingConversation && <LoaderCircle className="spin" size={22} />}
            {!loadingConversation && peer && messages.length === 0 && <p className="secondary">还没有消息，开始对话吧。</p>}
            {!loadingConversation && messages.map((message) => <article
              className="community-chat-bubble"
              key={message.id}
              data-own-message={message.senderUserId === session?.userId}
            >
              <p style={{ margin: 0 }}>{message.contentText}</p>
            </article>)}
          </div>
          <form onSubmit={send} style={{ display: "flex", gap: 10 }}>
            <input
              disabled={!peer || sending}
              maxLength={2000}
              onChange={(event) => setContent(event.target.value)}
              placeholder="输入消息"
              value={content}
            />
            <button className="primary-button" disabled={!peer || !content.trim() || sending} type="submit">
              {sending ? <LoaderCircle className="spin" size={16} /> : <Send size={16} />}
              发送
            </button>
          </form>
        </section>
      </section>
      {!session && <p className="inline-feedback error">请先登录后继续。</p>}
      <p className="secondary" style={{ display: "flex", gap: 6, marginTop: 14 }}><MessageCircle size={16} />消息会保存在当前会话中。</p>
    </main>
  </>;
}
