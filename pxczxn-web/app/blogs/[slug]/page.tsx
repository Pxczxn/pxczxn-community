import type { Metadata } from "next";
import { PublicBlogPage } from "./public-blog-page";

const fallbackMetadata: Metadata = {
  title: "博客主页",
  description: "浏览博客公开文章与作者资料。",
};

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  const { publicApi, contentMetadata } = await import("../../lib/seo");
  const blog = await publicApi<import("../../lib/community-api").PublicBlog>(`/api/v1/public/blogs/${encodeURIComponent(slug)}`);
  return blog ? contentMetadata({ title: blog.seoTitle || blog.name, description: blog.seoDescription || blog.summary, path: `/blogs/${blog.slug}` }) : { ...fallbackMetadata, robots: { index: false, follow: false } };
}

export default async function BlogRoute({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  return <PublicBlogPage slug={slug} />;
}

