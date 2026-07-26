import type { Metadata } from "next";
import { CommunityPlaceholderPage } from "../../components/community-placeholder-page";

export const metadata: Metadata = {
  title: "团队投稿",
};

export default function SubmissionDetailPage() {
  return (
    <CommunityPlaceholderPage
      title="团队投稿"
      phase="M3 外部投稿"
      description="投稿固定版本、团队领取和多级审核流程会随团队博客能力一起交付；当前不会展示虚构的投稿审核记录。"
    />
  );
}
