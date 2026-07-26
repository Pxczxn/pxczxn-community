import type { Metadata } from "next";
import { CommunityPlaceholderPage } from "../components/community-placeholder-page";

export const metadata: Metadata = { title: "团队" };
export default function TeamsPage() { return <CommunityPlaceholderPage title="团队博客" phase="M3 待开发" description="团队申请、成员、投稿、系列和共创会在 M3 以真实权限与审核流程开放。" />; }
