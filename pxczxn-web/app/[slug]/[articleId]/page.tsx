import { ArticleDetailPage } from "../../articles/[articleId]/article-detail-page";

export default async function UserArticleRoute({
  params,
}: {
  params: Promise<{ slug: string; articleId: string }>;
}) {
  const { articleId } = await params;
  return <ArticleDetailPage articleId={articleId} />;
}
