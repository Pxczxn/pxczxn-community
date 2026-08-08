import { COMMUNITY_API_BASE_URL } from "./client";

export function publicFileUrl(fileId?: string | null) {
  return fileId && COMMUNITY_API_BASE_URL
    ? `${COMMUNITY_API_BASE_URL}/api/v1/public/files/${encodeURIComponent(fileId)}/content`
    : null;
}

export interface CommunityFile {
  fileId: string;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  contentUrl: string;
}