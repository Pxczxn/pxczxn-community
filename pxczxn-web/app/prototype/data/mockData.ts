/**
 * 星语社区 (pxczxn-community V2.1) - 真实中文模拟数据
 */

import {
  User,
  Article,
  Moment,
  Series,
  Team,
  Submission,
  NotificationItem,
  ChatConversation,
  ChatMessage,
  CommentItem,
  IdeaItem,
  GovernanceCase,
} from '../types';

export const currentUser: User = {
  id: 'u-1',
  username: 'pxczxn',
  displayName: '星语客',
  avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
  bio: '星语社区核心架构师 | 专注于全栈开发、AI Agent 协同与知识沉淀体系建设',
  blogSlug: 'pxczxn-blog',
  followersCount: 1280,
  followingCount: 342,
  articlesCount: 48,
  seriesCount: 5,
  verified: true,
  role: 'CREATOR',
  location: '杭州 / 远程',
  website: 'https://pxczxn.community',
  tags: ['Java 21', 'React 19', 'Spring Boot 3.5', 'AI Agent', '知识架构'],
};

export const sampleUsers: User[] = [
  currentUser,
  {
    id: 'u-2',
    username: 'cloud_architect',
    displayName: '云深拓荒者',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
    bio: '专注 Kubernetes 与 Serverless 云原生实践，星语云原生团队队长。',
    blogSlug: 'cloud-arch',
    followersCount: 3520,
    followingCount: 189,
    articlesCount: 62,
    seriesCount: 4,
    verified: true,
    role: 'TEAM_ADMIN',
    location: '北京',
    tags: ['K8s', 'Docker', 'Go', 'Service Mesh'],
  },
  {
    id: 'u-3',
    username: 'react_craftsman',
    displayName: '前端匠人',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80',
    bio: 'React 19 特性研究员，专注 Web 性能优化与交互美学。',
    blogSlug: 'react-craft',
    followersCount: 2190,
    followingCount: 204,
    articlesCount: 35,
    seriesCount: 3,
    verified: true,
    role: 'CREATOR',
    location: '上海',
    tags: ['React 19', 'Tailwind', 'Next.js', 'Vite'],
  },
  {
    id: 'u-4',
    username: 'ai_explorer',
    displayName: '智语探索家',
    avatar: 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150&auto=format&fit=crop&q=80',
    bio: '探索 Gemini 与 Antigravity Agent 在企业知识工作流中的落地。',
    blogSlug: 'ai-explorer',
    followersCount: 4100,
    followingCount: 156,
    articlesCount: 51,
    seriesCount: 6,
    verified: true,
    role: 'CREATOR',
    location: '深圳',
    tags: ['LLM', 'Gemini', 'Multi-Agent', 'RAG'],
  },
];

export const mockArticles: Article[] = [
  {
    id: 'art-101',
    title: '星语社区 V2.1 架构演进：从单体博客到多端协作知识矩阵',
    slug: 'pxczxn-community-v2.1-architecture',
    summary: '详细剖析星语社区如何通过 Java 21 虚拟线程、Spring Boot 3.5 与 React 19 架构实现低延迟、高可用的内容沉淀与团队协作系统。',
    content: `## 1. 架构演进背景

星语社区并不是简单的个人博客程序，也不是单纯的即时社交应用。它的核心价值在于**知识生产 -> 协作流转 -> 长期沉淀 -> 社区传播**。

在 V2.1 演进中，我们确立了六大核心主视图：
- **首页**：Personal Dashboard（聚焦“我”的核心信息与待办）
- **发现**：Editorial Explore Hub（非对称策展大图与社区洞察）
- **文章**：Content Index（高密度、高效率的内容检索）
- **动态**：Social Timeline（实时轻量交流与转发）
- **系列**：Reading Shelf & Catalog（长内容编排与追更）
- **团队**：Collaboration Hub（组织、投稿、共创与多角色工作台）

\`\`\`ts
// Java 21 虚拟线程支持示例
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(() -> processCommunityEvent(event));
\`\`\`

## 2. 首屏视口优化 (600-700px CSS)

针对常见的桌面 600px~700px CSS 可视高度，全站应用了弹性自适应网格与紧凑间距机制，核心模块无需滚动即可全貌掌控！`,
    coverImage: 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&auto=format&fit=crop&q=80',
    author: currentUser,
    blogId: 'pxczxn-blog',
    teamId: 'team-1',
    teamName: '星语核心研发组',
    publishedAt: '2026-08-09 14:30',
    updatedAt: '2026-08-10 02:15',
    viewsCount: 3820,
    likesCount: 245,
    favoritesCount: 189,
    commentsCount: 32,
    tags: ['Java 21', 'Spring Boot 3.5', 'React 19', '架构设计'],
    readingTimeMinutes: 8,
    wordCount: 2400,
    isLiked: true,
    isFavorited: true,
    seriesId: 'ser-1',
    seriesTitle: '星语社区系统架构白皮书',
    seriesChapterIndex: 1,
    coAuthors: [{ user: sampleUsers[1], role: '技术审阅' }],
    version: 'v2.1.0',
    followingArticleId: 'art-102',
  },
  {
    id: 'art-102',
    title: 'React 19 异步组件与 Server Actions 在高并发社区中的最佳实践',
    slug: 'react-19-server-actions-community',
    summary: '全面拆解 React 19 的 React Compiler、use Action 钩子以及乐观 UI 更新，如何彻底改善知识社区的前端交互流畅度。',
    content: `## React 19 核心改进

在星语社区的前端重构中，我们全面升级至 React 19。

### 1. 乐观更新（Optimistic UI）
在点赞、收藏和追更等频发互动场景，UI 立即反馈，后台异步同步 API：

\`\`\`tsx
const [optimisticLikes, setOptimisticLikes] = useOptimistic(
  article.likesCount,
  (current, delta: number) => current + delta
);
\`\`\`

### 2. 视觉层无缝响应
针对 600~700px 的桌面短视口，布局能够动态压缩内边距与边框，带来呼吸感与高密度的完美平衡。`,
    coverImage: 'https://images.unsplash.com/photo-1633356122544-f134324a6cee?w=800&auto=format&fit=crop&q=80',
    author: sampleUsers[2],
    blogId: 'react-craft',
    publishedAt: '2026-08-08 10:15',
    updatedAt: '2026-08-08 10:15',
    viewsCount: 2150,
    likesCount: 178,
    favoritesCount: 120,
    commentsCount: 18,
    tags: ['React 19', 'Next.js 16', 'Web前端', '性能优化'],
    readingTimeMinutes: 6,
    wordCount: 1850,
    isLiked: false,
    isFavorited: true,
    seriesId: 'ser-2',
    seriesTitle: 'React 19 全栈实战演练',
    seriesChapterIndex: 2,
    precedingArticleId: 'art-101',
  },
  {
    id: 'art-103',
    title: '大模型 Agent 在知识沉淀中的应用：从孤立笔记到协同知识网',
    slug: 'agent-knowledge-mesh-architecture',
    summary: '介绍如何通过 Gemini 与 Antigravity Agent 构建灵感箱（Idea Box）、自动目录建议与前置/后置知识关联网络。',
    content: `## 为什么孤立笔记是无效的？

传统笔记工具容易陷入“收集即忘记”的黑洞。星语社区通过 Agent 的自动分类、语义关联与多端投稿机制，让零碎想法顺畅转化为结构化系列文章。`,
    coverImage: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80',
    author: sampleUsers[3],
    blogId: 'ai-explorer',
    publishedAt: '2026-08-07 18:40',
    updatedAt: '2026-08-07 18:40',
    viewsCount: 4120,
    likesCount: 310,
    favoritesCount: 260,
    commentsCount: 45,
    tags: ['AI Agent', 'Gemini', '知识图谱', '创作工具'],
    readingTimeMinutes: 10,
    wordCount: 3200,
    isLiked: true,
    isFavorited: false,
  },
];

export const mockSeries: Series[] = [
  {
    id: 'ser-1',
    title: '星语社区系统架构白皮书',
    slug: 'pxczxn-community-architecture-whitepaper',
    description: '涵盖模块化单体后端、版本化数据库迁移、六视图 UI 体系与团队共创全链路设计。',
    coverImage: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80',
    author: currentUser,
    blogName: '星语客官方博客',
    isTeam: true,
    status: 'ONGOING',
    totalChapters: 8,
    followersCount: 890,
    updatedAt: '2026-08-09',
    readingProgress: {
      lastReadChapterIndex: 1,
      lastReadChapterTitle: '1. 星语社区 V2.1 架构演进',
      completedPercentage: 25,
    },
    isFollowing: true,
    chapters: [
      { id: 'chap-1', index: 1, articleId: 'art-101', title: '星语社区 V2.1 架构演进：从单体博客到多端协作', readTime: '8 min', isRead: true },
      { id: 'chap-2', index: 2, articleId: 'art-102', title: 'MySQL 迁移校验与数据强一致性守护', readTime: '10 min', isRead: false },
      { id: 'chap-3', index: 3, articleId: 'art-103', title: '团队工作台 (Workspace) 权限与投稿模型', readTime: '12 min', isRead: false },
      { id: 'chap-4', index: 4, articleId: 'art-104', title: '实时社交 Timeline 与消息分发架构', readTime: '9 min', isRead: false },
    ],
  },
  {
    id: 'ser-2',
    title: 'React 19 & Next.js 16 全栈实战指南',
    slug: 'react-19-next-16-fullstack',
    description: '从组件设计到服务端渲染，一步步打造具备响应式与高度美感的现代化 Web 应用。',
    coverImage: 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&auto=format&fit=crop&q=80',
    author: sampleUsers[2],
    blogName: '前端匠人专栏',
    isTeam: false,
    status: 'COMPLETED',
    totalChapters: 6,
    followersCount: 1450,
    updatedAt: '2026-08-05',
    readingProgress: {
      lastReadChapterIndex: 4,
      lastReadChapterTitle: '4. 状态管理与上下文的最佳融合',
      completedPercentage: 66,
    },
    isFollowing: true,
    chapters: [
      { id: 'r19-1', index: 1, articleId: 'art-201', title: 'React 19 核心理念与 Hooks 全解析', readTime: '7 min', isRead: true },
      { id: 'r19-2', index: 2, articleId: 'art-102', title: 'React 19 异步组件与 Server Actions 实践', readTime: '6 min', isRead: true },
      { id: 'r19-3', index: 3, articleId: 'art-203', title: 'Tailwind CSS v4 样式引擎深度探索', readTime: '8 min', isRead: true },
      { id: 'r19-4', index: 4, articleId: 'art-204', title: '状态管理与上下文的最佳融合', readTime: '9 min', isRead: true },
      { id: 'r19-5', index: 5, articleId: 'art-205', title: '动画与交互：Motion React 指南', readTime: '11 min', isRead: false },
    ],
  },
  {
    id: 'ser-3',
    title: '云原生微服务与 Go/Java 混合架构',
    slug: 'cloud-native-microservices-guide',
    description: '探讨跨语言微服务、Prometheus 监控、Grafana 告警与极速部署实践。',
    coverImage: 'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=600&auto=format&fit=crop&q=80',
    author: sampleUsers[1],
    blogName: '云深拓荒队',
    isTeam: true,
    status: 'ONGOING',
    totalChapters: 12,
    followersCount: 620,
    updatedAt: '2026-08-07',
    isFollowing: false,
    chapters: [
      { id: 'cn-1', index: 1, articleId: 'art-301', title: '云原生服务治理基础概览', readTime: '10 min', isRead: false },
      { id: 'cn-2', index: 2, articleId: 'art-302', title: 'Prometheus + Grafana 社区运维实战', readTime: '14 min', isRead: false },
    ],
  },
];

export const mockMoments: Moment[] = [
  {
    id: 'mom-1',
    author: currentUser,
    momentType: 'TEXT',
    textContent: '今天完成了星语社区 V2.1 整体架构设计！新增了【我的书架】与【团队工作台 6 大子板块】，在 600-700px 短视口下视觉自适应非常流畅🚀',
    createdAt: '10分钟前',
    likesCount: 28,
    favoritesCount: 14,
    commentsCount: 6,
    isLiked: true,
    isFavorited: true,
    visibility: 'PUBLIC',
  },
  {
    id: 'mom-2',
    author: sampleUsers[3],
    momentType: 'ARTICLE_SHARE',
    textContent: '强烈推荐大家阅读这篇文章，对理解大模型 Agent 与社区知识图谱的结合有很大启发：',
    articleId: 'art-103',
    articleTitle: '大模型 Agent 在知识沉淀中的应用：从孤立笔记到协同知识网',
    createdAt: '1小时前',
    likesCount: 42,
    favoritesCount: 19,
    commentsCount: 8,
    isLiked: false,
    isFavorited: false,
    visibility: 'PUBLIC',
  },
  {
    id: 'mom-3',
    author: sampleUsers[2],
    momentType: 'LINK',
    textContent: '分享一个刚发现的极简 Tailwind V4 样式与自适应 Flex 工具箱，很适合低视口布局调优：',
    linkUrl: 'https://tailwindcss.com/docs/v4-beta',
    createdAt: '3小时前',
    likesCount: 15,
    favoritesCount: 9,
    commentsCount: 2,
    isLiked: false,
    isFavorited: false,
    visibility: 'PUBLIC',
  },
];

export const mockTeams: Team[] = [
  {
    id: 'team-1',
    name: '星语核心研发组',
    slug: 'starry-core-dev',
    description: '负责星语社区核心模块、架构演进与全栈基础组件的迭代研发。',
    avatar: 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150&auto=format&fit=crop&q=80',
    bgBanner: 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&auto=format&fit=crop&q=80',
    blogId: 'team-blog-1',
    category: '技术研发',
    membersCount: 12,
    articlesCount: 38,
    seriesCount: 4,
    followersCount: 1240,
    pendingSubmissionsCount: 3,
    myRole: 'OWNER',
    owner: currentUser,
    isFollowing: true,
    contentDirection: '专注高性能 Java、React 19、云原生架构与系统演进',
    allowSubmissions: true,
    submissionGuidelines: '欢迎提交高质量的技术原创文章，要求排版整洁并包含实际代码示例。',
  },
  {
    id: 'team-2',
    name: '云原生架构探索队',
    slug: 'cloud-native-explorers',
    description: '关注 Kubernetes、Docker、Service Mesh 与可观测性运维体系。',
    avatar: 'https://images.unsplash.com/photo-1531482615713-2afd69097998?w=150&auto=format&fit=crop&q=80',
    bgBanner: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop&q=80',
    blogId: 'team-blog-2',
    category: '云原生/运维',
    membersCount: 8,
    articlesCount: 22,
    seriesCount: 2,
    followersCount: 860,
    pendingSubmissionsCount: 1,
    myRole: 'EDITOR',
    owner: sampleUsers[1],
    isFollowing: false,
    contentDirection: 'K8s 运维、Prometheus 告警与云基础设施实践',
    allowSubmissions: true,
  },
  {
    id: 'team-3',
    name: 'AI Agent & 知识图谱实验室',
    slug: 'ai-knowledge-lab',
    description: '探索 Gemini API、智能辅助写作与企业级协同知识网。',
    avatar: 'https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=150&auto=format&fit=crop&q=80',
    bgBanner: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80',
    blogId: 'team-blog-3',
    category: '人工智能',
    membersCount: 15,
    articlesCount: 29,
    seriesCount: 3,
    followersCount: 1980,
    pendingSubmissionsCount: 0,
    owner: sampleUsers[3],
    isFollowing: true,
    contentDirection: '大模型集成、RAG、Agent 智能工作流',
    allowSubmissions: true,
  },
];

export const mockSubmissions: Submission[] = [
  {
    id: 'sub-1',
    articleId: 'art-102',
    articleTitle: 'React 19 异步组件与 Server Actions 在高并发社区中的最佳实践',
    author: sampleUsers[2],
    teamId: 'team-1',
    teamName: '星语核心研发组',
    submittedAt: '2026-08-09 16:20',
    status: 'PENDING',
    reviewComment: '等待团队管理员组审核内容深度与示例代码完整度。',
  },
  {
    id: 'sub-2',
    articleId: 'art-103',
    articleTitle: '大模型 Agent 在知识沉淀中的应用：从孤立笔记到协同知识网',
    author: sampleUsers[3],
    teamId: 'team-1',
    teamName: '星语核心研发组',
    submittedAt: '2026-08-08 11:00',
    status: 'APPROVED',
    reviewComment: '审核通过：内容符合团队技术方向，已正式收入星语团队博客。',
  },
];

export const mockNotifications: NotificationItem[] = [
  {
    id: 'notif-1',
    category: 'COMMENT',
    title: '收到新的文章评论',
    content: '前端匠人 评论了你的文章《星语社区 V2.1 架构演进》：“分析得很彻底，特别是首屏 600px 视口适配的设计启发很大！”',
    createdAt: '15分钟前',
    isRead: false,
    sender: sampleUsers[2],
    targetUrl: '/articles/art-101',
  },
  {
    id: 'notif-2',
    category: 'SUBMISSION',
    title: '收到新的团队投稿',
    content: '前端匠人 向【星语核心研发组】提交了文章《React 19 异步组件与 Server Actions...》，请及时审核。',
    createdAt: '1小时前',
    isRead: false,
    sender: sampleUsers[2],
    targetUrl: '/teams/starry-core-dev/workspace',
  },
  {
    id: 'notif-3',
    category: 'FOLLOW',
    title: '新增粉丝关注',
    content: '智语探索家 关注了你的个人博客【星语客】',
    createdAt: '3小时前',
    isRead: true,
    sender: sampleUsers[3],
  },
  {
    id: 'notif-4',
    category: 'INTERACTION',
    title: '文章收到点赞',
    content: '云深拓荒者 点赞了你的文章《星语社区 V2.1 架构演进》',
    createdAt: '昨天 19:30',
    isRead: true,
    sender: sampleUsers[1],
  },
];

export const mockConversations: ChatConversation[] = [
  {
    id: 'conv-1',
    peerUser: sampleUsers[2],
    lastMessage: '星语社区 V2.1 的 600-700px 视口紧凑排版效果太棒了！',
    lastTime: '14:20',
    unreadCount: 1,
  },
  {
    id: 'conv-2',
    peerUser: sampleUsers[3],
    lastMessage: '大模型 Agent 灵感箱（Idea Box）已经梳理好逻辑，有空一起交流~',
    lastTime: '昨天',
    unreadCount: 0,
  },
];

export const mockChatMessages: ChatMessage[] = [
  {
    id: 'msg-1',
    senderId: 'u-3',
    receiverId: 'u-1',
    text: '嗨，星语客！我认真看了 V2.1 架构白皮书，团队工作台的投稿流程非常清晰。',
    timestamp: '14:15',
    isSelf: false,
  },
  {
    id: 'msg-2',
    senderId: 'u-1',
    receiverId: 'u-3',
    text: '谢谢支持！现在全站已经支持了首屏 600~700px CSS 可视高度自适应，无论笔记本还是大屏体验都不错。',
    timestamp: '14:18',
    isSelf: true,
  },
  {
    id: 'msg-3',
    senderId: 'u-3',
    receiverId: 'u-1',
    text: '星语社区 V2.1 的 600-700px 视口紧凑排版效果太棒了！',
    timestamp: '14:20',
    isSelf: false,
  },
];

export const mockComments: CommentItem[] = [
  {
    id: 'comm-1',
    author: sampleUsers[2],
    content: '这篇文章深入浅出，特别是在团队协作和多模式发布上的梳理非常透彻！点赞支持👍',
    createdAt: '2小时前',
    likesCount: 12,
    isLiked: true,
    replies: [
      {
        id: 'comm-1-1',
        author: currentUser,
        content: '感谢认可！稍后会在团队工作台中加入更多协同编辑历史与 SLA 监控能力。',
        createdAt: '1小时前',
        likesCount: 5,
        isLiked: false,
      },
    ],
  },
  {
    id: 'comm-2',
    author: sampleUsers[1],
    content: '关于后端 Spring Boot 3.5 与 MySQL 迁移的强校验部分，能否展开讲讲事务隔离？',
    createdAt: '昨天 16:40',
    likesCount: 8,
    isLiked: false,
  },
];

export const mockIdeas: IdeaItem[] = [
  {
    id: 'idea-1',
    title: '基于 WebAssembly 的本地 Markdown 实时语法校验器',
    content: '可以在编辑器侧边栏增加 WASM 加速的本地 Lint，零网络延迟提示标点符号与标题结构问题。',
    tags: ['WASM', '前端', '编辑器'],
    createdAt: '2026-08-09',
    sourceType: 'MANUAL',
  },
  {
    id: 'idea-2',
    title: '团队投稿版本对比 Diff 组件设计',
    content: '当作者重新提交修正稿时，审核员能在工作台直接看到 Git-like 的侧边 Diff 高亮。',
    tags: ['团队协作', 'Diff', 'UX'],
    createdAt: '2026-08-08',
    sourceType: 'ARTICLE',
  },
];

export const mockGovernanceCases: GovernanceCase[] = [
  {
    id: 'case-1',
    targetType: 'COMMENT',
    targetTitle: '动态 #102 下的不友善言论举报',
    reason: '包含无意义人身攻击与垃圾广告推广',
    status: 'RESOLVED',
    createdAt: '2026-08-08',
    result: '举报属实，已删除违规评论并对违规账号发出 moderation 警告。',
  },
  {
    id: 'case-2',
    targetType: 'ARTICLE',
    targetTitle: '《未授权转载技术文章》申诉',
    reason: '原创度争议复审',
    status: 'APPEALED',
    createdAt: '2026-08-09',
    result: '申诉已受理，治理小组正在复核源文章创作时间戳。',
  },
];
