import type { Metadata } from "next";
import { CommunityPlaceholderPage } from "../components/community-placeholder-page";

export const metadata: Metadata = { title: "聊天" };
export default function ChatPage() { return <CommunityPlaceholderPage title="即时聊天" phase="社区用户端待接入" description="平台人员即时聊天已具备真实服务能力；社区用户聊天界面会在权限与治理规则统一后开放。" />; }
