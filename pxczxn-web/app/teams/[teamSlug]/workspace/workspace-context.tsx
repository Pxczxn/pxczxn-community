"use client";

import { createContext, useContext } from "react";
import type { TeamWorkspace } from "../../../lib/community-api";

interface WorkspaceContextValue {
  teamSlug: string;
  teamId: string | null;
  workspace: TeamWorkspace | null;
  /** 重新加载当前团队工作台（保留现有界面，数据到达后替换）。 */
  reloadWorkspace: () => void;
}

const WorkspaceContext = createContext<WorkspaceContextValue>({
  teamSlug: "",
  teamId: null,
  workspace: null,
  reloadWorkspace: () => {},
});

export const WorkspaceContextProvider = WorkspaceContext.Provider;

export function useWorkspace(): WorkspaceContextValue {
  return useContext(WorkspaceContext);
}
