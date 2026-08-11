import type { Metadata } from "next";
import { ArticlesPageView } from "./articles-page";

export const metadata: Metadata = { title: "文章" };

export default function ArticlesRoute() {
  return <ArticlesPageView />;
}
