import type { Metadata } from "next";
import { SearchPage } from "./search-page";

export const metadata: Metadata = {
  title: "统一搜索",
  description: "搜索公开文章、动态、博客、连载、标签和用户。",
};

export default function SearchRoute() {
  return <SearchPage />;
}
