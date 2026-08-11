/**
 * Discover Pilot Page Route
 * 
 * Routes to the Stitch v2.1_15 pilot implementation of the Discover page.
 * Accessible at /discover/pilot
 */

import type { Metadata } from "next";
import PilotPage from "./PilotPage";

export const metadata: Metadata = {
  title: "发现 - 设计系统试点",
  description: "Stitch v2.1_15 设计系统试点页面，展示新的设计语言和布局。",
};

export default function DiscoverPilotRoute() {
  return <PilotPage />;
}
