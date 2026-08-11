import { ArticleDetailPage } from "../../../articles/[articleId]/article-detail-page";

export default async function CanonicalArticleRoute({
  params,
}: {
  params: Promise<{ slug: string; articleId: string; articleSlug: string }>;
}) {
  const { articleId } = await params;
  return <ArticleDetailPage articleId={articleId} />;
}
