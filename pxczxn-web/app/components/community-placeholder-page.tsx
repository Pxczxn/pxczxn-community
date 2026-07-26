import Link from "next/link";
import { ArrowLeft, Construction } from "lucide-react";
import { UserTopbar } from "./prototype-ui";

export function CommunityPlaceholderPage({
  title,
  description,
  phase,
}: {
  title: string;
  description: string;
  phase: string;
}) {
  return <>
    <UserTopbar title={title} />
    <main className="community-placeholder-page page-shell">
      <section className="surface-lg shadow-sm community-placeholder-card">
        <Construction size={28} />
        <span className="eyebrow">{phase}</span>
        <h1>{title}</h1>
        <p>{description}</p>
        <p className="secondary">此页面已使用正式路由，不展示硬编码的原型内容或虚构业务数据。</p>
        <Link className="secondary-button" href="/discover"><ArrowLeft size={16} /> 返回发现</Link>
      </section>
    </main>
  </>;
}
