"use client";

import { createContext, useContext } from "react";
import type { TeamWorkspace } from "../../../lib/community-api";

interface WorkspaceContextValue {
  teamSlug: string;
  teamId: string | null;
  workspace: TeamWorkspace | null;
}

const WorkspaceContext = createContext<WorkspaceContextValue>({
  teamSlug: "",
  teamId: null,
  workspace: null,
});

export const WorkspaceContextProvider = WorkspaceContext.Provider;

export function useWorkspace(): WorkspaceContextValue {
  return useContext(WorkspaceContext);
}
