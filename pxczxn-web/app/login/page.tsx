import type { Metadata } from "next";
import { AuthPanel } from "./auth-panel";

export const metadata: Metadata = {
  title: "登录 / 注册",
  description: "登录星语社区，开始发现与创作。",
};

export default function LoginPage() {
  return (
    <main className="auth-page">
      <section className="auth-story">
        <div className="auth-brand">
          <span className="brand-mark">A</span>
          <span>星语社区</span>
        </div>
        <h1>
          发现有价值的内容
          <br />
          与有趣的人
        </h1>
        <div aria-label="蓝色群山与远行者插画" className="mountain-scene" role="img" />
      </section>
      <AuthPanel />
    </main>
  );
}
