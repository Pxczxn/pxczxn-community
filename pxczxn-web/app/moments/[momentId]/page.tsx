import type { Metadata } from "next";
import { MomentsCommunityPage } from "../moments-community-page";

export const metadata: Metadata = {
  title: "动态详情",
};

export default async function MomentDetailPage({
  params,
}: {
  params: Promise<{ momentId: string }>;
}) {
  const { momentId } = await params;
  return <MomentsCommunityPage initialMomentId={momentId} />;
}
