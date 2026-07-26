import { CommunityPlaceholderPage } from "../../../components/community-placeholder-page";

export default async function CollaborationArticlePage({ params }: { params: Promise<{ articleId: string }> }) {
  const { articleId } = await params;
  return <CommunityPlaceholderPage title="文章共创" phase="M3 待开发" description={`文章 “${articleId}” 的共创关系尚未开放；当前不会展示虚构协作者或权限。`} />;
}
