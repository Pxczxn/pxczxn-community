import type { Metadata } from "next";
import { ArticleEditorPanel } from "../article-editor-panel";

export const metadata: Metadata = {
  title: "编辑文章",
};

export default async function EditArticlePage({
  params,
}: {
  params: Promise<{ articleId: string }>;
}) {
  const { articleId } = await params;
  return <ArticleEditorPanel articleId={articleId} />;
}

