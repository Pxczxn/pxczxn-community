import type { Metadata } from "next";
import type { ReactNode } from "react";

export const metadata: Metadata = {
  title: "团队邀请",
  description: "处理收到的团队成员邀请",
};

export default function TeamInvitationsLayout({ children }: Readonly<{ children: ReactNode }>) {
  return children;
}
