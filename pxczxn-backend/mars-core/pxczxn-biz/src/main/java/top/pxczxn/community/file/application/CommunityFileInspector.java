package top.pxczxn.community.file.application;

import com.mars.common.exception.BusinessException;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class CommunityFileInspector {

    private static final Pattern EVENT_HANDLER =
            Pattern.compile("\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXTERNAL_HREF =
            Pattern.compile("(?:href|xlink:href)\\s*=\\s*[\"'](?!#)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md");

    private CommunityFileInspector() {
    }

    static InspectedFile inspect(String originalName, byte[] source) {
        if (source == null || source.length == 0) {
            throw new BusinessException(400, "文件内容不能为空");
        }
        String extension = extension(originalName);
        if (isJpeg(source) && Set.of("jpg", "jpeg").contains(extension)) {
            return new InspectedFile(source, "image/jpeg", "jpg");
        }
        if (isPng(source) && "png".equals(extension)) {
            return new InspectedFile(source, "image/png", "png");
        }
        if (isGif(source) && "gif".equals(extension)) {
            return new InspectedFile(source, "image/gif", "gif");
        }
        if (isWebp(source) && "webp".equals(extension)) {
            return new InspectedFile(source, "image/webp", "webp");
        }
        if (isPdf(source) && "pdf".equals(extension)) {
            return new InspectedFile(source, "application/pdf", "pdf");
        }
        if ("docx".equals(extension) && isDocx(source)) {
            return new InspectedFile(
                    source,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "docx"
            );
        }
        if ("svg".equals(extension)) {
            return inspectSvg(source);
        }
        if (TEXT_EXTENSIONS.contains(extension)) {
            requireUtf8Text(source);
            return new InspectedFile(
                    source,
                    "md".equals(extension) ? "text/markdown" : "text/plain",
                    extension
            );
        }
        throw new BusinessException(400, "文件真实类型不在允许列表中");
    }

    private static InspectedFile inspectSvg(byte[] source) {
        String svg = requireUtf8Text(source).trim();
        String lower = svg.toLowerCase(Locale.ROOT);
        boolean svgDocument = lower.startsWith("<svg")
                || (lower.startsWith("<?xml") && lower.contains("<svg"));
        if (!svgDocument
                || lower.contains("<!doctype")
                || lower.contains("<!entity")
                || lower.contains("<script")
                || lower.contains("<style")
                || lower.contains("<foreignobject")
                || lower.contains("<iframe")
                || lower.contains("<object")
                || lower.contains("<embed")
                || lower.contains("javascript:")
                || lower.contains("data:text/html")
                || lower.contains("style=")
                || EVENT_HANDLER.matcher(svg).find()
                || EXTERNAL_HREF.matcher(svg).find()) {
            throw new BusinessException(400, "SVG 包含不安全内容");
        }
        return new InspectedFile(
                svg.getBytes(StandardCharsets.UTF_8),
                "image/svg+xml",
                "svg"
        );
    }

    private static String requireUtf8Text(byte[] source) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(source))
                    .toString();
            if (text.indexOf('\0') >= 0) {
                throw new BusinessException(400, "文本文件包含非法空字节");
            }
            return text;
        } catch (CharacterCodingException exception) {
            throw new BusinessException(400, "文本文件必须使用 UTF-8 编码");
        }
    }

    private static boolean isDocx(byte[] source) {
        if (source.length < 4 || source[0] != 'P' || source[1] != 'K') {
            return false;
        }
        boolean hasContentTypes = false;
        boolean hasWordDocument = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(source))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                hasContentTypes |= "[Content_Types].xml".equals(name);
                hasWordDocument |= "word/document.xml".equals(name);
                if (hasContentTypes && hasWordDocument) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private static String extension(String originalName) {
        if (originalName == null) {
            return "";
        }
        String normalized = originalName.replace('\\', '/');
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1);
        int dot = baseName.lastIndexOf('.');
        return dot < 0 ? "" : baseName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && unsigned(bytes[0]) == 0xff
                && unsigned(bytes[1]) == 0xd8
                && unsigned(bytes[2]) == 0xff;
    }

    private static boolean isPng(byte[] bytes) {
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
        return startsWith(bytes, signature);
    }

    private static boolean isGif(byte[] bytes) {
        return startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && startsWith(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII))
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }

    private static boolean isPdf(byte[] bytes) {
        return startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean startsWith(byte[] source, byte[] signature) {
        if (source.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (source[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private static int unsigned(byte value) {
        return value & 0xff;
    }

    record InspectedFile(byte[] content, String mimeType, String extension) {
    }
}
