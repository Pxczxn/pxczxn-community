import type { Metadata } from "next";
import { CommunityPlaceholderPage } from "../../components/community-placeholder-page";

export const metadata: Metadata = {
  title: "团队工作台",
};

export default function TeamWorkspacePage() {
  return (
    <CommunityPlaceholderPage
      title="团队工作台"
      phase="M3 团队协作创作"
      description="团队资料、成员权限、投稿队列和系列管理将在团队博客能力上线后开放。当前不再保留写死的团队、成员和统计演示数据。"
    />
  );
}
