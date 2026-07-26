import type { Metadata } from "next";
import { ArticleDetailPage } from "./article-detail-page";

export const metadata: Metadata = {
  title: "文章详情",
  description: "阅读星语社区公开文章。",
};

export default async function ArticleRoute({
  params,
}: {
  params: Promise<{ articleId: string }>;
}) {
  const { articleId } = await params;
  return <ArticleDetailPage articleId={articleId} />;
}
