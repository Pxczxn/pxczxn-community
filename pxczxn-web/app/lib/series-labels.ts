/**
 * 系列模块共享的中文标签与状态判断。
 *
 * 系列已经从"团队专属"泛化为"博客归属"，个人博客与团队博客共用同一套语义，
 * 因此这份字典必须放在中立位置，而不是继续挂在 teams/team-labels.ts 下面。
 * team-labels.ts 会从这里转发，保证全站只有一份事实来源。
 */

export const SERIALIZATION_LABELS: Record<string, string> = {
  ONGOING: "连载中",
  COMPLETED: "已完结",
  PAUSED: "已暂停",
};

export const SERIES_REVIEW_LABELS: Record<string, string> = {
  DRAFT: "草稿",
  PENDING_REVIEW: "审核中",
  APPROVED: "已通过",
  REJECTED: "已退回",
};

/** 连载状态下拉选项，创建与编辑系列共用。 */
export const SERIALIZATION_OPTIONS = [
  { value: "ONGOING", label: "连载中" },
  { value: "COMPLETED", label: "已完结" },
  { value: "PAUSED", label: "已暂停" },
] as const;

/** 博客类型标签，用于系列卡片上标注"这是谁的系列"。 */
export const BLOG_TYPE_LABELS: Record<string, string> = {
  PERSONAL: "个人博客",
  TEAM: "团队博客",
};

export function serializationLabel(value: string | null | undefined): string {
  return (value && SERIALIZATION_LABELS[value]) || "连载";
}

export function seriesReviewLabel(value: string | null | undefined): string {
  return (value && SERIES_REVIEW_LABELS[value]) || value || "未知状态";
}

export function blogTypeLabel(value: string | null | undefined): string {
  return (value && BLOG_TYPE_LABELS[value]) || "博客";
}

/** 只有草稿与被退回的系列可以改标题、调章节；审核中与已公开的必须走新一轮流程。 */
export function isSeriesEditable(reviewStatus: string): boolean {
  return reviewStatus === "DRAFT" || reviewStatus === "REJECTED";
}

/**
 * 阅读进度百分比。章节数为 0 时返回 0 而不是 NaN，
 * 已读章节多于总章节（作者删章的历史进度）时截断到 100。
 */
export function readingPercent(readChapterCount: number, chapterCount: number): number {
  if (!chapterCount || chapterCount <= 0) return 0;
  return Math.min(100, Math.round((readChapterCount / chapterCount) * 100));
}
