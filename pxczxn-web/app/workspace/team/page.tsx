import type { Metadata } from "next";
import Link from "next/link";
import {
  ChevronRight,
  CircleCheck,
  FilePenLine,
  FolderPlus,
  Settings,
  UserPlus,
} from "lucide-react";
import { Metric, SideNavigation } from "../../components/prototype-ui";

export const metadata: Metadata = {
  title: "团队工作台",
};

const pending = [
  ["深度学习模型压缩方法综述", "陈三", "今天 10:23"],
  ["基于知识图谱的问答系统实践", "李四", "今天 10:41"],
  ["大模型在推荐系统中的应用", "王五", "今天 09:35"],
];

export default function TeamWorkspacePage() {
  return (
    <main className="workspace-layout">
      <SideNavigation active="overview" />
      <section className="workspace-main">
        <header className="workspace-header">
          <div>
            <span className="eyebrow">团队概览</span>
            <h1>上午好，欢迎回来</h1>
          </div>
          <Link className="primary-button" href="/collaboration/articles/agent-patterns">
            <FilePenLine size={17} /> 新建文章
          </Link>
        </header>

        <div className="workspace-metrics">
          <Metric label="文章数" value="256" trend="较上周 +16" />
          <Metric label="待审核投稿" value="12" trend="较上周 +4" />
          <Metric label="成员数" value="36" trend="较上周 +2" />
          <Metric label="总计阅读" value="8.6k" trend="较上周 +128" />
        </div>

        <div className="workspace-top-grid">
          <section className="surface workspace-card">
            <div className="card-title-row">
              <h2 className="card-heading">待处理投稿</h2>
              <Link className="link" href="/submissions/ai-agent">查看全部</Link>
            </div>
            <div className="pending-list">
              {pending.map(([title, author, time]) => (
                <Link href="/submissions/ai-agent" key={title}>
                  <span>
                    <strong>{title}</strong>
                    <small>{author}</small>
                  </span>
                  <small>{time}</small>
                </Link>
              ))}
            </div>
            <Link className="link see-all" href="/submissions/ai-agent">
              查看更多 <ChevronRight size={15} />
            </Link>
          </section>

          <section className="surface workspace-card role-card">
            <h2 className="card-heading">成员角色分布</h2>
            <div className="role-content">
              <div className="donut-chart">
                <span><strong>36</strong><small>总成员</small></span>
              </div>
              <ul>
                <li><i className="dot dot-blue" />管理员 <strong>3</strong></li>
                <li><i className="dot dot-indigo" />编辑 <strong>7</strong></li>
                <li><i className="dot dot-cyan" />作者 <strong>18</strong></li>
                <li><i className="dot dot-orange" />贡献者 <strong>8</strong></li>
              </ul>
            </div>
          </section>

          <section className="surface workspace-card quick-card">
            <h2 className="card-heading">快捷操作</h2>
            <div className="quick-actions">
              <button type="button"><FilePenLine size={17} />发布公告</button>
              <button type="button"><UserPlus size={17} />邀请成员</button>
              <button type="button"><FolderPlus size={17} />创建系列</button>
              <Link href="/settings"><Settings size={17} />团队设置</Link>
            </div>
          </section>
        </div>

        <section className="surface workspace-card trend-card">
          <div className="card-title-row">
            <h2 className="card-heading">数据趋势 <small>（近 30 天）</small></h2>
            <div className="chart-legend">
              <span><i className="dot dot-blue" />文章浏览量</span>
              <span><i className="dot dot-indigo" />成员活跃度</span>
            </div>
          </div>
          <div aria-label="近 30 天数据趋势折线图" className="trend-chart">
            <div className="chart-grid" />
            <div className="chart-area chart-area--primary" />
            <div className="chart-area chart-area--secondary" />
            <span className="chart-label chart-label--a">03-18</span>
            <span className="chart-label chart-label--b">03-24</span>
            <span className="chart-label chart-label--c">03-30</span>
            <span className="chart-label chart-label--d">04-05</span>
            <span className="chart-label chart-label--e">04-16</span>
          </div>
        </section>

        <section className="workspace-mobile-summary surface">
          <CircleCheck size={20} />
          <span>今天有 12 篇投稿等待处理</span>
        </section>
      </section>
    </main>
  );
}
