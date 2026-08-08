const SESSION_KEY = "pxczxn-community-session";
export const SESSION_EVENT = "pxczxn-session-change";
export const NOTIFICATION_EVENT = "pxczxn-notification-change";

export interface CommunitySession {
  tokenName: string;
  tokenValue: string;
  expiresIn: number;
  userId: string;
  username: string;
  forcePasswordChange?: boolean;
  displayName?: string | null;
  blogSlug?: string | null;
  status?: string;
}

export function readSession(): CommunitySession | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as CommunitySession;
  } catch {
    window.localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function saveSession(session: CommunitySession | null) {
  if (typeof window === "undefined") return;
  if (session) {
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } else {
    window.localStorage.removeItem(SESSION_KEY);
  }
  window.dispatchEvent(new CustomEvent(SESSION_EVENT));
}