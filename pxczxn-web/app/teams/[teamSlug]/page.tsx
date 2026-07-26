import { CommunityPlaceholderPage } from "../../components/community-placeholder-page";

export default async function TeamDetailPage({ params }: { params: Promise<{ teamSlug: string }> }) {
  const { teamSlug } = await params;
  return <CommunityPlaceholderPage title="团队博客" phase="M3 待开发" description={`团队 “${teamSlug}” 尚未开放公开主页；不会回退到硬编码原型团队。`} />;
}
