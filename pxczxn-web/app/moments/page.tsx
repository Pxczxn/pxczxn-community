import type { Metadata } from "next";
import { MomentsCommunityPage } from "./moments-community-page";

export const metadata: Metadata = {
  title: "动态广场",
};

export default function MomentsIndexPage() {
  return <MomentsCommunityPage />;
}
