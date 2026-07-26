import type { Metadata } from "next";
import Link from "next/link";
import {
  CheckCircle2,
  ChevronDown,
  Plus,
  Send,
  ShieldCheck,
} from "lucide-react";
import { Avatar, UserTopbar } from "../../../components/prototype-ui";

export const metadata: Metadata = {
  title: "共创文章协作页",
};

const collaborators = [
  ["程序员小明", "创建者 · 负责整体框架", "程"],
  ["AI探索Eva", "编辑者 · 负责模型相关章节", "E"],
  ["产品经理Tom", "贡献者 · 产品案例部分", "T"],
  ["设计小蓝", "贡献者 · 负责体验部分", "蓝"],
  ["算法小白", "贡献者 · 维护算法说明", "算"],
  ["数据工程师Ben", "贡献者 · 负责数据相关", "B"],
];

export default function CollaborationPage() {
  return (
    <>
      <UserTopbar title="共创空间" />
      <main className="collab-page page-shell">
        <header className="collab-heading">
          <div>
            <div className="collab-title-line">
              <h1>共创文章：大模型应用落地的 10 个关键思考</h1>
              <span className="chip status-success">协作中</span>
            </div>
            <p>创建者：程序员小明　创建于 2024-04-10</p>
          </div>
          <Link className="primary-button" href="/teams/ai-explorers">
            查看文章
          </Link>
        </header>

        <nav className="tabs collab-tabs" aria-label="共创文章">
          <a className="tab active">协作成员</a>
          <a className="tab">章节管理</a>
          <a className="tab">成员管理</a>
          <a className="tab">版本记录</a>
          <a className="tab">设置</a>
        </nav>

        <section className="collab-grid">
          <div className="stack">
            <section className="surface collab-card">
              <div className="card-title-row">
                <h2 className="card-heading">协作成员（6）</h2>
                <button className="secondary-button" type="button">
                  <Plus size={16} /> 邀请成员
                </button>
              </div>
              <div className="collaborator-list">
                {collaborators.map(([name, role, label]) => (
                  <div className="collaborator-row" key={name}>
                    <Avatar label={label} size="md" />
                    <span>
                      <strong>{name}</strong>
                      <small>{role}</small>
                    </span>
                    <span className="chip">已加入</span>
                  </div>
                ))}
              </div>
            </section>

            <section className="surface recent-activity">
              <div className="card-title-row">
                <h2 className="card-heading">最近动态</h2>
                <ChevronDown size={17} className="muted" />
              </div>
              <div className="activity-row">
                <span className="activity-dot" />
                <p>
                  AI探索Eva 编辑了 <a className="link">第 2 章 · 大模型架构</a>
                  <small>10 分钟前</small>
                </p>
              </div>
            </section>
          </div>

          <aside className="stack">
            <section className="surface collab-card">
              <h2 className="card-heading">协作规则</h2>
              <ul className="rule-list">
                <li><CheckCircle2 size={15} />尊重他人，友善协作</li>
                <li><CheckCircle2 size={15} />及时沟通，保持同步</li>
                <li><CheckCircle2 size={15} />内容须原创，禁止抄袭</li>
                <li><CheckCircle2 size={15} />发布前需所有主要成员确认</li>
              </ul>
            </section>

            <section className="surface collab-card invite-card">
              <h2 className="card-heading">邀请协作者</h2>
              <label>
                <span>输入邮箱或用户名</span>
                <input className="field" placeholder="name@example.com" />
              </label>
              <label>
                <span>贡献者</span>
                <select className="field" defaultValue="contributor">
                  <option value="contributor">贡献者</option>
                  <option value="editor">编辑者</option>
                </select>
              </label>
              <label>
                <span>邀请信息（可选）</span>
                <textarea
                  className="field textarea"
                  defaultValue="你好，邀请你一起参与这篇文章的创作。"
                />
              </label>
              <button className="primary-button" type="button">
                <Send size={16} /> 发送邀请
              </button>
            </section>

            <section className="collab-security">
              <ShieldCheck size={18} />
              <span>成员权限由创建者统一管理</span>
            </section>
          </aside>
        </section>
      </main>
    </>
  );
}
