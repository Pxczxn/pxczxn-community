package top.pxczxn.community.file.application;

public record UploadCommunityFileCommand(
        String originalName,
        String claimedMimeType,
        byte[] content
) {
}
