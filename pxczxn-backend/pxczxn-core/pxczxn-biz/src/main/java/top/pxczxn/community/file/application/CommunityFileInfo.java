package top.pxczxn.community.file.application;

public record CommunityFileInfo(
        Long fileId,
        String originalName,
        String mimeType,
        long sizeBytes,
        String sha256,
        String status,
        Long ownerUserId
) {
}
