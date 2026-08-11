"use client";

import { readSession, saveSession } from "./session";

const configuredApiBaseUrl =
  process.env.COMMUNITY_API_BASE_URL?.trim() ||
  process.env.NEXT_PUBLIC_COMMUNITY_API_BASE_URL?.trim();
const browserHostname =
  typeof window !== "undefined" ? window.location.hostname : "";
const isLocalBrowser =
  typeof window !== "undefined" &&
  (
    ["localhost", "127.0.0.1", "::1"].includes(browserHostname)
    || /^10\./.test(browserHostname)
    || /^192\.168\./.test(browserHostname)
    || /^172\.(1[6-9]|2\d|3[01])\./.test(browserHostname)
    || /^198\.(18|19)\./.test(browserHostname)
  );
const inferredLocalApiBaseUrl =
  isLocalBrowser
    ? `${window.location.protocol}//${browserHostname}:8849`
    : "";
const inferredSameOriginApiBaseUrl =
  typeof window !== "undefined" && !isLocalBrowser
    ? window.location.origin
    : "";
const inferredSsrApiBaseUrl =
  typeof window === "undefined" ? "http://127.0.0.1:8849" : "";

export const COMMUNITY_API_BASE_URL =
  (configuredApiBaseUrl || inferredLocalApiBaseUrl || inferredSameOriginApiBaseUrl || inferredSsrApiBaseUrl)
    .replace(/\/+$/, "");

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export class CommunityApiError extends Error {
  constructor(
    message: string,
    public readonly code: number,
    public readonly status: number,
  ) {
    super(message);
    this.name = "CommunityApiError";
  }
}

function normalizePaginationNumbers<T>(value: T): T {
  if (!value || typeof value !== "object" || Array.isArray(value)) return value;

  const source = value as Record<string, unknown>;
  const normalized: Record<string, unknown> = { ...source };
  for (const key of ["total", "pageNum", "pageSize"] as const) {
    if (typeof source[key] === "string" && /^\d+$/.test(source[key])) {
      normalized[key] = Number(source[key]);
    }
  }
  if (source.page && typeof source.page === "object" && !Array.isArray(source.page)) {
    normalized.page = normalizePaginationNumbers(source.page);
  }
  return normalized as T;
}

export async function communityRequest<T>(
  path: string,
  init: RequestInit = {},
  authenticated: boolean | "optional" = true,
): Promise<T> {
  if (!COMMUNITY_API_BASE_URL) {
    throw new CommunityApiError(
      "线上预览未配置社区 API；可先浏览原型页面，本地完整业务使用 8849 后端。",
      503,
      503,
    );
  }

  const headers = new Headers(init.headers);
  if (init.body && !(typeof FormData !== "undefined" && init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (authenticated) {
    const session = readSession();
    if (!session && authenticated !== "optional") {
      throw new CommunityApiError("请先登录后继续操作", 401, 401);
    }
    if (session) {
      headers.set(session.tokenName || "pxczxn-community-token", session.tokenValue);
    }
  }

  let response: Response;
  try {
    response = await fetch(`${COMMUNITY_API_BASE_URL}${path}`, {
      ...init,
      headers,
      credentials: "include",
      // 禁用 HTTP 缓存，避免 GET 接口（如未读计数）返回 304 导致角标展示脏数据
      cache: "no-store",
    });
  } catch {
    throw new CommunityApiError(
      `无法连接社区服务（${COMMUNITY_API_BASE_URL}）`,
      503,
      503,
    );
  }

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json() as ApiResult<T>
    : null;

  if (!response.ok || !payload || payload.code !== 200) {
    const code = payload?.code ?? response.status;
    if (code === 401 || response.status === 401) {
      saveSession(null);
    }
    throw new CommunityApiError(
      payload?.message || `请求失败（HTTP ${response.status}）`,
      code,
      response.status,
    );
  }
  return normalizePaginationNumbers(payload.data);
}
