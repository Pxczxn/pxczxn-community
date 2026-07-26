import type { Metadata } from "next";
import { UserTopbar } from "../components/prototype-ui";
import { SettingsPanel } from "./settings-panel";

export const metadata: Metadata = {
  title: "账号与主题设置",
};

export default function SettingsPage() {
  return (
    <>
      <UserTopbar title="设置中心" />
      <SettingsPanel />
    </>
  );
}
