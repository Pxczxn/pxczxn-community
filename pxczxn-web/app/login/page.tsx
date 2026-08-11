import type { Metadata } from "next";
import { AuthPanel } from "./auth-panel";

export const metadata: Metadata = {
  title: "登录 / 注册",
  description: "登录星语社区，开始发现与创作。",
};

export default function LoginPage() {
  return (
    <main className="auth-page">
      {/* 左侧品牌展示区 */}
      <section className="auth-story-panel">
        <div className="auth-brand">
          <span className="brand-logo">星</span>
          <span>星语社区</span>
        </div>
        <h1>
          发现有价值的内容
          <br />
          与有趣的人
        </h1>
        <div className="mountain-illustration" aria-hidden="true" />
      </section>

      {/* 右侧表单区 */}
      <section className="auth-form-panel">
        <div className="auth-form-container">
          <AuthPanel />
        </div>
      </section>
    </main>
  );
}
