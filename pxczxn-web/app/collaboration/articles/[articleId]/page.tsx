import { CollaborationArticlePanel } from "./collaboration-article-panel";

export default async function CollaborationArticlePage({ params }: { params: Promise<{ articleId: string }> }) {
  const { articleId } = await params;
  return <CollaborationArticlePanel articleId={articleId} />;
}
