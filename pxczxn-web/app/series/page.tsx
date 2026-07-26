import type { Metadata } from "next";
import { CommunityPlaceholderPage } from "../components/community-placeholder-page";

export const metadata: Metadata = { title: "系列" };
export default function SeriesPage() { return <CommunityPlaceholderPage title="文章系列" phase="M3 待开发" description="系列、章节排序与连载状态依赖团队与内容授权模型，将在 M3 正式开放。" />; }
