package top.pxczxn.community.web.file;

import top.pxczxn.community.file.application.CommunityFileInfo;

public record CommunityFileResponse(
        String fileId,
        String originalName,
        String mimeType,
        long sizeBytes,
        String sha256,
        String status,
        String contentUrl
) {
    static CommunityFileResponse from(CommunityFileInfo file) {
        return new CommunityFileResponse(
                file.fileId().toString(),
                file.originalName(),
                file.mimeType(),
                file.sizeBytes(),
                file.sha256(),
                file.status(),
                "/api/v1/files/" + file.fileId() + "/content"
        );
    }
}
