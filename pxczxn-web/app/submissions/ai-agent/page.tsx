import type { Metadata } from "next";
import { SubmissionDetailPanel } from "./submission-detail-panel";

export const metadata: Metadata = {
  title: "我的投稿",
};

export default function SubmissionDetailPage() {
  return <SubmissionDetailPanel />;
}

