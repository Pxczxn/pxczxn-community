import type { Metadata } from "next";
import { PublicBlogPage } from "./public-blog-page";

export const metadata: Metadata = {
  title: "博客主页",
  description: "浏览博客公开文章与作者资料。",
};

export default async function BlogRoute({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  return <PublicBlogPage slug={slug} />;
}

