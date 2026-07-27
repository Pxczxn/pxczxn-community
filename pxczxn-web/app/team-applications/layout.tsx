import type { Metadata } from "next";
import type { ReactNode } from "react";

export const metadata: Metadata = {
  title: "团队申请 - pxczxn Community",
  description: "提交团队博客创建申请，平台审核通过后将自动创建团队资源",
};

export default function TeamApplicationsLayout({
  children,
}: Readonly<{ children: ReactNode }>) {
  return children;
}
