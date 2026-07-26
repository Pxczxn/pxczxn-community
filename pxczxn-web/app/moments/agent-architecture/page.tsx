import type { Metadata } from "next";
import { MomentsCommunityPage } from "../moments-community-page";

export const metadata: Metadata = {
  title: "动态广场",
  description: "发布动态、参与讨论，发现星语社区里的新鲜观点。",
};

export default function MomentsPage() {
  return <MomentsCommunityPage />;
}
