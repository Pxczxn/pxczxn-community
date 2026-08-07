import type { Metadata } from "next";
import { MomentDetailPage } from "./moment-detail-page";
import { contentMetadata, publicApi } from "../../lib/seo";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ momentId: string }>;
}): Promise<Metadata> {
  const { momentId } = await params;
  const moment = await publicApi<{
    momentId: string;
    textContent: string | null;
    canonicalPath: string;
  }>(`/api/v1/moments/${encodeURIComponent(momentId)}`);
  if (!moment) {
    return {
      title: "动态详情",
      robots: { index: false, follow: false },
    };
  }
  return contentMetadata({
    title: moment.textContent ? `${moment.textContent.slice(0, 30)} · 星语社区` : "动态详情 · 星语社区",
    description: moment.textContent || "星语社区的一条动态。",
    path: moment.canonicalPath || `/moments/${momentId}`,
  });
}

export default async function MomentDetailRoute({
  params,
}: {
  params: Promise<{ momentId: string }>;
}) {
  const { momentId } = await params;
  return <MomentDetailPage momentId={momentId} />;
}
