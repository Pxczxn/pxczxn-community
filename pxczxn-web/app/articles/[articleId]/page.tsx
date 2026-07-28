import type { Metadata } from "next";
import { ArticleDetailPage } from "./article-detail-page";

const fallbackMetadata: Metadata = {
  title: "文章详情",
  description: "阅读星语社区公开文章。",
};

export async function generateMetadata({ params }: { params: Promise<{ articleId: string }> }): Promise<Metadata> {
  const { articleId } = await params;
  const { publicApi, contentMetadata } = await import("../../lib/seo");
  const article = await publicApi<import("../../lib/community-api").PublicArticleDetail>(`/api/v1/public/articles/${encodeURIComponent(articleId)}`);
  return article ? contentMetadata({ title: article.seo?.title || article.title, description: article.seo?.description || article.summary, path: article.canonicalPath }) : { ...fallbackMetadata, robots: { index: false, follow: false } };
}

export default async function ArticleRoute({
  params,
}: {
  params: Promise<{ articleId: string }>;
}) {
  const { articleId } = await params;
  return <ArticleDetailPage articleId={articleId} />;
}
