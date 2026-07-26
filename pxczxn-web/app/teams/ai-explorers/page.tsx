import type { Metadata } from "next";
import Link from "next/link";
import {
  Bell,
  Bookmark,
  ChevronRight,
  Eye,
  Heart,
  MoreHorizontal,
  Search,
  Users,
} from "lucide-react";
import { ArticleThumb, Avatar } from "../../components/prototype-ui";

export const metadata: Metadata = {
  title: "AI探索者团队",
};

const articles = [
  {
    title: "大模型微调实战：从数据构造到效果评估",
    tags: ["大模型", "微调", "实战"],
    time: "2 小时前",
    views: 138,
    likes: 28,
    comments: 56,
  },
  {
    title: "RAG 系统落地实践与性能优化",
    tags: ["RAG", "向量检索"],
    time: "9 小时前",
    views: 96,
    likes: 21,
    comments: 34,
  },
  {
    title: "使用 LangChain 构建多智能体应用",
    tags: ["LangChain", "多智能体"],
    time: "3 天前",
    views: 76,
    likes: 18,
    comments: 29,
  },
  {
    title: "AI 时代的产品设计思考",
    tags: ["产品设计", "思考"],
    time: "5 天前",
    views: 62,
    likes: 13,
    comments: 22,
  },
];

export default function TeamBlogPage() {
  return (
    <main className="team-page">
      <div className="team-blog surface-lg shadow-sm">
        <div className="team-cover">
          <Link className="team-cover__home" href="/login">
            星语社区
          </Link>
        </div>
        <section className="team-identity">
          <div className="team-logo">AI</div>
          <div className="team-identity__copy">
            <div className="team-name-row">
              <h1>AI探索者团队</h1>
              <span className="verified">✓</span>
            </div>
            <p>专注于人工智能前沿研究与实践分享，探索前沿技术与未来趋势。</p>
            <div className="team-stats-inline">
              <span>文章 <strong>256</strong></span>
              <span>成员 <strong>36</strong></span>
              <span>关注 <strong>1.2k</strong></span>
              <span>粉丝 <strong>8.6k</strong></span>
            </div>
          </div>
          <div className="team-actions">
            <Link className="primary-button" href="/workspace/team">
              <Users size={17} /> 进入团队
            </Link>
            <button aria-label="更多操作" className="icon-button" type="button">
              <MoreHorizontal size={18} />
            </button>
          </div>
        </section>

        <div className="team-tabs-row">
          <nav className="tabs" aria-label="团队内容">
            <Link className="tab active" href="/teams/ai-explorers">文章</Link>
            <Link className="tab" href="/moments/agent-architecture">动态</Link>
            <a className="tab">系列</a>
            <a className="tab">成员</a>
            <Link className="tab" href="/submissions/ai-agent">投稿</Link>
          </nav>
          <label className="team-search">
            <input aria-label="搜索团队文章" placeholder="搜索团队文章" />
            <Search size={16} />
          </label>
        </div>

        <section className="team-content">
          <div className="article-list">
            {articles.map((article, index) => (
              <Link
                className="team-article"
                href="/collaboration/articles/agent-patterns"
                key={article.title}
              >
                <ArticleThumb variant={index + 1} />
                <span className="team-article__body">
                  <strong>{article.title}</strong>
                  <span className="article-tags">
                    {article.tags.map((tag) => <span className="chip" key={tag}>{tag}</span>)}
                  </span>
                  <small className="muted">{article.time}</small>
                </span>
                <span className="team-article__meta">
                  <small><Eye size={14} /> {article.views}</small>
                  <small><Heart size={14} /> {article.likes}</small>
                  <small><Bookmark size={14} /> {article.comments}</small>
                </span>
              </Link>
            ))}
            <Link className="view-more link" href="/workspace/team">
              查看更多文章 <ChevronRight size={15} />
            </Link>
          </div>

          <aside className="team-aside stack">
            <div className="surface side-card">
              <h2 className="card-heading">团队数据</h2>
              <dl className="team-data-list">
                <div><dt>文章数</dt><dd>256</dd></div>
                <div><dt>动态数</dt><dd>128</dd></div>
                <div><dt>成员数</dt><dd>36</dd></div>
                <div><dt>关注数</dt><dd>1.2k</dd></div>
                <div><dt>粉丝数</dt><dd>8.6k</dd></div>
              </dl>
            </div>
            <div className="surface side-card">
              <h2 className="card-heading">活跃成员</h2>
              <div className="active-members">
                {["程序员小明", "AI探索Eva", "产品经理Tom", "算法小白", "设计小蓝"].map(
                  (member, index) => (
                    <div className="member-row" key={member}>
                      <Avatar label={member.slice(-1)} size="sm" />
                      <span>{member}</span>
                      <small>文章 {86 - index * 14}</small>
                    </div>
                  ),
                )}
              </div>
              <a className="view-more link">
                查看更多成员 <ChevronRight size={15} />
              </a>
            </div>
            <div className="surface side-card team-notice">
              <Bell size={18} />
              <div>
                <strong>团队公告</strong>
                <p>每周五晚进行技术分享，欢迎成员提交选题。</p>
              </div>
            </div>
          </aside>
        </section>
      </div>
    </main>
  );
}
