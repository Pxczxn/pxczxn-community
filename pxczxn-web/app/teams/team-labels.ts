/** 团队模块共享的中文标签与格式化工具。 */

export const ROLE_LABELS: Record<string, string> = {
  OWNER: "所有者",
  ADMIN: "管理员",
  EDITOR: "编辑",
  AUTHOR: "作者",
};

/** 团队分类选项结构：value 入库、label 展示、desc 用于建团申请的选项说明。 */
export interface TeamCategoryOption {
  value: string;
  label: string;
  desc: string;
}

/** 团队分类字典（团队模块唯一来源）。设置页、发现筛选、建团申请共用同一份。 */
export const TEAM_CATEGORIES: TeamCategoryOption[] = [
  { value: "技术社区", label: "技术社区", desc: "技术交流、工程实践与前沿探讨" },
  { value: "开源项目", label: "开源项目", desc: "开源库、工具与社区协作" },
  { value: "兴趣小组", label: "兴趣小组", desc: "同好交流、兴趣驱动的内容" },
  { value: "校园社团", label: "校园社团", desc: "高校社团与校园创作者" },
  { value: "游戏攻略", label: "游戏攻略", desc: "游戏玩法、攻略与赛事" },
  { value: "小说创作", label: "小说创作", desc: "原创小说与文学创作" },
  { value: "其他", label: "其他", desc: "不属于以上分类的方向" },
];

/**
 * 团队权限码，与 `team_permission` 种子数据(V023)逐字对齐，是前端判断权限的唯一事实来源。
 *
 * 页面里散写字符串曾经导致真实故障：投稿页用了后端不存在的 `SUBMISSION_REVIEW`，
 * 审核 Tab 因此永远不显示，而概览页却在提示"有待审投稿"。集中定义后类型系统会拦住这类拼写。
 */
export const TEAM_PERMISSIONS = {
  MANAGE_TEAM: "MANAGE_TEAM",
  MANAGE_MEMBERS: "MANAGE_MEMBERS",
  TRANSFER_OWNERSHIP: "TRANSFER_OWNERSHIP",
  DISBAND_TEAM: "DISBAND_TEAM",
  EDIT_ALL_ARTICLES: "EDIT_ALL_ARTICLES",
  DELETE_ALL_ARTICLES: "DELETE_ALL_ARTICLES",
  MANAGE_SUBMISSIONS: "MANAGE_SUBMISSIONS",
  MANAGE_SERIES: "MANAGE_SERIES",
  VIEW_AUDIT: "VIEW_AUDIT",
  EDIT_OWN_ARTICLES: "EDIT_OWN_ARTICLES",
} as const;

export type TeamPermission = (typeof TEAM_PERMISSIONS)[keyof typeof TEAM_PERMISSIONS];

/** 判断权限集合是否包含某权限码；permissions 可能因接口未就绪而为空。 */
export function hasTeamPermission(
  permissions: readonly string[] | null | undefined,
  permission: TeamPermission,
): boolean {
  return Boolean(permissions?.includes(permission));
}

export const PUBLISH_STATUS_LABELS: Record<string, string> = {
  DRAFT: "草稿",
  PENDING_REVIEW: "审核中",
  APPROVED: "已通过",
  SCHEDULED: "定时发布",
  PUBLISHED: "已发布",
  HIDDEN: "已隐藏",
  TAKEN_DOWN: "已下线",
  PUBLISH_FAILED: "发布失败",
  DELETED: "已删除",
};

export const REVIEW_STATUS_LABELS: Record<string, string> = {
  NOT_SUBMITTED: "未提交",
  QUEUED: "排队中",
  AUTO_REVIEWING: "自动审核中",
  MANUAL_REVIEWING: "人工审核中",
  APPROVED: "已通过",
  REVISION_REQUIRED: "要求修改",
  REJECTED: "已退回",
  CANCELLED: "已取消",
  EXPIRED: "已过期",
};

/**
 * 系列相关标签已随"系列泛化为博客归属"迁到 lib/series-labels.ts。
 * 这里保留转发，团队模块内既有 import 无需改动，同时避免出现第二份字典。
 */
export { SERIALIZATION_LABELS, SERIES_REVIEW_LABELS } from "../lib/series-labels";

export const SUBMISSION_STATUS_LABELS: Record<string, string> = {
  TEAM_PENDING: "待团队审核",
  TEAM_REVISION_REQUIRED: "要求修改",
  TEAM_REJECTED: "已拒绝",
  PLATFORM_PENDING: "平台审核中",
  PLATFORM_REVISION_REQUIRED: "平台要求修改",
  PLATFORM_REJECTED: "平台已拒绝",
  PLATFORM_PUBLISHING: "发布中",
  PUBLISHED: "已发布",
};

export const INVITATION_STATUS_LABELS: Record<string, string> = {
  PENDING: "待处理",
  ACCEPTED: "已接受",
  REJECTED: "已拒绝",
  EXPIRED: "已过期",
  REVOKED: "已撤销",
};

export const APPLICATION_STATUS_LABELS: Record<string, string> = {
  PENDING: "审核中",
  APPROVED: "已通过",
  REJECTED: "已拒绝",
  CANCELLED: "已撤回",
};

/** 团队协作活动事件的中文描述模板。 */
export const ACTIVITY_LABELS: Record<string, (actor: string | null) => string> = {
  MEMBER_INVITED: (actor) => `${actor || "成员"} 邀请了新成员加入团队`,
  INVITATION_ACCEPTED: (actor) => `${actor || "成员"} 接受了邀请，加入了团队`,
  INVITATION_REJECTED: (actor) => `${actor || "成员"} 拒绝了团队邀请`,
  INVITATION_REVOKED: (actor) => `${actor || "成员"} 撤销了一条团队邀请`,
  MEMBER_LEFT: (actor) => `${actor || "成员"} 退出了团队`,
  MEMBER_REMOVED: (actor) => `${actor || "成员"} 将一名成员移出团队`,
  MEMBER_ROLE_CHANGED: (actor) => `${actor || "成员"} 调整了成员角色`,
  OWNERSHIP_TRANSFERRED: (actor) => `${actor || "所有者"} 转让了团队所有权`,
  TEAM_DISBANDED: (actor) => `${actor || "所有者"} 解散了团队`,
  SERIES_CREATED: (actor) => `${actor || "成员"} 创建了新系列`,
  SERIES_UPDATED: (actor) => `${actor || "成员"} 更新了系列`,
  SERIES_CHAPTERS_REORDERED: (actor) => `${actor || "成员"} 调整了系列章节`,
  SERIES_SUBMITTED_FOR_REVIEW: (actor) => `${actor || "成员"} 提交了系列审核`,
  EXTERNAL_SUBMISSION_CREATED: (actor) => `${actor || "成员"} 向团队提交了投稿`,
  EXTERNAL_SUBMISSION_TEAM_DECISION: (actor) => `${actor || "成员"} 审核了投稿`,
  EXTERNAL_SUBMISSION_TEAM_REVISION: (actor) => `${actor || "成员"} 要求修改投稿`,
  EXTERNAL_SUBMISSION_TEAM_REJECTED: (actor) => `${actor || "成员"} 拒绝了投稿`,
  EXTERNAL_SUBMISSION_PLATFORM_APPROVED: () => "投稿已通过平台审核并发布",
  EXTERNAL_SUBMISSION_PLATFORM_DECISION: () => "投稿已由平台处理",
};

export function activityText(eventType: string, actor: string | null): string {
  const template = ACTIVITY_LABELS[eventType];
  return template ? template(actor) : `${actor || "成员"} 更新了团队动态`;
}

export function formatDateTime(value?: string | null): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  const now = new Date();
  const diffMinutes = Math.floor((now.getTime() - date.getTime()) / 60000);
  if (diffMinutes < 1) return "刚刚";
  if (diffMinutes < 60) return `${diffMinutes} 分钟前`;
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours} 小时前`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays} 天前`;
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** 团队切换器记住上次选择，后端数据仍是事实来源。 */
const TEAM_SWITCHER_KEY = "pxczxn-teams-last-selection";

export function readLastTeamSelection(): string | null {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem(TEAM_SWITCHER_KEY);
  } catch {
    return null;
  }
}

export function saveLastTeamSelection(teamId: string) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(TEAM_SWITCHER_KEY, teamId);
  } catch {
    // 本地存储不可用时忽略，不影响功能
  }
}

export function formatCount(value: number | undefined | null): string {
  const n = Number(value) || 0;
  if (n >= 10000) return `${(n / 10000).toFixed(1)}万`;
  return String(n);
}
