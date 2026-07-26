import type { Metadata } from "next";
import { SocialCenterPage } from "./social-center-page";

export const metadata: Metadata = {
  title: "收藏与社交",
  description: "管理收藏、喜欢、关注、粉丝与互关关系。",
};

export default function FavoritesPage() {
  return <SocialCenterPage />;
}
