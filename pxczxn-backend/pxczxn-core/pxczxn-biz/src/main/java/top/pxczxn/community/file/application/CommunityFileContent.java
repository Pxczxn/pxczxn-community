package top.pxczxn.community.file.application;

public record CommunityFileContent(
        String originalName,
        String mimeType,
        byte[] content
) {
}
