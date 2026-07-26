import type { Metadata } from "next";
import { DiscoverPage } from "./discover-page";

export const metadata: Metadata = {
  title: "发现",
  description: "浏览星语社区最新发布的文章、动态与创作者内容。",
};

export default function DiscoverRoute() {
  return <DiscoverPage />;
}
