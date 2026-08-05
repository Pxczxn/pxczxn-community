"use client";

import { useEffect, useState } from "react";
import { SocialCenterPage } from "../me/favorites/social-center-page";
import { PublicBlogPage } from "../blogs/[slug]/public-blog-page";
import { readSession } from "../lib/community-api";

export function ProfileEntry({ slug }: { slug: string }) {
  const [isMine, setIsMine] = useState<boolean | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => setIsMine(readSession()?.blogSlug === slug), 0);
    return () => window.clearTimeout(timer);
  }, [slug]);

  if (isMine === null) return null;
  return isMine ? <SocialCenterPage /> : <PublicBlogPage slug={slug} />;
}
