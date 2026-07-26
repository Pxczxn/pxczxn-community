import type { Metadata } from "next";
import { DiscoverPage } from "../discover/discover-page";

export const metadata: Metadata = { title: "文章" };

export default function ArticlesPage() {
  return <DiscoverPage articlesOnly />;
}
