import type { Metadata } from "next";
import { DiscoverPage } from "./discover/discover-page";

export const metadata: Metadata = {
  title: "首页",
  description: "发现有价值的内容，与有趣的人一起创作。",
};

export default function Home() {
  return <DiscoverPage />;
}
