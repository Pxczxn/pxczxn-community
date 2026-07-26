export type StatusTone = 'default' | 'success' | 'warning' | 'error' | 'info'

const labelMap: Record<string, string> = {
  NORMAL: '正常',
  DISABLED: '禁用',
  BANNED: '封禁',
  ACTIVE: '启用',
  INACTIVE: '停用',
  HIDDEN: '隐藏',
  MERGED: '已合并',
  DELETED: '已删除',
  VERIFIED: '已认证',
  UNVERIFIED: '未认证',
  PENDING: '待认证',
  PERSONAL: '个人博客',
  TEAM: '团队博客',
  PUBLIC: '公开',
  PRIVATE: '私密',
  UNLISTED: '不列出',
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  APPROVED: '已通过',
  REVISION_REQUIRED: '待修改',
  REJECTED: '已驳回',
  PUBLISHED: '已发布',
  SCHEDULED: '定时发布',
  PUBLISH_FAILED: '发布失败',
  MANUAL: '手动',
  IMMEDIATE: '立即',
  QUEUED: '待领取',
  AUTO_REVIEWING: '自动审核中',
  MANUAL_REVIEWING: '人工审核中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
  CRITICAL: '严重风险',
  AUTO_PUBLISHED: '自动发布',
  AUTO_REVIEW_QUEUED: '自动转人工',
  USER_DELETED: '用户删除',
  AUTHOR_HIDDEN: '作者隐藏',
  BLOG_HIDDEN: '博客隐藏',
  PLATFORM_APPROVED: '平台审核通过',
  PLATFORM_REJECTED: '平台审核驳回',
  PLATFORM_TAKEN_DOWN: '平台下架',
  PLATFORM_RESTORED: '平台恢复',
  HIDDEN_BY_AUTHOR: '作者隐藏',
  HIDDEN_BY_BLOG: '博客隐藏',
  DELETED_BY_USER: '用户删除',
  TAKEN_DOWN: '平台下架',
  SPAM: '垃圾内容',
  ARTICLE: '文章',
  MOMENT: '动态',
  COMMENT: '评论',
  BLOG: '博客',
  LIKE: '点赞',
  FAVORITE: '收藏',
  FOLLOW: '关注',
  TEXT: '文字',
  IMAGE: '图片',
  LINK: '链接',
  ARTICLE_SHARE: '文章分享',
  PROJECT_UPDATE: '项目更新',
  CODE: '代码',
  POLL: '投票',
  TEAM_NOTICE: '团队公告',
  REPOST: '转发',
  QUOTE: '引用',
  VIDEO_LINK: '视频链接',
  FOLLOWERS_ONLY: '仅关注者',
  IMPORTANT: '重要动态',
  MUTED: '免打扰'
}

export function statusLabel(value?: string | null): string {
  if (!value) return '—'
  return labelMap[value] || value
}

export function statusTone(value?: string | null): StatusTone {
  if (!value) return 'default'
  if (['NORMAL', 'ACTIVE', 'VERIFIED', 'APPROVED', 'PUBLISHED', 'COMPLETED', 'PLATFORM_APPROVED', 'PLATFORM_RESTORED'].includes(value)) {
    return 'success'
  }
  if (['PENDING', 'PENDING_REVIEW', 'SCHEDULED', 'QUEUED', 'AUTO_REVIEWING', 'MANUAL_REVIEWING', 'MEDIUM'].includes(value)) {
    return 'warning'
  }
  if (['DISABLED', 'BANNED', 'DELETED', 'REJECTED', 'PUBLISH_FAILED', 'CRITICAL', 'HIGH', 'TAKEN_DOWN', 'SPAM', 'PLATFORM_REJECTED', 'PLATFORM_TAKEN_DOWN'].includes(value)) {
    return 'error'
  }
  if (['PUBLIC', 'PERSONAL', 'TEAM', 'HIDDEN', 'MERGED', 'LOW', 'REVISION_REQUIRED'].includes(value)) {
    return 'info'
  }
  return 'default'
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date).replace(/\//g, '-')
}

export function compactNumber(value: number): string {
  return new Intl.NumberFormat('zh-CN', {
    notation: value >= 10000 ? 'compact' : 'standard',
    maximumFractionDigits: 1
  }).format(value)
}
