import type { Metadata } from "next";
import { ArticleEditorPanel } from "../article-editor-panel";

export const metadata: Metadata = {
  title: "新建文章",
};

export default function NewArticlePage() {
  return <ArticleEditorPanel />;
}

