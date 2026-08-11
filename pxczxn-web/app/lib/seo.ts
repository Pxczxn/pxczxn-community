import type { Metadata } from "next";
import { COMMUNITY_API_BASE_URL } from "@/app/lib/community/client";

export function siteUrl() {
  return process.env.NEXT_PUBLIC_SITE_URL?.replace(/\/+$/, "") || "http://localhost:8847";
}

export async function publicApi<T>(path: string): Promise<T | null> {
  if (!COMMUNITY_API_BASE_URL) return null;
  try {
    const response = await fetch(`${COMMUNITY_API_BASE_URL}${path}`, {
      next: { revalidate: 300 },
    });
    if (!response.ok) return null;
    const payload = (await response.json()) as { code?: number; data?: T };
    return payload.code === 200 ? payload.data || null : null;
  } catch {
    return null;
  }
}

export function contentMetadata(input: {
  title: string;
  description?: string | null;
  path: string;
  image?: string | null;
}): Metadata {
  const url = `${siteUrl()}${input.path}`;
  return {
    title: input.title,
    description: input.description || "星语社区公开内容。",
    alternates: { canonical: input.path },
    openGraph: {
      type: "article",
      title: input.title,
      description: input.description || undefined,
      url,
      images: input.image ? [input.image] : undefined,
    },
    twitter: {
      card: "summary_large_image",
      title: input.title,
      description: input.description || undefined,
      images: input.image ? [input.image] : undefined,
    },
  };
}
