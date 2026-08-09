import type { Metadata } from "next";
import { HomePage } from "./home/home-page";

export const metadata: Metadata = {
  title: "首页",
  description: "发现有价值的内容，与有趣的人一起创作。",
};

export default function Home() {
  return <HomePage />;
}