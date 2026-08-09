"use client";

import { SeriesManager } from "../../../../components/series-manager";
import { useWorkspace } from "../workspace-context";

/**
 * 团队工作台的系列管理。系列泛化为博客归属之后，这里不再有专属逻辑，
 * 只是把 team.blogId 交给共享的 SeriesManager，交互与个人中心完全一致。
 */
export default function WorkspaceSeriesPage() {
  const { teamSlug, workspace } = useWorkspace();
  const blogId = workspace?.team.team.blogId ?? null;

  return (
    <div className="workspace-series-page">
      <header className="workspace-content-page__header">
        <div>
          <span className="eyebrow">连载管理</span>
          <p>将团队文章编排成连载，提交审核后公开展示。</p>
        </div>
      </header>

      <SeriesManager
        blogId={blogId}
        publicHref={`/teams/${teamSlug}?tab=series`}
        scopeNoun="团队"
        articleSourceHint="可编排的是团队博客下已发布的文章；成员投稿通过后会自动出现在这里。"
      />
    </div>
  );
}
