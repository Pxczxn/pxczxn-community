/* eslint-disable @next/next/no-html-link-for-pages -- Vinext's client package proxy cannot load next/link at runtime on this entry route. */
import type { Metadata } from "next";
import { ArrowUpRight, BookOpenText, Orbit, Sparkles, UsersRound } from "lucide-react";
import { AuthPanel } from "./auth-panel";

export const metadata: Metadata = {
  title: "登录 / 注册",
  description: "登录星语社区，开始发现与创作。",
};

export default function LoginPage() {
  return (
    <main className="auth-page">
      <section className="auth-story-panel">
        <div className="auth-story-orbit auth-story-orbit--one" aria-hidden="true" />
        <div className="auth-story-orbit auth-story-orbit--two" aria-hidden="true" />
        <div className="auth-story-content">
          <a href="/discover" className="auth-brand" aria-label="前往星语社区发现页">
            <span className="brand-logo"><Sparkles size={19} /></span>
            <span>星语社区</span>
            <span className="auth-brand-version">V2.2</span>
          </a>

          <p className="auth-story-eyebrow"><Sparkles size={15} /> 为持续创作而生</p>
          <h1>
            让每一次表达，
            <span>抵达同频的人。</span>
          </h1>
          <p className="auth-story-copy">
            记录灵感、连载知识、参与团队协作。星语社区为认真创作的人，留出一片可被看见的空间。
          </p>

          <div className="auth-story-points" aria-label="星语社区能力">
            <div className="auth-story-point">
              <BookOpenText size={18} />
              <span>沉浸式写作与连载</span>
            </div>
            <div className="auth-story-point">
              <UsersRound size={18} />
              <span>和团队一起共创</span>
            </div>
            <div className="auth-story-point">
              <Orbit size={18} />
              <span>发现值得讨论的内容</span>
            </div>
          </div>

          <a href="/discover" className="auth-story-link">
            先看看社区正在发生什么 <ArrowUpRight size={16} />
          </a>
        </div>
      </section>

      <section className="auth-form-panel">
        <div className="auth-form-container">
          <AuthPanel />
        </div>
      </section>
    </main>
  );
}
