"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

/**
 * 旧的独立"团队邀请"页已并入 `/teams?tab=requests`。
 *
 * 邀请、团队建立申请、我的团队现在共用同一个入口，保留这条路由只是为了不让历史链接和书签 404。
 */
export default function TeamInvitationsRedirectPage() {
  const router = useRouter();

  useEffect(() => {
    router.replace("/teams?tab=requests");
  }, [router]);

  return (
    <main className="page-shell" style={{ paddingTop: 48, paddingBottom: 48 }}>
      <section className="surface" style={{ padding: 24, textAlign: "center" }} aria-live="polite">
        <p style={{ margin: "0 0 12px" }}>团队邀请已合并到「团队 · 邀请与申请」。</p>
        <Link className="primary-button" href="/teams?tab=requests">前往邀请与申请</Link>
      </section>
    </main>
  );
}
