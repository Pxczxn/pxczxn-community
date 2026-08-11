"use client";

import type { ReactNode } from "react";
import { Empty, Result, Spin } from "@/components/ui/community-ui";
import { AlertCircle } from "lucide-react";

export function TeamLoading({ label }: { label: string }) {
  return (
    <div className="team-ui-loading" aria-live="polite" aria-label={label}>
      <Spin size="large" tip={label} />
    </div>
  );
}

export function TeamError({
  title,
  description,
  extra,
}: {
  title: string;
  description: ReactNode;
  extra?: ReactNode;
}) {
  return (
    <Result
      className="team-ui-result"
      status="error"
      icon={<AlertCircle aria-hidden="true" />}
      title={title}
      subTitle={description}
      extra={extra}
    />
  );
}

export function TeamEmpty({
  description,
  action,
}: {
  description: string;
  action?: ReactNode;
}) {
  return <Empty className="team-ui-empty" description={description}>{action}</Empty>;
}
