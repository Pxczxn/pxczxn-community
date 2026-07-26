import type { Metadata } from "next";
import { NotificationsPage } from "./notifications-page";

export const metadata: Metadata = {
  title: "通知中心",
  description: "查看星语社区互动、关注、评论和审核通知。",
};

export default function NotificationCenterPage() {
  return <NotificationsPage />;
}
